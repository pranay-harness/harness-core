package migrations.all;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.apache.commons.collections4.CollectionUtils;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.SSOType;
import software.wings.dl.WingsPersistence;

/**
 * Prior this migration for each LDAPSetting we were supporting
 * on User Query filter.
 *
 * Now we will need to support multiple User and Group Query filters.
 *
 * THis Migration will convert single LDAP users & group query to
 * list of user & group queries.
 *
 */
@Slf4j
public class LdapSettingsMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    logger.info("Starting LdapSettingsMigration for all eligible accountIds");

    try (HIterator<LdapSettings> ldapSettingsHIterator =
             new HIterator<>(wingsPersistence.createQuery(LdapSettings.class).filter("type", SSOType.LDAP).fetch())) {
      while (ldapSettingsHIterator.hasNext()) {
        LdapSettings ldapSettings = ldapSettingsHIterator.next();
        if (ldapSettings.getUserSettings() != null && CollectionUtils.isEmpty(ldapSettings.getUserSettingsList())) {
          logger.info("Migrating Ldap UserSettings for accountId: {}", ldapSettings.getAccountId());
          ldapSettings.setUserSettingsList(Lists.newArrayList(ldapSettings.getUserSettings()));
        }
        if (ldapSettings.getGroupSettings() != null && CollectionUtils.isEmpty(ldapSettings.getGroupSettingsList())) {
          logger.info("Migrating Ldap GroupSettings for accountId: {}", ldapSettings.getAccountId());
          ldapSettings.setGroupSettingsList(Lists.newArrayList(ldapSettings.getGroupSettings()));
        }
        wingsPersistence.save(ldapSettings);
      }
    }
  }
}
