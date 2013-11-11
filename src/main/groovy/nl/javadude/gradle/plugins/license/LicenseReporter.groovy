package nl.javadude.gradle.plugins.license

import com.google.common.collect.HashMultimap
import groovy.xml.MarkupBuilder

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

    /**
     * Generate xml report grouping by dependencies.
     *
     * @param dependencyMetadataSet set with dependencies
     * @param fileName report file name
     */
    public void generateXMLReport4DependencyToLicense(Set<DependencyMetadata> dependencyMetadataSet, String fileName) {
        MarkupBuilder xml = getMarkupBuilder(fileName, xmlOutputDir)
        xml.dependencies() {
            dependencyMetadataSet.each {
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
     * @param dependencyMetadataSet set with dependencies
     * @param fileName report file name
     */
    public void generateXMLReport4LicenseToDependency(Set<DependencyMetadata> dependencyMetadataSet, String fileName) {
        MarkupBuilder xml = getMarkupBuilder(fileName, xmlOutputDir)
        HashMultimap<LicenseMetadata, String> licensesMap = getLicenseMap(dependencyMetadataSet)

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

    /**
     * Generate report by dependency.
     *
     * @param dependencyMetadataSet set with dependencies
     * @param fileName report file name
     */
    public void generateHTMLReport4DependencyToLicense(Set<DependencyMetadata> dependencyMetadataSet, String fileName) {
        MarkupBuilder html = getMarkupBuilder(fileName, htmlOutputDir)

        html.html {
            head {
                title("HTML License report")
            }
            script(type: "text/javascript") {
                mkp.yieldUnescaped '''
                                function showLicense(id, src, a) {
                                    var iframeElem = document.getElementById(id)
                                    if( iframeElem.style.display == "none" ) {
                                         iframeElem.style.display = "block";
                                         iframeElem.src = src;
                                        a.innerHTML = "Hide license agreement";
                                    } else if( iframeElem.style.display == "block" ) {
                                         iframeElem.src = "";
                                         iframeElem.style.display = "none";
                                         a.innerHTML = "Show license agreement";
                                    }
                                 }'''
            }
            style(
             '''table {
                  width: 70%;
                  border-collapse: collapse;
                  text-align: center;
                }
                .dependencies {
                  text-align: left;
                }
                tr {
                  border: 1px solid black;
                }
                td {
                  border: 1px solid black;
                  font-weight: bold;
                  color: #2E2E2E
                }
                th {
                  border: 1px solid black;
                }
                h3 {
                  text-align:center;
                  margin:3px
                }
                .license {
                    width:70%
                }

                .licenseName {
                    width:15%
                }
                ''')
            body {
                table(align: 'center') {
                    tr {
                        th(){ h3("Dependency") }
                        th(){ h3("License name") }
                        th(){ h3("License text URL") }
                    }

                    int i = 0
                    dependencyMetadataSet.each {
                        entry ->
                            entry.licenseMetadataList.each { license ->
                                tr {
                                    td("$entry.dependency", class: 'dependencies')
                                    td(license.licenseName, class: 'licenseName')
                                    td(class: 'license') {
                                        if (!isNullOrEmpty(license.licenseTextUrl)) {
                                            a(href: "#", onClick: "showLicense('$i', '$license.licenseTextUrl', this)", "Show license agreement")
                                            iframe(' ', id: "$i", src: "", style: "border: 0px;display:none", width: '95%', height: '85%')
                                            ++i
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    /**
     * Generate html report by license type.
     *
     * @param dependencyMetadataSet set with dependencies
     * @param fileName report file name
     */
    public void generateHTMLReport4LicenseToDependency(Set<DependencyMetadata> dependencyMetadataSet, String fileName) {
        MarkupBuilder html = getMarkupBuilder(fileName, htmlOutputDir)
        HashMultimap<LicenseMetadata, String> licensesMap = getLicenseMap(dependencyMetadataSet)

        html.html {
            head {
                title("HTML License report")
            }
            script(type: "text/javascript") {
                mkp.yieldUnescaped '''
                                function showLicense(id, src, a) {
                                    var iframeElem = document.getElementById(id)
                                    if( iframeElem.style.display == "none" ) {
                                         iframeElem.style.display = "block";
                                         iframeElem.src = src;
                                        a.innerHTML = "Hide license agreement";
                                    } else if( iframeElem.style.display == "block" ) {
                                         iframeElem.src = "";
                                         iframeElem.style.display = "none";
                                         a.innerHTML = "Show license agreement";
                                    }
                                }'''
            }
            style(
             '''table {
                  width: 70%;
                  border-collapse: collapse;
                  text-align: center;
                }

                .dependencies {
                  text-align: left;
                  width:15%;
                }

                tr {
                  border: 1px solid black;
                }

                td {
                  border: 1px solid black;
                  font-weight: bold;
                  color: #2E2E2E
                }

                th {
                  border: 1px solid black;
                }

                h3 {
                  text-align:center;
                  margin:3px
                }

                .license {
                    width:70%
                }

                .licenseName {
                    width:15%
                }
                ''')
            body {
                table(align: 'center') {
                    tr {
                        th(){ h3("License") }
                        th(){ h3("License text URL") }
                        th(){ h3("Dependency") }
                    }

                    int i = 0
                    licensesMap.asMap().each {
                        entry ->
                            tr {
                                td("$entry.key.licenseName", class: 'licenseName')
                                td(class: 'license') {
                                    if (!isNullOrEmpty(entry.key.licenseTextUrl)) {
                                        a(href: "#", onClick: "showLicense('$i', '$entry.key.licenseTextUrl', this)", "Show license agreement")
                                        iframe(' ', id: "$i", src: "", style: "border: 0px;display:none", width: '95%', height: '85%')
                                        ++i
                                    }
                                }
                                td(class: "dependencies") {
                                    ul() {
                                        entry.value.each {
                                            dependency ->
                                                li("$dependency")
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }
    }

    // Utility
    private HashMultimap<LicenseMetadata, String> getLicenseMap(Set<DependencyMetadata> pomMetadataSet) {
        HashMultimap<LicenseMetadata, String> licensesMap = HashMultimap.create()

        pomMetadataSet.each {
            pom ->
                pom.licenseMetadataList.each {
                    license -> licensesMap.put(license, pom.dependency)
                }
        }

        licensesMap
    }

    private MarkupBuilder getMarkupBuilder(String fileName, File outputDir) {
        File licenseReport = new File(outputDir, fileName)
        licenseReport.createNewFile()
        def writer = new FileWriter(licenseReport)

        new MarkupBuilder(writer)
    }

}
