package migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.security.authentication.AuthenticationMechanism;

/**
 * @author Vaibhav Tulsyan
 * 12/Jun/2019
 */
@Slf4j
@Singleton
public class OauthEnabledFieldMigration implements Migration {
  private WingsPersistence wingsPersistence;

  @Inject
  public OauthEnabledFieldMigration(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public void migrate() {
    logger.info("Starting iterating through all accounts ...");
    try (HIterator<Account> accountHIterator =
             new HIterator<>(wingsPersistence.createQuery(Account.class, excludeAuthority).fetch())) {
      while (accountHIterator.hasNext()) {
        Account account = accountHIterator.next();
        AuthenticationMechanism authenticationMechanism = account.getAuthenticationMechanism();
        logger.info("Processing accountId {}", account.getUuid());
        try {
          boolean oauthEnabled = AuthenticationMechanism.OAUTH == authenticationMechanism;
          account.setOauthEnabled(oauthEnabled);
          wingsPersistence.save(account);
          logger.info("Successfully set oauthEnabled={} for accountId {}", oauthEnabled, account.getUuid());
        } catch (Exception e) {
          logger.error(
              "Failed to set oauthEnabled field as true for accountId {} with exception {}.", account.getUuid(), e);
        }
      }
    } catch (Exception e) {
      logger.error("Failure occurred in OauthEnabledFieldMigration with exception {}", e);
    }
    logger.info("OauthEnabledFieldMigration has completed.");
  }
}
