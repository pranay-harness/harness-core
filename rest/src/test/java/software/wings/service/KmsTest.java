package software.wings.service;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.EntityType;
import software.wings.beans.KmsConfig;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.OverrideType;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.rules.RealMongo;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.KmsDelegateServiceImpl;
import software.wings.service.intfc.kms.KmsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 9/29/17.
 */
public class KmsTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(KmsTest.class);
  @Inject private KmsService kmsService;
  @Inject private WingsPersistence wingsPersistence;
  @Mock private DelegateProxyFactory delegateProxyFactory;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    Mockito.when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(new KmsDelegateServiceImpl());
    Whitebox.setInternalState(kmsService, "delegateProxyFactory", delegateProxyFactory);
  }

  @Test
  public void getKmsConfigFromMainConfig() {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setAccessKey(UUID.randomUUID().toString());
    kmsConfig.setSecretKey(UUID.randomUUID().toString());
    kmsConfig.setKmsArn(UUID.randomUUID().toString());
    kmsConfig.setType(SettingVariableTypes.KMS.name());

    final MainConfiguration mainConfiguration = new MainConfiguration();
    mainConfiguration.setPortal(new PortalConfig());
    mainConfiguration.getPortal().setKmsConfig(kmsConfig);

    Whitebox.setInternalState(kmsService, "mainConfiguration", mainConfiguration);

    KmsConfig savedConfig = kmsService.getKmsConfig(kmsConfig.getAccountId());
    Assert.assertEquals(kmsConfig, savedConfig);
  }

  @Test
  @RealMongo
  public void getKmsConfigForAccount() {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setAccessKey(UUID.randomUUID().toString());
    kmsConfig.setSecretKey(UUID.randomUUID().toString());
    kmsConfig.setKmsArn(UUID.randomUUID().toString());
    kmsConfig.setAccountId(UUID.randomUUID().toString());
    kmsConfig.setType(SettingVariableTypes.KMS.name());

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(kmsConfig.getAccountId())
                                            .withValue(kmsConfig)
                                            .build();
    wingsPersistence.save(settingAttribute);

    KmsConfig savedConfig = kmsService.getKmsConfig(kmsConfig.getAccountId());
    Assert.assertEquals(kmsConfig, savedConfig);
  }

  @Test
  public void localNullEncryption() throws Exception {
    final char[] keyToEncrypt = null;
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt, null);
    Assert.assertNull(encryptedData.getEncryptedValue());
    Assert.assertFalse(StringUtils.isBlank(encryptedData.getEncryptionKey()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null);
    Assert.assertNull(decryptedValue);
  }

  @Test
  public void localEncryption() throws Exception {
    final String keyToEncrypt = UUID.randomUUID().toString();
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt.toCharArray(), null);
    Assert.assertNotEquals(keyToEncrypt, new String(encryptedData.getEncryptedValue()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null);
    Assert.assertEquals(keyToEncrypt, new String(decryptedValue));
  }

  @Test
  public void kmsNullEncryption() throws Exception {
    if (StringUtils.isBlank(System.getenv().get(AWS_CREDENTIALS_LOCATION))) {
      logger.error("Not testing kms since the credentials file is not set");
      return;
    }

    File file = new File(System.getenv().get(AWS_CREDENTIALS_LOCATION).trim());
    FileInputStream fileInput = new FileInputStream(file);
    Properties properties = new Properties();
    properties.load(fileInput);
    fileInput.close();

    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    Enumeration enuKeys = properties.keys();
    while (enuKeys.hasMoreElements()) {
      String key = (String) enuKeys.nextElement();
      String value = properties.getProperty(key);
      logger.info(key + ": " + value);
      if (key.equals("aws_access_key_id")) {
        kmsConfig.setAccessKey(value);
      }

      if (key.equals("aws_secret_access_key")) {
        kmsConfig.setSecretKey(value);
      }
    }
    final char[] keyToEncrypt = null;
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt, kmsConfig);
    Assert.assertNull(encryptedData.getEncryptedValue());
    Assert.assertFalse(StringUtils.isBlank(encryptedData.getEncryptionKey()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, kmsConfig);
    Assert.assertNull(decryptedValue);
  }

  @Test
  public void kmsEncryption() throws Exception {
    if (StringUtils.isBlank(System.getenv().get(AWS_CREDENTIALS_LOCATION))) {
      logger.error("Not testing kms since the credentials file is not set");
      return;
    }

    File file = new File(System.getenv().get(AWS_CREDENTIALS_LOCATION).trim());
    FileInputStream fileInput = new FileInputStream(file);
    Properties properties = new Properties();
    properties.load(fileInput);
    fileInput.close();

    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    Enumeration enuKeys = properties.keys();
    while (enuKeys.hasMoreElements()) {
      String key = (String) enuKeys.nextElement();
      String value = properties.getProperty(key);
      logger.info(key + ": " + value);
      if (key.equals("aws_access_key_id")) {
        kmsConfig.setAccessKey(value);
      }

      if (key.equals("aws_secret_access_key")) {
        kmsConfig.setSecretKey(value);
      }
    }
    final String keyToEncrypt = UUID.randomUUID().toString();
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt.toCharArray(), kmsConfig);
    Assert.assertNotEquals(keyToEncrypt, new String(encryptedData.getEncryptedValue()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, kmsConfig);
    Assert.assertEquals(keyToEncrypt, new String(decryptedValue));
  }

  @Test
  @RealMongo
  public void localEncryptionWhileSaving() {
    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(UUID.randomUUID().toString())
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(appDynamicsConfig.getAccountId())
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    Assert.assertEquals(settingAttribute, savedAttribute);
  }

  @Test
  @RealMongo
  public void kmsEncryptionWhileSaving() throws IOException {
    if (StringUtils.isBlank(System.getenv().get(AWS_CREDENTIALS_LOCATION))) {
      logger.error("Not testing kms since the credentials file is not set");
      return;
    }

    File file = new File(System.getenv().get(AWS_CREDENTIALS_LOCATION).trim());
    FileInputStream fileInput = new FileInputStream(file);
    Properties properties = new Properties();
    properties.load(fileInput);
    fileInput.close();

    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    Enumeration enuKeys = properties.keys();
    while (enuKeys.hasMoreElements()) {
      String key = (String) enuKeys.nextElement();
      String value = properties.getProperty(key);
      logger.info(key + ": " + value);
      if (key.equals("aws_access_key_id")) {
        kmsConfig.setAccessKey(value);
      }

      if (key.equals("aws_secret_access_key")) {
        kmsConfig.setSecretKey(value);
      }
    }

    SettingAttribute kmsAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(kmsConfig).build();
    wingsPersistence.save(kmsAttribute);
    Whitebox.setInternalState(wingsPersistence, "kmsService", kmsService);

    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    Assert.assertEquals(settingAttribute, savedAttribute);
  }

  @Test
  @RealMongo
  public void kmsEncryptionUpdateObject() throws IOException {
    if (StringUtils.isBlank(System.getenv().get(AWS_CREDENTIALS_LOCATION))) {
      logger.error("Not testing kms since the credentials file is not set");
      return;
    }

    File file = new File(System.getenv().get(AWS_CREDENTIALS_LOCATION).trim());
    FileInputStream fileInput = new FileInputStream(file);
    Properties properties = new Properties();
    properties.load(fileInput);
    fileInput.close();

    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setAccountId(accountId);
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    Enumeration enuKeys = properties.keys();
    while (enuKeys.hasMoreElements()) {
      String key = (String) enuKeys.nextElement();
      String value = properties.getProperty(key);
      logger.info(key + ": " + value);
      if (key.equals("aws_access_key_id")) {
        kmsConfig.setAccessKey(value);
      }

      if (key.equals("aws_secret_access_key")) {
        kmsConfig.setSecretKey(value);
      }
    }

    SettingAttribute kmsAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(kmsConfig).build();
    wingsPersistence.save(kmsAttribute);
    Whitebox.setInternalState(wingsPersistence, "kmsService", kmsService);

    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    Assert.assertEquals(settingAttribute, savedAttribute);
    Assert.assertEquals(2, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    Assert.assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    ((AppDynamicsConfig) savedAttribute.getValue()).setUsername(UUID.randomUUID().toString());
    ((AppDynamicsConfig) savedAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    wingsPersistence.save(savedAttribute);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    Assert.assertEquals(savedAttribute, updatedAttribute);
    Assert.assertEquals(2, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    Assert.assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  @RealMongo
  public void kmsEncryptionUpdateFieldSettingAttribute() throws IOException {
    if (StringUtils.isBlank(System.getenv().get(AWS_CREDENTIALS_LOCATION))) {
      logger.error("Not testing kms since the credentials file is not set");
      return;
    }

    File file = new File(System.getenv().get(AWS_CREDENTIALS_LOCATION).trim());
    FileInputStream fileInput = new FileInputStream(file);
    Properties properties = new Properties();
    properties.load(fileInput);
    fileInput.close();

    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setAccountId(accountId);
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    Enumeration enuKeys = properties.keys();
    while (enuKeys.hasMoreElements()) {
      String key = (String) enuKeys.nextElement();
      String value = properties.getProperty(key);
      logger.info(key + ": " + value);
      if (key.equals("aws_access_key_id")) {
        kmsConfig.setAccessKey(value);
      }

      if (key.equals("aws_secret_access_key")) {
        kmsConfig.setSecretKey(value);
      }
    }

    SettingAttribute kmsAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(kmsConfig).build();
    wingsPersistence.save(kmsAttribute);
    Whitebox.setInternalState(wingsPersistence, "kmsService", kmsService);

    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    String savedAttributeId = wingsPersistence.save(settingAttribute);
    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    Assert.assertEquals(settingAttribute, savedAttribute);
    Assert.assertEquals(2, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    Assert.assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    String updatedAppId = UUID.randomUUID().toString();
    wingsPersistence.updateField(SettingAttribute.class, savedAttributeId, "appId", updatedAppId);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    Assert.assertEquals(updatedAppId, updatedAttribute.getAppId());
    savedAttribute.setAppId(updatedAppId);
    Assert.assertEquals(savedAttribute, updatedAttribute);
    Assert.assertEquals(2, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    Assert.assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    final String newPassWord = UUID.randomUUID().toString();
    final AppDynamicsConfig newAppDynamicsConfig = AppDynamicsConfig.builder()
                                                       .accountId(accountId)
                                                       .controllerUrl(UUID.randomUUID().toString())
                                                       .username(UUID.randomUUID().toString())
                                                       .password(newPassWord.toCharArray())
                                                       .accountname(UUID.randomUUID().toString())
                                                       .build();

    updatedAppId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("value", newAppDynamicsConfig);

    wingsPersistence.updateFields(SettingAttribute.class, savedAttributeId, keyValuePairs);
    updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    Assert.assertEquals(updatedAppId, updatedAttribute.getAppId());
    Assert.assertEquals(updatedName, updatedAttribute.getName());

    newAppDynamicsConfig.setPassword(newPassWord.toCharArray());
    Assert.assertEquals(newAppDynamicsConfig, updatedAttribute.getValue());

    Assert.assertEquals(2, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    Assert.assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  @RealMongo
  public void kmsEncryptionSaveServiceVariable() throws IOException {
    if (StringUtils.isBlank(System.getenv().get(AWS_CREDENTIALS_LOCATION))) {
      logger.error("Not testing kms since the credentials file is not set");
      return;
    }

    File file = new File(System.getenv().get(AWS_CREDENTIALS_LOCATION).trim());
    FileInputStream fileInput = new FileInputStream(file);
    Properties properties = new Properties();
    properties.load(fileInput);
    fileInput.close();

    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    Enumeration enuKeys = properties.keys();
    while (enuKeys.hasMoreElements()) {
      String key = (String) enuKeys.nextElement();
      String value = properties.getProperty(key);
      logger.info(key + ": " + value);
      if (key.equals("aws_access_key_id")) {
        kmsConfig.setAccessKey(value);
      }

      if (key.equals("aws_secret_access_key")) {
        kmsConfig.setSecretKey(value);
      }
    }

    SettingAttribute kmsAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(kmsConfig).build();
    wingsPersistence.save(kmsAttribute);
    Whitebox.setInternalState(wingsPersistence, "kmsService", kmsService);

    final ServiceVariable serviceVariable = ServiceVariable.Builder.aServiceVariable()
                                                .withTemplateId(UUID.randomUUID().toString())
                                                .withEnvId(UUID.randomUUID().toString())
                                                .withEntityType(EntityType.APPLICATION)
                                                .withEntityId(UUID.randomUUID().toString())
                                                .withParentServiceVariableId(UUID.randomUUID().toString())
                                                .withOverrideType(OverrideType.ALL)
                                                .withInstances(Collections.singletonList(UUID.randomUUID().toString()))
                                                .withExpression(UUID.randomUUID().toString())
                                                .withAccountId(UUID.randomUUID().toString())
                                                .withName(UUID.randomUUID().toString())
                                                .withValue(UUID.randomUUID().toString().toCharArray())
                                                .withType(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    Assert.assertEquals(serviceVariable, savedAttribute);
    Assert.assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    Assert.assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  @RealMongo
  public void kmsEncryptionUpdateServiceVariable() throws IOException {
    if (StringUtils.isBlank(System.getenv().get(AWS_CREDENTIALS_LOCATION))) {
      logger.error("Not testing kms since the credentials file is not set");
      return;
    }

    File file = new File(System.getenv().get(AWS_CREDENTIALS_LOCATION).trim());
    FileInputStream fileInput = new FileInputStream(file);
    Properties properties = new Properties();
    properties.load(fileInput);
    fileInput.close();

    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    Enumeration enuKeys = properties.keys();
    while (enuKeys.hasMoreElements()) {
      String key = (String) enuKeys.nextElement();
      String value = properties.getProperty(key);
      logger.info(key + ": " + value);
      if (key.equals("aws_access_key_id")) {
        kmsConfig.setAccessKey(value);
      }

      if (key.equals("aws_secret_access_key")) {
        kmsConfig.setSecretKey(value);
      }
    }

    SettingAttribute kmsAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(kmsConfig).build();
    wingsPersistence.save(kmsAttribute);
    Whitebox.setInternalState(wingsPersistence, "kmsService", kmsService);

    final ServiceVariable serviceVariable = ServiceVariable.Builder.aServiceVariable()
                                                .withTemplateId(UUID.randomUUID().toString())
                                                .withEnvId(UUID.randomUUID().toString())
                                                .withEntityType(EntityType.APPLICATION)
                                                .withEntityId(UUID.randomUUID().toString())
                                                .withParentServiceVariableId(UUID.randomUUID().toString())
                                                .withOverrideType(OverrideType.ALL)
                                                .withInstances(Collections.singletonList(UUID.randomUUID().toString()))
                                                .withExpression(UUID.randomUUID().toString())
                                                .withAccountId(UUID.randomUUID().toString())
                                                .withName(UUID.randomUUID().toString())
                                                .withValue(UUID.randomUUID().toString().toCharArray())
                                                .withType(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);
    ServiceVariable savedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    Assert.assertEquals(serviceVariable, savedAttribute);
    Assert.assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    Assert.assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    String updatedEnvId = UUID.randomUUID().toString();
    wingsPersistence.updateField(ServiceVariable.class, savedAttributeId, "envId", updatedEnvId);

    ServiceVariable updatedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    Assert.assertEquals(updatedEnvId, updatedAttribute.getEnvId());
    savedAttribute.setEnvId(updatedEnvId);
    Assert.assertEquals(savedAttribute, updatedAttribute);
    Assert.assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    Assert.assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    updatedEnvId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    char[] updatedValue = UUID.randomUUID().toString().toCharArray();

    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("envId", updatedEnvId);
    keyValuePairs.put("value", updatedValue);

    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    updatedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    Assert.assertEquals(updatedEnvId, updatedAttribute.getEnvId());
    Assert.assertEquals(updatedName, updatedAttribute.getName());
    Assert.assertEquals(new String(updatedValue), new String(updatedAttribute.getValue()));
    Assert.assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    Assert.assertEquals(1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  @RealMongo
  public void kmsEncryptionDeleteSettingAttribute() throws IOException {
    if (StringUtils.isBlank(System.getenv().get(AWS_CREDENTIALS_LOCATION))) {
      logger.error("Not testing kms since the credentials file is not set");
      return;
    }

    File file = new File(System.getenv().get(AWS_CREDENTIALS_LOCATION).trim());
    FileInputStream fileInput = new FileInputStream(file);
    Properties properties = new Properties();
    properties.load(fileInput);
    fileInput.close();

    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setAccountId(accountId);
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    Enumeration enuKeys = properties.keys();
    while (enuKeys.hasMoreElements()) {
      String key = (String) enuKeys.nextElement();
      String value = properties.getProperty(key);
      logger.info(key + ": " + value);
      if (key.equals("aws_access_key_id")) {
        kmsConfig.setAccessKey(value);
      }

      if (key.equals("aws_secret_access_key")) {
        kmsConfig.setSecretKey(value);
      }
    }

    SettingAttribute kmsAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(kmsConfig).build();
    wingsPersistence.save(kmsAttribute);
    Whitebox.setInternalState(wingsPersistence, "kmsService", kmsService);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                      .accountId(accountId)
                                                      .controllerUrl(UUID.randomUUID().toString())
                                                      .username(UUID.randomUUID().toString())
                                                      .password(UUID.randomUUID().toString().toCharArray())
                                                      .accountname(UUID.randomUUID().toString())
                                                      .build();

      SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                              .withAccountId(accountId)
                                              .withValue(appDynamicsConfig)
                                              .withAppId(UUID.randomUUID().toString())
                                              .withCategory(Category.CONNECTOR)
                                              .withEnvId(UUID.randomUUID().toString())
                                              .withName(UUID.randomUUID().toString())
                                              .build();

      wingsPersistence.save(settingAttribute);
      settingAttributes.add(settingAttribute);
    }

    Assert.assertEquals(
        numOfSettingAttributes + 1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    Assert.assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).asList().size());
    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(settingAttributes.get(i));
      Assert.assertEquals(
          (numOfSettingAttributes + 1) - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      Assert.assertEquals(
          numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }
  }

  @Test
  @RealMongo
  public void kmsEncryptionDeleteSettingAttributeQueryUuid() throws IOException {
    if (StringUtils.isBlank(System.getenv().get(AWS_CREDENTIALS_LOCATION))) {
      logger.error("Not testing kms since the credentials file is not set");
      return;
    }

    File file = new File(System.getenv().get(AWS_CREDENTIALS_LOCATION).trim());
    FileInputStream fileInput = new FileInputStream(file);
    Properties properties = new Properties();
    properties.load(fileInput);
    fileInput.close();

    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setAccountId(accountId);
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    Enumeration enuKeys = properties.keys();
    while (enuKeys.hasMoreElements()) {
      String key = (String) enuKeys.nextElement();
      String value = properties.getProperty(key);
      logger.info(key + ": " + value);
      if (key.equals("aws_access_key_id")) {
        kmsConfig.setAccessKey(value);
      }

      if (key.equals("aws_secret_access_key")) {
        kmsConfig.setSecretKey(value);
      }
    }

    SettingAttribute kmsAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(kmsConfig).build();
    wingsPersistence.save(kmsAttribute);
    Whitebox.setInternalState(wingsPersistence, "kmsService", kmsService);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                      .accountId(accountId)
                                                      .controllerUrl(UUID.randomUUID().toString())
                                                      .username(UUID.randomUUID().toString())
                                                      .password(UUID.randomUUID().toString().toCharArray())
                                                      .accountname(UUID.randomUUID().toString())
                                                      .build();

      SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                              .withAccountId(accountId)
                                              .withValue(appDynamicsConfig)
                                              .withAppId(UUID.randomUUID().toString())
                                              .withCategory(Category.CONNECTOR)
                                              .withEnvId(UUID.randomUUID().toString())
                                              .withName(UUID.randomUUID().toString())
                                              .build();

      wingsPersistence.save(settingAttribute);
      settingAttributes.add(settingAttribute);
    }

    Assert.assertEquals(
        numOfSettingAttributes + 1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    Assert.assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(SettingAttribute.class, settingAttributes.get(i).getUuid());
      Assert.assertEquals(
          (numOfSettingAttributes + 1) - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      Assert.assertEquals(
          numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }

    wingsPersistence.save(settingAttributes);
    Assert.assertEquals(
        numOfSettingAttributes + 1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    Assert.assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(
          SettingAttribute.class, settingAttributes.get(i).getAppId(), settingAttributes.get(i).getUuid());
      Assert.assertEquals(
          (numOfSettingAttributes + 1) - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      Assert.assertEquals(
          numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }
  }

  @Test
  @RealMongo
  public void kmsEncryptionDeleteSettingAttributeQuery() throws IOException {
    if (StringUtils.isBlank(System.getenv().get(AWS_CREDENTIALS_LOCATION))) {
      logger.error("Not testing kms since the credentials file is not set");
      return;
    }

    File file = new File(System.getenv().get(AWS_CREDENTIALS_LOCATION).trim());
    FileInputStream fileInput = new FileInputStream(file);
    Properties properties = new Properties();
    properties.load(fileInput);
    fileInput.close();

    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setAccountId(accountId);
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    Enumeration enuKeys = properties.keys();
    while (enuKeys.hasMoreElements()) {
      String key = (String) enuKeys.nextElement();
      String value = properties.getProperty(key);
      logger.info(key + ": " + value);
      if (key.equals("aws_access_key_id")) {
        kmsConfig.setAccessKey(value);
      }

      if (key.equals("aws_secret_access_key")) {
        kmsConfig.setSecretKey(value);
      }
    }

    SettingAttribute kmsAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(kmsConfig).build();
    wingsPersistence.save(kmsAttribute);
    Whitebox.setInternalState(wingsPersistence, "kmsService", kmsService);

    int numOfSettingAttributes = 5;
    List<SettingAttribute> settingAttributes = new ArrayList<>();
    for (int i = 0; i < numOfSettingAttributes; i++) {
      final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                      .accountId(accountId)
                                                      .controllerUrl(UUID.randomUUID().toString())
                                                      .username(UUID.randomUUID().toString())
                                                      .password(UUID.randomUUID().toString().toCharArray())
                                                      .accountname(UUID.randomUUID().toString())
                                                      .build();

      SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                              .withAccountId(accountId)
                                              .withValue(appDynamicsConfig)
                                              .withAppId(UUID.randomUUID().toString())
                                              .withCategory(Category.CONNECTOR)
                                              .withEnvId(UUID.randomUUID().toString())
                                              .withName(UUID.randomUUID().toString())
                                              .build();

      wingsPersistence.save(settingAttribute);
      settingAttributes.add(settingAttribute);
    }

    Assert.assertEquals(
        numOfSettingAttributes + 1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    Assert.assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    Set<String> idsToDelete = new HashSet<>();
    idsToDelete.add(settingAttributes.get(0).getUuid());
    idsToDelete.add(settingAttributes.get(1).getUuid());
    Query<SettingAttribute> query =
        wingsPersistence.createQuery(SettingAttribute.class).field(Mapper.ID_KEY).hasAnyOf(idsToDelete);
    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(query);
      Assert.assertEquals((numOfSettingAttributes + 1) - idsToDelete.size(),
          wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      Assert.assertEquals(numOfSettingAttributes - idsToDelete.size(),
          wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }
  }
}
