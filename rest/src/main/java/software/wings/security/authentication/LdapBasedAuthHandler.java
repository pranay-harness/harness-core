package software.wings.security.authentication;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.exception.WingsException;
import software.wings.beans.Account;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.User;
import software.wings.beans.sso.LdapSettings;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapResponse.Status;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.ldap.LdapDelegateService;
import software.wings.service.intfc.security.SecretManager;

@Singleton
public class LdapBasedAuthHandler implements AuthHandler {
  @Inject private SSOSettingService ssoSettingService;
  @Inject private AuthenticationUtil authenticationUtil;
  @Inject private SecretManager secretManager;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  private UserService userService;

  @SuppressFBWarnings("URF_UNREAD_FIELD")
  @Inject
  public LdapBasedAuthHandler(UserService userService) {
    this.userService = userService;
  }

  @Override
  public User authenticate(String... credentials) {
    if (credentials == null || credentials.length != 2) {
      throw new WingsException(INVALID_ARGUMENT);
    }

    String username = credentials[0];
    String password = credentials[1];

    User user = getUser(username);
    if (user == null) {
      throw new WingsException(USER_DOES_NOT_EXIST, USER);
    }

    Account account = authenticationUtil.getPrimaryAccount(user);
    LdapSettings settings = ssoSettingService.getLdapSettingsByAccountId(account.getUuid());
    EncryptedDataDetail settingsEncryptedDataDetail = settings.getEncryptedDataDetails(secretManager);
    String encryptedPassword = secretManager.encrypt(settings.getAccountId(), password, null);
    EncryptedDataDetail passwordEncryptedDataDetail =
        secretManager.encryptedDataDetails(settings.getAccountId(), "password", encryptedPassword).get();
    try {
      SyncTaskContext syncTaskContext =
          aContext().withAccountId(settings.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
      LdapResponse authenticationResponse =
          delegateProxyFactory.get(LdapDelegateService.class, syncTaskContext)
              .authenticate(settings, settingsEncryptedDataDetail, username, passwordEncryptedDataDetail);
      if (authenticationResponse.getStatus().equals(Status.SUCCESS)) {
        return user;
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
