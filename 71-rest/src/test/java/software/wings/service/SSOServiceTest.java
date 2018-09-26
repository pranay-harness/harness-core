package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import com.coveo.saml.SamlException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.ldaptive.ResultCode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.sso.LdapConnectionSettings;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapGroupSettings;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapTestResponse;
import software.wings.beans.sso.LdapTestResponse.Status;
import software.wings.beans.sso.LdapUserSettings;
import software.wings.beans.sso.SamlSettings;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.SSOConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.ldap.LdapDelegateService;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class SSOServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence WINGS_PERSISTENCE;
  @Mock private AccountService ACCOUNT_SERVICE;
  @Mock private SSOSettingService SSO_SETTING_SERVICE;
  @Mock private SamlClientService SAML_CLIENT_SERVICE;
  @Mock private DelegateProxyFactory DELEGATE_PROXY_FACTORY;
  @Mock private LdapDelegateService LDAP_DELEGATE_SERVICE;
  @Mock private SecretManager SECRET_MANAGER;

  private LdapSettings ldapSettings;

  @Inject @InjectMocks SSOService ssoService;

  @Before
  public void setup() {
    EncryptedData encryptedData = mock(EncryptedData.class);
    EncryptedDataDetail encryptedDataDetail = mock(EncryptedDataDetail.class);
    when(SECRET_MANAGER.encryptedDataDetails(eq(ACCOUNT_ID), eq(LdapConstants.BIND_PASSWORD_KEY), any()))
        .thenReturn(Optional.of(encryptedDataDetail));
    when(SECRET_MANAGER.encryptedDataDetails(eq(ACCOUNT_ID), eq(LdapConstants.USER_PASSWORD_KEY), any()))
        .thenReturn(Optional.of(encryptedDataDetail));
    when(SECRET_MANAGER.deleteSecretUsingUuid(anyString())).thenReturn(true);
    when(encryptedDataDetail.getEncryptedData()).thenReturn(encryptedData);
    when(encryptedData.getUuid()).thenReturn("UUID");
    LdapConnectionSettings connectionSettings = new LdapConnectionSettings();
    connectionSettings.setBindDN("testBindDN");
    connectionSettings.setBindPassword("testBindPassword");
    LdapUserSettings userSettings = new LdapUserSettings();
    userSettings.setBaseDN("testBaseDN");
    LdapGroupSettings groupSettings = new LdapGroupSettings();
    groupSettings.setBaseDN("testBaseDN");
    ldapSettings = new LdapSettings("testSettings", ACCOUNT_ID, connectionSettings, userSettings, groupSettings);
  }

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

    settings = ssoService.setAuthenticationMechanism("testAccount", AuthenticationMechanism.LDAP);
    assertThat(settings.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.LDAP);
  }

  @Test
  public void validateLdapConnectionSettings() {
    when(DELEGATE_PROXY_FACTORY.get(any(), any())).thenReturn(LDAP_DELEGATE_SERVICE);
    when(LDAP_DELEGATE_SERVICE.validateLdapConnectionSettings(any(), any()))
        .thenReturn(LdapTestResponse.builder().status(Status.SUCCESS).build());
    LdapTestResponse response = ssoService.validateLdapConnectionSettings(ldapSettings, "testAccount");
    assertThat(response.getStatus()).isEqualTo(Status.SUCCESS);
  }

  @Test
  public void validateLdapUserSettings() {
    when(DELEGATE_PROXY_FACTORY.get(any(), any())).thenReturn(LDAP_DELEGATE_SERVICE);
    when(LDAP_DELEGATE_SERVICE.validateLdapUserSettings(any(), any()))
        .thenReturn(LdapTestResponse.builder().status(Status.SUCCESS).build());
    LdapTestResponse response = ssoService.validateLdapUserSettings(ldapSettings, "testAccount");
    assertThat(response.getStatus()).isEqualTo(Status.SUCCESS);
  }

  @Test
  public void validateLdapGroupSettings() {
    when(DELEGATE_PROXY_FACTORY.get(any(), any())).thenReturn(LDAP_DELEGATE_SERVICE);
    when(LDAP_DELEGATE_SERVICE.validateLdapGroupSettings(any(), any()))
        .thenReturn(LdapTestResponse.builder().status(Status.SUCCESS).build());
    LdapTestResponse response = ssoService.validateLdapGroupSettings(ldapSettings, "testAccount");
    assertThat(response.getStatus()).isEqualTo(Status.SUCCESS);
  }

  @Test
  public void validateLdapAuthentication() {
    when(DELEGATE_PROXY_FACTORY.get(any(), any())).thenReturn(LDAP_DELEGATE_SERVICE);
    when(LDAP_DELEGATE_SERVICE.authenticate(any(), any(), any(), any()))
        .thenReturn(LdapResponse.builder().status(LdapResponse.Status.SUCCESS).build());
    LdapResponse response = ssoService.validateLdapAuthentication(ldapSettings, "username", "password");
    assertThat(response.getStatus()).isEqualTo(LdapResponse.Status.SUCCESS);
    when(LDAP_DELEGATE_SERVICE.authenticate(any(), any(), any(), any()))
        .thenReturn(LdapResponse.builder()
                        .status(LdapResponse.Status.FAILURE)
                        .message(ResultCode.INAPPROPRIATE_MATCHING.toString())
                        .build());
    response = ssoService.validateLdapAuthentication(ldapSettings, "username", "password");
    assertThat(response.getStatus()).isEqualTo(LdapResponse.Status.FAILURE);
    assertThat(response.getMessage()).contains(ResultCode.INAPPROPRIATE_MATCHING.toString());
  }

  @Test
  public void searchGroupsByName() {
    EncryptedDataDetail encryptedDataDetail = mock(EncryptedDataDetail.class);
    LdapSettings spyLdapSettings = spy(ldapSettings);
    when(DELEGATE_PROXY_FACTORY.get(any(), any())).thenReturn(LDAP_DELEGATE_SERVICE);
    when(LDAP_DELEGATE_SERVICE.searchGroupsByName(any(), any(), any()))
        .thenReturn(Collections.singletonList(LdapGroupResponse.builder().name("testGroup").build()));
    when(SSO_SETTING_SERVICE.getLdapSettingsByUuid(any())).thenReturn(spyLdapSettings);
    doReturn(encryptedDataDetail).when(spyLdapSettings).getEncryptedDataDetails(any());

    Collection<LdapGroupResponse> responses = ssoService.searchGroupsByName("testLdapId", "testQuery");
    assertThat(responses.size()).isEqualTo(1);
    for (LdapGroupResponse response : responses) {
      assertThat(response.getName()).isEqualTo("testGroup");
    }
  }
}
