package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.sso.OauthSettings;
import software.wings.beans.sso.SSOSettings.SSOSettingsKeys;
import software.wings.beans.sso.SSOType;
import software.wings.dl.WingsPersistence;

import java.util.Arrays;
import java.util.List;

/**
 * @author Vaibhav Tulsyan
 * 24/May/2019
 */

@Singleton
@Slf4j
public class AddWhitelistedDomainsToAccountMigration implements Migration {
  private WingsPersistence wingsPersistence;

  @Inject
  public AddWhitelistedDomainsToAccountMigration(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public void migrate() {
    logger.info("Migrating email filters of SSOSettings that have a non-empty email filter ...");
    try (HIterator<OauthSettings> ssoSettingsHIterator = new HIterator<>(
             wingsPersistence.createQuery(OauthSettings.class).filter(SSOSettingsKeys.type, SSOType.OAUTH).fetch())) {
      while (ssoSettingsHIterator.hasNext()) {
        OauthSettings ssoSettings = ssoSettingsHIterator.next();
        if (isEmpty(ssoSettings.getFilter())) {
          logger.info("Filter associated with SSOSetting {} is empty. Ignoring ...", ssoSettings.getUuid());
          continue;
        }
        Account account = wingsPersistence.get(Account.class, ssoSettings.getAccountId());
        logger.info("Updating SSOSetting with UUID {}", ssoSettings.getUuid());
        try {
          if (null == account) {
            logger.error(
                "Account {} associated with SSOSetting {} is null", ssoSettings.getAccountId(), ssoSettings.getUuid());
            continue;
          }

          String filter = ssoSettings.getFilter();

          List<String> domains = Arrays.asList(filter.split("\\s*,\\s*"));
          if (isEmpty(domains)) {
            logger.info("List of domains for SSOSetting {} is empty. Ignoring ...");
            continue;
          }

          UpdateOperations<Account> operations = wingsPersistence.createUpdateOperations(Account.class);

          logger.info("Adding whitelistedDomains {} to accountId {}", domains, account.getUuid());
          operations.set(AccountKeys.whitelistedDomains, Sets.newHashSet(domains));
          wingsPersistence.update(account, operations);
          logger.info("Added whitelistedDomains {} to accountId {} successfully!", domains, account.getUuid());
        } catch (Exception e) {
          logger.error("Failed to update whitelistedDomains for SSOSetting {}, accountId {}", ssoSettings.getUuid());
        }
      }
    } catch (Exception ex) {
      logger.error(
          "Migration to add whitelistedDomains failed while updating the filter of SSOSettings with email filter.");
    }

    logger.info("Setting email filter as null for remaining accounts");
    try (HIterator<Account> accountHIterator = new HIterator<>(
             wingsPersistence.createQuery(Account.class).field("whitelistedDomains").not().exists().fetch())) {
      while (accountHIterator.hasNext()) {
        Account account = accountHIterator.next();
        try {
          UpdateOperations<Account> operations = wingsPersistence.createUpdateOperations(Account.class);
          logger.info("Adding empty whitelistedDomains to accountId {}", account.getUuid());
          operations.set(AccountKeys.whitelistedDomains, Sets.newHashSet(Lists.newArrayList()));
          wingsPersistence.update(account, operations);
          logger.info("Added empty whitelistedDomains to accountId {} successfully!", account.getUuid());
        } catch (Exception e) {
          logger.error("Failed to add empty whitelistedDomains to accountId {}", account.getUuid());
        }
      }
    } catch (Exception e) {
      logger.error("Migration failed while updating an account that didn't have an email filter before.");
    }
  }
}
