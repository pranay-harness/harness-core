/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication;

import static io.harness.rule.OwnerRule.RUSHABH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.sso.SamlSettings;
import software.wings.security.saml.SamlClientService;
import software.wings.security.saml.SamlUserGroupSync;
import software.wings.service.intfc.SSOSettingService;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import com.coveo.saml.SamlResponse;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class SamlBasedAuthHandlerTest extends WingsBaseTest {
  @Mock AuthenticationUtils authenticationUtils;
  @Mock SSOSettingService ssoSettingService;
  @Mock SamlUserGroupSync samlUserGroupSync;
  @Inject @InjectMocks @Spy SamlClientService samlClientService;
  @Inject @InjectMocks private SamlBasedAuthHandler authHandler;

  private static final String oktaIdpUrl =
      "https://dev-274703.oktapreview.com/app/harnessiodev274703_testapp_1/exkefa5xlgHhrU1Mc0h7/sso/saml";

  private static final String googleIdpUrl1 =
      "https://accounts.google.com/o/saml2/initsso?idpid=C00pxqnjz&spid=256731830644&forceauthn=false&from_login=1&as=DzxFw6iKFGy_LYb42_bZ4g&pli=1&authuser=0";
  private static final String azureIdpUrl2 =
      "https://login.microsoftonline.com/b229b2bb-5f33-4d22-bce0-730f6474e906/saml2?SAMLRequest=jZFfS8MwFMW%2FSsl7%2F2eahbbQrRsMpoxNffAtdHcu0CY1Nxn67c06HYIovh7O73DOvQWKvht47exRbeHVAdrgre8UlsQZxbVAiVyJHpDblu%2FquzXPooT3YMVeWEGCVVOSJaMNY6yu8xml83TGKM1vJzVdsCxNmumCBE9gUGpVEg97BtHBSqEVynopSVmYTMOUPmQ5T6Y8T6PsZvJ89m0EojxBSQ6iQyBBjQjG%2BqS5Vuh6MDswJ9nC43ZdkqO1A%2FI4bmV0FEYBYiR1LAYZO09hfJ4advpFKnLZyMfxfy8djLa61R2pirG2%2Bc95xFdNUn02KeILXRWXg997ZtVsdCfb92CpTS%2Fs75FplI6K3IeH0cqdwgFaeZCw98Xin5lX8ftnqw8%3D";

  @Before
  public void initMocks() throws IOException {
    String xml = IOUtils.toString(getClass().getResourceAsStream("/okta-IDP-metadata.xml"), Charset.defaultCharset());
    SamlSettings samlSettings = SamlSettings.builder()
                                    .metaDataFile(xml)
                                    .url(oktaIdpUrl)
                                    .accountId("TestAccountID")
                                    .displayName("Okta")
                                    .origin("dev-274703.oktapreview.com")
                                    .build();
    when(ssoSettingService.getSamlSettingsByOrigin("dev-274703.oktapreview.com")).thenReturn(samlSettings);
    when(ssoSettingService.getSamlSettingsByAccountId(anyString())).thenReturn(samlSettings);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testSamlBasedValidationAssertionFails() throws IOException {
    try {
      User user = new User();
      Account account = new Account();
      user.setAccounts(Arrays.asList(account));
      String samlResponse =
          IOUtils.toString(getClass().getResourceAsStream("/SamlResponse.txt"), Charset.defaultCharset());
      account.setAuthenticationMechanism(io.harness.ng.core.account.AuthenticationMechanism.SAML);
      when(authenticationUtils.getUser(anyString())).thenReturn(user);
      when(authenticationUtils.getDefaultAccount(any(User.class))).thenReturn(account);
      assertThat(authHandler.getAuthenticationMechanism())
          .isEqualTo(io.harness.ng.core.account.AuthenticationMechanism.SAML);
      authHandler.authenticate(oktaIdpUrl, samlResponse);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo("Saml Authentication Failed");
    }
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testSamlBasedValidationValidAssertionForOkta() throws IOException, SamlException {
    User user = new User();
    user.setDefaultAccountId("kmpySmUISimoRrJL6NL73w");
    user.setUuid("kmpySmUISimoRrJL6NL73w");
    Account account = new Account();
    account.setUuid("AC1");
    user.setAccounts(Arrays.asList(account));

    String samlResponseString =
        IOUtils.toString(getClass().getResourceAsStream("/SamlResponse.txt"), Charset.defaultCharset());
    account.setAuthenticationMechanism(io.harness.ng.core.account.AuthenticationMechanism.SAML);
    when(authenticationUtils.getUser(anyString())).thenReturn(user);
    when(authenticationUtils.getDefaultAccount(any(User.class))).thenReturn(account);
    SamlResponse samlResponse = mock(SamlResponse.class);
    when(samlResponse.getNameID()).thenReturn("rushabh@harness.io");
    SamlClient samlClient = mock(SamlClient.class);
    final SamlSettings samlSettings = mock(SamlSettings.class);
    when(samlSettings.getAccountId()).thenReturn("AC1");
    List<SamlSettings> samlSettingsList = Arrays.asList(samlSettings);
    doReturn(samlSettingsList.iterator()).when(samlClientService).getSamlSettingsFromOrigin(anyString(), anyString());
    doReturn(samlClient).when(samlClientService).getSamlClient(samlSettings);
    when(samlClient.decodeAndValidateSamlResponse(anyString())).thenReturn(samlResponse);

    User returnedUser = authHandler.authenticate(oktaIdpUrl, samlResponseString).getUser();
    assertThat(returnedUser).isEqualTo(user);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testSamlBasedValidationValidAssertionForGoogle() throws IOException, SamlException {
    User user = new User();

    Account account = new Account();
    account.setUuid("TestGoogleAuthAccount1");
    user.setAccounts(Arrays.asList(account));
    user.setDefaultAccountId("kmpySmUISimoRrJL6NL73w");
    user.setUuid("kmpySmUISimoRrJL6NL73w");
    String samlResponseString =
        IOUtils.toString(getClass().getResourceAsStream("/SamlResponse.txt"), Charset.defaultCharset());
    account.setAuthenticationMechanism(io.harness.ng.core.account.AuthenticationMechanism.SAML);
    when(authenticationUtils.getUser(anyString())).thenReturn(user);
    when(authenticationUtils.getDefaultAccount(any(User.class))).thenReturn(account);
    SamlResponse samlResponse = mock(SamlResponse.class);
    when(samlResponse.getNameID()).thenReturn("rushabh@harness.io");
    SamlClient samlClient = mock(SamlClient.class);

    String xml = IOUtils.toString(getClass().getResourceAsStream("/GoogleIDPMetadata.xml"), Charset.defaultCharset());

    SamlSettings googleSamlSettings1 = SamlSettings.builder()
                                           .metaDataFile(xml)
                                           .url("https://accounts.google.com/o/saml2/initsso?idpid=C00pxqnjz")
                                           .accountId("TestGoogleAuthAccount1")
                                           .displayName("Google 1")
                                           .origin("accounts.google.com")
                                           .build();

    SamlSettings googleSamlSettings2 = SamlSettings.builder()
                                           .metaDataFile(xml)
                                           .url("https://accounts.google.com/o/saml2/initsso?idpid=C00pxqnjAAA")
                                           .accountId("TestGoogleAuthAccount2")
                                           .displayName("Google 2")
                                           .origin("accounts.google.com")
                                           .build();

    googleSamlSettings1 = spy(googleSamlSettings1);
    googleSamlSettings2 = spy(googleSamlSettings2);

    when(ssoSettingService.getSamlSettingsIteratorByOrigin("accounts.google.com", "TestGoogleAuthAccount2"))
        .thenReturn(Arrays.asList(googleSamlSettings1, googleSamlSettings2).iterator());

    doReturn(samlClient).when(samlClientService).getSamlClient(any(SamlSettings.class));
    when(samlClient.decodeAndValidateSamlResponse(anyString())).thenReturn(samlResponse);

    User returnedUser = authHandler.authenticate(googleIdpUrl1, samlResponseString, "TestGoogleAuthAccount2").getUser();
    assertThat(returnedUser).isEqualTo(user);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testAzureSaml() throws IOException, SamlException {
    User user = new User();
    user.setDefaultAccountId("kmpySmUISimoRrJL6NL73w");
    user.setUuid("kmpySmUISimoRrJL6NL73w");
    Account account = new Account();
    account.setUuid("TestAzureAccount1");
    user.setAccounts(Arrays.asList(account));

    String samlResponseString =
        IOUtils.toString(getClass().getResourceAsStream("/SamlResponse.txt"), Charset.defaultCharset());
    account.setAuthenticationMechanism(io.harness.ng.core.account.AuthenticationMechanism.SAML);
    when(authenticationUtils.getUser(anyString())).thenReturn(user);
    when(authenticationUtils.getDefaultAccount(any(User.class))).thenReturn(account);
    SamlResponse samlResponse = mock(SamlResponse.class);
    when(samlResponse.getNameID()).thenReturn("rushabh@harness.io");
    SamlClient samlClient = mock(SamlClient.class);

    String xml = IOUtils.toString(getClass().getResourceAsStream("/Azure-1-metadata.xml"), Charset.defaultCharset());

    SamlSettings azureSetting1 =
        SamlSettings.builder()
            .metaDataFile(xml)
            .url("https://login.microsoftonline.com/b229b2bb-5f33-4d22-bce0-730f6474e906/saml2")
            .accountId("TestAzureAccount1")
            .displayName("Azure 1")
            .origin("login.microsoftonline.com")
            .build();

    SamlSettings azureSetting2 = SamlSettings.builder()
                                     .metaDataFile(xml)
                                     .url("https://login.microsoftonline.com/b229b2bb-5f33-4d22-bce0-fakedata/saml2")
                                     .accountId("TestAzureAccount2")
                                     .displayName("Azure 2")
                                     .origin("login.microsoftonline.com")
                                     .build();

    azureSetting1 = spy(azureSetting1);
    azureSetting2 = spy(azureSetting2);

    when(ssoSettingService.getSamlSettingsIteratorByOrigin("login.microsoftonline.com", null))
        .thenReturn(Arrays.asList(azureSetting1, azureSetting2).iterator());

    doReturn(samlClient).when(samlClientService).getSamlClient(any(SamlSettings.class));
    when(samlClient.decodeAndValidateSamlResponse(anyString())).thenReturn(samlResponse);

    User returnedUser = authHandler.authenticate(azureIdpUrl2, samlResponseString, null).getUser();
    assertThat(returnedUser).isEqualTo(user);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testSamlAuthenticationAndGroupExtractionForOktaShouldSucceed() throws IOException, SamlException {
    User user = new User();
    user.setDefaultAccountId("kmpySmUISimoRrJL6NL73w");
    user.setUuid("kmpySmUISimoRrJL6NL73w");
    Account account = new Account();
    account.setUuid("AC1");
    user.setAccounts(Arrays.asList(account));

    String samlResponseString =
        IOUtils.toString(getClass().getResourceAsStream("/SamlResponse.txt"), Charset.defaultCharset());
    account.setAuthenticationMechanism(io.harness.ng.core.account.AuthenticationMechanism.SAML);
    when(authenticationUtils.getUser(anyString())).thenReturn(user);
    when(authenticationUtils.getDefaultAccount(any(User.class))).thenReturn(account);
    SamlResponse samlResponse = mock(SamlResponse.class);
    when(samlResponse.getNameID()).thenReturn("rushabh@harness.io");
    SamlClient samlClient = mock(SamlClient.class);
    final SamlSettings samlSettings = mock(SamlSettings.class);
    when(samlSettings.getAccountId()).thenReturn("AC1");
    List<SamlSettings> samlSettingsList = Arrays.asList(samlSettings);
    doReturn(samlSettingsList.iterator()).when(samlClientService).getSamlSettingsFromOrigin(anyString(), anyString());
    doReturn(samlClient).when(samlClientService).getSamlClient(samlSettings);
    when(samlClient.decodeAndValidateSamlResponse(anyString())).thenReturn(samlResponse);

    doNothing().when(samlUserGroupSync).syncUserGroup(any(SamlUserAuthorization.class), anyString(), anyString());
    doReturn(true).when(samlSettings).isAuthorizationEnabled();
    User returnedUser = authHandler.authenticate(oktaIdpUrl, samlResponseString).getUser();
    assertThat(returnedUser).isEqualTo(user);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testValidateUser() {
    User user = new User();
    user.setEmail("test@test.com");
    Account account1 = new Account();
    account1.setUuid("AC1");

    Account account2 = new Account();
    account2.setUuid("AC2");

    user.setAccounts(Arrays.asList(account1, account2));

    try {
      authHandler.validateUser(user, "AC3");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(WingsException.class);
    }

    try {
      authHandler.validateUser(user, "AC2");
    } catch (Exception e) {
      fail(e.getMessage());
    }

    try {
      authHandler.validateUser(user, "AC1");
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  /**
   * Tests if ErrorCode.SAML_TEST_SUCCESS_MECHANISM_NOT_ENABLED is thrown when SAML is authenticated without enabling it
   * @throws IOException
   * @throws SamlException
   */
  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testAuthenticationWithSamlNotEnabled() throws IOException, SamlException {
    User user = new User();
    user.setDefaultAccountId("kmpySmUISimoRrJL6NL73w");
    user.setUuid("kmpySmUISimoRrJL6NL73w");
    Account account = new Account();
    account.setUuid("AC1");
    user.setAccounts(Arrays.asList(account));

    String samlResponseString =
        IOUtils.toString(getClass().getResourceAsStream("/SamlResponse.txt"), Charset.defaultCharset());
    account.setAuthenticationMechanism(AuthenticationMechanism.OAUTH);
    when(authenticationUtils.getUser(anyString())).thenReturn(user);
    when(authenticationUtils.getDefaultAccount(any(User.class))).thenReturn(account);
    SamlResponse samlResponse = mock(SamlResponse.class);
    when(samlResponse.getNameID()).thenReturn("rushabh@harness.io");
    SamlClient samlClient = mock(SamlClient.class);
    final SamlSettings samlSettings = mock(SamlSettings.class);
    when(samlSettings.getAccountId()).thenReturn("AC1");
    List<SamlSettings> samlSettingsList = Arrays.asList(samlSettings);
    doReturn(samlSettingsList.iterator()).when(samlClientService).getSamlSettingsFromOrigin(anyString(), anyString());
    doReturn(samlClient).when(samlClientService).getSamlClient(samlSettings);
    when(samlClient.decodeAndValidateSamlResponse(anyString())).thenReturn(samlResponse);

    doNothing().when(samlUserGroupSync).syncUserGroup(any(SamlUserAuthorization.class), anyString(), anyString());
    doReturn(true).when(samlSettings).isAuthorizationEnabled();
    try {
      authHandler.authenticate(oktaIdpUrl, samlResponseString);
    } catch (WingsException e) {
      assertThat(e.getCode()).isEqualTo(ErrorCode.SAML_TEST_SUCCESS_MECHANISM_NOT_ENABLED);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}
