package software.wings.delegatetasks.delegatecapability;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CapabilityHelperTest extends WingsBaseTest {
  public static final String HTTP_PORT = "80";
  public static final String HTTPS_PORT = "443";
  public static final String HTTP_VAUTL_URL = "http://vautl.com";
  public static final String GOOGLE_COM = "http://google.com";
  public static final String US_EAST_2 = "us-east-2";
  public static final String AWS_KMS_URL = "https://kms.us-east-2.amazonaws.com";

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
    assertThat(task.getExecutionCapabilities()).isNotNull();
    assertThat(task.getExecutionCapabilities()).hasSize(2);

    Set<String> criteriaSet = new HashSet<>();
    criteriaSet.add(HTTP_VAUTL_URL + ":" + HTTP_PORT);
    criteriaSet.add(GOOGLE_COM + ":" + HTTP_PORT);

    task.getExecutionCapabilities().forEach(
        executionCapability -> assertThat(criteriaSet.contains(executionCapability.fetchCapabilityBasis())).isTrue());
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
    assertThat(task.getExecutionCapabilities()).isNotNull();
    assertThat(task.getExecutionCapabilities()).hasSize(2);

    Set<String> criterias = new HashSet<>();
    criterias.add(AWS_KMS_URL + ":" + HTTPS_PORT);
    criterias.add(GOOGLE_COM + ":" + HTTP_PORT);

    task.getExecutionCapabilities().forEach(
        executionCapability -> assertThat(criterias.contains(executionCapability.fetchCapabilityBasis())).isTrue());
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchEncryptionDetailsListFromParameters() {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    encryptedDataDetails.add(
        EncryptedDataDetail.builder()
            .encryptedData(EncryptedRecordData.builder().encryptionType(EncryptionType.LOCAL).build())
            .build());

    TaskData taskData =
        TaskData.builder().parameters(new Object[] {JenkinsConfig.builder().build(), encryptedDataDetails}).build();

    Map encryptionMap = CapabilityHelper.fetchEncryptionDetailsListFromParameters(taskData);
    assertThat(encryptionMap).isNotNull();
    assertThat(encryptionMap).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchEncryptionDetailsListFromParameters_VaultConfig() throws Exception {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    encryptedDataDetails.add(
        EncryptedDataDetail.builder()
            .encryptedData(EncryptedRecordData.builder().encryptionType(EncryptionType.VAULT).build())
            .encryptionConfig(VaultConfig.builder().vaultUrl(HTTP_VAUTL_URL).build())
            .build());

    TaskData taskData =
        TaskData.builder().parameters(new Object[] {JenkinsConfig.builder().build(), encryptedDataDetails}).build();

    Map encryptionMap = CapabilityHelper.fetchEncryptionDetailsListFromParameters(taskData);
    assertThat(encryptionMap).isNotNull();
    assertThat(encryptionMap).hasSize(1);
    EncryptionConfig encryptionConfig = (EncryptionConfig) encryptionMap.values().iterator().next();

    assertThat(encryptionConfig.getEncryptionType()).isEqualTo(EncryptionType.VAULT);
    assertThat(encryptionConfig instanceof VaultConfig).isTrue();
    assertThat(((VaultConfig) encryptionConfig).getVaultUrl()).isEqualTo(HTTP_VAUTL_URL);
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchEncryptionDetailsListFromParameters_KmsConfig() throws Exception {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    encryptedDataDetails.add(
        EncryptedDataDetail.builder()
            .encryptedData(EncryptedRecordData.builder().encryptionType(EncryptionType.KMS).build())
            .encryptionConfig(KmsConfig.builder().region(US_EAST_2).build())
            .build());

    TaskData taskData =
        TaskData.builder().parameters(new Object[] {JenkinsConfig.builder().build(), encryptedDataDetails}).build();

    Map encryptionMap = CapabilityHelper.fetchEncryptionDetailsListFromParameters(taskData);
    assertThat(encryptionMap).isNotNull();
    assertThat(encryptionMap).hasSize(1);
    EncryptionConfig encryptionConfig = (EncryptionConfig) encryptionMap.values().iterator().next();
    assertThat(encryptionConfig.getEncryptionType()).isEqualTo(EncryptionType.KMS);
    assertThat(encryptionConfig instanceof KmsConfig).isTrue();
    assertThat(((KmsConfig) encryptionConfig).getRegion()).isEqualTo(US_EAST_2);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetHttpCapabilityForDecryption_VaultConfig() throws Exception {
    EncryptionConfig encryptionConfig = VaultConfig.builder().vaultUrl(HTTP_VAUTL_URL).build();
    HttpConnectionExecutionCapability capability = CapabilityHelper.getHttpCapabilityForDecryption(encryptionConfig);
    assertThat(HTTP_VAUTL_URL + ":" + HTTP_PORT).isEqualTo(capability.fetchCapabilityBasis());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetHttpCapabilityForDecryption_KmsConfig() throws Exception {
    EncryptionConfig encryptionConfig = KmsConfig.builder().region(US_EAST_2).build();
    HttpConnectionExecutionCapability capability = CapabilityHelper.getHttpCapabilityForDecryption(encryptionConfig);
    assertThat(AWS_KMS_URL + ":" + HTTPS_PORT).isEqualTo(capability.fetchCapabilityBasis());
  }
}
