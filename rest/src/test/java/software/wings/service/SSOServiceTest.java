package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import com.coveo.saml.SamlException;
import io.harness.eraro.ErrorCode;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.sso.SamlSettings;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.SSOConfig;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;

import java.io.IOException;
import java.nio.charset.Charset;

public class SSOServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence WINGS_PERSISTENCE;
  @Mock private AccountService ACCOUNT_SERVICE;
  @Mock private SSOSettingService SSO_SETTING_SERVICE;
  @Mock private SamlClientService SAML_CLIENT_SERVICE;

  @Inject @InjectMocks SSOService ssoService;

  @Test
  public void uploadSamlConfiguration() throws IOException, SamlException {
    Account account = new Account();
    String xml = IOUtils.toString(getClass().getResourceAsStream("/okta-IDP-metadata.xml"), Charset.defaultCharset());
    when(ACCOUNT_SERVICE.get(anyString())).thenReturn(account);
    when(ACCOUNT_SERVICE.update(account)).thenReturn(account);
    when(SAML_CLIENT_SERVICE.getSamlClient(anyString())).thenCallRealMethod();

    SamlSettings mockSamlSettings = SamlSettings.builder().build();
    mockSamlSettings.setAccountId("TestAccountID");
    mockSamlSettings.setUrl(
        "https://dev-274703.oktapreview.com/app/harnessiodev274703_testapp_1/exkefa5xlgHhrU1Mc0h7/sso/saml");
    mockSamlSettings.setMetaDataFile(xml);
    mockSamlSettings.setDisplayName("Okta");
    when(SSO_SETTING_SERVICE.getSamlSettingsByAccountId(anyString())).thenReturn(mockSamlSettings);

    SSOConfig settings = ssoService.uploadSamlConfiguration(
        "testAccountID", getClass().getResourceAsStream("/okta-IDP-metadata.xml"), "Okta");
    String idpRedirectUrl = ((SamlSettings) settings.getSsoSettings().get(0)).getUrl();
    assertThat(idpRedirectUrl)
        .isEqualTo("https://dev-274703.oktapreview.com/app/harnessiodev274703_testapp_1/exkefa5xlgHhrU1Mc0h7/sso/saml");
    assertThat(settings.getSsoSettings().get(0).getDisplayName()).isEqualTo("Okta");

    try {
      ssoService.uploadSamlConfiguration("testAccountID", getClass().getResourceAsStream("/SamlResponse.txt"), "Okta");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_SAML_CONFIGURATION.name());
    }
  }

  @Test
  public void setAuthenticationMechanism() {
    Account account = new Account();
    when(ACCOUNT_SERVICE.get(anyString())).thenReturn(account);
    when(ACCOUNT_SERVICE.update(account)).thenReturn(account);
    SSOConfig settings = ssoService.setAuthenticationMechanism("testAccount", AuthenticationMechanism.SAML);
    assertThat(settings.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.SAML);

    settings = ssoService.setAuthenticationMechanism("testAccount", AuthenticationMechanism.USER_PASSWORD);
    assertThat(settings.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.USER_PASSWORD);
  }
}
