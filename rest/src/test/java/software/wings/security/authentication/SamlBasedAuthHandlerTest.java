package software.wings.security.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import com.coveo.saml.SamlResponse;
import io.harness.exception.WingsException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.sso.SamlSettings;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.SSOSettingService;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class SamlBasedAuthHandlerTest extends WingsBaseTest {
  @Mock AuthenticationUtil authenticationUtil;
  @Mock SSOSettingService ssoSettingService;
  @Inject @InjectMocks @Spy SamlClientService samlClientService;
  @Inject @InjectMocks private SamlBasedAuthHandler authHandler;

  private static final String idpUrl =
      "https://dev-274703.oktapreview.com/app/harnessiodev274703_testapp_1/exkefa5xlgHhrU1Mc0h7/sso/saml";

  @Before
  public void initMocks() throws IOException {
    String xml = IOUtils.toString(getClass().getResourceAsStream("/okta-IDP-metadata.xml"), Charset.defaultCharset());
    SamlSettings samlSettings = SamlSettings.builder()
                                    .metaDataFile(xml)
                                    .url(idpUrl)
                                    .accountId("TestAccountID")
                                    .displayName("Okta")
                                    .origin("dev-274703.oktapreview.com")
                                    .build();
    when(ssoSettingService.getSamlSettingsByOrigin("dev-274703.oktapreview.com")).thenReturn(samlSettings);
  }

  @Test
  public void testSamlBasedValidationAssertionFails() throws IOException {
    try {
      User user = new User();
      Account account = new Account();
      user.setAccounts(Arrays.asList(account));
      String samlResponse =
          IOUtils.toString(getClass().getResourceAsStream("/SamlResponse.txt"), Charset.defaultCharset());
      account.setAuthenticationMechanism(AuthenticationMechanism.SAML);
      when(authenticationUtil.getUser(anyString())).thenReturn(user);
      when(authenticationUtil.getPrimaryAccount(any(User.class))).thenReturn(account);
      assertThat(authHandler.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.SAML);
      authHandler.authenticate(idpUrl, samlResponse);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo("Saml Authentication Failed");
    }
  }

  @Test
  public void testSamlBasedValidationValidAssertion() throws IOException, SamlException {
    User user = new User();
    Account account = new Account();
    user.setAccounts(Arrays.asList(account));

    String samlResponseString =
        IOUtils.toString(getClass().getResourceAsStream("/SamlResponse.txt"), Charset.defaultCharset());
    account.setAuthenticationMechanism(AuthenticationMechanism.SAML);
    when(authenticationUtil.getUser(anyString())).thenReturn(user);
    when(authenticationUtil.getPrimaryAccount(any(User.class))).thenReturn(account);
    SamlResponse samlResponse = mock(SamlResponse.class);
    when(samlResponse.getNameID()).thenReturn("rushabh@harness.io");
    SamlClient samlClient = mock(SamlClient.class);
    doReturn(samlClient).when(samlClientService).getSamlClientFromOrigin(anyString());
    when(samlClient.decodeAndValidateSamlResponse(anyString())).thenReturn(samlResponse);

    User returnedUser = authHandler.authenticate(idpUrl, samlResponseString);
    assertThat(returnedUser).isEqualTo(user);
  }
}
