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
  local t_script t_render t_jruby t_rendered t_end
  t_script="$(ts_of 'docToolchain v4' "$log")"
  t_render="$(ts_of 'Rendering microsite' "$log")"
  t_jruby="$(ts_of 'WARNING' "$log")"
  t_rendered="$(ts_of 'MicrositeBaker: rendered' "$log")"
  t_end="$(ts_of 'Microsite generated' "$log")"
  echo "  $(basename "$log"):"
  printf '    %-38s %ss\n' 'dtcw + JVM + classpath/config boot' "$(delta 0 "$t_script")"
  printf '    %-38s %ss\n' 'copy theme/docs + fix headers'      "$(delta "$t_script" "$t_render")"
  # The AsciidoctorJ bake (lib load + page render) is the big block. Split it
  # into JRuby-load vs render only if the JRuby WARNING marker is present;
  # otherwise show the bake as one number so the time is never lost.
  if [[ -n "$t_jruby" ]]; then
    printf '    %-38s %ss  <- LIBS\n' 'AsciidoctorJ/JRuby native load' "$(delta "$t_render" "$t_jruby")"
    printf '    %-38s %ss\n'          'render pages'                    "$(delta "$t_jruby" "$t_rendered")"
  else
    printf '    %-38s %ss  <- LIBS+RENDER (no JRuby marker)\n' \
           'AsciidoctorJ bake (load + render)' "$(delta "$t_render" "$t_rendered")"
  fi
  printf '    %-38s %ss\n' 'copy images + finish'              "$(delta "$t_rendered" "$t_end")"
}

run_tool v3 "${V3_CMD[@]}"
run_tool v4 "${V4_CMD[@]}"

echo "=== v4 phase breakdown ==="
echo "(run 1 is cold; compare it against the warm runs to isolate gem-unpack)"
for ((i=1; i<=ITERATIONS; i++)); do
  phase_report "$LOGDIR/v4_$i.log"
done
echo
echo "Full logs kept in: $LOGDIR"
