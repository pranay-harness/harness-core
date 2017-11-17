package software.wings.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.service.impl.security.KmsServiceImpl.SECRET_MASK;
import static software.wings.service.impl.security.SecretManagerImpl.HARNESS_DEFAULT_SECRET_MANAGER;
import static software.wings.settings.SettingValue.SettingVariableTypes.SECRET_TEXT;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.KmsConfig;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.OverrideType;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.User;
import software.wings.beans.VaultConfig;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.impl.security.SecretManagerImpl;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 11/3/17.
 */
@RunWith(Parameterized.class)
public class SecretTextTest extends WingsBaseTest {
  private static String VAULT_TOKEN = System.getProperty("vault.token");

  @Parameter public EncryptionType encryptionType;
  @Inject private VaultService vaultService;
  @Inject private KmsService kmsService;
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ConfigService configService;
  @Inject private EncryptionService encryptionService;
  @Inject private ServiceVariableService serviceVariableService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  private final String userEmail = "rsingh@harness.io";
  private final String userName = "raghu";
  private final User user = User.Builder.anUser().withEmail(userEmail).withName(userName).build();
  private String accountId;
  private String appId;
  private String workflowExecutionId;
  private String workflowName;
  private String kmsId;
  private String envId;
  private String encryptedBy;

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{EncryptionType.LOCAL}, {EncryptionType.VAULT}, {EncryptionType.KMS}});
  }

  @Before
  public void setup() throws IOException {
    initMocks(this);
    appId = UUID.randomUUID().toString();
    workflowName = UUID.randomUUID().toString();
    envId = UUID.randomUUID().toString();
    workflowExecutionId = wingsPersistence.save(
        WorkflowExecutionBuilder.aWorkflowExecution().withName(workflowName).withEnvId(envId).build());
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class)))
        .thenReturn(new SecretManagementDelegateServiceImpl());
    setInternalState(vaultService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(kmsService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(secretManager, "kmsService", kmsService);
    setInternalState(secretManager, "vaultService", vaultService);
    setInternalState(wingsPersistence, "secretManager", secretManager);
    setInternalState(vaultService, "kmsService", kmsService);
    setInternalState(configService, "secretManager", secretManager);
    wingsPersistence.save(user);
    UserThreadLocal.set(user);

    accountId = UUID.randomUUID().toString();
    switch (encryptionType) {
      case LOCAL:
        kmsId = null;
        encryptedBy = HARNESS_DEFAULT_SECRET_MANAGER;
        break;

      case KMS:
        KmsConfig kmsConfig = getKmsConfig();
        kmsId = kmsService.saveKmsConfig(accountId, kmsConfig);
        enableKmsFeatureFlag();
        encryptedBy = kmsConfig.getName();
        break;

      case VAULT:
        VaultConfig vaultConfig = getVaultConfig();
        kmsId = vaultService.saveVaultConfig(accountId, vaultConfig);
        enableKmsFeatureFlag();
        encryptedBy = vaultConfig.getName();
        break;

      default:
        throw new IllegalArgumentException("Invalid type " + encryptionType);
    }
  }

  @Test
  public void saveSecret() throws IOException, IllegalAccessException {
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue);
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
    List<EncryptedData> encryptedDataList = query.asList();
    assertEquals(1, encryptedDataList.size());
    EncryptedData encryptedData = encryptedDataList.get(0);
    assertEquals(secretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertNull(encryptedData.getParentIds());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertEquals(encryptionType, encryptedData.getEncryptionType());
    assertEquals(SECRET_TEXT, encryptedData.getType());

    final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                .templateId(UUID.randomUUID().toString())
                                                .envId(UUID.randomUUID().toString())
                                                .entityType(EntityType.APPLICATION)
                                                .entityId(UUID.randomUUID().toString())
                                                .parentServiceVariableId(UUID.randomUUID().toString())
                                                .overrideType(OverrideType.ALL)
                                                .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                                .expression(UUID.randomUUID().toString())
                                                .accountId(accountId)
                                                .name(UUID.randomUUID().toString())
                                                .value(secretId.toCharArray())
                                                .encryptedValue(secretId)
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);

    query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
    assertEquals(1, query.asList().size());
    encryptedData = query.asList().get(0);
    assertEquals(secretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertEquals(encryptionType, encryptedData.getEncryptionType());
    assertEquals(SECRET_TEXT, encryptedData.getType());
    assertEquals(1, encryptedData.getParentIds().size());
    assertTrue(encryptedData.getParentIds().contains(savedAttributeId));

    ServiceVariable savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertNull(savedVariable.getValue());
    assertEquals(secretId, savedVariable.getEncryptedValue());

    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue, String.valueOf(savedVariable.getValue()));

    List<SecretChangeLog> changeLogs = secretManager.getChangeLogs(secretId, SECRET_TEXT);
    assertEquals(1, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertEquals(accountId, secretChangeLog.getAccountId());
    assertEquals("Created", secretChangeLog.getDescription());
    assertEquals(secretId, secretChangeLog.getEncryptedDataId());
    assertEquals(userName, secretChangeLog.getUser().getName());
    assertEquals(userEmail, secretChangeLog.getUser().getEmail());

    String newSecretName = UUID.randomUUID().toString();
    String newSecretValue = UUID.randomUUID().toString();
    secretManager.updateSecret(accountId, secretId, newSecretName, newSecretValue);

    changeLogs = secretManager.getChangeLogs(secretId, SECRET_TEXT);
    assertEquals(2, changeLogs.size());

    secretChangeLog = changeLogs.get(0);
    assertEquals(accountId, secretChangeLog.getAccountId());
    assertEquals("Changed name & value", secretChangeLog.getDescription());
    assertEquals(secretId, secretChangeLog.getEncryptedDataId());
    assertEquals(userName, secretChangeLog.getUser().getName());
    assertEquals(userEmail, secretChangeLog.getUser().getEmail());

    secretChangeLog = changeLogs.get(1);
    assertEquals(accountId, secretChangeLog.getAccountId());
    assertEquals("Created", secretChangeLog.getDescription());
    assertEquals(secretId, secretChangeLog.getEncryptedDataId());
    assertEquals(userName, secretChangeLog.getUser().getName());
    assertEquals(userEmail, secretChangeLog.getUser().getEmail());

    query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
    encryptedDataList = query.asList();
    assertEquals(1, encryptedDataList.size());
    encryptedData = encryptedDataList.get(0);
    assertEquals(newSecretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertEquals(encryptionType, encryptedData.getEncryptionType());
    assertEquals(SECRET_TEXT, encryptedData.getType());
    assertEquals(1, encryptedData.getParentIds().size());
    assertTrue(encryptedData.getParentIds().contains(savedAttributeId));

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertNull(savedVariable.getValue());
    assertEquals(secretId, savedVariable.getEncryptedValue());

    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(newSecretValue, String.valueOf(savedVariable.getValue()));
  }

  @Test
  public void updateSecretRef() throws IOException, IllegalAccessException {
    String secretName1 = "s1";
    String secretValue1 = "v2";
    String secretId1 = secretManager.saveSecret(accountId, secretName1, secretValue1);

    String secretName2 = "s2";
    String secretValue2 = "v2";
    String secretId2 = secretManager.saveSecret(accountId, secretName2, secretValue2);

    String secretName3 = "s3";
    String secretValue3 = "v3";
    String secretId3 = secretManager.saveSecret(accountId, secretName3, secretValue3);

    final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                .templateId(UUID.randomUUID().toString())
                                                .envId(UUID.randomUUID().toString())
                                                .entityType(EntityType.APPLICATION)
                                                .entityId(UUID.randomUUID().toString())
                                                .parentServiceVariableId(UUID.randomUUID().toString())
                                                .overrideType(OverrideType.ALL)
                                                .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                                .expression(UUID.randomUUID().toString())
                                                .accountId(accountId)
                                                .name("service_var")
                                                .value(secretId1.toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);

    ServiceVariable savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue1, String.valueOf(savedVariable.getValue()));
    EncryptedData encryptedData =
        wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName1).asList().get(0);
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName2).asList().get(0);
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName3).asList().get(0);
    assertNull(encryptedData.getParentIds());

    savedVariable.setValue(secretId2.toCharArray());
    wingsPersistence.save(savedVariable);

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue2, String.valueOf(savedVariable.getValue()));

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName1).asList().get(0);
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName2).asList().get(0);
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName3).asList().get(0);
    assertNull(encryptedData.getParentIds());

    String updatedName = "updatedName";
    String updatedAppId = UUID.randomUUID().toString();
    final Map<String, Object> keyValuePairs = new HashMap<>();
    keyValuePairs.put("name", updatedName);
    keyValuePairs.put("appId", updatedAppId);
    keyValuePairs.put("type", Type.ENCRYPTED_TEXT);
    keyValuePairs.put("value", secretId3.toCharArray());

    wingsPersistence.updateFields(ServiceVariable.class, savedAttributeId, keyValuePairs);

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue3, String.valueOf(savedVariable.getValue()));

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName1).asList().get(0);
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName2).asList().get(0);
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName3).asList().get(0);
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    savedVariable.setValue(secretId1.toCharArray());
    serviceVariableService.update(savedVariable);

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue1, String.valueOf(savedVariable.getValue()));

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName1).asList().get(0);
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName2).asList().get(0);
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).field("name").equal(secretName3).asList().get(0);
    assertNull(encryptedData.getParentIds());
  }

  @Test
  public void multipleVariableRefrence() throws IOException, IllegalAccessException {
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue);
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
    List<EncryptedData> encryptedDataList = query.asList();
    assertEquals(1, encryptedDataList.size());
    EncryptedData encryptedData = encryptedDataList.get(0);
    assertEquals(secretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertNull(encryptedData.getParentIds());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertEquals(encryptionType, encryptedData.getEncryptionType());
    assertEquals(SECRET_TEXT, encryptedData.getType());

    int numOfVariable = 10;
    Set<String> variableIds = new HashSet<>();
    for (int i = 0; i < numOfVariable; i++) {
      final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                  .templateId(UUID.randomUUID().toString())
                                                  .envId(UUID.randomUUID().toString())
                                                  .entityType(EntityType.APPLICATION)
                                                  .entityId(UUID.randomUUID().toString())
                                                  .parentServiceVariableId(UUID.randomUUID().toString())
                                                  .overrideType(OverrideType.ALL)
                                                  .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                                  .expression(UUID.randomUUID().toString())
                                                  .accountId(accountId)
                                                  .name(UUID.randomUUID().toString())
                                                  .value(secretId.toCharArray())
                                                  .type(Type.ENCRYPTED_TEXT)
                                                  .build();

      String savedAttributeId = wingsPersistence.save(serviceVariable);
      variableIds.add(savedAttributeId);

      query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
      assertEquals(1, query.asList().size());
      encryptedData = query.asList().get(0);
      assertEquals(secretName, encryptedData.getName());
      assertNotNull(encryptedData.getEncryptionKey());
      assertNotNull(encryptedData.getEncryptedValue());
      assertEquals(accountId, encryptedData.getAccountId());
      assertTrue(encryptedData.isEnabled());
      assertEquals(kmsId, encryptedData.getKmsId());
      assertEquals(encryptionType, encryptedData.getEncryptionType());
      assertEquals(SECRET_TEXT, encryptedData.getType());
      assertEquals(i + 1, encryptedData.getParentIds().size());
      assertEquals(variableIds, encryptedData.getParentIds());
    }

    Set<String> remainingVariables = new HashSet<>(variableIds);
    int i = numOfVariable - 1;
    for (String variableId : variableIds) {
      remainingVariables.remove(variableId);
      wingsPersistence.delete(ServiceVariable.class, variableId);

      query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
      assertEquals(1, query.asList().size());
      encryptedData = query.asList().get(0);
      assertEquals(secretName, encryptedData.getName());
      assertNotNull(encryptedData.getEncryptionKey());
      assertNotNull(encryptedData.getEncryptedValue());
      assertEquals(accountId, encryptedData.getAccountId());
      assertTrue(encryptedData.isEnabled());
      assertEquals(kmsId, encryptedData.getKmsId());
      assertEquals(encryptionType, encryptedData.getEncryptionType());
      assertEquals(SECRET_TEXT, encryptedData.getType());

      if (i == 0) {
        assertNull(encryptedData.getParentIds());
      } else {
        assertEquals(i, encryptedData.getParentIds().size());
        assertEquals(remainingVariables, encryptedData.getParentIds());
      }
      i--;
    }

    query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
    assertEquals(1, query.asList().size());
    encryptedData = query.asList().get(0);
    assertEquals(secretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertEquals(encryptionType, encryptedData.getEncryptionType());
    assertEquals(SECRET_TEXT, encryptedData.getType());
    assertNull(encryptedData.getParentIds());
  }

  @Test
  public void deleteSecret() throws IOException, IllegalAccessException {
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue);
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
    List<EncryptedData> encryptedDataList = query.asList();
    assertEquals(1, encryptedDataList.size());
    EncryptedData encryptedData = encryptedDataList.get(0);
    assertEquals(secretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertNull(encryptedData.getParentIds());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertEquals(encryptionType, encryptedData.getEncryptionType());
    assertEquals(SECRET_TEXT, encryptedData.getType());

    int numOfVariable = 10;
    Set<String> variableIds = new HashSet<>();
    for (int i = 0; i < numOfVariable; i++) {
      final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                  .templateId(UUID.randomUUID().toString())
                                                  .envId(UUID.randomUUID().toString())
                                                  .entityType(EntityType.APPLICATION)
                                                  .entityId(UUID.randomUUID().toString())
                                                  .parentServiceVariableId(UUID.randomUUID().toString())
                                                  .overrideType(OverrideType.ALL)
                                                  .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                                  .expression(UUID.randomUUID().toString())
                                                  .accountId(accountId)
                                                  .name(UUID.randomUUID().toString())
                                                  .value(secretId.toCharArray())
                                                  .type(Type.ENCRYPTED_TEXT)
                                                  .build();

      String savedAttributeId = wingsPersistence.save(serviceVariable);
      variableIds.add(savedAttributeId);

      query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
      assertEquals(1, query.asList().size());
      encryptedData = query.asList().get(0);
      assertEquals(secretName, encryptedData.getName());
      assertNotNull(encryptedData.getEncryptionKey());
      assertNotNull(encryptedData.getEncryptedValue());
      assertEquals(accountId, encryptedData.getAccountId());
      assertTrue(encryptedData.isEnabled());
      assertEquals(kmsId, encryptedData.getKmsId());
      assertEquals(encryptionType, encryptedData.getEncryptionType());
      assertEquals(SECRET_TEXT, encryptedData.getType());
      assertEquals(i + 1, encryptedData.getParentIds().size());
      assertEquals(variableIds, encryptedData.getParentIds());
    }

    Set<String> remainingVariables = new HashSet<>(variableIds);
    int i = numOfVariable - 1;
    for (String variableId : variableIds) {
      remainingVariables.remove(variableId);
      wingsPersistence.delete(ServiceVariable.class, variableId);

      if (i == 0) {
        secretManager.deleteSecret(accountId, secretId);
        query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
        assertTrue(query.asList().isEmpty());
      } else {
        try {
          secretManager.deleteSecret(accountId, secretId);
          fail("Deleted referenced secret");
        } catch (WingsException e) {
          // expected
        }
        query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
        assertEquals(1, query.asList().size());
      }
      i--;
    }
  }

  @Test
  public void listSecrets() throws IOException, IllegalAccessException {
    int numOfSecrets = 10;
    List<EncryptedData> secrets = secretManager.listSecrets(accountId);
    assertTrue(secrets.isEmpty());
    for (int i = 0; i < numOfSecrets; i++) {
      String secretName = UUID.randomUUID().toString();
      String secretValue = UUID.randomUUID().toString();
      secretManager.saveSecret(accountId, secretName, secretValue);

      secrets = secretManager.listSecrets(accountId);
      assertEquals(i + 1, secrets.size());

      for (EncryptedData secret : secrets) {
        assertEquals(SECRET_MASK, secret.getEncryptionKey());
        assertEquals(SECRET_MASK, String.valueOf(secret.getEncryptedValue()));
        assertEquals(accountId, secret.getAccountId());
        assertTrue(secret.isEnabled());
        assertEquals(kmsId, secret.getKmsId());
        assertEquals(encryptionType, secret.getEncryptionType());
        assertEquals(SECRET_TEXT, secret.getType());
        assertEquals(encryptedBy, secret.getEncryptedBy());
      }
    }
  }

  @Test
  public void secretTextUsage() throws IOException, IllegalAccessException {
    String secretName = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    String secretId = secretManager.saveSecret(accountId, secretName, secretValue);

    int numOfVariable = 10;
    Set<ServiceVariable> serviceVariables = new HashSet<>();
    for (int i = 0; i < numOfVariable; i++) {
      final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                  .templateId(UUID.randomUUID().toString())
                                                  .envId(UUID.randomUUID().toString())
                                                  .entityType(EntityType.APPLICATION)
                                                  .entityId(UUID.randomUUID().toString())
                                                  .parentServiceVariableId(UUID.randomUUID().toString())
                                                  .overrideType(OverrideType.ALL)
                                                  .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                                  .expression(UUID.randomUUID().toString())
                                                  .accountId(accountId)
                                                  .name(UUID.randomUUID().toString())
                                                  .value(secretId.toCharArray())
                                                  .type(Type.ENCRYPTED_TEXT)
                                                  .build();

      wingsPersistence.save(serviceVariable);
      serviceVariable.setValue(SECRET_MASK.toCharArray());
      serviceVariable.setEncryptedBy(encryptedBy);
      serviceVariable.setEncryptionType(encryptionType);
      serviceVariables.add(serviceVariable);

      List<ServiceVariable> usages = secretManager.getSecretTextUsage(accountId, secretId);
      assertEquals(serviceVariables, new HashSet<>(usages));
    }

    Set<ServiceVariable> remainingVariables = new HashSet<>(serviceVariables);
    for (ServiceVariable serviceVariable : serviceVariables) {
      remainingVariables.remove(serviceVariable);
      wingsPersistence.delete(ServiceVariable.class, serviceVariable.getUuid());

      List<ServiceVariable> usages = secretManager.getSecretTextUsage(accountId, secretId);
      assertEquals(remainingVariables, new HashSet<>(usages));
    }

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).field("type").equal(SECRET_TEXT);
    assertEquals(1, query.asList().size());
    EncryptedData encryptedData = query.asList().get(0);
    assertEquals(secretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertEquals(encryptionType, encryptedData.getEncryptionType());
    assertEquals(SECRET_TEXT, encryptedData.getType());
    assertNull(encryptedData.getParentIds());
  }

  private VaultConfig getVaultConfig() throws IOException {
    URL resource = getClass().getClassLoader().getResource("vault_token.txt");

    if (resource == null) {
      System.out.println("reading vault token from environment variable");
    } else {
      System.out.println("reading vault token from file");
      VAULT_TOKEN = FileUtils.readFileToString(new File(resource.getFile()));
    }
    if (VAULT_TOKEN.endsWith("\n")) {
      VAULT_TOKEN = VAULT_TOKEN.replaceAll("\n", "");
    }
    System.out.println("VAULT_TOKEN: " + VAULT_TOKEN);
    return VaultConfig.builder()
        .vaultUrl("http://127.0.0.1:8200")
        .authToken(VAULT_TOKEN)
        .name("myVault")
        .isDefault(true)
        .build();
  }

  private KmsConfig getKmsConfig() {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setName("myKms");
    kmsConfig.setDefault(true);
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    kmsConfig.setAccessKey("AKIAJLEKM45P4PO5QUFQ");
    kmsConfig.setSecretKey("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");
    return kmsConfig;
  }

  private void enableKmsFeatureFlag() {
    FeatureFlag kmsFeatureFlag =
        FeatureFlag.builder().name(FeatureName.KMS.name()).enabled(true).obsolete(false).build();
    wingsPersistence.save(kmsFeatureFlag);
  }
}
