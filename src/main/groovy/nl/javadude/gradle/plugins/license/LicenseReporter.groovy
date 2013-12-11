package nl.javadude.gradle.plugins.license

import com.google.common.collect.HashMultimap
import groovy.xml.MarkupBuilder

import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

import static com.google.common.base.Strings.isNullOrEmpty

/**
 * License file reporter.
 */
class LicenseReporter {

    /**
     * Output directory for html reports.
     */
    File htmlOutputDir

    /**
     * Output directory for xml reports.
     */
    File xmlOutputDir


    public String generateXMLAsString4DependencyToLicense(Set<DependencyMetadata> dependencyMetadataSet) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.dependencies() {
            dependencyMetadataSet.each {
                entry ->
                    dependency(name: entry.dependency) {
                        file(entry.dependencyFileName)
                        entry.licenseMetadataList.each {
                            l ->
                                def attributes = [name: l.licenseName]

                                // Miss attribute if it's empty
                                if (!isNullOrEmpty(l.licenseTextUrl)) {
                                    attributes << [url: l.licenseTextUrl]
                                }

                                license(attributes)
                        }
                    }
            }
        }

        writer.toString()
    }

    public String generateXMLAsString4LicenseToDependency(Set<DependencyMetadata> dependencyMetadataSet) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        HashMultimap<LicenseMetadata, String> licensesMap = getLicenseMap(dependencyMetadataSet)

        xml.licenses() {
            licensesMap.asMap().each {
                entry ->
                    def attributes = [name: entry.key.licenseName]

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

        writer.toString()
    }

    /**
     * Generate xml report grouping by licenses.
     *
     * @param dependencyMetadataSet set with dependencies
     * @param fileName report file name
     */
    public void saveXmlReportToFile(String xmlReportText, String fileName) {
        File licenseReport = new File(xmlOutputDir, fileName)
        licenseReport.createNewFile()
        BufferedWriter writer = new BufferedWriter(new FileWriter(licenseReport))
        writer.write(xmlReportText)
        writer.flush()
        writer.close()
    }

    /**
     * Generate report by dependency.
     *
     * @param dependencyMetadataSet set with dependencies
     * @param fileName report file name
     */
    public void generateHTMLReport4DependencyToLicense(String xmlReport, String fileName, String xslt) {
        File output = new File(htmlOutputDir, fileName)
        output.createNewFile()
        def factory = TransformerFactory.newInstance()
        def transformer = factory.newTransformer(new StreamSource(new StringReader(xslt)))
        transformer.transform(new StreamSource(new StringReader(xmlReport)), new StreamResult(output))
    }

    /**
     * Generate html report by license type.
     *
     * @param dependencyMetadataSet set with dependencies
     * @param fileName report file name
     */
    public void generateHTMLReport4LicenseToDependency(String xmlReport, String fileName, String xslt) {
        File output = new File(htmlOutputDir, fileName)
        output.createNewFile()
        def factory = TransformerFactory.newInstance()
        def transformer = factory.newTransformer(new StreamSource(new StringReader(xslt)))
        transformer.transform(new StreamSource(new StringReader(xmlReport)), new StreamResult(output))
    }

    // Utility
    private HashMultimap<LicenseMetadata, String> getLicenseMap(Set<DependencyMetadata> dependencyMetadataSet) {
        HashMultimap<LicenseMetadata, String> licensesMap = HashMultimap.create()

        dependencyMetadataSet.each {
            dependencyMetadata ->
                dependencyMetadata.licenseMetadataList.each {
                    license -> licensesMap.put(license, dependencyMetadata.dependencyFileName)
                }
        }

        licensesMap
    }

}
