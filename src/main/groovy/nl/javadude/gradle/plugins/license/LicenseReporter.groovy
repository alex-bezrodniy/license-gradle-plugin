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
            link(rel: "stylesheet", href: "http://code.jquery.com/ui/1.10.1/themes/base/jquery-ui.css", type: "text/css", media: "all")
            script(src: "http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js", "")
            script(src: "http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/jquery-ui.min.js", "")
            script(type: "text/javascript") {
                mkp.yieldUnescaped '''
                    function showLicense(url) {
                    var $dialog = $('<div></div>')
                       .html('<iframe style="border: 0px; " src="' + url + '" width="100%" height="100%"></iframe>')
                       .dialog({
                       autoOpen: false,
                       modal: true,
                       height: 400,
                       width: 700,
                       title: "License agreement"
                      });

                    $dialog.dialog({ position: { my: "top", at: "top", of: window } });
                    $dialog.dialog('open');
                    } '''
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
                } ''')
            body {
                table(align: 'center') {
                    tr {
                        th(){ h3("Dependency") }
                        th(){ h3("License name") }
                        th(){ h3("License text URL") }
                    }

                    dependencyMetadataSet.each {
                        entry ->
                            entry.licenseMetadataList.each { license ->
                                tr {
                                    td("$entry.dependency")
                                    td(license.licenseName)
                                    td() {
                                        if (!isNullOrEmpty(license.licenseTextUrl)) {
                                            a(href: "#", onClick: "showLicense('${license.licenseTextUrl}')", "View license agreement text")
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
            link(rel: "stylesheet", href: "http://code.jquery.com/ui/1.10.1/themes/base/jquery-ui.css", type: "text/css", media: "all")
            script(src: "http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js", "")
            script(src: "http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/jquery-ui.min.js", "")
            script(type: "text/javascript") {
                mkp.yieldUnescaped '''
                    function showLicense(url) {
                    var $dialog = $('<div></div>')
                       .html('<iframe style="border: 0px; " src="' + url + '" width="100%" height="100%"></iframe>')
                       .dialog({
                       autoOpen: false,
                       modal: true,
                       height: 400,
                       width: 700,
                       title: "License agreement"
                      });

                    $dialog.dialog({ position: { my: "top", at: "top", of: window } });
                    $dialog.dialog('open');
                    } '''
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
                } ''')
            body {
                table(align: 'center') {
                    tr {
                        th(){ h3("License") }
                        th(){ h3("License text URL") }
                        th(){ h3("Dependency") }
                    }

                    licensesMap.asMap().each {
                        entry ->
                            tr {
                                td("$entry.key.licenseName")
                                td() {
                                    if (!isNullOrEmpty(entry.key.licenseTextUrl)) {
                                        a(href: "#", onClick: "showLicense('${entry.key.licenseTextUrl}')", "View license agreement text")
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
