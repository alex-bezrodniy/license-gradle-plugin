package nl.javadude.gradle.plugins.license

import org.junit.Before
import org.junit.Test

/**
 * Unit test for {@link DownloadLicensesExtension}.
 */
class DownloadLicensesExtensionTest {

    DownloadLicensesExtension extension;

    @Before
    public void setupProject() {
        extension = new DownloadLicensesExtension();
    }

    @Test
    public void ableToConstruct() {
        assert extension != null;
    }

    @Test
    public void licenseMetaDataBuilderWorksWell() {
        // WHEN
        extension.customLicensesMapping = [
                "org.gson:gson:1.4" : extension.license("Apache 2","http://google.com")
        ]

        // THEN
        assert extension.customLicensesMapping["org.gson:gson:1.4"] == new LicenseMetadata(
                licenseName: "Apache 2",
                licenseTextUrl: "http://google.com"
        )
    }

}
