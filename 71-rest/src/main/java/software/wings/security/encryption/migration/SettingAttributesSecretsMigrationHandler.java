package software.wings.security.encryption.migration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.persistence.HPersistence.returnNewOptions;
import static io.harness.persistence.UpdatedAtAware.LAST_UPDATED_AT_KEY;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.beans.FeatureName.CONNECTORS_REF_SECRETS_MIGRATION;
import static software.wings.beans.FeatureName.SECRET_PARENTS_MIGRATED;
import static software.wings.beans.SettingAttribute.VALUE_TYPE_KEY;
import static software.wings.service.impl.SettingServiceHelper.ATTRIBUTES_USING_REFERENCES;
import static software.wings.settings.SettingValue.SettingVariableTypes.APM_VERIFICATION;
import static software.wings.settings.SettingValue.SettingVariableTypes.SECRET_TEXT;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.Encryptable;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryption.EncryptionReflectUtils;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.settings.SettingValue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Singleton
public class SettingAttributesSecretsMigrationHandler implements Handler<SettingAttribute> {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private FeatureFlagService featureFlagService;

  public void registerIterators() {
    if (!featureFlagService.isGlobalEnabled(SECRET_PARENTS_MIGRATED)) {
      return;
    }

    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("SettingAttributesSecretsMigrationHandler")
            .poolSize(3)
            .interval(ofSeconds(30))
            .build(),
        SettingAttribute.class,
        MongoPersistenceIterator.<SettingAttribute>builder()
            .clazz(SettingAttribute.class)
            .fieldName(SettingAttributeKeys.nextSecretMigrationIteration)
            .targetInterval(ofMinutes(30))
            .acceptableNoAlertDelay(ofHours(1))
            .handler(this)
            .filterExpander(this ::createQuery)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @VisibleForTesting
  public void createQuery(@NonNull Query<SettingAttribute> query) {
    query.field(VALUE_TYPE_KEY).in(ATTRIBUTES_USING_REFERENCES);
  }

  public void handle(@NonNull SettingAttribute settingAttribute) {
    try {
      if (!featureFlagService.isEnabled(CONNECTORS_REF_SECRETS_MIGRATION, settingAttribute.getAccountId())
          || settingAttribute.isSecretsMigrated()) {
        return;
      }

      boolean isMigrationSuccess;
      if (settingAttribute.getValue().getSettingType() == APM_VERIFICATION) {
        isMigrationSuccess = migrateApmConnector(settingAttribute);
      } else {
        isMigrationSuccess = migrateSecrets(settingAttribute);
      }
      if (isMigrationSuccess) {
        isMigrationSuccess = updateSettingAttribute(settingAttribute);
      }
      if (!isMigrationSuccess) {
        logger.error("Could not migrate the setting attribute {} secrets.", settingAttribute.getUuid());
      }
    } catch (Exception e) {
      logger.error("Could not migrate the setting attribute {} due to error", settingAttribute.getUuid(), e);
    }
  }

  private boolean updateSettingAttribute(@NonNull SettingAttribute settingAttribute) {
    Query<SettingAttribute> query = wingsPersistence.createQuery(SettingAttribute.class)
                                        .field(ID_KEY)
                                        .equal(settingAttribute.getUuid())
                                        .field(LAST_UPDATED_AT_KEY)
                                        .equal(settingAttribute.getLastUpdatedAt());

    UpdateOperations<SettingAttribute> updateOperations =
        wingsPersistence.createUpdateOperations(SettingAttribute.class)
            .set(SettingAttributeKeys.secretsMigrated, Boolean.TRUE);

    return wingsPersistence.findAndModify(query, updateOperations, returnNewOptions) != null;
  }

  private boolean migrateSecrets(@NonNull SettingAttribute settingAttribute) {
    SettingValue settingValue = settingAttribute.getValue();
    if (!(settingValue instanceof EncryptableSetting)) {
      return true;
    }

    List<Field> encryptedFields = EncryptionReflectUtils.getEncryptedFields(settingValue.getClass());
    if (EmptyPredicate.isEmpty(encryptedFields)) {
      return true;
    }
    boolean isMigrationSuccess = true;
    for (Field encryptedField : encryptedFields) {
      Field encryptedRefField = EncryptionReflectUtils.getEncryptedRefField(encryptedField, (Encryptable) settingValue);
      encryptedRefField.setAccessible(true);
      try {
        String secretId = (String) encryptedRefField.get(settingValue);
        if (secretId != null) {
          isMigrationSuccess = isMigrationSuccess
              && migrateSecret(secretId, EncryptionReflectUtils.getEncryptedFieldTag(encryptedField), settingAttribute);
        }
      } catch (IllegalAccessException e) {
        logger.error("Unable to access encrypted field {} for settingAttribute {}", encryptedField.getName(),
            settingAttribute.getUuid(), e);
      }
    }
    return isMigrationSuccess;
  }

  private boolean migrateSecret(
      @NonNull String secretId, @NonNull String encryptedFieldName, @NonNull SettingAttribute settingAttribute) {
    EncryptedData secret = wingsPersistence.get(EncryptedData.class, secretId);
    if (secret == null || secret.getType() == SECRET_TEXT) {
      return true;
    }

    SettingValue settingValue = settingAttribute.getValue();
    String secretName = Optional.ofNullable(settingAttribute.getName()).orElse(UUIDGenerator.generateUuid());
    secretName = secretName.concat("_").concat(settingValue.getType()).concat("_").concat(encryptedFieldName);
    secret.addParent(
        new EncryptedDataParent(settingAttribute.getUuid(), settingValue.getSettingType(), encryptedFieldName));

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .field(ID_KEY)
                                     .equal(secret.getUuid())
                                     .field(LAST_UPDATED_AT_KEY)
                                     .equal(secret.getLastUpdatedAt());

    UpdateOperations<EncryptedData> updateOperations = wingsPersistence.createUpdateOperations(EncryptedData.class)
                                                           .set(EncryptedDataKeys.type, SECRET_TEXT)
                                                           .set(EncryptedDataKeys.name, secretName)
                                                           .set(EncryptedDataKeys.parents, secret.getParents());

    if (settingAttribute.getUsageRestrictions() != null) {
      updateOperations.set(EncryptedDataKeys.usageRestrictions, settingAttribute.getUsageRestrictions());
    }

    return wingsPersistence.findAndModify(query, updateOperations, returnNewOptions) != null;
  }

  private boolean migrateApmConnector(SettingAttribute settingAttribute) {
    logger.info("migrating apm connector {}", settingAttribute.getUuid());
    APMVerificationConfig apmVerificationConfig = (APMVerificationConfig) settingAttribute.getValue();
    final AtomicBoolean isMigrated = new AtomicBoolean(true);
    if (apmVerificationConfig.getHeadersList() != null) {
      apmVerificationConfig.getHeadersList()
          .stream()
          .filter(keyValues
              -> keyValues.isEncrypted() && APMVerificationConfig.MASKED_STRING.equals(keyValues.getValue())
                  && isNotEmpty(keyValues.getEncryptedValue()))
          .forEach(keyValues -> {
            isMigrated.compareAndSet(
                true, migrateSecret(keyValues.getEncryptedValue(), "header." + keyValues.getKey(), settingAttribute));
            keyValues.setValue(keyValues.getEncryptedValue());
          });
    }

    if (apmVerificationConfig.getOptionsList() != null) {
      apmVerificationConfig.getOptionsList()
          .stream()
          .filter(keyValues
              -> keyValues.isEncrypted() && APMVerificationConfig.MASKED_STRING.equals(keyValues.getValue())
                  && isNotEmpty(keyValues.getEncryptedValue()))
          .forEach(keyValues -> {
            isMigrated.compareAndSet(
                true, migrateSecret(keyValues.getEncryptedValue(), "option." + keyValues.getKey(), settingAttribute));
            keyValues.setValue(keyValues.getEncryptedValue());
          });
    }
    wingsPersistence.save(settingAttribute);
    return isMigrated.get();
  }
}