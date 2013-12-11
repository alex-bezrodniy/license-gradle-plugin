package nl.javadude.gradle.plugins.license
/**
 * Html license report.
 */
class HtmlLicenseReport extends LicensesReport {

    /**
     * XSLT transormation that will be applied to xml license report to generate html report.
     */
    Object xslt

    HtmlLicenseReport() {
    }

    HtmlLicenseReport(boolean enabled, File destination, Object xslt) {
        super(enabled, destination)
        this.xslt = xslt
    }
}
