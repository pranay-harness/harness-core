package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.event.handler.impl.MarketoHelper;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.network.Http;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Migration script to register all the existing users of trial accounts as marketo leads
 *
 * @author rktummala on 11/02/18
 */
@Slf4j
public class MarketoLeadDataMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;
  @Inject private LicenseService licenseService;
  @Inject private MarketoConfig marketoConfig;
  @Inject private MarketoHelper marketoHelper;

  @Override
  public void migrate() {
    if (!marketoConfig.isEnabled()) {
      logger.info("MarketoMigration - Marketo config is disabled. skipping...");
      return;
    }

    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(marketoConfig.getUrl())
                            .addConverterFactory(JacksonConverterFactory.create())
                            .client(Http.getUnsafeOkHttpClient(marketoConfig.getUrl()))
                            .build();

    logger.info("MarketoMigration - Start - registering all users of trial accounts as leads");

    String accessToken;
    try {
      accessToken =
          marketoHelper.getAccessToken(marketoConfig.getClientId(), marketoConfig.getClientSecret(), retrofit);
    } catch (IOException e) {
      logger.error("MarketoMigration - Error while getting the access token", e);
      return;
    }
    Query<Account> accountsQuery = wingsPersistence.createQuery(Account.class, excludeAuthority);
    try (HIterator<Account> records = new HIterator<>(accountsQuery.fetch())) {
      while (records.hasNext()) {
        Account account = null;
        try {
          account = records.next();

          if (account == null) {
            continue;
          }

          Account accountWithDecryptedLicenseInfo = licenseService.decryptLicenseInfo(account, false);
          LicenseInfo licenseInfo = accountWithDecryptedLicenseInfo.getLicenseInfo();
          if (licenseInfo == null) {
            continue;
          }

          String accountType = licenseInfo.getAccountType();

          if (!AccountType.isValid(accountType)) {
            logger.error(
                "MarketoMigration - Invalid accountType {} for account {}", accountType, account.getAccountName());
            continue;
          }

          if (!AccountType.TRIAL.equals(accountType)) {
            continue;
          }

          final String accountId = account.getUuid();
          final Account finalAccount = account;
          List<User> usersOfAccount = userService.getUsersOfAccount(accountId);
          usersOfAccount.stream().filter(user -> user.getMarketoLeadId() == 0L).forEach(user -> {
            try {
              marketoHelper.createOrUpdateLead(
                  finalAccount, user.getName(), user.getEmail(), accessToken, null, retrofit, null);
            } catch (IOException e) {
              logger.error("MarketoMigration - Error while registering lead for user {} in account: {}", user.getUuid(),
                  accountId, e);
            } catch (URISyntaxException e) {
              logger.error("MarketoMigration - Error while registering lead for user {} in account: {}", user.getUuid(),
                  accountId, e);
            }
          });

          logger.info("MarketoMigration - Created leads for account {}", account.getAccountName());
        } catch (Exception ex) {
          logger.error("MarketoMigration - Error while updating license info for account: {}",
              account != null ? account.getAccountName() : "", ex);
        }
      }

      logger.info("MarketoMigration - Done - registering all users of trial accounts as leads");
    } catch (Exception ex) {
      logger.error("MarketoMigration - Failed - registering all users of trial accounts as leads", ex);
    }
  }
}
