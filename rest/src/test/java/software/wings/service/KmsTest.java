package software.wings.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Base;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 9/29/17.
 */
public class KmsTest extends WingsBaseTest {
  @Inject private KmsService kmsService;
  @Inject private WingsPersistence wingsPersistence;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  final int numOfEncryptedValsForKms = 3;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    Mockito.when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(new KmsDelegateServiceImpl());
    Whitebox.setInternalState(kmsService, "delegateProxyFactory", delegateProxyFactory);
    Whitebox.setInternalState(wingsPersistence, "kmsService", kmsService);
  }

  @Test
  @RealMongo
  public void getKmsConfigGlobal() throws IOException {
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(Base.GLOBAL_ACCOUNT_ID);

    KmsConfig savedConfig = kmsService.getKmsConfig(UUID.randomUUID().toString());
    assertNull(savedConfig);

    kmsService.saveKmsConfig(Base.GLOBAL_ACCOUNT_ID, kmsConfig);

    savedConfig = kmsService.getKmsConfig(UUID.randomUUID().toString());
    kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(Base.GLOBAL_ACCOUNT_ID);
    assertEquals(kmsConfig, savedConfig);
  }

  @Test
  @RealMongo
  public void getKmsConfigForAccount() throws IOException {
    String accountId = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);

    kmsService.saveKmsConfig(kmsConfig.getAccountId(), kmsConfig);

    KmsConfig savedConfig = kmsService.getKmsConfig(kmsConfig.getAccountId());
    kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);
    assertEquals(kmsConfig, savedConfig);
  }

  @Test
  public void localNullEncryption() throws Exception {
    final char[] keyToEncrypt = null;
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt, null);
    assertNull(encryptedData.getEncryptedValue());
    assertFalse(StringUtils.isBlank(encryptedData.getEncryptionKey()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null);
    assertNull(decryptedValue);
  }

  @Test
  public void localEncryption() throws Exception {
    final String keyToEncrypt = UUID.randomUUID().toString();
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt.toCharArray(), null);
    assertNotEquals(keyToEncrypt, new String(encryptedData.getEncryptedValue()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, null);
    assertEquals(keyToEncrypt, new String(decryptedValue));
  }

  @Test
  public void kmsNullEncryption() throws Exception {
    final KmsConfig kmsConfig = getKmsConfig();
    final char[] keyToEncrypt = null;
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt, kmsConfig);
    assertNull(encryptedData.getEncryptedValue());
    assertFalse(StringUtils.isBlank(encryptedData.getEncryptionKey()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, kmsConfig);
    assertNull(decryptedValue);
  }

  @Test
  public void kmsEncryption() throws Exception {
    final KmsConfig kmsConfig = getKmsConfig();
    final String keyToEncrypt = UUID.randomUUID().toString();
    final EncryptedData encryptedData = kmsService.encrypt(keyToEncrypt.toCharArray(), kmsConfig);
    assertNotEquals(keyToEncrypt, new String(encryptedData.getEncryptedValue()));

    final char[] decryptedValue = kmsService.decrypt(encryptedData, kmsConfig);
    assertEquals(keyToEncrypt, new String(decryptedValue));
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
    assertEquals(settingAttribute, savedAttribute);
  }

  @Test
  @RealMongo
  public void kmsEncryptionWhileSaving() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);

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
    assertEquals(settingAttribute, savedAttribute);

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(settingAttribute.getUuid());
    assertEquals(1, query.asList().size());
    assertEquals(kmsConfig.getUuid(), query.asList().get(0).getKmsId());
  }

  @Test
  @RealMongo
  public void kmsEncryptionUpdateObject() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);

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
    assertEquals(settingAttribute, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    ((AppDynamicsConfig) savedAttribute.getValue()).setUsername(UUID.randomUUID().toString());
    ((AppDynamicsConfig) savedAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    wingsPersistence.save(savedAttribute);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(savedAttributeId);
    assertEquals(1, query.asList().size());
    assertEquals(kmsConfig.getUuid(), query.asList().get(0).getKmsId());
  }

  @Test
  @RealMongo
  public void kmsEncryptionUpdateFieldSettingAttribute() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);

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
    assertEquals(settingAttribute, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    String updatedAppId = UUID.randomUUID().toString();
    wingsPersistence.updateField(SettingAttribute.class, savedAttributeId, "appId", updatedAppId);

    SettingAttribute updatedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    assertEquals(updatedAppId, updatedAttribute.getAppId());
    savedAttribute.setAppId(updatedAppId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

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
    assertEquals(updatedAppId, updatedAttribute.getAppId());
    assertEquals(updatedName, updatedAttribute.getName());

    newAppDynamicsConfig.setPassword(newPassWord.toCharArray());
    assertEquals(newAppDynamicsConfig, updatedAttribute.getValue());

    assertEquals(1, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  @RealMongo
  public void kmsEncryptionSaveServiceVariable() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);

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
    assertEquals(serviceVariable, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  @RealMongo
  public void kmsEncryptionUpdateServiceVariable() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);

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
    assertEquals(serviceVariable, savedAttribute);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    String updatedEnvId = UUID.randomUUID().toString();
    wingsPersistence.updateField(ServiceVariable.class, savedAttributeId, "envId", updatedEnvId);

    ServiceVariable updatedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(updatedEnvId, updatedAttribute.getEnvId());
    savedAttribute.setEnvId(updatedEnvId);
    assertEquals(savedAttribute, updatedAttribute);
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());

    updatedEnvId = UUID.randomUUID().toString();
    String updatedName = UUID.randomUUID().toString();
    char[] updatedValue = UUID.randomUUID().toString().toCharArray();

    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("envId", updatedEnvId);
    keyValuePairs.put("value", updatedValue);

    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);
    updatedAttribute = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertEquals(updatedEnvId, updatedAttribute.getEnvId());
    assertEquals(updatedName, updatedAttribute.getName());
    assertEquals(new String(updatedValue), new String(updatedAttribute.getValue()));
    assertEquals(1, wingsPersistence.createQuery(ServiceVariable.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + 1, wingsPersistence.createQuery(EncryptedData.class).asList().size());
  }

  @Test
  @RealMongo
  public void kmsEncryptionDeleteSettingAttribute() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);

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

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes,
        wingsPersistence.createQuery(EncryptedData.class).asList().size());
    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(settingAttributes.get(i));
      assertEquals(
          numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }
  }

  @Test
  @RealMongo
  public void kmsEncryptionDeleteSettingAttributeQueryUuid() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);

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

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes,
        wingsPersistence.createQuery(EncryptedData.class).asList().size());

    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(SettingAttribute.class, settingAttributes.get(i).getUuid());
      assertEquals(
          numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }

    wingsPersistence.save(settingAttributes);
    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes,
        wingsPersistence.createQuery(EncryptedData.class).asList().size());

    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(
          SettingAttribute.class, settingAttributes.get(i).getAppId(), settingAttributes.get(i).getUuid());
      assertEquals(
          numOfSettingAttributes - (i + 1), wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - (i + 1),
          wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }
  }

  @Test
  @RealMongo
  public void kmsEncryptionDeleteSettingAttributeQuery() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    final KmsConfig kmsConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, kmsConfig);

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

    assertEquals(numOfSettingAttributes, wingsPersistence.createQuery(SettingAttribute.class).asList().size());
    assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes,
        wingsPersistence.createQuery(EncryptedData.class).asList().size());

    Set<String> idsToDelete = new HashSet<>();
    idsToDelete.add(settingAttributes.get(0).getUuid());
    idsToDelete.add(settingAttributes.get(1).getUuid());
    Query<SettingAttribute> query =
        wingsPersistence.createQuery(SettingAttribute.class).field(Mapper.ID_KEY).hasAnyOf(idsToDelete);
    for (int i = 0; i < numOfSettingAttributes; i++) {
      wingsPersistence.delete(query);
      assertEquals(numOfSettingAttributes - idsToDelete.size(),
          wingsPersistence.createQuery(SettingAttribute.class).asList().size());
      assertEquals(numOfEncryptedValsForKms + numOfSettingAttributes - idsToDelete.size(),
          wingsPersistence.createQuery(EncryptedData.class).asList().size());
    }
  }

  @Test
  @RealMongo
  public void kmsEncryptionSaveGlobalConfig() throws IOException {
    final String accountId = UUID.randomUUID().toString();
    KmsConfig kmsConfig = getKmsConfig();

    kmsService.saveKmsConfig(Base.GLOBAL_ACCOUNT_ID, kmsConfig);
    assertEquals(1, wingsPersistence.createQuery(KmsConfig.class).asList().size());

    KmsConfig savedKmsConfig = kmsService.getKmsConfig(accountId);
    assertNotNull(savedKmsConfig);

    kmsConfig = getKmsConfig();
    assertEquals(Base.GLOBAL_ACCOUNT_ID, savedKmsConfig.getAccountId());
    assertEquals(new String(kmsConfig.getAccessKey()), new String(savedKmsConfig.getAccessKey()));
    assertEquals(new String(kmsConfig.getSecretKey()), new String(savedKmsConfig.getSecretKey()));
    assertEquals(new String(kmsConfig.getKmsArn()), new String(savedKmsConfig.getKmsArn()));

    KmsConfig encryptedKms = wingsPersistence.getDatastore().createQuery(KmsConfig.class).asList().get(0);

    assertNotEquals(new String(encryptedKms.getAccessKey()), new String(savedKmsConfig.getAccessKey()));
    assertNotEquals(new String(encryptedKms.getSecretKey()), new String(savedKmsConfig.getSecretKey()));
    assertNotEquals(new String(encryptedKms.getKmsArn()), new String(savedKmsConfig.getKmsArn()));
  }

  private KmsConfig getKmsConfig() throws IOException {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    kmsConfig.setAccessKey("AKIAJLEKM45P4PO5QUFQ");
    kmsConfig.setSecretKey("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");
    return kmsConfig;
  }
}
