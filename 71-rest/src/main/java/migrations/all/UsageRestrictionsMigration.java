package migrations.all;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
import software.wings.security.EnvFilter;
import software.wings.security.EnvFilter.FilterType;
import software.wings.security.GenericEntityFilter;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;

import java.util.HashSet;
import java.util.Set;

/**
 * Migration script to transform usage restrictions with no restrictions to all app restrictions for setting attributes,
 * secrets
 * @author rktummala on 05/03/18
 */
public class UsageRestrictionsMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(UsageRestrictionsMigration.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private UsageRestrictionsService usageRestrictionsService;

  @Override
  public void migrate() {
    UsageRestrictions allAppEnvUsageRestrictions = getAllAppEnvUsageRestrictions();
    logger.info("Start - Updating usage restrictions for setting attributes");
    Query<SettingAttribute> settingAttributeQuery =
        wingsPersistence.createQuery(SettingAttribute.class, excludeAuthority);
    try (HIterator<SettingAttribute> records = new HIterator<>(settingAttributeQuery.fetch())) {
      while (records.hasNext()) {
        SettingAttribute settingAttribute = null;
        try {
          settingAttribute = records.next();

          if (usageRestrictionsService.hasNoRestrictions(settingAttribute.getUsageRestrictions())) {
            settingAttribute.setUsageRestrictions(allAppEnvUsageRestrictions);
            final UpdateOperations<SettingAttribute> updateOperations =
                wingsPersistence.createUpdateOperations(SettingAttribute.class);
            updateOperations.set("usageRestrictions", allAppEnvUsageRestrictions);
            wingsPersistence.update(settingAttribute, updateOperations);
          }
        } catch (Exception ex) {
          logger.warn("Error while updating setting attribute: {}",
              settingAttribute != null ? settingAttribute.getName() : "", ex);
        }
      }

      logger.info("Done - Updating usage restrictions for setting attributes");
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception ex) {
      logger.error("Failed - Updating usage restrictions for setting attributes", ex);
    }

    logger.info("Start - Updating usage restrictions for secrets");
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority);
    query.field("type").in(asList(SettingVariableTypes.SECRET_TEXT, SettingVariableTypes.CONFIG_FILE));
    try (HIterator<EncryptedData> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        EncryptedData encryptedData = null;
        try {
          encryptedData = records.next();

          if (usageRestrictionsService.hasNoRestrictions(encryptedData.getUsageRestrictions())) {
            encryptedData.setUsageRestrictions(allAppEnvUsageRestrictions);
            final UpdateOperations<EncryptedData> updateOperations =
                wingsPersistence.createUpdateOperations(EncryptedData.class);
            updateOperations.set("usageRestrictions", allAppEnvUsageRestrictions);
            wingsPersistence.update(encryptedData, updateOperations);
          }
        } catch (Exception ex) {
          logger.warn("Error while updating secret: {}", encryptedData != null ? encryptedData.getName() : "", ex);
        }
      }

      logger.info("Done - Updating usage restrictions for secrets");
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception ex) {
      logger.error("Failed - Updating usage restrictions for secrets", ex);
    }
  }

  private UsageRestrictions getAllAppEnvUsageRestrictions() {
    Set<AppEnvRestriction> appEnvRestrictions = new HashSet<>(2);
    appEnvRestrictions.add(
        AppEnvRestriction.builder()
            .appFilter(GenericEntityFilter.builder().filterType(GenericEntityFilter.FilterType.ALL).build())
            .envFilter(EnvFilter.builder().filterTypes(Sets.newHashSet(FilterType.PROD)).build())
            .build());
    appEnvRestrictions.add(
        AppEnvRestriction.builder()
            .appFilter(GenericEntityFilter.builder().filterType(GenericEntityFilter.FilterType.ALL).build())
            .envFilter(EnvFilter.builder().filterTypes(Sets.newHashSet(FilterType.NON_PROD)).build())
            .build());
    return UsageRestrictions.builder().appEnvRestrictions(appEnvRestrictions).build();
  }
}
