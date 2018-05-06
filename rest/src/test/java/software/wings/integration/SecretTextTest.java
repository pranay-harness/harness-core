package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.common.Constants.SECRET_MASK;
import static software.wings.service.impl.security.SecretManagerImpl.HARNESS_DEFAULT_SECRET_MANAGER;
import static software.wings.settings.SettingValue.SettingVariableTypes.CONFIG_FILE;
import static software.wings.settings.SettingValue.SettingVariableTypes.SECRET_TEXT;

import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigOverrideType;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.EntityType;
import software.wings.beans.KmsConfig;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.OverrideType;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.User;
import software.wings.beans.UuidAware;
import software.wings.beans.VaultConfig;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.resources.SecretManagementResource;
import software.wings.rules.RealMongo;
import software.wings.security.EncryptionType;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created by rsingh on 11/3/17.
 */
@RunWith(Parameterized.class)
@RealMongo
public class SecretTextTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(SecretTextTest.class);

  private static String VAULT_TOKEN = System.getProperty("vault.token");

  @Parameter public EncryptionType encryptionType;
  @Inject private VaultService vaultService;
  @Inject private KmsService kmsService;
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ConfigService configService;
  @Inject private EncryptionService encryptionService;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private FileService fileService;
  @Inject private SecretManagementResource secretManagementResource;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private SecretManagementDelegateService secretManagementDelegateService;
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
    return asList(new Object[][] {{EncryptionType.LOCAL}, {EncryptionType.KMS}, {EncryptionType.VAULT}});
  }

  @Before
  public void setup() throws IOException {
    initMocks(this);
    appId = generateUuid();
    workflowName = generateUuid();
    envId = generateUuid();
    workflowExecutionId = wingsPersistence.save(
        WorkflowExecutionBuilder.aWorkflowExecution().withName(workflowName).withEnvId(envId).build());
    when(secretManagementDelegateService.encrypt(anyString(), anyObject(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return encrypt((String) args[0], (char[]) args[1], (KmsConfig) args[2]);
    });

    when(secretManagementDelegateService.decrypt(anyObject(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedData) args[0], (KmsConfig) args[1]);
    });

    when(secretManagementDelegateService.encrypt(anyString(), anyString(), anyString(), any(SettingVariableTypes.class),
             any(VaultConfig.class), any(EncryptedData.class)))
        .then(invocation -> {
          Object[] args = invocation.getArguments();
          return encrypt((String) args[0], (String) args[1], (String) args[2], (SettingVariableTypes) args[3],
              (VaultConfig) args[4], (EncryptedData) args[5]);
        });

    when(secretManagementDelegateService.decrypt(anyObject(), any(VaultConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedData) args[0], (VaultConfig) args[1]);
    });

    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(secretManagementDelegateService);
    setInternalState(vaultService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(kmsService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(secretManager, "kmsService", kmsService);
    setInternalState(secretManager, "vaultService", vaultService);
    setInternalState(wingsPersistence, "secretManager", secretManager);
    setInternalState(vaultService, "kmsService", kmsService);
    setInternalState(configService, "secretManager", secretManager);
    setInternalState(encryptionService, "secretManagementDelegateService", secretManagementDelegateService);
    setInternalState(secretManager, "encryptionService", encryptionService);
    setInternalState(secretManagementResource, "secretManager", secretManager);
    wingsPersistence.save(user);
    UserThreadLocal.set(user);

    accountId = generateUuid();
    switch (encryptionType) {
      case LOCAL:
        kmsId = null;
        encryptedBy = HARNESS_DEFAULT_SECRET_MANAGER;
        break;

      case KMS:
        KmsConfig kmsConfig = getKmsConfig();
        kmsId = kmsService.saveKmsConfig(accountId, kmsConfig);
        encryptedBy = kmsConfig.getName();
        break;

      case VAULT:
        VaultConfig vaultConfig = getVaultConfig();
        kmsId = vaultService.saveVaultConfig(accountId, vaultConfig);
        encryptedBy = vaultConfig.getName();
        break;

      default:
        throw new IllegalArgumentException("Invalid type " + encryptionType);
    }
  }

  @Test
  public void saveSecret() throws IllegalAccessException {
    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();
    testSaveSecret(secretName, secretValue, secretId);
  }

  @Test
  public void saveSecretUsingLocalMode() throws IllegalAccessException {
    if (encryptionType != EncryptionType.LOCAL) {
      return;
    }
    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource
            .saveSecretUsingLocalMode(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();
    testSaveSecret(secretName, secretValue, secretId);
  }

  private void testSaveSecret(String secretName, String secretValue, String secretId) throws IllegalAccessException {
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
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
                                                .templateId(generateUuid())
                                                .envId(generateUuid())
                                                .entityType(EntityType.APPLICATION)
                                                .entityId(generateUuid())
                                                .parentServiceVariableId(generateUuid())
                                                .overrideType(OverrideType.ALL)
                                                .instances(Collections.singletonList(generateUuid()))
                                                .expression(generateUuid())
                                                .accountId(accountId)
                                                .name(generateUuid())
                                                .value(secretId.toCharArray())
                                                .encryptedValue(secretId)
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);

    query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
    assertEquals(1, query.count());
    encryptedData = query.get();
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

    List<SecretChangeLog> changeLogs =
        secretManagementResource.getChangeLogs(accountId, secretId, SECRET_TEXT).getResource();
    assertEquals(1, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertEquals(accountId, secretChangeLog.getAccountId());
    assertEquals("Created", secretChangeLog.getDescription());
    assertEquals(secretId, secretChangeLog.getEncryptedDataId());
    assertEquals(userName, secretChangeLog.getUser().getName());
    assertEquals(userEmail, secretChangeLog.getUser().getEmail());

    String newSecretName = generateUuid();
    String newSecretValue = generateUuid();
    secretManagementResource.updateSecret(
        accountId, secretId, SecretText.builder().name(newSecretName).value(newSecretValue).build());

    changeLogs = secretManagementResource.getChangeLogs(accountId, secretId, SECRET_TEXT).getResource();
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

    query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
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
  public void updateSecretRef() {
    String secretName1 = "s1" + generateUuid();
    String secretValue1 = "v2";
    String secretId1 = secretManagementResource
                           .saveSecret(accountId, SecretText.builder().name(secretName1).value(secretValue1).build())
                           .getResource();

    String secretName2 = "s2" + generateUuid();
    String secretValue2 = "v2";
    String secretId2 = secretManagementResource
                           .saveSecret(accountId, SecretText.builder().name(secretName2).value(secretValue2).build())
                           .getResource();

    String secretName3 = "s3" + generateUuid();
    String secretValue3 = "v3";
    String secretId3 = secretManagementResource
                           .saveSecret(accountId, SecretText.builder().name(secretName3).value(secretValue3).build())
                           .getResource();

    final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                .templateId(generateUuid())
                                                .envId(generateUuid())
                                                .entityType(EntityType.APPLICATION)
                                                .entityId(generateUuid())
                                                .parentServiceVariableId(generateUuid())
                                                .overrideType(OverrideType.ALL)
                                                .instances(Collections.singletonList(generateUuid()))
                                                .expression(generateUuid())
                                                .accountId(accountId)
                                                .name("service_var" + generateUuid())
                                                .value(secretId1.toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);

    ServiceVariable savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue1, String.valueOf(savedVariable.getValue()));
    EncryptedData encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName1).get();
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName2).get();
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName3).get();
    assertNull(encryptedData.getParentIds());

    savedVariable.setValue(secretId2.toCharArray());
    wingsPersistence.save(savedVariable);

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue2, String.valueOf(savedVariable.getValue()));

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName1).get();
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName2).get();
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName3).get();
    assertNull(encryptedData.getParentIds());

    String updatedName = "updatedName" + generateUuid();
    String updatedAppId = generateUuid();
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

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName1).get();
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName2).get();
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName3).get();
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    savedVariable.setValue(secretId1.toCharArray());
    serviceVariableService.update(savedVariable);

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue1, String.valueOf(savedVariable.getValue()));

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName1).get();
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName2).get();
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class).filter("name", secretName3).get();
    assertNull(encryptedData.getParentIds());
  }

  @Test
  public void multipleVariableReference() throws IOException, IllegalAccessException {
    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
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
                                                  .templateId(generateUuid())
                                                  .envId(generateUuid())
                                                  .entityType(EntityType.APPLICATION)
                                                  .entityId(generateUuid())
                                                  .parentServiceVariableId(generateUuid())
                                                  .overrideType(OverrideType.ALL)
                                                  .instances(Collections.singletonList(generateUuid()))
                                                  .expression(generateUuid())
                                                  .accountId(accountId)
                                                  .name(generateUuid())
                                                  .value(secretId.toCharArray())
                                                  .type(Type.ENCRYPTED_TEXT)
                                                  .build();

      String savedAttributeId = wingsPersistence.save(serviceVariable);
      variableIds.add(savedAttributeId);

      query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
      assertEquals(1, query.count());
      encryptedData = query.get();
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

      query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
      assertEquals(1, query.count());
      encryptedData = query.get();
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

    query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
    assertEquals(1, query.count());
    encryptedData = query.get();
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
    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
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
                                                  .templateId(generateUuid())
                                                  .envId(generateUuid())
                                                  .entityType(EntityType.APPLICATION)
                                                  .entityId(generateUuid())
                                                  .parentServiceVariableId(generateUuid())
                                                  .overrideType(OverrideType.ALL)
                                                  .instances(Collections.singletonList(generateUuid()))
                                                  .expression(generateUuid())
                                                  .accountId(accountId)
                                                  .name(generateUuid())
                                                  .value(secretId.toCharArray())
                                                  .type(Type.ENCRYPTED_TEXT)
                                                  .build();

      String savedAttributeId = wingsPersistence.save(serviceVariable);
      variableIds.add(savedAttributeId);

      query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
      assertEquals(1, query.count());
      encryptedData = query.get();
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
        secretManagementResource.deleteSecret(accountId, secretId);
        query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
        assertTrue(query.asList().isEmpty());
      } else {
        try {
          secretManagementResource.deleteSecret(accountId, secretId);
          fail("Deleted referenced secret");
        } catch (WingsException e) {
          // expected
        }
        query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
        assertEquals(1, query.count());
      }
      i--;
    }
  }

  @Test
  public void listSecrets() throws IOException, IllegalAccessException {
    int numOfSecrets = 3;
    int numOfVariable = 4;
    int numOfAccess = 3;
    int numOfUpdates = 2;
    List<EncryptedData> secrets = secretManagementResource.listSecrets(accountId, SECRET_TEXT).getResource();
    assertTrue(secrets.isEmpty());
    for (int i = 0; i < numOfSecrets; i++) {
      String secretName = generateUuid();
      String secretValue = generateUuid();
      String secretId = secretManagementResource
                            .saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
                            .getResource();

      for (int j = 0; j < numOfVariable; j++) {
        final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                    .templateId(generateUuid())
                                                    .envId(generateUuid())
                                                    .entityType(EntityType.APPLICATION)
                                                    .entityId(generateUuid())
                                                    .parentServiceVariableId(generateUuid())
                                                    .overrideType(OverrideType.ALL)
                                                    .instances(Collections.singletonList(generateUuid()))
                                                    .expression(generateUuid())
                                                    .accountId(accountId)
                                                    .name(generateUuid())
                                                    .value(secretId.toCharArray())
                                                    .type(Type.ENCRYPTED_TEXT)
                                                    .build();

        wingsPersistence.save(serviceVariable);
        for (int k = 0; k < numOfAccess; k++) {
          secretManager.getEncryptionDetails(serviceVariable, appId, workflowExecutionId);
        }

        for (int l = 0; l < numOfUpdates; l++) {
          secretManagementResource.updateSecret(
              accountId, secretId, SecretText.builder().name(generateUuid()).value(generateUuid()).build());
        }
      }

      secrets = secretManagementResource.listSecrets(accountId, SECRET_TEXT).getResource();
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

    secrets = secretManagementResource.listSecrets(accountId, SECRET_TEXT).getResource();
    for (EncryptedData secret : secrets) {
      assertEquals(numOfVariable, secret.getSetupUsage());
      assertEquals(numOfAccess * numOfVariable, secret.getRunTimeUsage());
      assertEquals(numOfUpdates * numOfVariable + 1, secret.getChangeLog());
    }
  }

  @Test
  public void secretTextUsage() throws IOException, IllegalAccessException {
    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();

    int numOfVariable = 10;
    Set<ServiceVariable> serviceVariables = new HashSet<>();
    for (int i = 0; i < numOfVariable; i++) {
      final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                  .templateId(generateUuid())
                                                  .envId(generateUuid())
                                                  .entityType(EntityType.APPLICATION)
                                                  .entityId(generateUuid())
                                                  .parentServiceVariableId(generateUuid())
                                                  .overrideType(OverrideType.ALL)
                                                  .instances(Collections.singletonList(generateUuid()))
                                                  .expression(generateUuid())
                                                  .accountId(accountId)
                                                  .name(generateUuid())
                                                  .value(secretId.toCharArray())
                                                  .type(Type.ENCRYPTED_TEXT)
                                                  .build();

      wingsPersistence.save(serviceVariable);
      serviceVariable.setValue(SECRET_MASK.toCharArray());
      serviceVariable.setEncryptedBy(encryptedBy);
      serviceVariable.setEncryptionType(encryptionType);
      serviceVariables.add(serviceVariable);

      List<UuidAware> usages = secretManagementResource.getSecretUsage(accountId, secretId).getResource();
      assertEquals(serviceVariables, new HashSet<>(usages));
    }

    Set<ServiceVariable> remainingVariables = new HashSet<>(serviceVariables);
    for (ServiceVariable serviceVariable : serviceVariables) {
      remainingVariables.remove(serviceVariable);
      wingsPersistence.delete(ServiceVariable.class, serviceVariable.getUuid());

      List<UuidAware> usages = secretManagementResource.getSecretUsage(accountId, secretId).getResource();
      assertEquals(remainingVariables, new HashSet<>(usages));
    }

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
    assertEquals(1, query.count());
    EncryptedData encryptedData = query.get();
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
  public void saveAndUpdateFile() throws IOException, IllegalAccessException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);

    String secretName = generateUuid();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId =
        secretManagementResource.saveFile(accountId, secretName, new FileInputStream(fileToSave)).getResource();

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
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
    assertEquals(CONFIG_FILE, encryptedData.getType());

    Service service = Service.builder().name(generateUuid()).appId(appId).build();
    wingsPersistence.save(service);

    ConfigFile configFile = ConfigFile.builder()
                                .templateId(generateUuid())
                                .envId(generateUuid())
                                .entityType(EntityType.SERVICE)
                                .entityId(service.getUuid())
                                .description(generateUuid())
                                .parentConfigFileId(generateUuid())
                                .relativeFilePath(generateUuid())
                                .targetToAllEnv(r.nextBoolean())
                                .defaultVersion(r.nextInt())
                                .envIdVersionMapString(generateUuid())
                                .setAsDefault(r.nextBoolean())
                                .notes(generateUuid())
                                .overridePath(generateUuid())
                                .configOverrideType(ConfigOverrideType.CUSTOM)
                                .configOverrideExpression(generateUuid())
                                .accountId(accountId)
                                .encryptedFileId(secretFileId)
                                .encrypted(true)
                                .build();
    configFile.setAppId(appId);

    String configFileId = configService.save(configFile, null);

    query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
    assertEquals(1, query.count());
    encryptedData = query.get();
    assertEquals(secretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertEquals(encryptionType, encryptedData.getEncryptionType());
    assertEquals(CONFIG_FILE, encryptedData.getType());
    assertEquals(1, encryptedData.getParentIds().size());
    assertTrue(encryptedData.getParentIds().contains(configFileId));

    String encryptedUuid = encryptedData.getUuid();

    File download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));

    List<SecretChangeLog> changeLogs =
        secretManagementResource.getChangeLogs(accountId, secretFileId, CONFIG_FILE).getResource();
    assertEquals(1, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertEquals(accountId, secretChangeLog.getAccountId());
    assertEquals("File uploaded", secretChangeLog.getDescription());
    assertEquals(secretFileId, secretChangeLog.getEncryptedDataId());
    assertEquals(userName, secretChangeLog.getUser().getName());
    assertEquals(userEmail, secretChangeLog.getUser().getEmail());

    String newSecretName = generateUuid();
    File fileToUpdate = new File(getClass().getClassLoader().getResource("./encryption/file_to_update.txt").getFile());
    secretManagementResource.updateFile(accountId, newSecretName, encryptedUuid, new FileInputStream(fileToUpdate));

    query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
    assertEquals(1, query.count());
    encryptedData = query.get();
    assertEquals(newSecretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertEquals(encryptionType, encryptedData.getEncryptionType());
    assertEquals(CONFIG_FILE, encryptedData.getType());
    assertEquals(1, encryptedData.getParentIds().size());
    assertTrue(encryptedData.getParentIds().contains(configFileId));

    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(fileToUpdate, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));

    changeLogs = secretManagementResource.getChangeLogs(accountId, secretFileId, SECRET_TEXT).getResource();
    assertEquals(2, changeLogs.size());

    secretChangeLog = changeLogs.get(0);
    assertEquals(accountId, secretChangeLog.getAccountId());
    assertEquals("Changed Name and File", secretChangeLog.getDescription());
    assertEquals(secretFileId, secretChangeLog.getEncryptedDataId());
    assertEquals(userName, secretChangeLog.getUser().getName());
    assertEquals(userEmail, secretChangeLog.getUser().getEmail());

    secretChangeLog = changeLogs.get(1);
    assertEquals(accountId, secretChangeLog.getAccountId());
    assertEquals("File uploaded", secretChangeLog.getDescription());
    assertEquals(secretFileId, secretChangeLog.getEncryptedDataId());
    assertEquals(userName, secretChangeLog.getUser().getName());
    assertEquals(userEmail, secretChangeLog.getUser().getEmail());

    File newFileToSave =
        new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String newSecretFileId =
        secretManagementResource.saveFile(accountId, secretName, new FileInputStream(fileToSave)).getResource();
    configFile.setEncryptedFileId(newSecretFileId);
    configService.update(configFile, null);

    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(newFileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
  }

  @Test
  public void multipleFileRefrence() throws IOException, IllegalAccessException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);

    String secretName = generateUuid();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId =
        secretManager.saveFile(accountId, secretName, new BoundedInputStream(new FileInputStream(fileToSave)));

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
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
    assertEquals(CONFIG_FILE, encryptedData.getType());
    assertEquals(secretFileId, encryptedData.getUuid());

    int numOfVariable = 10;
    Set<String> variableIds = new HashSet<>();
    for (int i = 0; i < numOfVariable; i++) {
      Service service = Service.builder().name(generateUuid()).appId(appId).build();
      wingsPersistence.save(service);

      ConfigFile configFile = ConfigFile.builder()
                                  .templateId(generateUuid())
                                  .envId(generateUuid())
                                  .entityType(EntityType.SERVICE)
                                  .entityId(service.getUuid())
                                  .description(generateUuid())
                                  .parentConfigFileId(generateUuid())
                                  .relativeFilePath(generateUuid())
                                  .targetToAllEnv(r.nextBoolean())
                                  .defaultVersion(r.nextInt())
                                  .envIdVersionMapString(generateUuid())
                                  .setAsDefault(r.nextBoolean())
                                  .notes(generateUuid())
                                  .overridePath(generateUuid())
                                  .configOverrideType(ConfigOverrideType.CUSTOM)
                                  .configOverrideExpression(generateUuid())
                                  .accountId(accountId)
                                  .encryptedFileId(secretFileId)
                                  .encrypted(true)
                                  .build();
      configFile.setAppId(appId);

      String configFileId = configService.save(configFile, null);
      variableIds.add(configFileId);

      query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
      assertEquals(1, query.count());
      encryptedData = query.get();
      assertEquals(secretName, encryptedData.getName());
      assertNotNull(encryptedData.getEncryptionKey());
      assertNotNull(encryptedData.getEncryptedValue());
      assertEquals(accountId, encryptedData.getAccountId());
      assertTrue(encryptedData.isEnabled());
      assertEquals(kmsId, encryptedData.getKmsId());
      assertEquals(encryptionType, encryptedData.getEncryptionType());
      assertEquals(CONFIG_FILE, encryptedData.getType());
      assertEquals(i + 1, encryptedData.getParentIds().size());
      assertEquals(variableIds, encryptedData.getParentIds());
    }

    Set<String> remainingVariables = new HashSet<>(variableIds);
    int i = numOfVariable - 1;
    for (String variableId : variableIds) {
      remainingVariables.remove(variableId);
      configService.delete(appId, variableId);

      query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
      assertEquals(1, query.count());
      encryptedData = query.get();
      assertEquals(secretName, encryptedData.getName());
      assertNotNull(encryptedData.getEncryptionKey());
      assertNotNull(encryptedData.getEncryptedValue());
      assertEquals(accountId, encryptedData.getAccountId());
      assertTrue(encryptedData.isEnabled());
      assertEquals(kmsId, encryptedData.getKmsId());
      assertEquals(encryptionType, encryptedData.getEncryptionType());
      assertEquals(CONFIG_FILE, encryptedData.getType());

      if (i == 0) {
        assertNull(encryptedData.getParentIds());
      } else {
        assertEquals(i, encryptedData.getParentIds().size());
        assertEquals(remainingVariables, encryptedData.getParentIds());
      }
      i--;
    }

    query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
    assertEquals(1, query.count());
    encryptedData = query.get();
    assertEquals(secretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertEquals(encryptionType, encryptedData.getEncryptionType());
    assertEquals(CONFIG_FILE, encryptedData.getType());
    assertNull(encryptedData.getParentIds());
  }

  @Test
  public void deleteSecretFile() throws IOException, IllegalAccessException, InterruptedException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);

    String secretName = generateUuid();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId =
        secretManager.saveFile(accountId, secretName, new BoundedInputStream(new FileInputStream(fileToSave)));

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
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
    assertEquals(CONFIG_FILE, encryptedData.getType());
    assertEquals(secretFileId, encryptedData.getUuid());

    int numOfVariable = 10;
    Set<String> variableIds = new HashSet<>();
    for (int i = 0; i < numOfVariable; i++) {
      Service service = Service.builder().name(generateUuid()).appId(appId).build();
      wingsPersistence.save(service);

      ConfigFile configFile = ConfigFile.builder()
                                  .templateId(generateUuid())
                                  .envId(generateUuid())
                                  .entityType(EntityType.SERVICE)
                                  .entityId(service.getUuid())
                                  .description(generateUuid())
                                  .parentConfigFileId(generateUuid())
                                  .relativeFilePath(generateUuid())
                                  .targetToAllEnv(r.nextBoolean())
                                  .defaultVersion(r.nextInt())
                                  .envIdVersionMapString(generateUuid())
                                  .setAsDefault(r.nextBoolean())
                                  .notes(generateUuid())
                                  .overridePath(generateUuid())
                                  .configOverrideType(ConfigOverrideType.CUSTOM)
                                  .configOverrideExpression(generateUuid())
                                  .accountId(accountId)
                                  .encryptedFileId(secretFileId)
                                  .encrypted(true)
                                  .build();
      configFile.setAppId(appId);

      String configFileId = configService.save(configFile, null);
      variableIds.add(configFileId);

      query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
      assertEquals(1, query.count());
      encryptedData = query.get();
      assertEquals(secretName, encryptedData.getName());
      assertNotNull(encryptedData.getEncryptionKey());
      assertNotNull(encryptedData.getEncryptedValue());
      assertEquals(accountId, encryptedData.getAccountId());
      assertTrue(encryptedData.isEnabled());
      assertEquals(kmsId, encryptedData.getKmsId());
      assertEquals(encryptionType, encryptedData.getEncryptionType());
      assertEquals(CONFIG_FILE, encryptedData.getType());
      assertEquals(i + 1, encryptedData.getParentIds().size());
      assertEquals(variableIds, encryptedData.getParentIds());
    }

    Set<String> remainingVariables = new HashSet<>(variableIds);
    int i = numOfVariable - 1;
    for (String variableId : variableIds) {
      Thread.sleep(50);

      remainingVariables.remove(variableId);
      configService.delete(appId, variableId);

      if (i == 0) {
        secretManagementResource.deleteSecret(accountId, secretFileId);
        query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
        assertTrue(query.asList().isEmpty());
      } else {
        try {
          secretManagementResource.deleteFile(accountId, secretFileId);
          fail("Deleted referenced secret");
        } catch (WingsException e) {
          // expected
        }
        query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
        assertEquals(1, query.count());
      }
      i--;
    }
  }

  @Test
  public void deleteEncryptedConfigFile() throws IOException, IllegalAccessException, InterruptedException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);

    String secretName = generateUuid();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId =
        secretManager.saveFile(accountId, secretName, new BoundedInputStream(new FileInputStream(fileToSave)));

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
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
    assertEquals(CONFIG_FILE, encryptedData.getType());
    assertEquals(secretFileId, encryptedData.getUuid());

    Service service = Service.builder().name(generateUuid()).appId(appId).build();
    wingsPersistence.save(service);

    ConfigFile configFile = ConfigFile.builder()
                                .templateId(generateUuid())
                                .envId(generateUuid())
                                .entityType(EntityType.SERVICE)
                                .entityId(service.getUuid())
                                .description(generateUuid())
                                .parentConfigFileId(generateUuid())
                                .relativeFilePath(generateUuid())
                                .targetToAllEnv(r.nextBoolean())
                                .defaultVersion(r.nextInt())
                                .envIdVersionMapString(generateUuid())
                                .setAsDefault(r.nextBoolean())
                                .notes(generateUuid())
                                .overridePath(generateUuid())
                                .configOverrideType(ConfigOverrideType.CUSTOM)
                                .configOverrideExpression(generateUuid())
                                .accountId(accountId)
                                .encryptedFileId(secretFileId)
                                .encrypted(true)
                                .build();
    configFile.setAppId(appId);

    String configFileId = configService.save(configFile, null);

    query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
    assertEquals(1, query.count());
    encryptedData = query.get();
    assertEquals(secretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertEquals(encryptionType, encryptedData.getEncryptionType());
    assertEquals(CONFIG_FILE, encryptedData.getType());

    try {
      secretManagementResource.deleteFile(accountId, secretFileId);
      fail("Deleted referenced secret");
    } catch (WingsException e) {
      // expected
    }

    configService.delete(appId, configFileId);
    Thread.sleep(2000);

    secretManagementResource.deleteSecret(accountId, secretFileId);
    query = wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE);
    assertTrue(query.asList().isEmpty());
  }

  @Test
  public void yamlPasswordDecryption() throws IOException, IllegalAccessException {
    final String accountId = generateUuid();
    KmsConfig fromConfig = getKmsConfig();
    kmsService.saveKmsConfig(accountId, fromConfig);

    String password = generateUuid();
    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl(generateUuid())
                                              .username(generateUuid())
                                              .password(password.toCharArray())
                                              .accountname(generateUuid())
                                              .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withValue(appDynamicsConfig)
                                            .withAppId(generateUuid())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(generateUuid())
                                            .withName(generateUuid())
                                            .build();
    wingsPersistence.save(settingAttribute);

    String yamlRef = secretManager.getEncryptedYamlRef(appDynamicsConfig);

    char[] decryptedRef = secretManager.decryptYamlRef(yamlRef);
    assertEquals(password, String.valueOf(decryptedRef));
  }

  @Test
  public void updateLocalToKms() throws IllegalAccessException {
    if (encryptionType != EncryptionType.LOCAL) {
      return;
    }

    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
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

    KmsConfig kmsConfig = getKmsConfig();
    kmsId = kmsService.saveKmsConfig(accountId, kmsConfig);

    final ServiceVariable serviceVariable = ServiceVariable.builder()
                                                .templateId(generateUuid())
                                                .envId(generateUuid())
                                                .entityType(EntityType.APPLICATION)
                                                .entityId(generateUuid())
                                                .parentServiceVariableId(generateUuid())
                                                .overrideType(OverrideType.ALL)
                                                .instances(Collections.singletonList(generateUuid()))
                                                .expression(generateUuid())
                                                .accountId(accountId)
                                                .name(generateUuid())
                                                .value(secretId.toCharArray())
                                                .encryptedValue(secretId)
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    String savedAttributeId = wingsPersistence.save(serviceVariable);

    ServiceVariable savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertNull(savedVariable.getValue());
    assertEquals(secretId, savedVariable.getEncryptedValue());

    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue, String.valueOf(savedVariable.getValue()));

    kmsConfig = getKmsConfig();
    kmsId = kmsService.saveKmsConfig(accountId, kmsConfig);

    String newSecretName = generateUuid();
    String newSecretValue = generateUuid();
    secretManagementResource.updateSecret(
        accountId, secretId, SecretText.builder().name(newSecretName).value(newSecretValue).build());

    query = wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT);
    encryptedDataList = query.asList();
    assertEquals(1, encryptedDataList.size());
    encryptedData = encryptedDataList.get(0);
    assertEquals(newSecretName, encryptedData.getName());
    assertNotNull(encryptedData.getEncryptionKey());
    assertNotNull(encryptedData.getEncryptedValue());
    assertNotNull(encryptedData.getParentIds());
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(savedAttributeId, encryptedData.getParentIds().iterator().next());
    assertEquals(accountId, encryptedData.getAccountId());
    assertTrue(encryptedData.isEnabled());
    assertEquals(kmsId, encryptedData.getKmsId());
    assertNotNull(encryptedData.getKmsId());
    assertEquals(EncryptionType.KMS, encryptedData.getEncryptionType());
    assertEquals(SECRET_TEXT, encryptedData.getType());

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertNull(savedVariable.getValue());
    assertEquals(secretId, savedVariable.getEncryptedValue());

    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(newSecretValue, String.valueOf(savedVariable.getValue()));
  }

  private VaultConfig getVaultConfig() throws IOException {
    return VaultConfig.builder()
        .vaultUrl("http://127.0.0.1:8200")
        .authToken(generateUuid())
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
}
