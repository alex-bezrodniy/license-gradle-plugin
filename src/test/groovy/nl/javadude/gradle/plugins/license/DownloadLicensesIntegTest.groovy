package nl.javadude.gradle.plugins.license

import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification

import static nl.javadude.gradle.plugins.license.DownloadLicensesExtension.license

/**
 * Integration test for {@link DownloadLicenses}.
 */
class DownloadLicensesIntegTest extends Specification {

    static final String LICENSE_REPORT = "licenseReport"

    @Shared def downloadLicenses
    @Shared Project project
    @Shared Project subproject
    @Shared File projectDir = new File("rootPrj")
    @Shared File subProjectDir = new File(projectDir, "subproject1")
    @Shared File outputDir = new File(LICENSE_REPORT)
    @Shared AntBuilder ant = new AntBuilder()

    def setup() {
        configureProjects()

        project.apply plugin: 'java'
        project.apply plugin: 'license'

        project.repositories {
            mavenCentral()
        }

        subproject.apply plugin: 'java'
        subproject.apply plugin: 'license'

        subproject.repositories {
            mavenCentral()
        }

        configurePlugin()
    }

    def cleanup() {
        ant.delete(dir: outputDir)
        ant.delete(dir: subProjectDir)
        ant.delete(dir: projectDir)
    }

    def "Test that report generating in multi module build includes transitive project dependencies"() {
        setup:
        subproject.dependencies {
            compile "org.jboss.logging:jboss-logging:3.1.3.GA"
            compile "com.google.guava:guava:15.0"
        }
        project.dependencies {
            compile project.project(":subproject1")
        }
        downloadLicenses.customLicensesMapping = [
                "com.google.guava:guava:15.0": license("MY_LICENSE", "MY_URL"),
                "org.jboss.logging:jboss-logging:3.1.3.GA": license("MY_LICENSE", "MY_URL")
        ]

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 2
        licensesInReport(xmlByLicense) == 1

        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "com.google.guava:guava:15.0", "MY_LICENSE")
        dependencyWithLicenseUrlPresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "com.google.guava:guava:15.0", "MY_URL")

        xmlByLicense.license.@name.text() == "MY_LICENSE"
        xmlByLicense.license.@url.text() == "MY_URL"
    }

    def "Test that default configuration is runtime"() {
        setup:
        project.dependencies {
            testCompile project.files("testDependency.jar")
            testRuntime "org.jboss.logging:jboss-logging:3.1.3.GA"
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 0
        licensesInReport(xmlByLicense) == 0
    }

    def "Test that aliases works well for different dependencies with the same license"() {
        setup:
        File dependencyJar1 = new File(projectDir, "testDependency1.jar")
        dependencyJar1.createNewFile()

        File dependencyJar2 = new File(projectDir, "testDependency2.jar")
        dependencyJar2.createNewFile()

        File dependencyJar3 = new File(projectDir, "testDependency3.jar")
        dependencyJar3.createNewFile()

        downloadLicenses.aliases = ["Apache 2": license("The Apache Software License, Version 2.0"),
                "The Apache 2": license("The Apache Software License, Version 2.0"),
                "Apache": license("The Apache Software License, Version 2.0")]

        downloadLicenses.customLicensesMapping = ["testDependency1.jar": license("Apache 2"),
                "testDependency2.jar": license("The Apache 2"),
                "testDependency3.jar": license("Apache")]

        project.dependencies {
            runtime project.files("testDependency1.jar")
            runtime project.files("testDependency2.jar")
            runtime project.files("testDependency3.jar")
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 3
        licensesInReport(xmlByLicense) == 1

        xmlByLicense.license.@name.text() == "The Apache Software License, Version 2.0"
        xmlByLicense.license.dependency.size() == 3

        dependencyWithLicensePresent(xmlByDependency, "testDependency1.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency2.jar", "The Apache Software License, Version 2.0")
        dependencyWithLicensePresent(xmlByDependency, "testDependency3.jar", "The Apache Software License, Version 2.0")
    }

    def "Test that we can specify license that will override existent license for dependency"() {
        setup:
        project.dependencies {
            compile "org.jboss.logging:jboss-logging:3.1.3.GA"
            compile "com.google.guava:guava:15.0"
        }
        downloadLicenses.customLicensesMapping = [
                "com.google.guava:guava:15.0": license("MY_LICENSE", "MY_URL"),
                "org.jboss.logging:jboss-logging:3.1.3.GA": license("MY_LICENSE", "MY_URL")
        ]

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 2
        licensesInReport(xmlByLicense) == 1

        dependencyWithLicensePresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "MY_LICENSE")
        dependencyWithLicensePresent(xmlByDependency, "com.google.guava:guava:15.0", "MY_LICENSE")
        dependencyWithLicenseUrlPresent(xmlByDependency, "org.jboss.logging:jboss-logging:3.1.3.GA", "MY_URL")
        dependencyWithLicenseUrlPresent(xmlByDependency, "com.google.guava:guava:15.0", "MY_URL")

        xmlByLicense.license.@name.text() == "MY_LICENSE"
        xmlByLicense.license.@url.text() == "MY_URL"
    }

    def "Test that file dependencies has no license by default"() {
        setup:
        File dependencyJar1 = new File(projectDir, "nolicense.jar")
        dependencyJar1.createNewFile()
        project.dependencies {
            runtime project.files("nolicense.jar")
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 1
        licensesInReport(xmlByLicense) == 1

        dependencyWithLicensePresent(xmlByDependency, "nolicense.jar", "No license found")
    }

    def "Test that dependency can have several licenses"() {
        setup:
        project.dependencies {
            compile 'org.codehaus.jackson:jackson-jaxrs:1.9.13'
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        xmlByDependency.dependency.find {
            it.@name.text() == 'org.codehaus.jackson:jackson-jaxrs:1.9.13'
        }.license.size() == 2

        xmlByLicense.license.dependency.findAll {
              it.text() == 'org.codehaus.jackson:jackson-jaxrs:1.9.13'
        }.size() == 2

    }

    def "Test that if dependency has no license url it will be omitted in the report"() {
        setup:
        project.dependencies {
            compile project.files("testDependency.jar")
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 1
        licensesInReport(xmlByLicense) == 1

        !xmlByDependency.dependency.license.find { it['@url'] == "testDependency.jar" }.asBoolean()
    }

    def "Test that plugin works if no dependencies defined in the project"() {
        setup:
        project.dependencies {
        }

        when:
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        assertLicenseReportsExist(f)

        def xmlByDependency = xml4LicenseByDependencyReport(f)
        def xmlByLicense = xml4DependencyByLicenseReport(f)

        dependenciesInReport(xmlByDependency) == 0
        licensesInReport(xmlByLicense) == 0
    }

    def "Test that plugin generates 4 reports"() {
        when:
        project.dependencies {
            compile 'com.google.guava:guava:14.0'
        }
        downloadLicenses.reportByDependency = true
        downloadLicenses.reportByLicenseType = true
        downloadLicenses.xml = true
        downloadLicenses.html = true
        downloadLicenses.execute()

        then:
        File f = getLicenseReportFolder()
        f.exists()
        f.listFiles().length == 4
    }

    def "Test that plugin generate no reports when all report types are disabled"() {
        setup:
        downloadLicenses.reportByDependency = false
        downloadLicenses.reportByLicenseType = false
        downloadLicenses.xml = true
        downloadLicenses.html = true
        project.dependencies {
            compile 'com.google.guava:guava:15.0'
        }

        when:
        downloadLicenses.execute()

        then:
        File f = new File(LICENSE_REPORT)
        f.exists()
        f.listFiles().length == 0
    }

    def "Test that plugin generate no reports when all report formats are disabled"() {
        setup:
        downloadLicenses.reportByDependency = true
        downloadLicenses.reportByLicenseType = true
        downloadLicenses.xml = false
        downloadLicenses.html = false
        project.dependencies {
            compile 'com.google.guava:guava:15.0'
        }

        when:
        downloadLicenses.execute()

        then:
        File f = new File(LICENSE_REPORT)
        f.exists()
        f.listFiles().length == 0
    }

    def "Test that plugin generate no reports when it is fully disabled"() {
        setup:
        downloadLicenses.enabled = false
        project.dependencies {
            compile 'com.google.guava:guava:15.0'
        }

        when:
        downloadLicenses.execute()

        then:
        File f = new File(LICENSE_REPORT)
        !f.exists()
    }

    def "Test correctness of defaults"() {
        expect:
        downloadLicenses.reportByLicenseType
        downloadLicenses.reportByDependency
        downloadLicenses.enabled
    }

    def configureProjects() {
        projectDir.mkdir()
        subProjectDir.mkdir()

        project = ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()

        subproject = ProjectBuilder.builder()
                .withParent(project)
                .withName("subproject1")
                .withProjectDir(subProjectDir)
                .build()

        File dependencyJar = new File(projectDir, "testDependency.jar")
        dependencyJar.createNewFile()
    }

    def configurePlugin() {
        downloadLicenses = project.tasks.downloadLicenses
        downloadLicenses.xml = true
        downloadLicenses.html = false
        downloadLicenses.customLicensesMapping = ["testDependency.jar": license("Apache 2")]
        downloadLicenses.outputDir = outputDir
    }

    def xml4DependencyByLicenseReport(File reportDir) {
        File reportByLicense = new File(reportDir, LicensePlugin.DEFAULT_FILE_NAME_FOR_REPORTS_BY_LICENSE + ".xml")
        new XmlSlurper().parse(reportByLicense)
    }

    def xml4LicenseByDependencyReport(File reportDir) {
        File reportByDependency = new File(reportDir, LicensePlugin.DEFAULT_FILE_NAME_FOR_REPORTS_BY_DEPENDENCY + ".xml")
        new XmlSlurper().parse(reportByDependency)
    }

    def getLicenseReportFolder() {
        new File(LICENSE_REPORT)
    }

    def dependencyWithLicensePresent(GPathResult xmlByDependency, String d, String l) {
        xmlByDependency.dependency.find {
            it.@name.text() == d
        }.license.@name == l
    }

    def dependencyWithLicenseUrlPresent(GPathResult xmlByDependency, String d, String lUrl) {
        xmlByDependency.dependency.find {
            it.@name.text() == d
        }.license.@url == lUrl
    }

    def assertLicenseReportsExist(File f) {
        f.exists()
        f.listFiles().length == 2
    }

    def dependenciesInReport(GPathResult xmlByDependency) {
        xmlByDependency.dependency.size()
    }

    def licensesInReport(GPathResult xmlByLicense) {
        xmlByLicense.license.size()
    }
}
