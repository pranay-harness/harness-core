package software.wings.integration.migration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.utils.WingsReflectionUtils.getEncryptedFields;

import com.google.inject.Inject;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.annotation.Encryptable;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by rsingh on 10/17/17.
 */
@Integration
@Ignore
public class SecretMigrationUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EncryptionService encryptionService;
  @Inject private SecretManager secretManager;
  @Inject private KmsService kmsService;
  @Inject private VaultService vaultService;
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
  public void migrateParentsOfEncryptedRecords() throws Exception {
    DBCursor encryptedDatas = wingsPersistence.getCollection("encryptedRecords").find();
    System.out.println("will go through " + encryptedDatas.size() + " records");

    int updated = 0;
    while (encryptedDatas.hasNext()) {
      DBObject next = encryptedDatas.next();
      String uuId = (String) next.get("_id");
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
      String parentId = (String) next.get("parentId");
      if (!StringUtils.isBlank(parentId)) {
        encryptedData.addParent(parentId);
      }
      String kmsId = (String) next.get("kmsId");
      SettingVariableTypes type = SettingVariableTypes.valueOf((String) next.get("type"));
      if (StringUtils.isBlank(kmsId) || type == SettingVariableTypes.KMS) {
        encryptedData.setEncryptionType(EncryptionType.LOCAL);
      } else {
        encryptedData.setEncryptionType(EncryptionType.KMS);
      }

      System.out.println("going to save " + encryptedData);
      updated++;
      //      wingsPersistence.save(encryptedData);
    }

    System.out.println("Complete. Updated " + updated + " records.");
  }

  @Test
  public void migrateSettingsVariable() throws InterruptedException, IllegalAccessException {
    List<SettingAttribute> settingAttributes = wingsPersistence.createQuery(SettingAttribute.class).asList();

    System.out.println("will go through " + settingAttributes.size() + " records");

    int changedObject = 0;
    for (SettingAttribute settingAttribute : settingAttributes) {
      SettingValue value = settingAttribute.getValue();

      if (!Encryptable.class.isInstance(value)) {
        System.out.println("nothing to do for " + settingAttribute);
        continue;
      }

      Encryptable toMigrate = (Encryptable) value;
      List<Field> encryptedFields = getEncryptedFields(toMigrate.getClass());
      boolean changeCount = false;
      for (Field encryptedField : encryptedFields) {
        encryptedField.setAccessible(true);
        char[] encryptedValue = (char[]) encryptedField.get(toMigrate);

        if (encryptedValue == null) {
          System.out.println("This seems like already is the new format, field: " + encryptedField.getName()
              + " uuid: " + settingAttribute.getUuid() + " object " + settingAttribute);
          continue;
        }
        SimpleEncryption simpleEncryption = new SimpleEncryption(toMigrate.getAccountId());
        char[] decryptedValue = simpleEncryption.decryptChars(encryptedValue);

        System.out.println("uuid: " + settingAttribute.getUuid());
        System.out.println(
            "going to encrypt " + String.valueOf(decryptedValue) + " for object uuid: " + settingAttribute.getUuid());
        encryptedField.set(toMigrate, decryptedValue);

        if (!changeCount) {
          changedObject++;
          changeCount = true;
        }
      }

      if (changeCount) {
        //        wingsPersistence.save(settingAttribute);
      }
    }

    System.out.println("Complete. Updated " + changedObject + " setting attributes.");
  }

  @Test
  public void migrateServiceVariable() throws InterruptedException, IllegalAccessException {
    List<ServiceVariable> serviceVariables = wingsPersistence.createQuery(ServiceVariable.class).asList();

    System.out.println("will go through " + serviceVariables.size() + " records");

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
          System.out.println("This seems like already is the new format, field: " + encryptedField.getName()
              + " uuid: " + serviceVariable.getUuid() + " object " + serviceVariable);
          continue;
        }
        SimpleEncryption simpleEncryption = new SimpleEncryption(serviceVariable.getAccountId());
        char[] decryptedValue = simpleEncryption.decryptChars(encryptedValue);

        System.out.println(
            "going to encrypt " + String.valueOf(decryptedValue) + " for object uuid: " + serviceVariable.getUuid());
        encryptedField.set(serviceVariable, decryptedValue);

        if (!changeCount) {
          changedObject++;
          changeCount = true;
        }
      }

      if (changeCount) {
        //        wingsPersistence.save(serviceVariable);
      }
    }

    System.out.println("Complete. Updated " + changedObject + " setting attributes.");
  }

  //  @Test
  //  public void test() throws InterruptedException, IllegalAccessException {
  //    List<String> ids = Arrays.asList("5qBdkRoRSMyUDXFk64_t4g", "TsN2TvImTUWRIU-J1tC85A", "XP-HW0H8Sb2nNxUVoB-a6g",
  //    "Z3NSq6fqTwSj3QgC88cwiA", "s_YVIj9ERFm6ab-rZEY5ww", "yYbYQkJuToOOP7Tq1Fvq8w", "MlkJA6LhQkyFaSLAIdBnlQ");
  //    for(String id : ids) {
  //      SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, id);
  //      SettingValue value = settingAttribute.getValue();
  //      if (!Encryptable.class.isInstance(value)) {
  //        System.out.println("nothing to do for " + settingAttribute);
  //        continue;
  //      }
  //
  //      List<Field> encryptedFields = getEncryptedFields(value.getClass());
  //      for(Field f : encryptedFields) {
  //        f.setAccessible(true);
  //
  //        SimpleEncryption simpleEncryption = new SimpleEncryption(settingAttribute.getAccountId());
  //        char[] decryptChars = simpleEncryption.decryptChars((char[]) f.get(value));
  //        System.out.println("decrypt1: " + String.valueOf(decryptChars));
  //
  //        System.out.println("Again: " + String.valueOf(simpleEncryption.decryptChars(decryptChars)));
  //
  //        f.set(value, simpleEncryption.decryptChars(decryptChars));
  //        wingsPersistence.save(settingAttribute);
  //      }
  //    }
  //
  //  }
}
