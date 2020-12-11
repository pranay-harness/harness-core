package io.harness.delegate.service;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.SRINIVAS;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.EncryptedData;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.SecretDetail;
import io.harness.delegate.beans.TaskData;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.rule.Owner;
import io.harness.security.encryption.DelegateDecryptionService;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.beans.KmsConfig;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Call;

public class DelegateAgentServiceImplTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private DelegateAgentManagerClient delegateAgentManagerClient;
  @Mock private Call<DelegateTaskPackage> delegatePackageCall;
  @Mock private DelegateDecryptionService delegateDecryptionService;

  @InjectMocks @Inject DelegateAgentServiceImpl delegateService;

  @Before
  public void setUp() {
    when(delegateAgentManagerClient.acquireTask(Matchers.anyString(), Matchers.anyString(), Matchers.anyString()))
        .thenReturn(delegatePackageCall);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldNotApplyFunctorIfNoSecrets() {
    String delegateTaskId = UUIDGenerator.generateUuid();
    String accountId = UUIDGenerator.generateUuid();

    DelegateTaskPackage delegateTaskPackage = DelegateTaskPackage.builder()
                                                  .accountId(accountId)
                                                  .delegateTaskId(delegateTaskId)
                                                  .data(TaskData.builder().async(true).taskType("HTTP").build())
                                                  .build();

    delegateService.applyDelegateSecretFunctor(delegateTaskPackage);
    verify(delegateDecryptionService, times(0)).decrypt(anyMap());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldApplyFunctorForSecrets() {
    String delegateTaskId = UUIDGenerator.generateUuid();
    String accountId = UUIDGenerator.generateUuid();

    Map<String, EncryptionConfig> encryptionConfigMap = new HashMap<>();
    KmsConfig kmsConfig = KmsConfig.builder().build();
    kmsConfig.setUuid("KMS_CONFIG_UUID");
    encryptionConfigMap.put("KMS_CONFIG_UUID", kmsConfig);

    Map<String, SecretDetail> secretDetails = new HashMap<>();
    SecretDetail secretDetail =
        SecretDetail.builder()
            .configUuid("KMS_CONFIG_UUID")
            .encryptedRecord(EncryptedData.builder().uuid("ENC_UUID").accountId("ACCOUNT_ID").build())
            .build();

    secretDetails.put("SECRET_UUID", secretDetail);

    DelegateTaskPackage delegateTaskPackage = DelegateTaskPackage.builder()
                                                  .accountId(accountId)
                                                  .delegateTaskId(delegateTaskId)
                                                  .data(TaskData.builder().async(true).taskType("HTTP").build())
                                                  .encryptionConfigs(encryptionConfigMap)
                                                  .secretDetails(secretDetails)
                                                  .build();

    Map<String, char[]> decryptedRecords = new HashMap<>();
    decryptedRecords.put("ENC_UUID", "test".toCharArray());
    when(delegateDecryptionService.decrypt(anyMap())).thenReturn(decryptedRecords);

    delegateService.applyDelegateSecretFunctor(delegateTaskPackage);
    verify(delegateDecryptionService, times(1)).decrypt(anyMap());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testPerformanceLog() {
    assertThatCode(() -> delegateService.obtainPerformance()).doesNotThrowAnyException();
  }
}
