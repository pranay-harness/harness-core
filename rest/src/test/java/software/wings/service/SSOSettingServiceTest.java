package software.wings.service;

import com.google.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.sso.SamlSettings;

import javax.validation.ConstraintViolationException;

public class SSOSettingServiceTest extends WingsBaseTest {
  @Inject software.wings.service.intfc.SSOSettingService ssoSettingService;

  @Test
  public void testSamlSettingsCRUD() {
    SamlSettings samlSettings = SamlSettings.builder()
                                    .metaDataFile("TestMetaDataFile")
                                    .url("TestURL")
                                    .accountId("TestAccountID")
                                    .displayName("Okta")
                                    .build();

    samlSettings = ssoSettingService.saveSamlSettings(samlSettings);
    Assertions.assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    Assertions.assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    Assertions.assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    Assertions.assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");

    samlSettings = ssoSettingService.getSamlSettingsByIdpUrl("TestURL");
    Assertions.assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    Assertions.assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    Assertions.assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    Assertions.assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");

    samlSettings = ssoSettingService.getSamlSettingsByAccountId("TestAccountID");
    Assertions.assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    Assertions.assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    Assertions.assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    Assertions.assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");

    samlSettings = SamlSettings.builder()
                       .metaDataFile("TestMetaDataFile2")
                       .url("TestURL2")
                       .accountId("TestAccountID")
                       .displayName("Okta")
                       .build();

    samlSettings = ssoSettingService.saveSamlSettings(samlSettings);
    Assertions.assertThat(samlSettings.getUrl()).isEqualTo("TestURL2");
    Assertions.assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    Assertions.assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile2");
    Assertions.assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");

    samlSettings = ssoSettingService.getSamlSettingsByIdpUrl("TestURL2");
    Assertions.assertThat(samlSettings.getUrl()).isEqualTo("TestURL2");
    Assertions.assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    Assertions.assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile2");
    Assertions.assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");

    samlSettings = ssoSettingService.getSamlSettingsByAccountId("TestAccountID");
    Assertions.assertThat(samlSettings.getUrl()).isEqualTo("TestURL2");
    Assertions.assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    Assertions.assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile2");
    Assertions.assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");

    Assertions.assertThat(ssoSettingService.getSamlSettingsByAccountId("TestAccountID3")).isNull();

    Assertions.assertThat(ssoSettingService.getSamlSettingsByIdpUrl("FakeURL")).isNull();

    Assertions.assertThat(ssoSettingService.deleteSamlSettings("TestAccountID3")).isFalse();
    Assertions.assertThat(ssoSettingService.deleteSamlSettings("TestAccountID")).isTrue();
    Assertions.assertThat(ssoSettingService.getSamlSettingsByAccountId("TestAccountID")).isNull();
    Assertions.assertThat(ssoSettingService.getSamlSettingsByIdpUrl("TestURL2")).isNull();
  }

  @Test
  public void testNegativeTest() {
    try {
      SamlSettings samlSettings = SamlSettings.builder().build();
      samlSettings = ssoSettingService.saveSamlSettings(samlSettings);
      Assertions.failBecauseExceptionWasNotThrown(ConstraintViolationException.class);
    } catch (javax.validation.ConstraintViolationException e) {
      Assertions.assertThat(e.getConstraintViolations().size()).isEqualTo(4);
    }
  }
}
