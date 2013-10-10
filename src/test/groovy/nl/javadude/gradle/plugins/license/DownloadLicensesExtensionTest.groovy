package nl.javadude.gradle.plugins.license

import org.junit.Before
import org.junit.Test

/**
 * Unit test for {@link DownloadLicensesExtension}
 */
class DownloadLicensesExtensionTest {

    DownloadLicensesExtension extension;

    @Before
    public void setupProject() {
        extension = new LicenseExtension();
    }

    @Test
    public void ableToConstruct() {
        assert extension != null;
    }

}
