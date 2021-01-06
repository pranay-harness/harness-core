package io.harness.perpetualtask.datacollection;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.DelegateTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.beans.K8ActivityDataCollectionInfo;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.delegate.service.KubernetesActivitiesStoreService;
import io.harness.delegate.task.k8s.K8sYamlToDelegateDTOMapper;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.k8s.apiclient.ApiClientFactory;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.k8s.watch.K8sWatchServiceDelegate;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.serializer.KryoSerializer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.kubernetes.client.openapi.ApiClient;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class K8ActivityCollectionPerpetualTaskExecutorTest extends DelegateTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  private K8ActivityCollectionPerpetualTaskExecutor dataCollector = new K8ActivityCollectionPerpetualTaskExecutor();
  @Mock private SecretDecryptionService secretDecryptionService;
  @Inject private K8sYamlToDelegateDTOMapper k8sYamlToDelegateDTOMapper;
  @Mock private ApiClientFactory apiClientFactory;
  @Mock private KubernetesActivitiesStoreService kubernetesActivitiesStoreService;
  @Mock private ApiClient apiClient;
  private KubernetesClusterConfigDTO kubernetesClusterConfigDTO;
  private String accountId;
  private String activitySourceId;

  private PerpetualTaskExecutionParams perpetualTaskParams;

  @Inject KryoSerializer kryoSerializer;

  @Before
  public void setup() throws IllegalAccessException {
    apiClient = Mockito.mock(ApiClient.class);
    when(apiClient.setVerifyingSsl(anyBoolean())).thenReturn(apiClient);
    when(apiClient.escapeString(anyString())).thenReturn(generateUuid());
    when(apiClientFactory.getClient(any(KubernetesConfig.class))).thenReturn(apiClient);
    on(dataCollector).set("kryoSerializer", kryoSerializer);
    accountId = generateUuid();
    activitySourceId = generateUuid();

    SecretRefData secretRefData = SecretRefData.builder()
                                      .scope(Scope.ACCOUNT)
                                      .identifier("secret")
                                      .decryptedValue(generateUuid().toCharArray())
                                      .build();
    kubernetesClusterConfigDTO =
        KubernetesClusterConfigDTO.builder()
            .credential(KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(KubernetesCredentialType.MANUAL_CREDENTIALS)
                            .config(KubernetesClusterDetailsDTO.builder()
                                        .masterUrl(generateUuid())
                                        .auth(KubernetesAuthDTO.builder()
                                                  .authType(KubernetesAuthType.USER_PASSWORD)
                                                  .credentials(KubernetesUserNamePasswordDTO.builder()
                                                                   .username(generateUuid())
                                                                   .passwordRef(secretRefData)
                                                                   .build())
                                                  .build())
                                        .build())
                            .build())
            .build();

    FieldUtils.writeField(dataCollector, "secretDecryptionService", secretDecryptionService, true);
    FieldUtils.writeField(dataCollector, "k8sYamlToDelegateDTOMapper", k8sYamlToDelegateDTOMapper, true);
    FieldUtils.writeField(dataCollector, "kubernetesActivitiesStoreService", kubernetesActivitiesStoreService, true);
    FieldUtils.writeField(dataCollector, "apiClientFactory", apiClientFactory, true);
  }

  private void createTaskParams() {
    K8ActivityDataCollectionInfo k8ActivityDataCollectionInfo =
        K8ActivityDataCollectionInfo.builder()
            .connectorConfigDTO(kubernetesClusterConfigDTO)
            .encryptedDataDetails(Lists.newArrayList())
            .dataCollectionType(DataCollectionType.KUBERNETES)
            .activitySourceDTO(KubernetesActivitySourceDTO.builder()
                                   .activitySourceConfigs(Sets.newHashSet(
                                       KubernetesActivitySourceDTO.KubernetesActivitySourceConfig.builder()
                                           .namespace(generateUuid())
                                           .workloadName(generateUuid())
                                           .build()))
                                   .build())
            .build();
    ByteString bytes = ByteString.copyFrom(kryoSerializer.asBytes(k8ActivityDataCollectionInfo));
    perpetualTaskParams = PerpetualTaskExecutionParams.newBuilder()
                              .setCustomizedParams(Any.pack(K8ActivityCollectionPerpetualTaskParams.newBuilder()
                                                                .setAccountId(accountId)
                                                                .setActivitySourceConfigId(activitySourceId)
                                                                .setDataCollectionInfo(bytes)
                                                                .build()))
                              .build();
  }

  @Test
  @Owner(developers = OwnerRule.RAGHU)
  @Category({UnitTests.class})
  public void testDataCollection_executeWatch() {
    createTaskParams();
    dataCollector.runOnce(PerpetualTaskId.newBuilder().build(), perpetualTaskParams, Instant.now());
    verifyCalls();
  }

  @Test
  @Owner(developers = OwnerRule.RAGHU)
  @Category({UnitTests.class})
  public void testDataCollection_cleanUp() throws IllegalAccessException {
    final Map<String, K8sWatchServiceDelegate.WatcherGroup> watchMap = new ConcurrentHashMap<>();
    FieldUtils.writeField(dataCollector, "watchMap", watchMap, true);
    String taskId = generateUuid();
    createTaskParams();
    dataCollector.runOnce(PerpetualTaskId.newBuilder().setId(taskId).build(), perpetualTaskParams, Instant.now());
    assertThat(watchMap.containsKey(taskId)).isTrue();
    dataCollector.cleanup(PerpetualTaskId.newBuilder().setId(taskId).build(), perpetualTaskParams);
    assertThat(watchMap.containsKey(taskId)).isFalse();
  }

  private void verifyCalls() {
    ArgumentCaptor<KubernetesConfig> kubernetesConfigArgumentCaptor = ArgumentCaptor.forClass(KubernetesConfig.class);
    ArgumentCaptor<RuntimeParameters> runtimeParams = ArgumentCaptor.forClass(RuntimeParameters.class);
    verify(apiClientFactory).getClient(kubernetesConfigArgumentCaptor.capture());
  }
}
