#!/usr/bin/env bash
#
# benchmark-generateSite.sh — compare docToolchain v3 vs v4 `generateSite`
# fairly and break the v4 run into phases, so you can see how much time goes
# into loading libraries (JVM + AsciidoctorJ/JRuby boot) versus rendering.
#
# Why this exists: `time ./dtcw ...` lies. v3 offloads the work to a forked
# Gradle daemon whose CPU `time` never sees, and Gradle's up-to-date cache
# skips the render entirely. So we compare wall-clock `real` only, and force a
# full render with `rm -rf build` before every single run.
#
# How the phase split works: every output line gets an elapsed-seconds stamp.
# AsciidoctorJ's JRuby prints its `WARNING:` lines exactly when it loads its
# native libs, so the gap between "Rendering microsite..." and the first
# WARNING is the library-load cost you suspect.
#
# Usage:
#   ./benchmark-generateSite.sh
#   ITERATIONS=5 ./benchmark-generateSite.sh
#   V3_CMD="./dtcw generateSite" V4_CMD="./dtcw4 local generateSite" ./benchmark-generateSite.sh
#
set -uo pipefail

ITERATIONS="${ITERATIONS:-3}"
BUILD_DIR="${BUILD_DIR:-build}"
read -ra V3_CMD <<< "${V3_CMD:-./dtcw generateSite}"
read -ra V4_CMD <<< "${V4_CMD:-./dtcw4 local generateSite}"

LOGDIR="$(mktemp -d -t dtc-bench-XXXXXX)"
echo "Logs:        $LOGDIR"
echo "Iterations:  $ITERATIONS per tool"
echo "Build dir:   $BUILD_DIR (wiped before every run)"
echo

have_perl=0
command -v perl >/dev/null 2>&1 && have_perl=1
if [[ "$have_perl" -eq 0 ]]; then
  echo "NOTE: perl not found — only total 'real' times, no phase breakdown." >&2
  echo
fi

# Prepend an elapsed-seconds stamp to every line (relative to process start).
stamp() {
  if [[ "$have_perl" -eq 1 ]]; then
    perl -MTime::HiRes=time -ne 'BEGIN{$s=time} printf "[%8.3f] %s", time-$s, $_'
  else
    cat
  fi
}

# First stamp of the line containing $1 in file $2 (empty if not found).
ts_of() {
  grep -m1 -F -- "$1" "$2" 2>/dev/null | sed -E 's/^\[ *([0-9.]+)\].*/\1/'
}

# Seconds delta a..b with 2 decimals; '   -  ' if either bound is missing.
delta() {
  if [[ -z "${1:-}" || -z "${2:-}" ]]; then printf '   -  '; return; fi
  awk -v a="$1" -v b="$2" 'BEGIN{ printf "%6.2f", b-a }'
}

run_tool() {
  local name="$1"; shift
  local -a cmd=("$@")
  echo "=== $name : ${cmd[*]} ==="
  local i total=0
  for ((i=1; i<=ITERATIONS; i++)); do
    local log="$LOGDIR/${name}_$i.log"
    rm -rf "$BUILD_DIR"
    local start end wall
    start="$(date +%s.%N)"
    "${cmd[@]}" 2>&1 | stamp > "$log"
    end="$(date +%s.%N)"
    wall="$(awk -v a="$start" -v b="$end" 'BEGIN{printf "%.2f", b-a}')"
    total="$(awk -v t="$total" -v w="$wall" 'BEGIN{printf "%.2f", t+w}')"
    local note=""
    [[ "$i" -eq 1 ]] && note="   (cold: includes gem/cache unpack)"
    printf '  run %d: real %7ss%s\n' "$i" "$wall" "$note"
    # If Gradle reported its own build time, surface it (v3).
    local gline
    gline="$(grep -m1 'BUILD SUCCESSFUL in' "$log" 2>/dev/null | sed -E 's/^\[[^]]*\] *//')"
    [[ -n "$gline" ]] && printf '         gradle: %s\n' "$gline"
  done
  awk -v t="$total" -v n="$ITERATIONS" 'BEGIN{printf "  avg  : real %7.2fs\n", t/n}'
  echo
}

# Phase breakdown for a v4 log.
phase_report() {
  local log="$1"
  [[ -f "$log" ]] || { echo "  (no log: $log)"; return; }
  local t_script t_render t_ready t_rendered t_end t_load
  t_script="$(ts_of 'docToolchain v4' "$log")"
  t_render="$(ts_of 'Rendering microsite' "$log")"
  t_ready="$(ts_of 'BENCH-MARKER: engine ready' "$log")"
  t_rendered="$(ts_of 'MicrositeBaker: rendered' "$log")"
  t_end="$(ts_of 'Microsite generated' "$log")"
  # Prefer the injected marker (precise); fall back to the JRuby WARNING line.
  t_load="$t_ready"
  [[ -z "$t_load" ]] && t_load="$(ts_of 'WARNING' "$log")"
  echo "  $(basename "$log"):"
  printf '    %-38s %ss\n' 'dtcw + JVM + classpath/config boot' "$(delta 0 "$t_script")"
  printf '    %-38s %ss\n' 'copy theme/docs + fix headers'      "$(delta "$t_script" "$t_render")"
  # The AsciidoctorJ bake (lib load + page render) is the big block. Split it
  # into load vs render when a marker is available; otherwise show the bake as
  # one number so the time is never lost.
  if [[ -n "$t_load" ]]; then
    printf '    %-38s %ss  <- LIBS\n' 'AsciidoctorJ/JRuby load'  "$(delta "$t_render" "$t_load")"
    printf '    %-38s %ss\n'          'render pages + diagrams'  "$(delta "$t_load" "$t_rendered")"
  else
    printf '    %-38s %ss  <- LIBS+RENDER (no marker)\n' \
           'AsciidoctorJ bake (load + render)' "$(delta "$t_render" "$t_rendered")"
  fi
  printf '    %-38s %ss\n' 'copy images + finish'              "$(delta "$t_rendered" "$t_end")"
}

# --- Temporary monkey-patch -------------------------------------------------
# Inject a phase marker into the *installed* v4 MicrositeBaker so we can split
# "library load" from "render" without changing the product. A println right
# before crawl() marks the moment AsciidoctorJ + asciidoctor-diagram are fully
# loaded. The patch is reverted on exit and on interrupt (trap), and self-heals
# if a previous run was killed mid-patch.
MB_PATCHED=""

find_mb() {
  if [[ -n "${MB_FILE:-}" ]]; then printf '%s\n' "$MB_FILE"; return; fi
  # Bash expands the glob in sorted order, so the last existing match is the
  # highest version. (find/ls avoided to keep shellcheck SC2012 happy.)
  local f latest=""
  for f in "$HOME"/.doctoolchain/docToolchain-*/scripts/lib/MicrositeBaker.groovy; do
    [[ -e "$f" ]] && latest="$f"
  done
  [[ -n "$latest" ]] && printf '%s\n' "$latest"
}

patch_v4() {
  local f; f="$(find_mb)"
  if [[ -z "$f" || ! -f "$f" ]]; then
    echo "NOTE: MicrositeBaker.groovy not found — phase split falls back to the" >&2
    echo "      JRuby WARNING marker. Set MB_FILE=/path/to/MicrositeBaker.groovy." >&2
    return
  fi
  # Recover a clean base if a prior run left the file patched.
  if grep -q 'BENCH-MARKER: engine ready' "$f" && [[ -f "$f.bench-bak" ]]; then
    mv -f "$f.bench-bak" "$f"
  fi
  if ! grep -qE '^[[:space:]]*crawl\(\)[[:space:]]*$' "$f"; then
    echo "NOTE: crawl() anchor not found in $f — skipping marker patch." >&2
    return
  fi
  cp "$f" "$f.bench-bak"
  sed -i '/^[[:space:]]*crawl()[[:space:]]*$/i\println "BENCH-MARKER: engine ready"' "$f"
  MB_PATCHED="$f"
  echo "Patched (temporary): $f"
}

unpatch_v4() {
  if [[ -n "$MB_PATCHED" && -f "$MB_PATCHED.bench-bak" ]]; then
    mv -f "$MB_PATCHED.bench-bak" "$MB_PATCHED"
    echo "Reverted patch:      $MB_PATCHED"
    MB_PATCHED=""
  fi
}
trap unpatch_v4 EXIT INT TERM
# ---------------------------------------------------------------------------

run_tool v3 "${V3_CMD[@]}"

patch_v4
run_tool v4 "${V4_CMD[@]}"
unpatch_v4

echo "=== v4 phase breakdown ==="
echo "(run 1 is cold; compare it against the warm runs to isolate gem-unpack)"
for ((i=1; i<=ITERATIONS; i++)); do
  phase_report "$LOGDIR/v4_$i.log"
done
echo
echo "Full logs kept in: $LOGDIR"
