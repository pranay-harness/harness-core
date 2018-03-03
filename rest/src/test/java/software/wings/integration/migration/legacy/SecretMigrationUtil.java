package software.wings.integration.migration.legacy;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;
import static software.wings.utils.WingsReflectionUtils.getEncryptedFields;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.inject.Inject;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.annotation.Encryptable;
import software.wings.beans.ConfigFile;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.EntityType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.rules.Integration;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

/**
 * Created by rsingh on 10/17/17.
 */
@Integration
@Ignore
public class SecretMigrationUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(SecretMigrationUtil.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private EncryptionService encryptionService;
  @Inject private SecretManager secretManager;
  @Inject private KmsService kmsService;
  @Inject private VaultService vaultService;
  @Inject private FileService fileService;
  @Inject private ConfigService configService;
  @Inject private AppService appService;
  @Mock private DelegateProxyFactory delegateProxyFactory;

  @Before
  public void setUp() {
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class)))
        .thenReturn(new SecretManagementDelegateServiceImpl());
    setInternalState(kmsService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(vaultService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(secretManager, "kmsService", kmsService);
    setInternalState(secretManager, "vaultService", vaultService);
    setInternalState(wingsPersistence, "secretManager", secretManager);
  }

  @Test
  public void migrateInfraMappings() throws Exception {
    List<InfrastructureMapping> infrastructureMappings =
        wingsPersistence.createQuery(InfrastructureMapping.class).asList();

    logger.info("will go through " + infrastructureMappings.size() + " records");

    int updated = 0;
    for (InfrastructureMapping infrastructureMapping : infrastructureMappings) {
      infrastructureMapping.setAccountId(appService.get(infrastructureMapping.getAppId()).getAccountId());

      //      wingsPersistence.save(infrastructureMapping);
      updated++;
    }
    logger.info("Complete. Updated " + updated + " records.");
  }

  @Test
  public void migrateConfigFilesRef() throws Exception {
    List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class).asList();
    int updated = 0;
    for (ConfigFile configFile : configFiles) {
      if (!configFile.isEncrypted()) {
        continue;
      }

      logger.info("Processing " + configFile.getUuid());
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, configFile.getEncryptedFileId());
      Preconditions.checkNotNull(encryptedData, "Did not find reference for " + configFile.getUuid());

      encryptedData.setName(configFile.getFileName());
      encryptedData.setEncryptedValue(configFile.getFileUuid().toCharArray());

      logger.info("setting name of " + encryptedData.getUuid() + "  to " + configFile.getFileName());

      //      wingsPersistence.save(encryptedData);
      updated++;
    }
    logger.info("Complete. Updated " + updated + " records.");
  }

  @Test
  public void migrateServiceVariablesForSecretText() throws Exception {
    List<ServiceVariable> serviceVariables = wingsPersistence.createQuery(ServiceVariable.class).asList();
    int updated = 0;
    for (ServiceVariable serviceVariable : serviceVariables) {
      if (serviceVariable.getType() != Type.ENCRYPTED_TEXT) {
        continue;
      }

      logger.info("Processing " + serviceVariable.getUuid());
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, serviceVariable.getEncryptedValue());
      Preconditions.checkNotNull(encryptedData, "Did not find reference for " + serviceVariable.getUuid());

      if (serviceVariable.getEntityType() == EntityType.SERVICE_TEMPLATE) {
        ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, serviceVariable.getEntityId());
        Preconditions.checkNotNull(serviceTemplate, "can't find service template " + serviceVariable);
        serviceVariable.setEntityId(serviceTemplate.getServiceId());
      }

      String secretTextName = serviceVariable.getName();
      encryptedData.setName(secretTextName);
      encryptedData.setType(SettingVariableTypes.SECRET_TEXT);
      logger.info("setting name of " + encryptedData.getUuid() + "  to " + secretTextName);

      //      wingsPersistence.save(encryptedData);
      updated++;
    }
    logger.info("Complete. Updated " + updated + " records.");
  }

  @Test
  public void migrateParentsOfEncryptedRecords() throws Exception {
    DBCursor encryptedDatas = wingsPersistence.getCollection("encryptedRecords").find();
    logger.info("will go through " + encryptedDatas.size() + " records");

    int updated = 0;
    while (encryptedDatas.hasNext()) {
      DBObject next = encryptedDatas.next();
      String uuId = (String) next.get("_id");
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
      String parentId = (String) next.get("parentId");
      if (isNotBlank(parentId)) {
        encryptedData.addParent(parentId);
      }
      String kmsId = (String) next.get("kmsId");
      SettingVariableTypes type = SettingVariableTypes.valueOf((String) next.get("type"));
      if (isBlank(kmsId) || type == SettingVariableTypes.KMS) {
        encryptedData.setEncryptionType(EncryptionType.LOCAL);
      } else {
        encryptedData.setEncryptionType(EncryptionType.KMS);
      }

      logger.info("going to save " + encryptedData);
      updated++;
      //      wingsPersistence.save(encryptedData);
    }

    logger.info("Complete. Updated " + updated + " records.");
  }

  @Test
  public void migrateRecordsWithNoName() throws Exception {
    List<EncryptedData> encryptedDataRecords = wingsPersistence.createQuery(EncryptedData.class).asList();

    logger.info("will go through " + encryptedDataRecords.size() + " records");

    int updated = 0;
    for (EncryptedData encryptedData : encryptedDataRecords) {
      if (isBlank(encryptedData.getName())) {
        encryptedData.setName(UUID.randomUUID().toString());
        //        wingsPersistence.save(encryptedData);
        updated++;
      }
    }

    logger.info("Complete. Updated " + updated + " records.");
  }

  @Test
  public void migrateSettingsVariable() throws InterruptedException, IllegalAccessException {
    List<SettingAttribute> settingAttributes = wingsPersistence.createQuery(SettingAttribute.class).asList();

    logger.info("will go through " + settingAttributes.size() + " records");

    int changedObject = 0;
    for (SettingAttribute settingAttribute : settingAttributes) {
      SettingValue value = settingAttribute.getValue();

      if (!Encryptable.class.isInstance(value)) {
        logger.info("nothing to do for " + settingAttribute);
        continue;
      }

      Encryptable toMigrate = (Encryptable) value;
      List<Field> encryptedFields = getEncryptedFields(toMigrate.getClass());
      boolean changeCount = false;
      for (Field encryptedField : encryptedFields) {
        encryptedField.setAccessible(true);
        char[] encryptedValue = (char[]) encryptedField.get(toMigrate);

        if (encryptedValue == null) {
          logger.info("This seems like already is the new format, field: " + encryptedField.getName()
              + " uuid: " + settingAttribute.getUuid() + " object " + settingAttribute);
          continue;
        }
        SimpleEncryption simpleEncryption = new SimpleEncryption(toMigrate.getAccountId());
        char[] decryptedValue = simpleEncryption.decryptChars(encryptedValue);

        logger.info("uuid: " + settingAttribute.getUuid());
        logger.info(
            "going to encrypt " + String.valueOf(decryptedValue) + " for object uuid: " + settingAttribute.getUuid());
        encryptedField.set(toMigrate, decryptedValue);

        if (!changeCount) {
          changedObject++;
          changeCount = true;
        }
      }

      // if (changeCount) {
      //   wingsPersistence.save(settingAttribute);
      // }
    }

    logger.info("Complete. Updated " + changedObject + " setting attributes.");
  }

  @Test
  public void migrateServiceVariable() throws InterruptedException, IllegalAccessException {
    List<ServiceVariable> serviceVariables = wingsPersistence.createQuery(ServiceVariable.class).asList();

    logger.info("will go through " + serviceVariables.size() + " records");

    int changedObject = 0;
    for (ServiceVariable serviceVariable : serviceVariables) {
      if (serviceVariable.getType() != Type.ENCRYPTED_TEXT) {
        continue;
      }

      List<Field> encryptedFields = getEncryptedFields(serviceVariable.getClass());
      boolean changeCount = false;
      for (Field encryptedField : encryptedFields) {
        encryptedField.setAccessible(true);
        char[] encryptedValue = (char[]) encryptedField.get(serviceVariable);

        if (encryptedValue == null) {
          logger.info("This seems like already is the new format, field: " + encryptedField.getName()
              + " uuid: " + serviceVariable.getUuid() + " object " + serviceVariable);
          continue;
        }
        SimpleEncryption simpleEncryption = new SimpleEncryption(serviceVariable.getAccountId());
        char[] decryptedValue = simpleEncryption.decryptChars(encryptedValue);

        logger.info(
            "going to encrypt " + String.valueOf(decryptedValue) + " for object uuid: " + serviceVariable.getUuid());
        encryptedField.set(serviceVariable, decryptedValue);

        if (!changeCount) {
          changedObject++;
          changeCount = true;
        }
      }

      // if (changeCount) {
      //   wingsPersistence.save(serviceVariable);
      // }
    }

    logger.info("Complete. Updated " + changedObject + " setting attributes.");
  }

  @Test
  public void migrateConfigFiles() throws InterruptedException, IllegalAccessException, IOException {
    List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class).asList();

    logger.info("will go through " + configFiles.size() + " records");

    int changedObject = 0;
    for (ConfigFile configFile : configFiles) {
      if (!configFile.isEncrypted()) {
        continue;
      }
      File file = new File(Files.createTempDir(), new File(configFile.getRelativeFilePath()).getName());
      fileService.download(configFile.getFileUuid(), file, CONFIGS);
      logger.info("processing " + configFile);
      EncryptionUtils.decrypt(file, configFile.getAccountId());
      logger.info("going to save: " + FileUtils.readFileToString(file, Charset.defaultCharset()));

      //      configService.save(configFile, new BoundedInputStream(new FileInputStream(file)));
      changedObject++;
    }

    logger.info("Complete. Updated " + changedObject + " file attributes.");
  }

  @Test
  public void migrateSMTPConfigs() throws InterruptedException, IllegalAccessException {
    List<SettingAttribute> settingAttributes = wingsPersistence.createQuery(SettingAttribute.class).asList();

    logger.info("will go through " + settingAttributes.size() + " records");

    int changedObject = 0;
    for (SettingAttribute settingAttribute : settingAttributes) {
      SettingValue value = settingAttribute.getValue();
      if (!(value instanceof SmtpConfig)) {
        continue;
      }
      logger.info("Processing " + settingAttribute.getUuid());

      SmtpConfig config = (SmtpConfig) value;
      if (isBlank(config.getEncryptedPassword())) {
        logger.info("-------- Found value to be migrated ----------");
      } else {
        logger.info("All good, continuing");
        continue;
      }

      //      wingsPersistence.save(settingAttribute);
      changedObject++;
    }

    logger.info("Complete. Updated " + changedObject + " setting attributes.");
  }

  //  @Test
  //  public void test() throws InterruptedException, IllegalAccessException {
  //    List<String> ids = asList("5qBdkRoRSMyUDXFk64_t4g", "TsN2TvImTUWRIU-J1tC85A", "XP-HW0H8Sb2nNxUVoB-a6g",
  //    "Z3NSq6fqTwSj3QgC88cwiA", "s_YVIj9ERFm6ab-rZEY5ww", "yYbYQkJuToOOP7Tq1Fvq8w", "MlkJA6LhQkyFaSLAIdBnlQ");
  //    for(String id : ids) {
  //      SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, id);
  //      SettingValue value = settingAttribute.getValue();
  //      if (!Encryptable.class.isInstance(value)) {
  //        logger.info("nothing to do for " + settingAttribute);
  //        continue;
  //      }
  //
  //      List<Field> encryptedFields = getEncryptedFields(value.getClass());
  //      for(Field f : encryptedFields) {
  //        f.setAccessible(true);
  //
  //        SimpleEncryption simpleEncryption = new SimpleEncryption(settingAttribute.getAccountId());
  //        char[] decryptChars = simpleEncryption.decryptChars((char[]) f.get(value));
  //        logger.info("decrypt1: " + String.valueOf(decryptChars));
  //
  //        logger.info("Again: " + String.valueOf(simpleEncryption.decryptChars(decryptChars)));
  //
  //        f.set(value, simpleEncryption.decryptChars(decryptChars));
  //        wingsPersistence.save(settingAttribute);
  //      }
  //    }
  //
  //  }
}
