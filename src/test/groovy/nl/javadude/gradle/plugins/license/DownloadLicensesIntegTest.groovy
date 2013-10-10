package nl.javadude.gradle.plugins.license

import com.google.common.io.Files
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification

/**
 * Integration test for {@link DownloadLicenses}
 */
class DownloadLicensesIntegTest extends Specification {

    // Tested task
    @Shared def downloadLicenses

    @Shared def project
    @Shared def projectDir
    @Shared def outputDir
    @Shared def ant = new AntBuilder()

    def setup() {
        projectDir = Files.createTempDir()
        project = ProjectBuilder.builder().withProjectDir(projectDir).build()

        project.apply plugin: 'java'
        project.apply plugin: 'license'

        downloadLicenses = project.tasks.downloadLicenses
        downloadLicenses.enabled = true

        outputDir = new File("licenseReport")
        downloadLicenses.outputDir = outputDir
    }

    def cleanup() {
        ant.delete(dir: projectDir)
        ant.delete(dir: outputDir)
    }

    def "Test that plugin works if no dependencies defined in the project"() {
        when:
        project.dependencies {
        }

        then:
        downloadLicenses.execute()
    }

    def "Test that plugin generates reports"() {
        when:
        project.dependencies {
            compile 'com.google.guava:guava:15.0'
        }
        downloadLicenses.reportByDependency = true
        downloadLicenses.reportByLicenseType = true

        downloadLicenses.execute()

        then:
        File f = new File("licenseReport")
        f.exists()
        f.listFiles().length == 2
    }

    def "Test that plugin generate no reports when they are disabled"() {
        when:
        downloadLicenses.reportByDependency = false
        downloadLicenses.reportByLicenseType = false
        project.dependencies {
            compile 'com.google.guava:guava:15.0'
        }
        downloadLicenses.execute()

        then:
        File f = new File("licenseReport")
        f.exists()
        f.listFiles().length == 0
    }

    def "Test that plugin generate no reports when it is fully disabled"() {
        when:
        downloadLicenses.enabled = false
        project.dependencies {
            compile 'com.google.guava:guava:15.0'
        }
        downloadLicenses.execute()

        then:
        File f = new File("licenseReport")
        !f.exists()
    }

    def "Test correctness of defaults" () {
        expect:
        downloadLicenses.reportByLicenseType == false
        downloadLicenses.reportByDependency == true
        downloadLicenses.format == "xml"
        downloadLicenses.enabled == true
    }

}
