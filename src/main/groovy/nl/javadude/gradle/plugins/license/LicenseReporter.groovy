package nl.javadude.gradle.plugins.license

import com.google.common.collect.HashMultimap
import groovy.xml.MarkupBuilder

import static com.google.common.base.Strings.isNullOrEmpty

/**
 * License file reporter.
 */
class LicenseReporter {

    /**
     * Directory for reports.
     */
    File outputDir

    /**
     * Generate xml report grouping by dependencies.
     *
     * @param dependencyToLicenseMap
     * @param fileName file name for report
     */
    public void generateXMLReport4DependencyToLicense(Set<DependencyMetadata> pomMetadataSet, String fileName) {
        MarkupBuilder xml = getMarkupBuilder(fileName)
        xml.dependencies() {
            pomMetadataSet.each {
                entry ->
                    dependency(name: "$entry.dependency") {
                        entry.licenseMetadataList.each {
                            l ->
                                def attributes = [name: "$l.licenseName"]

                                // Miss attribute if it's empty
                                if (!isNullOrEmpty(l.licenseTextUrl)) {
                                    attributes << [url: l.licenseTextUrl]
                                }

                                license(attributes)
                        }
                    }
            }
        }
    }

    /**
     * Generate xml report grouping by licenses.
     *
     * @param pomMetadataSet
     * @param fileName file name for report
     */
    public void generateXMLReport4LicenseToDependency(Set<DependencyMetadata> pomMetadataSet, String fileName) {
        MarkupBuilder xml = getMarkupBuilder(fileName)
        HashMultimap<LicenseMetadata, String> licensesMap = HashMultimap.create()

        pomMetadataSet.each {
            pom -> pom.licenseMetadataList.each {
                license -> licensesMap.put(license, pom.dependency)
            }
        }

        xml.licenses() {
            licensesMap.asMap().each {
                entry ->
                    def attributes = [name: "$entry.key.licenseName"]

                    // Miss attribute if it's empty
                    if(!isNullOrEmpty(entry.key.licenseTextUrl)) {
                        attributes << [url:  entry.key.licenseTextUrl]
                    }
                    license(attributes) {
                        entry.value.each {
                            d -> dependency(d)
                        }
                    }
            }
        }
    }

    private MarkupBuilder getMarkupBuilder(String fileName) {
        File licenseReport = new File(outputDir, fileName)
        licenseReport.createNewFile()
        def writer = new FileWriter(licenseReport)
        new MarkupBuilder(writer)
    }

}
