package software.wings.security.authentication;

import static io.harness.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.Account;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.User;
import software.wings.beans.sso.LdapSettings;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapResponse.Status;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.ldap.LdapDelegateService;
import software.wings.service.intfc.security.SecretManager;

@Singleton
public class LdapBasedAuthHandler implements AuthHandler {
  @Inject private SSOSettingService ssoSettingService;
  @Inject private AuthenticationUtils authenticationUtils;
  @Inject private SecretManager secretManager;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  private UserService userService;
  private DomainWhitelistCheckerService domainWhitelistCheckerService;

  @Inject
  public LdapBasedAuthHandler(UserService userService, DomainWhitelistCheckerService domainWhitelistCheckerService) {
    this.userService = userService;
    this.domainWhitelistCheckerService = domainWhitelistCheckerService;
  }

  @Override
  public AuthenticationResponse authenticate(String... credentials) {
    if (credentials == null || credentials.length != 2) {
      throw new WingsException(INVALID_ARGUMENT);
    }

    String username = credentials[0];
    String password = credentials[1];

    User user = getUser(username);
    if (user == null) {
      throw new WingsException(USER_DOES_NOT_EXIST, USER);
    }

    Account account = authenticationUtils.getDefaultAccount(user);
    if (!domainWhitelistCheckerService.isDomainWhitelisted(user, account)) {
      domainWhitelistCheckerService.throwDomainWhitelistFilterException();
    }
    LdapSettings settings = ssoSettingService.getLdapSettingsByAccountId(account.getUuid());
    EncryptedDataDetail settingsEncryptedDataDetail = settings.getEncryptedDataDetails(secretManager);
    String encryptedPassword = secretManager.encrypt(settings.getAccountId(), password, null);
    EncryptedDataDetail passwordEncryptedDataDetail =
        secretManager.encryptedDataDetails(settings.getAccountId(), "password", encryptedPassword).get();
    try {
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settings.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                            .build();
      LdapResponse authenticationResponse =
          delegateProxyFactory.get(LdapDelegateService.class, syncTaskContext)
              .authenticate(settings, settingsEncryptedDataDetail, username, passwordEncryptedDataDetail);
      if (authenticationResponse.getStatus().equals(Status.SUCCESS)) {
        return new AuthenticationResponse(user);
      }
      throw new WingsException(INVALID_CREDENTIAL, USER);
    } finally {
      secretManager.deleteSecretUsingUuid(passwordEncryptedDataDetail.getEncryptedData().getUuid());
    }
  }

  @Override
  public AuthenticationMechanism getAuthenticationMechanism() {
    return AuthenticationMechanism.LDAP;
  }

  protected User getUser(String email) {
    return userService.getUserByEmail(email);
  }
}
