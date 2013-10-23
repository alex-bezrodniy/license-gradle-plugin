package nl.javadude.gradle.plugins.license

import groovy.xml.MarkupBuilder

/**
 * License file reporter.
 * Supported formats: xml
 */
class LicenseReporter {

    /**
     * Directory for reports.
     */
    def outputDir

    /**
     * Generate xml report grouping by dependencies.
     * @param dependencyToLicenseMap map with dependency-license information
     * @param fileName file name for report
     */
    public def generateXMLReport4DependencyToLicense(dependencyToLicenseMap, fileName) {
        def xml = getMarkupBuilder(fileName)
        xml.dependencies() {
            dependencyToLicenseMap.each {
                entry ->
                    dependency(name: "$entry.key") {
                        entry.value.each {
                            l -> license("$l")
                        }
                    }
            }
        }
    }

    /**
     * Generate xml report grouping by licenses.
     * @param dependencyToLicenseMap map with dependency-license information
     * @param fileName file name for report
     */
    public def generateXMLReport4LicenseToDependency(dependencyToLicenseMap, fileName) {
        def xml = getMarkupBuilder(fileName)
        xml.licenses() {
            dependencyToLicenseMap.each {
                entry ->
                    license(name: "$entry.key") {
                        entry.value.each {
                            d -> dependency(d)
                        }
                    }
            }
        }
    }

    private def getMarkupBuilder(String fileName) {
        def licenseReport = new File(outputDir, fileName)
        licenseReport.createNewFile()
        def writer = new FileWriter(licenseReport)
        new MarkupBuilder(writer)
    }

}
