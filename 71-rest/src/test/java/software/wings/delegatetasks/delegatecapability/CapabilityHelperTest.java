package software.wings.delegatetasks.delegatecapability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CapabilityHelperTest extends WingsBaseTest {
  public static final String HTTP_VAUTL_URL = "http://vautl.com";
  public static final String GOOGLE_COM = "http://google.com";
  public static final String US_EAST_2 = "us-east-2";
  public static final String AWS_KMS_URL = "https://apigateway.us-east-2.amazonaws.com";

  @Test
  @Category(UnitTests.class)
  public void testEmbedCapabilitiesInDelegateTask_HTTP_VaultConfig() {
    TaskData taskData =
        TaskData.builder().parameters(new Object[] {HttpTaskParameters.builder().url(GOOGLE_COM).build()}).build();
    DelegateTask task = DelegateTask.builder().data(taskData).build();

    Collection<EncryptionConfig> encryptionConfigs = new ArrayList<>();
    EncryptionConfig encryptionConfig = VaultConfig.builder().vaultUrl(HTTP_VAUTL_URL).build();
    encryptionConfigs.add(encryptionConfig);

    CapabilityHelper.embedCapabilitiesInDelegateTask(task, encryptionConfigs);
    assertNotNull(task.getExecutionCapabilities());
    assertEquals(2, task.getExecutionCapabilities().size());

    Set<String> criterias = new HashSet<>();
    criterias.add(HTTP_VAUTL_URL);
    criterias.add(GOOGLE_COM);

    task.getExecutionCapabilities().forEach(
        executionCapability -> assertTrue(criterias.contains(executionCapability.fetchCapabilityBasis())));
  }

  @Test
  @Category(UnitTests.class)
  public void testEmbedCapabilitiesInDelegateTask_HTTP_KmsConfig() {
    TaskData taskData =
        TaskData.builder().parameters(new Object[] {HttpTaskParameters.builder().url(GOOGLE_COM).build()}).build();
    DelegateTask task = DelegateTask.builder().data(taskData).build();

    Collection<EncryptionConfig> encryptionConfigs = new ArrayList<>();
    EncryptionConfig encryptionConfig = KmsConfig.builder().region(US_EAST_2).build();
    encryptionConfigs.add(encryptionConfig);

    CapabilityHelper.embedCapabilitiesInDelegateTask(task, encryptionConfigs);
    assertNotNull(task.getExecutionCapabilities());
    assertEquals(2, task.getExecutionCapabilities().size());

    Set<String> criterias = new HashSet<>();
    criterias.add(AWS_KMS_URL);
    criterias.add(GOOGLE_COM);

    task.getExecutionCapabilities().forEach(
        executionCapability -> assertTrue(criterias.contains(executionCapability.fetchCapabilityBasis())));
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchEncryptionDetailsListFromParameters() {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    encryptedDataDetails.add(EncryptedDataDetail.builder().encryptionType(EncryptionType.LOCAL).build());

    TaskData taskData =
        TaskData.builder().parameters(new Object[] {JenkinsConfig.builder().build(), encryptedDataDetails}).build();

    Map encryptionMap = CapabilityHelper.fetchEncryptionDetailsListFromParameters(taskData);
    assertNotNull(encryptionMap);
    assertEquals(0, encryptionMap.size());
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchEncryptionDetailsListFromParameters_VaultConfig() throws Exception {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    encryptedDataDetails.add(
        EncryptedDataDetail.builder()
            .encryptionType(EncryptionType.VAULT)
            .encryptionConfig(
                VaultConfig.builder().encryptionType(EncryptionType.VAULT).vaultUrl(HTTP_VAUTL_URL).build())
            .build());

    TaskData taskData =
        TaskData.builder().parameters(new Object[] {JenkinsConfig.builder().build(), encryptedDataDetails}).build();

    Map encryptionMap = CapabilityHelper.fetchEncryptionDetailsListFromParameters(taskData);
    assertNotNull(encryptionMap);
    assertEquals(1, encryptionMap.size());
    EncryptionConfig encryptionConfig = (EncryptionConfig) encryptionMap.values().iterator().next();

    assertEquals(EncryptionType.VAULT, encryptionConfig.getEncryptionType());
    assertTrue(encryptionConfig instanceof VaultConfig);
    assertEquals(HTTP_VAUTL_URL, ((VaultConfig) encryptionConfig).getVaultUrl());
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchEncryptionDetailsListFromParameters_KmsConfig() throws Exception {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    encryptedDataDetails.add(
        EncryptedDataDetail.builder()
            .encryptionType(EncryptionType.KMS)
            .encryptionConfig(KmsConfig.builder().encryptionType(EncryptionType.KMS).region(US_EAST_2).build())
            .build());

    TaskData taskData =
        TaskData.builder().parameters(new Object[] {JenkinsConfig.builder().build(), encryptedDataDetails}).build();

    Map encryptionMap = CapabilityHelper.fetchEncryptionDetailsListFromParameters(taskData);
    assertNotNull(encryptionMap);
    assertEquals(1, encryptionMap.size());
    EncryptionConfig encryptionConfig = (EncryptionConfig) encryptionMap.values().iterator().next();
    assertEquals(EncryptionType.KMS, encryptionConfig.getEncryptionType());
    assertTrue(encryptionConfig instanceof KmsConfig);
    assertEquals(US_EAST_2, ((KmsConfig) encryptionConfig).getRegion());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetHttpCapabilityForDecryption_VaultConfig() throws Exception {
    EncryptionConfig encryptionConfig = VaultConfig.builder().vaultUrl(HTTP_VAUTL_URL).build();
    HttpConnectionExecutionCapability capability = CapabilityHelper.getHttpCapabilityForDecryption(encryptionConfig);
    capability.fetchCapabilityBasis().equals(HTTP_VAUTL_URL);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetHttpCapabilityForDecryption_KmsConfig() throws Exception {
    EncryptionConfig encryptionConfig = KmsConfig.builder().region(US_EAST_2).build();
    HttpConnectionExecutionCapability capability = CapabilityHelper.getHttpCapabilityForDecryption(encryptionConfig);
    capability.fetchCapabilityBasis().equals(AWS_KMS_URL);
  }
}
