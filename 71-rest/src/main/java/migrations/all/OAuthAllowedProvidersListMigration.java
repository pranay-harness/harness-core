package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOSettings.SSOSettingsKeys;
import software.wings.beans.sso.SSOType;
import software.wings.dl.WingsPersistence;

/**
 * @author Vaibhav Tulsyan
 * 17/May/2019
 * Changes that will be made by this migration:
 *  - Add allowedProviders list. If a user has "GITHUB" in displayName right now, we will add "GITHUB"
 *    to the list allowedProviders
 */
@Slf4j
@Singleton
public class OAuthAllowedProvidersListMigration implements Migration {
  private WingsPersistence wingsPersistence;

  @Inject
  public OAuthAllowedProvidersListMigration(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public void migrate() {
    logger.info("Started OAuthAllowedProvidersListMigration ...");
    try (HIterator<SSOSettings> ssoSettingsHIterator =
             new HIterator<>(wingsPersistence.createQuery(SSOSettings.class)
                                 .filter(SSOSettingsKeys.type, SSOType.OAUTH.name())
                                 .fetch())) {
      while (ssoSettingsHIterator.hasNext()) {
        SSOSettings ssoSettings = ssoSettingsHIterator.next();
        logger.info("Updating SSOSetting with UUID {}", ssoSettings.getUuid());
        String displayNameInUpperCase;
        UpdateOperations<SSOSettings> operations;
        try {
          if (isEmpty(ssoSettings.getDisplayName())) {
            logger.warn("Expected displayName to be non-empty");
            continue;
          }
          displayNameInUpperCase = ssoSettings.getDisplayName().toUpperCase();
          operations = wingsPersistence.createUpdateOperations(SSOSettings.class);
        } catch (Exception e) {
          logger.error("Failed to create Update Operations for SSOSetting with UUID {}", ssoSettings.getUuid());
          continue;
        }
        try {
          logger.info("Setting allowedProviders for SSOSetting with UUID {} to {}", ssoSettings.getUuid(),
              Sets.newHashSet(displayNameInUpperCase));
          operations.set("allowedProviders", Sets.newHashSet(displayNameInUpperCase));
          wingsPersistence.update(ssoSettings, operations);
          logger.info("Completed updating SSOSetting with UUID {}", ssoSettings.getUuid());
        } catch (Exception e) {
          logger.error("Failed to update SSOSetting with UUID {}", ssoSettings.getUuid());
        }
      }
      logger.info("Finished OAuthAllowedProvidersListMigration ...");
    } catch (Exception ex) {
      logger.error("OAuthAllowedProvidersListMigration failed.", ex);
    }
  }
}
