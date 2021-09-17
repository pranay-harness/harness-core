package io.harness.delegate.service;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.EncryptedData;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.DelegateProfileExecutedAtResponse;
import io.harness.delegate.beans.DelegateProfileParams;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.SecretDetail;
import io.harness.delegate.beans.TaskData;
import io.harness.grpc.DelegateServiceGrpcAgentClient;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.rule.Owner;
import io.harness.security.encryption.DelegateDecryptionService;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.beans.KmsConfig;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
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
import retrofit2.Response;

public class DelegateAgentServiceImplTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private DelegateAgentManagerClient delegateAgentManagerClient;
  @Mock private Call<DelegateTaskPackage> delegatePackageCall;
  @Mock private DelegateDecryptionService delegateDecryptionService;
  @Mock private TimeLimiter timeLimiter;
  @Mock private Injector injector;
  @Mock(name = "artifactExecutor") private ThreadPoolExecutor artifactExecutor;
  @Mock(name = "asyncExecutor") private ThreadPoolExecutor asyncExecutor;
  @Mock private DelegateServiceGrpcAgentClient delegateServiceGrpcAgentClient;

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

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void testDispatchDelegateTaskNoProfileApplied() {
    // mock
    DelegateAgentServiceImpl sut = spy(delegateService);
    DelegateTaskEvent taskEvent = mock(DelegateTaskEvent.class);
    when(taskEvent.getDelegateTaskId()).thenReturn("some delegate task id");
    // execute
    sut.dispatchDelegateTask(taskEvent);
    // validate
    verify(sut, never()).executeTask(any());
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void testDispatchDelegateTaskProfileApplied() throws IOException {
    // mock
    DelegateAgentServiceImpl delegateAgentService = spy(delegateService);
    delegateAgentService.accountId = "some_account_id";
    DelegateAgentServiceImpl.delegateId = "some_delegate_id";
    when(delegateServiceGrpcAgentClient.clearProfileExecutedAt(any(), any())).thenReturn(true);
    DelegateTaskEvent taskEvent = mock(DelegateTaskEvent.class);
    when(taskEvent.getDelegateTaskId()).thenReturn("some delegate task id");
    DelegateProfileParams delegateProfileParams = mock(DelegateProfileParams.class);
    when(delegateProfileParams.getScriptContent()).thenReturn("echo Hello");
    delegateAgentService.applyProfile(delegateProfileParams);
    String delegateTaskId = UUIDGenerator.generateUuid();
    String accountId = UUIDGenerator.generateUuid();
    Map<String, EncryptionConfig> encryptionConfigMap = new HashMap<>();
    KmsConfig kmsConfig = KmsConfig.builder().build();
    kmsConfig.setUuid("KMS_CONFIG_UUID");
    encryptionConfigMap.put("KMS_CONFIG_UUID", kmsConfig);
    Map<String, SecretDetail> secretDetails = new HashMap<>();
    DelegateTaskPackage delegateTaskPackage = DelegateTaskPackage.builder()
                                                  .accountId(accountId)
                                                  .delegateTaskId(delegateTaskId)
                                                  .data(TaskData.builder().async(true).taskType("HTTP").build())
                                                  .encryptionConfigs(encryptionConfigMap)
                                                  .secretDetails(secretDetails)
                                                  .build();
    Response<DelegateTaskPackage> delegatePackageResponse = Response.success(delegateTaskPackage);
    when(delegatePackageCall.execute()).thenReturn(delegatePackageResponse);
    Future<List<DelegateConnectionResult>> future = mock(Future.class);
    when(asyncExecutor.submit(any(Callable.class))).thenReturn(future);
    // execute
    delegateAgentService.dispatchDelegateTask(taskEvent);
    // validate
    verify(delegateAgentService, times(2)).updateCounterIfLessThanCurrent(any(), anyInt());
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldSendDelegateInstanceStatus() throws IOException {
    // mock
    DelegateAgentServiceImpl delegateAgentService = spy(delegateService);
    DelegateProfileParams profile = mock(DelegateProfileParams.class);
    delegateAgentService.accountId = "some_account_id";
    DelegateAgentServiceImpl.delegateId = "some_delegate_id";
    when(delegateServiceGrpcAgentClient.clearProfileExecutedAt(any(), any())).thenReturn(true);
    // execute
    delegateAgentService.applyProfile(profile);
    // validate
    verify(delegateServiceGrpcAgentClient, never()).clearProfileExecutedAt(any(), any());
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldResolveProfileExecutedAt() throws IOException {
    // mock
    DelegateAgentServiceImpl delegateAgentService = spy(delegateService);
    DelegateProfileParams profile = mock(DelegateProfileParams.class);
    delegateAgentService.accountId = "some_account_id";
    DelegateAgentServiceImpl.delegateId = "some_delegate_id";
    DelegateProfileExecutedAtResponse response =
        DelegateProfileExecutedAtResponse.newBuilder().setProfileId("123").setProfileExecutedAt(0L).build();
    when(delegateServiceGrpcAgentClient.fetchProfileExecutedAt(any(), any())).thenReturn(response);
    // execute
    boolean result = delegateAgentService.resolveProfileExecuted(profile);
    // validate
    assertThat(result).isEqualTo(false);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldResolveProfileExecutedAtTimeSet() throws IOException {
    // mock
    DelegateAgentServiceImpl delegateAgentService = spy(delegateService);
    DelegateProfileParams profile = mock(DelegateProfileParams.class);
    delegateAgentService.accountId = "some_account_id";
    DelegateAgentServiceImpl.delegateId = "some_delegate_id";
    DelegateProfileExecutedAtResponse response =
        DelegateProfileExecutedAtResponse.newBuilder().setProfileId("123").setProfileExecutedAt(123L).build();
    when(delegateServiceGrpcAgentClient.fetchProfileExecutedAt(any(), any())).thenReturn(response);
    // execute
    boolean result = delegateAgentService.resolveProfileExecuted(profile);
    // validate
    assertThat(result).isEqualTo(true);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldResolveProfileExecutedAtProfileNotSet() throws IOException {
    // mock
    DelegateAgentServiceImpl delegateAgentService = spy(delegateService);
    DelegateProfileParams profile = mock(DelegateProfileParams.class);
    delegateAgentService.accountId = "some_account_id";
    DelegateAgentServiceImpl.delegateId = "some_delegate_id";
    DelegateProfileExecutedAtResponse response =
        DelegateProfileExecutedAtResponse.newBuilder().setProfileExecutedAt(123L).build();
    when(delegateServiceGrpcAgentClient.fetchProfileExecutedAt(any(), any())).thenReturn(response);
    // execute
    boolean result = delegateAgentService.resolveProfileExecuted(profile);
    // validate
    assertThat(result).isEqualTo(true);
  }
}
