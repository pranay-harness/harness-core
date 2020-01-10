package software.wings.events;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import software.wings.beans.Account;
import software.wings.beans.Account.Builder;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.security.authentication.AuthenticationMechanism;

/**
 * @author rktummala on 12/05/18
 */
@Singleton
public class TestUtils {
  @Inject private ScmSecret scmSecret;

  public Account createAccount() {
    return Builder.anAccount()
        .withAccountName(ACCOUNT_NAME)
        .withCompanyName(COMPANY_NAME)
        .withAuthenticationMechanism(AuthenticationMechanism.USER_PASSWORD)
        .withUuid(ACCOUNT_ID)
        .withLicenseInfo(LicenseInfo.builder()
                             .accountType(AccountType.TRIAL)
                             .accountStatus(AccountStatus.ACTIVE)
                             .expiryTime(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000)
                             .licenseUnits(100)
                             .build())
        .build();
  }

  public MarketoConfig initializeMarketoConfig() {
    MarketoConfig marketoConfig = new MarketoConfig();
    marketoConfig.setEnabled(true);
    marketoConfig.setUrl(scmSecret.decryptToString(new SecretName("marketo_url")));
    marketoConfig.setClientId(scmSecret.decryptToString(new SecretName("marketo_client_id")));
    marketoConfig.setClientSecret(scmSecret.decryptToString(new SecretName("marketo_client_secret")));
    return marketoConfig;
  }

  public User createUser(Account account) {
    return anUser()
        .uuid(generateUuid())
        .appId(APP_ID)
        .email(USER_EMAIL)
        .name(USER_NAME)
        .password(PASSWORD)
        .accountName(ACCOUNT_NAME)
        .companyName(COMPANY_NAME)
        .accounts(Lists.newArrayList(account))
        .build();
  }
}
