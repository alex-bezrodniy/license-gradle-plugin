package nl.javadude.gradle.plugins.license

/**
 * Report container.
 */
class DownloadLicensesReportExtension {
    LicensesReport xml = new LicensesReport()
    HtmlLicenseReport html = new HtmlLicenseReport()
}
