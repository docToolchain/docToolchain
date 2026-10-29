package org.docToolchain.scripts

import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Regression tests for issue #1701: icon paths in src/site/assets/manifest.json
 * must resolve relative to the manifest URL, so the site works both at the
 * domain root and under a sub path (e.g. /docToolchain/v4.0.x/).
 */
class ManifestIconPathsSpec extends Specification {

    // Same repository-root resolution pattern used by CopyLandingPageSpec:
    // try a few candidate working directories and pick the one that actually
    // contains the expected file, so the test does not depend on a single cwd.
    private static final File REPO_ROOT = ['.', '..', '../..']
            .collect { new File(it).canonicalFile }
            .find { new File(it, 'src/site/assets/manifest.json').exists() }

    @Shared File manifestFile
    @Shared def manifest

    def setupSpec() {
        assert REPO_ROOT != null : 'could not locate repository root (src/site/assets/manifest.json)'
        manifestFile = new File(REPO_ROOT, 'src/site/assets/manifest.json')
        assert manifestFile.exists() : 'could not locate src/site/assets/manifest.json'
        manifest = new JsonSlurper().parse(manifestFile)
    }

    def "manifest is valid JSON with a non-empty icons list"() {
        expect:
        manifest instanceof Map
        manifest.icons instanceof List
        !manifest.icons.isEmpty()
    }

    def "every icon src resolves inside the manifest directory for root and subpath publishing"() {
        given:
        def iconSrcs = manifest.icons.collect { it.src.toString() }
        def rootManifestUrl = new URI('https://example.test/manifest.json')
        def subManifestUrl = new URI('https://example.test/docToolchain/v4.0.x/manifest.json')
        Path rootBasePath = Paths.get(rootManifestUrl.resolve('./').path).normalize()
        Path subBasePath = Paths.get(subManifestUrl.resolve('./').path).normalize()

        expect:
        iconSrcs.every { src ->
            // Absolute paths (leading '/') resolve against the domain root and
            // therefore escape any sub path: never acceptable.
            if (src.startsWith('/')) {
                return false
            }
            URI rootResolved = rootManifestUrl.resolve(src)
            URI subResolved = subManifestUrl.resolve(src)
            Path rootResolvedPath = Paths.get(rootResolved.path).normalize()
            Path subResolvedPath = Paths.get(subResolved.path).normalize()
            // Path.startsWith compares path segments (not a string prefix), so
            // it also rejects escapes via '..' or any other traversal.
            rootResolvedPath.startsWith(rootBasePath) &&
                subResolvedPath.startsWith(subBasePath)
        }
    }

    def "every icon references an existing asset file"() {
        given:
        Path assetsBase = Paths.get(REPO_ROOT.absolutePath, 'src', 'site', 'assets').normalize()

        expect:
        manifest.icons.every { icon ->
            String src = icon.src.toString()
            String relativePath = new URI(src).path ?: ''
            // Reject any traversal segment, even if a file with the same final
            // name happens to exist inside assets/.
            if (relativePath.split('/').any { it == '..' }) {
                return false
            }
            Path resolved = assetsBase.resolve(relativePath).normalize()
            resolved.startsWith(assetsBase) && resolved.toFile().exists()
        }
    }
}
