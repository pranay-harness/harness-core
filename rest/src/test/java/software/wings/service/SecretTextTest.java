package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.common.Constants.SECRET_MASK;
import static software.wings.service.impl.security.SecretManagerImpl.HARNESS_DEFAULT_SECRET_MANAGER;
import static software.wings.settings.SettingValue.SettingVariableTypes.CONFIG_FILE;
import static software.wings.settings.SettingValue.SettingVariableTypes.SECRET_TEXT;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.CollectionUtils;
import io.harness.exception.WingsException;
import io.harness.persistence.UuidAware;
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
import software.wings.beans.Base;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigOverrideType;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.KmsConfig;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.OverrideType;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.User;
import software.wings.beans.VaultConfig;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.resources.SecretManagementResource;
import software.wings.resources.ServiceVariableResource;
import software.wings.rules.RealMongo;
import software.wings.security.EncryptionType;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by rsingh on 11/3/17.
 */
@RunWith(Parameterized.class)
public class SecretTextTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(SecretTextTest.class);

  @Parameter public EncryptionType encryptionType;
  @Inject private VaultService vaultService;
  @Inject private KmsService kmsService;
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ConfigService configService;
  @Inject private EncryptionService encryptionService;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private ServiceVariableResource serviceVariableResource;
  @Inject private FileService fileService;
  @Inject private SecretManagementResource secretManagementResource;
  @Inject private AppService appService;
  @Inject private EnvironmentService environmentService;
  @Inject private ServiceResourceService serviceResourceService;
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
    accountId = generateUuid();
    appId = wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUuid()).build());
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
    when(delegateProxyFactory.get(eq(EncryptionService.class), any(SyncTaskContext.class)))
        .thenReturn(encryptionService);
    setInternalState(vaultService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(kmsService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(managerDecryptionService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(secretManager, "kmsService", kmsService);
    setInternalState(secretManager, "vaultService", vaultService);
    setInternalState(wingsPersistence, "secretManager", secretManager);
    setInternalState(vaultService, "kmsService", kmsService);
    setInternalState(configService, "secretManager", secretManager);
    setInternalState(encryptionService, "secretManagementDelegateService", secretManagementDelegateService);
    setInternalState(secretManager, "managerDecryptionService", managerDecryptionService);
    setInternalState(secretManagementResource, "secretManager", secretManager);
    wingsPersistence.save(user);
    UserThreadLocal.set(user);

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

  private String getRandomServiceVariableName() {
    return generateUuid().replaceAll("-", "_");
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
  public void saveAndUpdateSecret() throws IllegalAccessException {
    UsageRestrictions usageRestrictions = UsageRestrictions.builder().isEditable(true).build();
    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource
            .saveSecret(accountId,
                SecretText.builder().name(secretName).value(secretValue).usageRestrictions(usageRestrictions).build())
            .getResource();
    List<SecretChangeLog> changeLogs =
        secretManagementResource.getChangeLogs(accountId, secretId, SECRET_TEXT).getResource();
    assertEquals(1, changeLogs.size());
    SecretChangeLog secretChangeLog = changeLogs.get(0);
    assertEquals(accountId, secretChangeLog.getAccountId());
    assertEquals("Created", secretChangeLog.getDescription());
    assertEquals(secretId, secretChangeLog.getEncryptedDataId());
    assertEquals(userName, secretChangeLog.getUser().getName());
    assertEquals(userEmail, secretChangeLog.getUser().getEmail());
    assertEquals(usageRestrictions, wingsPersistence.get(EncryptedData.class, secretId).getUsageRestrictions());

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
                                                .name(getRandomServiceVariableName())
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

    // check only change in usage restrictions triggers change logs
    secretManagementResource.updateSecret(accountId, secretId,
        SecretText.builder()
            .name(secretName)
            .value(Constants.SECRET_MASK)
            .usageRestrictions(usageRestrictions)
            .build());
    changeLogs = secretManagementResource.getChangeLogs(accountId, secretId, SECRET_TEXT).getResource();
    assertEquals(2, changeLogs.size());
    secretChangeLog = changeLogs.get(0);
    assertEquals(accountId, secretChangeLog.getAccountId());
    assertEquals("Changed name & usage restrictions", secretChangeLog.getDescription());
    secretChangeLog = changeLogs.get(1);
    assertEquals(accountId, secretChangeLog.getAccountId());
    assertEquals("Created", secretChangeLog.getDescription());

    // check just changing the name still gives old value
    String newSecretName = generateUuid();
    secretManagementResource.updateSecret(
        accountId, secretId, SecretText.builder().name(newSecretName).value(Constants.SECRET_MASK).build());

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertNull(savedVariable.getValue());
    assertEquals(secretId, savedVariable.getEncryptedValue());

    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue, String.valueOf(savedVariable.getValue()));

    changeLogs = secretManagementResource.getChangeLogs(accountId, secretId, SECRET_TEXT).getResource();
    assertEquals(3, changeLogs.size());
    assertEquals("Changed name", changeLogs.get(0).getDescription());
    assertEquals("Changed name & usage restrictions", changeLogs.get(1).getDescription());
    assertEquals("Created", changeLogs.get(2).getDescription());

    // change both name and value and test
    newSecretName = generateUuid();
    String newSecretValue = generateUuid();
    secretManagementResource.updateSecret(
        accountId, secretId, SecretText.builder().name(newSecretName).value(newSecretValue).build());
    changeLogs = secretManagementResource.getChangeLogs(accountId, secretId, SECRET_TEXT).getResource();
    assertEquals(4, changeLogs.size());
    assertEquals("Changed name & value", changeLogs.get(0).getDescription());
    assertEquals("Changed name", changeLogs.get(1).getDescription());
    assertEquals("Changed name & usage restrictions", changeLogs.get(2).getDescription());
    assertEquals("Created", changeLogs.get(3).getDescription());

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    assertNull(savedVariable.getValue());
    assertEquals(secretId, savedVariable.getEncryptedValue());

    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(newSecretValue, String.valueOf(savedVariable.getValue()));
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
    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT).filter("accountId", accountId);
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
                                                .name(getRandomServiceVariableName())
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

    query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT).filter("accountId", accountId);
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
  @RealMongo
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
                                                .entityType(EntityType.SERVICE)
                                                .entityId(generateUuid())
                                                .parentServiceVariableId(generateUuid())
                                                .overrideType(OverrideType.ALL)
                                                .instances(Collections.singletonList(generateUuid()))
                                                .expression(generateUuid())
                                                .accountId(accountId)
                                                .name("service_var" + getRandomServiceVariableName())
                                                .value(secretId1.toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();

    serviceVariable.setAppId(
        wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUuid()).build()));
    String savedAttributeId = wingsPersistence.save(serviceVariable);

    ServiceVariable savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue1, String.valueOf(savedVariable.getValue()));
    EncryptedData encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                                      .filter("name", secretName1)
                                      .filter("accountId", accountId)
                                      .get();
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                        .filter("name", secretName2)
                        .filter("accountId", accountId)
                        .get();
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                        .filter("name", secretName3)
                        .filter("accountId", accountId)
                        .get();
    assertNull(encryptedData.getParentIds());

    savedVariable.setValue(secretId2.toCharArray());
    serviceVariableService.update(savedVariable);

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue2, String.valueOf(savedVariable.getValue()));

    encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                        .filter("name", secretName1)
                        .filter("accountId", accountId)
                        .get();
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                        .filter("name", secretName2)
                        .filter("accountId", accountId)
                        .get();
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                        .filter("name", secretName3)
                        .filter("accountId", accountId)
                        .get();
    assertNull(encryptedData.getParentIds());

    String updatedName = "updatedName" + getRandomServiceVariableName();
    String updatedAppId =
        wingsPersistence.save(anApplication().withAccountId(accountId).withName(generateUuid()).build());
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

    encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                        .filter("name", secretName1)
                        .filter("accountId", accountId)
                        .get();
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                        .filter("name", secretName2)
                        .filter("accountId", accountId)
                        .get();
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                        .filter("name", secretName3)
                        .filter("accountId", accountId)
                        .get();
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    savedVariable.setValue(secretId1.toCharArray());
    serviceVariableService.update(savedVariable);

    savedVariable = wingsPersistence.get(ServiceVariable.class, savedAttributeId);
    encryptionService.decrypt(
        savedVariable, secretManager.getEncryptionDetails(savedVariable, appId, workflowExecutionId));
    assertEquals(secretValue1, String.valueOf(savedVariable.getValue()));

    encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                        .filter("name", secretName1)
                        .filter("accountId", accountId)
                        .get();
    assertEquals(1, encryptedData.getParentIds().size());
    assertEquals(serviceVariable.getUuid(), encryptedData.getParentIds().iterator().next());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                        .filter("name", secretName2)
                        .filter("accountId", accountId)
                        .get();
    assertNull(encryptedData.getParentIds());

    encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                        .filter("name", secretName3)
                        .filter("accountId", accountId)
                        .get();
    assertNull(encryptedData.getParentIds());
  }

  @Test
  public void multipleVariableReference() throws IOException, IllegalAccessException {
    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT).filter("accountId", accountId);
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

      query =
          wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT).filter("accountId", accountId);
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

      query =
          wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT).filter("accountId", accountId);
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

    query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT).filter("accountId", accountId);
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
    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT).filter("accountId", accountId);
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

      query =
          wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT).filter("accountId", accountId);
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
        query = wingsPersistence.createQuery(EncryptedData.class)
                    .filter("type", SECRET_TEXT)
                    .filter("accountId", accountId);
        assertTrue(query.asList().isEmpty());
      } else {
        try {
          secretManagementResource.deleteSecret(accountId, secretId);
          fail("Deleted referenced secret");
        } catch (WingsException e) {
          // expected
        }
        query = wingsPersistence.createQuery(EncryptedData.class)
                    .filter("type", SECRET_TEXT)
                    .filter("accountId", accountId);
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
    PageResponse<EncryptedData> pageResponse =
        (PageResponse<EncryptedData>) secretManagementResource
            .listSecrets(accountId, SECRET_TEXT, null, null, aPageRequest().build())
            .getResource();
    List<EncryptedData> secrets = pageResponse.getResponse();

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

      pageResponse = (PageResponse<EncryptedData>) secretManagementResource
                         .listSecrets(accountId, SECRET_TEXT, null, null, aPageRequest().build())
                         .getResource();
      secrets = pageResponse.getResponse();
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

    pageResponse = (PageResponse<EncryptedData>) secretManagementResource
                       .listSecrets(accountId, SECRET_TEXT, null, null, aPageRequest().build())
                       .getResource();
    secrets = pageResponse.getResponse();

    assertFalse(isEmpty(secrets));
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

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT).filter("accountId", accountId);
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
  @RealMongo
  public void saveAndUpdateFile() throws IOException, IllegalAccessException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);

    String secretName = generateUuid();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId =
        secretManagementResource.saveFile(accountId, secretName, new FileInputStream(fileToSave), null).getResource();

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
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

    query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
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
    secretManagementResource.updateFile(
        accountId, newSecretName, null, encryptedUuid, new FileInputStream(fileToUpdate));

    query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
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
        secretManagementResource.saveFile(accountId, secretName, new FileInputStream(fileToSave), null).getResource();
    configFile.setEncryptedFileId(newSecretFileId);
    configService.update(configFile, null);

    download = configService.download(appId, configFileId);
    assertEquals(FileUtils.readFileToString(newFileToSave, Charset.defaultCharset()),
        FileUtils.readFileToString(download, Charset.defaultCharset()));
  }

  @Test
  @RealMongo
  public void multipleFileRefrence() throws IOException, IllegalAccessException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);

    String secretName = generateUuid();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId =
        secretManager.saveFile(accountId, secretName, null, new BoundedInputStream(new FileInputStream(fileToSave)));

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
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

      query =
          wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
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

      query =
          wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
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

    query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
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
  @RealMongo
  public void deleteSecretFile() throws IOException, IllegalAccessException, InterruptedException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);

    String secretName = generateUuid();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId =
        secretManager.saveFile(accountId, secretName, null, new BoundedInputStream(new FileInputStream(fileToSave)));

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
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

      query =
          wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
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
        query = wingsPersistence.createQuery(EncryptedData.class)
                    .filter("type", CONFIG_FILE)
                    .filter("accountId", accountId);
        assertTrue(query.asList().isEmpty());
      } else {
        try {
          secretManagementResource.deleteFile(accountId, secretFileId);
          fail("Deleted referenced secret");
        } catch (WingsException e) {
          // expected
        }
        query = wingsPersistence.createQuery(EncryptedData.class)
                    .filter("type", CONFIG_FILE)
                    .filter("accountId", accountId);
        assertEquals(1, query.count());
      }
      i--;
    }
  }

  @Test
  @RealMongo
  public void deleteEncryptedConfigFile() throws IOException, IllegalAccessException, InterruptedException {
    final long seed = System.currentTimeMillis();
    logger.info("seed: " + seed);
    Random r = new Random(seed);

    String secretName = generateUuid();
    File fileToSave = new File(getClass().getClassLoader().getResource("./encryption/file_to_encrypt.txt").getFile());
    String secretFileId =
        secretManager.saveFile(accountId, secretName, null, new BoundedInputStream(new FileInputStream(fileToSave)));

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
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

    query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
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
    query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", CONFIG_FILE).filter("accountId", accountId);
    assertTrue(query.asList().isEmpty());
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

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT).filter("accountId", accountId);
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

    query =
        wingsPersistence.createQuery(EncryptedData.class).filter("type", SECRET_TEXT).filter("accountId", accountId);
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

  @Test
  @RealMongo
  public void serviceVariableSearchTags() throws IllegalAccessException, InterruptedException {
    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();

    int numOfServices = 2;
    int numOfServiceVariables = 3;

    List<String> serviceNames = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> appIds = new ArrayList<>();
    List<String> serviceVariableIds = new ArrayList<>();
    Set<String> serviceVariableNames = new HashSet<>();
    for (int i = 0; i < numOfServices; i++) {
      Service service = Service.builder().appId(appId).name("service-" + i).build();
      String serviceId = wingsPersistence.save(service);

      for (int j = 0; j < numOfServiceVariables; j++) {
        ServiceVariable serviceVariable = ServiceVariable.builder()
                                              .templateId(UUID.randomUUID().toString())
                                              .envId(Base.GLOBAL_ENV_ID)
                                              .entityType(EntityType.SERVICE)
                                              .entityId(service.getUuid())
                                              .parentServiceVariableId(UUID.randomUUID().toString())
                                              .overrideType(OverrideType.ALL)
                                              .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                              .expression(UUID.randomUUID().toString())
                                              .accountId(accountId)
                                              .name("service_variable_" + i + "_j" + j)
                                              .value(secretId.toCharArray())
                                              .type(Type.ENCRYPTED_TEXT)
                                              .build();
        serviceVariableNames.add(serviceVariable.getName());
        serviceVariableIds.add(serviceVariableResource.save(appId, serviceVariable).getResource().getUuid());
        serviceIds.add(serviceId);
        serviceNames.add(service.getName());
        appIds.add(appId);
      }
    }

    Thread.sleep(2000);
    List<EncryptedData> encryptedDataList = wingsPersistence.createQuery(EncryptedData.class)
                                                .filter("accountId", accountId)
                                                .filter("type", SettingVariableTypes.SECRET_TEXT)
                                                .asList();
    assertEquals(1, encryptedDataList.size());

    EncryptedData encryptedData = encryptedDataList.get(0);
    assertEquals(appIds, encryptedData.getAppIds());
    assertEquals(numOfServices * numOfServiceVariables, encryptedData.getAppIds().size());
    Map<String, AtomicInteger> searchTags = encryptedData.getSearchTags();

    String appName = appService.get(appId).getName();
    assertTrue(searchTags.containsKey(appName));
    assertEquals(numOfServices * numOfServiceVariables, searchTags.get(appName).get());

    assertEquals(serviceIds, encryptedData.getServiceIds());
    serviceNames.forEach(serviceName -> {
      assertTrue(searchTags.containsKey(serviceName));
      assertEquals(numOfServiceVariables, searchTags.get(serviceName).get());
    });

    assertTrue(encryptedData.getServiceVariableIds().containsAll(serviceVariableIds));
    serviceVariableNames.forEach(servicevariableName -> {
      assertTrue(searchTags.containsKey(servicevariableName));
      assertEquals(1, searchTags.get(servicevariableName).get());
    });

    // update and test
    secretName = generateUuid();
    secretValue = generateUuid();
    String newSecretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();

    for (int i = 0; i < numOfServices; i++) {
      for (int j = 0; j < numOfServiceVariables; j++) {
        int serviceVariableIndex = i * numOfServiceVariables + j + 1;
        logger.info("loop i: " + i + " j: " + j + " index: " + serviceVariableIndex);
        String serviceVariableId = serviceVariableIds.get(serviceVariableIndex - 1);
        ServiceVariable serviceVariable = wingsPersistence.get(ServiceVariable.class, serviceVariableId);
        serviceVariable.setValue(newSecretId.toCharArray());
        serviceVariableResource.update(appId, serviceVariableId, serviceVariable);

        EncryptedData oldEncryptedData = wingsPersistence.get(EncryptedData.class, secretId);
        EncryptedData newEncryptedData = wingsPersistence.get(EncryptedData.class, newSecretId);
        if (serviceVariableIndex == numOfServices * numOfServiceVariables) {
          assertNull(oldEncryptedData.getAppIds());
        } else {
          assertEquals(
              numOfServices * numOfServiceVariables - serviceVariableIndex, oldEncryptedData.getAppIds().size());
        }
        assertEquals(serviceVariableIndex, newEncryptedData.getAppIds().size());

        if (serviceVariableIndex != numOfServices * numOfServiceVariables) {
          assertEquals(numOfServices * numOfServiceVariables - serviceVariableIndex,
              oldEncryptedData.getSearchTags().get(appName).get());
          assertEquals(serviceVariableIndex, newEncryptedData.getSearchTags().get(appName).get());
          String serviceId = serviceVariable.getEntityId();
          String serviceName = serviceResourceService.get(appId, serviceId).getName();

          if (j != numOfServiceVariables - 1) {
            assertEquals(numOfServiceVariables - j - 1, oldEncryptedData.getSearchTags().get(serviceName).get());
          } else {
            assertNull(oldEncryptedData.getSearchTags().get(serviceName));
          }
          assertEquals(j + 1, newEncryptedData.getSearchTags().get(serviceName).get());

          assertNull(oldEncryptedData.getSearchTags().get(serviceVariable.getName()));
          assertEquals(1, newEncryptedData.getSearchTags().get(serviceVariable.getName()).get());
        } else {
          assertNull(oldEncryptedData.getSearchTags());
          assertNull(oldEncryptedData.getAppIds());
          assertNull(oldEncryptedData.getServiceIds());
        }
      }
    }

    // delete service variable and test
    for (int i = 0; i < numOfServices; i++) {
      for (int j = 0; j < numOfServiceVariables; j++) {
        int serviceVariableIndex = i * numOfServiceVariables + j + 1;
        logger.info("loop i: " + i + " j: " + j + " index: " + serviceVariableIndex);
        String serviceVariableId = serviceVariableIds.get(serviceVariableIndex - 1);
        ServiceVariable serviceVariable = wingsPersistence.get(ServiceVariable.class, serviceVariableId);
        serviceVariable.setValue(newSecretId.toCharArray());
        serviceVariableResource.delete(appId, serviceVariableId);

        EncryptedData newEncryptedData = wingsPersistence.get(EncryptedData.class, newSecretId);
        if (serviceVariableIndex == numOfServices * numOfServiceVariables) {
          assertNull(newEncryptedData.getAppIds());
        } else {
          assertEquals(
              numOfServices * numOfServiceVariables - serviceVariableIndex, newEncryptedData.getAppIds().size());
        }
        if (serviceVariableIndex != numOfServices * numOfServiceVariables) {
          assertEquals(numOfServices * numOfServiceVariables - serviceVariableIndex,
              newEncryptedData.getSearchTags().get(appName).get());
          String serviceId = serviceVariable.getEntityId();
          String serviceName = serviceResourceService.get(appId, serviceId).getName();

          if (j != numOfServiceVariables - 1) {
            assertEquals(numOfServiceVariables - j - 1, newEncryptedData.getSearchTags().get(serviceName).get());
          }

          assertNull(newEncryptedData.getSearchTags().get(serviceVariable.getName()));
        } else {
          assertNull(newEncryptedData.getSearchTags());
          assertNull(newEncryptedData.getAppIds());
          assertNull(newEncryptedData.getServiceIds());
        }
      }
    }
  }

  @Test
  @RealMongo
  public void serviceVariableTemplateSearchTags() throws IllegalAccessException {
    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();

    int numOfServices = 2;
    int numOfEnvs = 3;
    int numOfServiceVariables = 4;

    List<String> serviceNames = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> appIds = new ArrayList<>();
    List<String> serviceVariableTemplateIds = new ArrayList<>();
    List<String> serviceVariableIds = new ArrayList<>();
    List<String> envIds = new ArrayList<>();
    List<String> envNames = new ArrayList<>();
    Set<String> serviceVariableTemplateNames = new HashSet<>();
    for (int i = 0; i < numOfServices; i++) {
      Service service = Service.builder().appId(appId).name("service-" + i).build();
      String serviceId = wingsPersistence.save(service);

      for (int j = 0; j < numOfEnvs; j++) {
        String envId = wingsPersistence.save(
            Environment.Builder.anEnvironment().withAppId(appId).withName("env-" + i + "-j" + j).build());
        ServiceTemplate serviceTemplate = aServiceTemplate()
                                              .withAppId(appId)
                                              .withServiceId(serviceId)
                                              .withEnvId(envId)
                                              .withName("serviceTemplate-" + i + "-j" + j)
                                              .build();
        String serviceTemplateId = wingsPersistence.save(serviceTemplate);
        envNames.add(serviceTemplate.getName());

        for (int k = 0; k < numOfServiceVariables; k++) {
          ServiceVariable serviceVariable = ServiceVariable.builder()
                                                .templateId(UUID.randomUUID().toString())
                                                .envId(envId)
                                                .entityType(EntityType.SERVICE_TEMPLATE)
                                                .entityId(serviceTemplateId)
                                                .templateId(serviceTemplateId)
                                                .parentServiceVariableId(UUID.randomUUID().toString())
                                                .overrideType(OverrideType.ALL)
                                                .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                                .expression(UUID.randomUUID().toString())
                                                .accountId(accountId)
                                                .name("service_variable_" + i + "_j" + j + "_k" + k)
                                                .value(secretId.toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();
          serviceVariableTemplateNames.add(serviceTemplate.getName());
          serviceVariableIds.add(serviceVariableResource.save(appId, serviceVariable).getResource().getUuid());
          serviceVariableTemplateIds.add(serviceTemplateId);
          serviceIds.add(serviceId);
          serviceNames.add(service.getName());
          appIds.add(appId);
          envIds.add(envId);
        }
      }
    }

    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
    assertEquals(appIds, encryptedData.getAppIds());
    assertEquals(numOfServices * numOfServiceVariables * numOfEnvs, encryptedData.getAppIds().size());
    Map<String, AtomicInteger> searchTags = encryptedData.getSearchTags();

    String appName = appService.get(appId).getName();
    assertTrue(searchTags.containsKey(appName));
    assertEquals(numOfServices * numOfServiceVariables * numOfEnvs, searchTags.get(appName).get());

    assertEquals(envIds, encryptedData.getEnvIds());
    envNames.forEach(envName -> {
      assertTrue(searchTags.containsKey(envName));
      assertEquals(numOfServiceVariables, searchTags.get(envName).get());
    });

    assertEquals(serviceIds, encryptedData.getServiceIds());
    serviceNames.forEach(serviceName -> {
      assertTrue(searchTags.containsKey(serviceName));
      assertEquals(numOfEnvs * numOfServiceVariables, searchTags.get(serviceName).get());
    });

    assertTrue(encryptedData.getServiceVariableIds().containsAll(serviceVariableTemplateIds));
    serviceVariableTemplateNames.forEach(servicevariableName -> {
      assertTrue(searchTags.containsKey(servicevariableName));
      assertEquals(numOfServiceVariables, searchTags.get(servicevariableName).get());
    });

    // update and test
    secretName = generateUuid();
    secretValue = generateUuid();
    String newSecretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();

    for (int i = 0; i < numOfServices; i++) {
      for (int j = 0; j < numOfEnvs; j++) {
        for (int k = 0; k < numOfServiceVariables; k++) {
          int serviceVariableIndex = i * numOfEnvs * numOfServiceVariables + j * numOfServiceVariables + k + 1;
          logger.info("loop i: " + i + " j: " + j + " k: " + k + " index: " + serviceVariableIndex);
          String serviceVariableId = serviceVariableIds.get(serviceVariableIndex - 1);
          ServiceVariable serviceVariable = wingsPersistence.get(ServiceVariable.class, serviceVariableId);
          serviceVariable.setValue(newSecretId.toCharArray());
          serviceVariableResource.update(appId, serviceVariableId, serviceVariable);

          EncryptedData oldEncryptedData = wingsPersistence.get(EncryptedData.class, secretId);
          EncryptedData newEncryptedData = wingsPersistence.get(EncryptedData.class, newSecretId);
          assertEquals(serviceVariableIndex, newEncryptedData.getAppIds().size());

          if (serviceVariableIndex != numOfServices * numOfEnvs * numOfServiceVariables) {
            assertEquals(numOfServices * numOfEnvs * numOfServiceVariables - serviceVariableIndex,
                oldEncryptedData.getAppIds().size());
            assertEquals(numOfServices * numOfEnvs * numOfServiceVariables - serviceVariableIndex,
                oldEncryptedData.getSearchTags().get(appName).get());
            assertEquals(serviceVariableIndex, newEncryptedData.getSearchTags().get(appName).get());
            String serviceTemplateId = serviceVariable.getEntityId();
            ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, serviceTemplateId);
            String serviceId = serviceTemplate.getServiceId();
            String serviceName = serviceResourceService.get(appId, serviceId).getName();
            if (j == numOfEnvs - 1 && k == numOfServiceVariables - 1) {
              assertNull(oldEncryptedData.getSearchTags().get(serviceName));
            } else {
              assertEquals(numOfEnvs * numOfServiceVariables - j * numOfServiceVariables - k - 1,
                  oldEncryptedData.getSearchTags().get(serviceName).get());
            }
            assertEquals(j * numOfServiceVariables + k + 1, newEncryptedData.getSearchTags().get(serviceName).get());

            assertNull(oldEncryptedData.getSearchTags().get(serviceVariable.getName()));
            assertEquals(k + 1, newEncryptedData.getSearchTags().get(serviceTemplate.getName()).get());
          } else {
            assertNull(oldEncryptedData.getSearchTags());
            assertNull(oldEncryptedData.getAppIds());
            assertNull(oldEncryptedData.getServiceIds());
          }
        }
      }
    }

    // delete service variable and test
    for (int i = 0; i < numOfServices; i++) {
      for (int j = 0; j < numOfEnvs; j++) {
        for (int k = 0; k < numOfServiceVariables; k++) {
          int serviceVariableIndex = i * numOfEnvs * numOfServiceVariables + j * numOfServiceVariables + k + 1;
          logger.info("loop i: " + i + " j: " + j + " k: " + k + " index: " + serviceVariableIndex);
          String serviceVariableId = serviceVariableIds.get(serviceVariableIndex - 1);
          ServiceVariable serviceVariable = wingsPersistence.get(ServiceVariable.class, serviceVariableId);
          serviceVariable.setValue(newSecretId.toCharArray());
          serviceVariableResource.delete(appId, serviceVariableId);

          EncryptedData newEncryptedData = wingsPersistence.get(EncryptedData.class, newSecretId);

          if (serviceVariableIndex != numOfServices * numOfEnvs * numOfServiceVariables) {
            assertEquals(numOfServices * numOfEnvs * numOfServiceVariables - serviceVariableIndex,
                newEncryptedData.getAppIds().size());
            assertEquals(numOfServices * numOfEnvs * numOfServiceVariables - serviceVariableIndex,
                newEncryptedData.getSearchTags().get(appName).get());
            String serviceTemplateId = serviceVariable.getEntityId();
            ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, serviceTemplateId);
            String serviceId = serviceTemplate.getServiceId();
            String serviceName = serviceResourceService.get(appId, serviceId).getName();

            if (j == numOfEnvs - 1 && k == numOfServiceVariables - 1) {
              assertNull(newEncryptedData.getSearchTags().get(serviceName));
            } else {
              assertEquals(numOfEnvs * numOfServiceVariables - j * numOfServiceVariables - k - 1,
                  newEncryptedData.getSearchTags().get(serviceName).get());
            }

            assertNull(newEncryptedData.getSearchTags().get(serviceVariable.getName()));
          } else {
            assertNull(newEncryptedData.getSearchTags());
            assertNull(newEncryptedData.getAppIds());
            assertNull(newEncryptedData.getServiceIds());
          }
        }
      }
    }
  }

  @Test
  @RealMongo
  public void serviceVariableEnvironmentSearchTags() throws IllegalAccessException {
    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();

    int numOfEnvs = 3;
    int numOfServiceVariables = 4;

    List<String> appIds = new ArrayList<>();
    List<String> serviceVariableIds = new ArrayList<>();
    List<String> envIds = new ArrayList<>();
    List<String> envNames = new ArrayList<>();

    for (int j = 0; j < numOfEnvs; j++) {
      String envId =
          wingsPersistence.save(Environment.Builder.anEnvironment().withAppId(appId).withName("env-j" + j).build());
      envNames.add("env-j" + j);

      for (int k = 0; k < numOfServiceVariables; k++) {
        ServiceVariable serviceVariable = ServiceVariable.builder()
                                              .templateId(UUID.randomUUID().toString())
                                              .envId(envId)
                                              .entityType(EntityType.ENVIRONMENT)
                                              .entityId(envId)
                                              .parentServiceVariableId(UUID.randomUUID().toString())
                                              .overrideType(OverrideType.ALL)
                                              .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                              .expression(UUID.randomUUID().toString())
                                              .accountId(accountId)
                                              .name("service_variable_j" + j + "_k" + k)
                                              .value(secretId.toCharArray())
                                              .type(Type.ENCRYPTED_TEXT)
                                              .build();
        serviceVariableIds.add(serviceVariableResource.save(appId, serviceVariable).getResource().getUuid());
        appIds.add(appId);
        envIds.add(envId);
      }
    }

    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
    assertEquals(appIds, encryptedData.getAppIds());
    assertEquals(numOfServiceVariables * numOfEnvs, encryptedData.getAppIds().size());
    Map<String, AtomicInteger> searchTags = encryptedData.getSearchTags();

    String appName = appService.get(appId).getName();
    assertTrue(searchTags.containsKey(appName));
    assertEquals(numOfServiceVariables * numOfEnvs, searchTags.get(appName).get());

    assertEquals(envIds, encryptedData.getEnvIds());
    envNames.forEach(envName -> {
      assertTrue(searchTags.containsKey(envName));
      assertEquals(numOfServiceVariables, searchTags.get(envName).get());
    });

    // update and test
    secretName = generateUuid();
    secretValue = generateUuid();
    String newSecretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();

    for (int j = 0; j < numOfEnvs; j++) {
      for (int k = 0; k < numOfServiceVariables; k++) {
        int serviceVariableIndex = j * numOfServiceVariables + k + 1;
        logger.info("loop j: " + j + " k: " + k + " index: " + serviceVariableIndex);
        String serviceVariableId = serviceVariableIds.get(serviceVariableIndex - 1);
        ServiceVariable serviceVariable = wingsPersistence.get(ServiceVariable.class, serviceVariableId);
        serviceVariable.setValue(newSecretId.toCharArray());
        serviceVariableResource.update(appId, serviceVariableId, serviceVariable);

        EncryptedData oldEncryptedData = wingsPersistence.get(EncryptedData.class, secretId);
        EncryptedData newEncryptedData = wingsPersistence.get(EncryptedData.class, newSecretId);
        assertEquals(serviceVariableIndex, newEncryptedData.getAppIds().size());

        if (serviceVariableIndex != numOfEnvs * numOfServiceVariables) {
          assertEquals(numOfEnvs * numOfServiceVariables - serviceVariableIndex, oldEncryptedData.getAppIds().size());
          assertEquals(numOfEnvs * numOfServiceVariables - serviceVariableIndex,
              oldEncryptedData.getSearchTags().get(appName).get());
          assertEquals(serviceVariableIndex, newEncryptedData.getSearchTags().get(appName).get());
          String envId = serviceVariable.getEntityId();
          String envName = environmentService.get(appId, envId).getName();
          if (k == numOfServiceVariables - 1) {
            assertNull(oldEncryptedData.getSearchTags().get(envName));
          } else {
            assertEquals(numOfServiceVariables - k - 1, oldEncryptedData.getSearchTags().get(envName).get());
          }
          assertNull(oldEncryptedData.getSearchTags().get(serviceVariable.getName()));
          assertEquals(k + 1, newEncryptedData.getSearchTags().get(envName).get());
        } else {
          assertNull(oldEncryptedData.getSearchTags());
          assertNull(oldEncryptedData.getAppIds());
          assertNull(oldEncryptedData.getServiceIds());
        }
      }
    }

    // delete service variable and test
    for (int j = 0; j < numOfEnvs; j++) {
      for (int k = 0; k < numOfServiceVariables; k++) {
        int serviceVariableIndex = j * numOfServiceVariables + k + 1;
        logger.info("loop  j: " + j + " k: " + k + " index: " + serviceVariableIndex);
        String serviceVariableId = serviceVariableIds.get(serviceVariableIndex - 1);
        ServiceVariable serviceVariable = wingsPersistence.get(ServiceVariable.class, serviceVariableId);
        serviceVariable.setValue(newSecretId.toCharArray());
        serviceVariableResource.delete(appId, serviceVariableId);

        EncryptedData newEncryptedData = wingsPersistence.get(EncryptedData.class, newSecretId);

        if (serviceVariableIndex != numOfEnvs * numOfServiceVariables) {
          assertEquals(numOfEnvs * numOfServiceVariables - serviceVariableIndex, newEncryptedData.getAppIds().size());
          assertEquals(numOfEnvs * numOfServiceVariables - serviceVariableIndex,
              newEncryptedData.getSearchTags().get(appName).get());
          String envId = serviceVariable.getEntityId();
          String envName = environmentService.get(appId, envId).getName();

          if (k == numOfServiceVariables - 1) {
            assertNull(newEncryptedData.getSearchTags().get(envName));
          } else {
            assertEquals(numOfServiceVariables - k - 1, newEncryptedData.getSearchTags().get(envName).get());
          }

          assertNull(newEncryptedData.getSearchTags().get(serviceVariable.getName()));
        } else {
          assertNull(newEncryptedData.getSearchTags());
          assertNull(newEncryptedData.getAppIds());
          assertNull(newEncryptedData.getServiceIds());
        }
      }
    }
  }

  @Test
  public void serviceVariableSyncSearchTags() throws IllegalAccessException {
    String secretName = generateUuid();
    String secretValue = generateUuid();
    String secretId =
        secretManagementResource.saveSecret(accountId, SecretText.builder().name(secretName).value(secretValue).build())
            .getResource();

    int numOfServices = 3;
    int numOfServiceVariables = 3;

    List<String> serviceNames = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> appIds = new ArrayList<>();
    List<String> serviceVariableIds = new ArrayList<>();
    Set<String> serviceVariableNames = new HashSet<>();
    for (int i = 0; i < numOfServices; i++) {
      Service service = Service.builder().appId(appId).name("service-" + i).build();
      String serviceId = wingsPersistence.save(service);

      for (int j = 0; j < numOfServiceVariables; j++) {
        ServiceVariable serviceVariable = ServiceVariable.builder()
                                              .templateId(UUID.randomUUID().toString())
                                              .envId(Base.GLOBAL_ENV_ID)
                                              .entityType(EntityType.SERVICE)
                                              .entityId(service.getUuid())
                                              .parentServiceVariableId(UUID.randomUUID().toString())
                                              .overrideType(OverrideType.ALL)
                                              .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                              .expression(UUID.randomUUID().toString())
                                              .accountId(accountId)
                                              .name("service_variable_" + i + "_j" + j)
                                              .value(secretId.toCharArray())
                                              .type(Type.ENCRYPTED_TEXT)
                                              .build();
        serviceVariableNames.add(serviceVariable.getName());
        serviceVariableIds.add(serviceVariableResource.save(appId, serviceVariable).getResource().getUuid());
        serviceIds.add(serviceId);
        serviceNames.add(service.getName());
        appIds.add(appId);
      }
    }

    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
    encryptedData.clearSearchTags();
    wingsPersistence.save(encryptedData);

    encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
    assertNull(encryptedData.getAppIds());
    assertNull(encryptedData.getServiceIds());
    assertNull(encryptedData.getEnvIds());
    assertNull(encryptedData.getServiceVariableIds());
    assertNull(encryptedData.getSearchTags());

    serviceVariableService.updateSearchTagsForSecrets(accountId);

    encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
    assertEquals(appIds, encryptedData.getAppIds());
    assertEquals(numOfServices * numOfServiceVariables, encryptedData.getAppIds().size());
    Map<String, AtomicInteger> searchTags = encryptedData.getSearchTags();

    String appName = appService.get(appId).getName();
    assertTrue(searchTags.containsKey(appName));
    assertEquals(numOfServices * numOfServiceVariables, searchTags.get(appName).get());

    assertTrue(CollectionUtils.isEqualCollection(serviceIds, encryptedData.getServiceIds()));
    serviceNames.forEach(serviceName -> {
      assertTrue(searchTags.containsKey(serviceName));
      assertEquals(numOfServiceVariables, searchTags.get(serviceName).get());
    });

    assertTrue(encryptedData.getServiceVariableIds().containsAll(serviceVariableIds));
    serviceVariableNames.forEach(servicevariableName -> {
      assertTrue(searchTags.containsKey(servicevariableName));
      assertEquals(1, searchTags.get(servicevariableName).get());
    });
  }

  @Test
  public void filterSecretSearchTags() throws IllegalAccessException {
    int numOfServiceVariables = 6;
    int numOfSecrets = numOfServiceVariables * 5;
    List<String> secretIds = new ArrayList<>();
    List<SecretText> secretTexts = new ArrayList<>();
    for (int i = 0; i < numOfSecrets; i++) {
      SecretText secretText = SecretText.builder().name(generateUuid()).value(generateUuid()).build();
      secretIds.add(secretManagementResource.saveSecret(accountId, secretText).getResource());
      secretTexts.add(secretText);
    }
    int numOfServices = 5;
    int numOfEnvs = 4;

    List<String> serviceNames = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> appIds = new ArrayList<>();
    List<String> serviceVariableTemplateIds = new ArrayList<>();
    List<String> serviceVariableIds = new ArrayList<>();
    List<String> envIds = new ArrayList<>();
    List<String> envNames = new ArrayList<>();
    Set<String> serviceVariableTemplateNames = new HashSet<>();
    for (int i = 0; i < numOfServices; i++) {
      Service service = Service.builder().appId(appId).name("service-" + i).build();
      String serviceId = wingsPersistence.save(service);

      for (int j = 0; j < numOfEnvs; j++) {
        String envId = wingsPersistence.save(
            Environment.Builder.anEnvironment().withAppId(appId).withName("env-" + i + "-" + j).build());
        ServiceTemplate serviceTemplate = aServiceTemplate()
                                              .withAppId(appId)
                                              .withServiceId(serviceId)
                                              .withEnvId(envId)
                                              .withName("serviceTemplate-" + i + "-" + j)
                                              .build();
        String serviceTemplateId = wingsPersistence.save(serviceTemplate);
        envNames.add(serviceTemplate.getName());

        for (int k = 0; k < numOfServiceVariables; k++) {
          ServiceVariable serviceVariable = ServiceVariable.builder()
                                                .templateId(UUID.randomUUID().toString())
                                                .envId(envId)
                                                .entityType(EntityType.SERVICE_TEMPLATE)
                                                .entityId(serviceTemplateId)
                                                .templateId(serviceTemplateId)
                                                .parentServiceVariableId(UUID.randomUUID().toString())
                                                .overrideType(OverrideType.ALL)
                                                .instances(Collections.singletonList(UUID.randomUUID().toString()))
                                                .expression(UUID.randomUUID().toString())
                                                .accountId(accountId)
                                                .name("service_variable_" + i + "_" + j + "_" + k)
                                                .value(secretIds.get(j * numOfServiceVariables).toCharArray())
                                                .type(Type.ENCRYPTED_TEXT)
                                                .build();
          serviceVariableTemplateNames.add(serviceTemplate.getName());
          serviceVariableIds.add(serviceVariableResource.save(appId, serviceVariable).getResource().getUuid());
          serviceVariableTemplateIds.add(serviceTemplateId);
          serviceIds.add(serviceId);
          serviceNames.add(service.getName());
          appIds.add(appId);
          envIds.add(envId);
        }
      }
    }

    String appName = appService.get(appId).getName();
    PageRequest<EncryptedData> pageRequest = aPageRequest()
                                                 .addFilter("accountId", Operator.EQ, accountId)
                                                 .addFilter("keywords", Operator.CONTAINS, appName)
                                                 .build();
    List<EncryptedData> encryptedDataList =
        secretManagementResource.listSecrets(accountId, SettingVariableTypes.SECRET_TEXT, null, null, pageRequest)
            .getResource();
    assertEquals(numOfEnvs, encryptedDataList.size());

    pageRequest = aPageRequest()
                      .addFilter("accountId", Operator.EQ, accountId)
                      .addFilter("keywords", Operator.CONTAINS, "service-variable")
                      .build();

    encryptedDataList =
        secretManagementResource.listSecrets(accountId, SettingVariableTypes.SECRET_TEXT, null, null, pageRequest)
            .getResource();
    assertEquals(0, encryptedDataList.size());

    pageRequest = aPageRequest()
                      .addFilter("accountId", Operator.EQ, accountId)
                      .addFilter("keywords", Operator.CONTAINS, "env")
                      .build();
    encryptedDataList =
        secretManagementResource.listSecrets(accountId, SettingVariableTypes.SECRET_TEXT, null, null, pageRequest)
            .getResource();
    assertEquals(numOfEnvs, encryptedDataList.size());

    pageRequest = aPageRequest()
                      .addFilter("accountId", Operator.EQ, accountId)
                      .addFilter("keywords", Operator.CONTAINS, "env-0")
                      .build();
    encryptedDataList =
        secretManagementResource.listSecrets(accountId, SettingVariableTypes.SECRET_TEXT, null, null, pageRequest)
            .getResource();
    assertEquals(numOfEnvs, encryptedDataList.size());
    for (int i = 0; i < numOfServices; i++) {
      for (int j = 0; j < numOfEnvs; j++) {
        pageRequest = aPageRequest()
                          .addFilter("accountId", Operator.EQ, accountId)
                          .addFilter("keywords", Operator.CONTAINS, "env-" + i + "-" + j)
                          .build();
        encryptedDataList =
            secretManagementResource.listSecrets(accountId, SettingVariableTypes.SECRET_TEXT, null, null, pageRequest)
                .getResource();
        assertEquals(1, encryptedDataList.size());
        assertEquals(secretTexts.get(j * numOfServiceVariables).getName(), encryptedDataList.get(0).getName());

        pageRequest = aPageRequest()
                          .addFilter("accountId", Operator.EQ, accountId)
                          .addFilter("keywords", Operator.CONTAINS, "serviceTemplate-" + i + "-" + j)
                          .build();
        encryptedDataList =
            secretManagementResource.listSecrets(accountId, SettingVariableTypes.SECRET_TEXT, null, null, pageRequest)
                .getResource();
        assertEquals(1, encryptedDataList.size());
        assertEquals(secretTexts.get(j * numOfServiceVariables).getName(), encryptedDataList.get(0).getName());
      }
    }
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
    kmsConfig.setKmsArn(generateUuid());
    kmsConfig.setAccessKey(generateUuid());
    kmsConfig.setSecretKey(generateUuid());
    return kmsConfig;
  }
}
