package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import com.google.inject.Inject;

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
                                    .origin("TestOrigin")
                                    .build();

    samlSettings = ssoSettingService.saveSamlSettings(samlSettings);
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin");

    samlSettings = ssoSettingService.getSamlSettingsByIdpUrl("TestURL");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin");

    samlSettings = ssoSettingService.getSamlSettingsByAccountId("TestAccountID");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin");

    samlSettings = ssoSettingService.getSamlSettingsByOrigin("TestOrigin");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin");

    samlSettings = SamlSettings.builder()
                       .metaDataFile("TestMetaDataFile2")
                       .url("TestURL2")
                       .accountId("TestAccountID")
                       .displayName("Okta")
                       .origin("TestOrigin2")
                       .build();

    samlSettings = ssoSettingService.saveSamlSettings(samlSettings);
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL2");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile2");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin2");

    samlSettings = ssoSettingService.getSamlSettingsByIdpUrl("TestURL2");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL2");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile2");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin2");

    samlSettings = ssoSettingService.getSamlSettingsByAccountId("TestAccountID");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL2");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile2");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin2");

    samlSettings = ssoSettingService.getSamlSettingsByOrigin("TestOrigin2");
    assertThat(samlSettings.getUrl()).isEqualTo("TestURL2");
    assertThat(samlSettings.getAccountId()).isEqualTo("TestAccountID");
    assertThat(samlSettings.getMetaDataFile()).isEqualTo("TestMetaDataFile2");
    assertThat(samlSettings.getDisplayName()).isEqualTo("Okta");
    assertThat(samlSettings.getOrigin()).isEqualTo("TestOrigin2");

    assertThat(ssoSettingService.getSamlSettingsByAccountId("TestAccountID3")).isNull();

    assertThat(ssoSettingService.getSamlSettingsByIdpUrl("FakeURL")).isNull();
    assertThat(ssoSettingService.getSamlSettingsByOrigin("FakeOrigin")).isNull();

    assertThat(ssoSettingService.deleteSamlSettings("TestAccountID3")).isFalse();
    assertThat(ssoSettingService.deleteSamlSettings("TestAccountID")).isTrue();
    assertThat(ssoSettingService.getSamlSettingsByAccountId("TestAccountID")).isNull();
    assertThat(ssoSettingService.getSamlSettingsByIdpUrl("TestURL2")).isNull();
  }

  @Test
  public void testNegativeTest() {
    try {
      SamlSettings samlSettings = SamlSettings.builder().build();
      samlSettings = ssoSettingService.saveSamlSettings(samlSettings);
      failBecauseExceptionWasNotThrown(ConstraintViolationException.class);
    } catch (javax.validation.ConstraintViolationException e) {
      assertThat(e.getConstraintViolations().size()).isEqualTo(5);
    }
  }
}
