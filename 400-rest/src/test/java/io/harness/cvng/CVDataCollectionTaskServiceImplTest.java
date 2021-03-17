package io.harness.cvng;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.perpetualtask.PerpetualTaskType.DATA_COLLECTION_TASK;
import static io.harness.perpetualtask.PerpetualTaskType.K8_ACTIVITY_COLLECTION_TASK;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.cvng.beans.CVNGPerpetualTaskDTO;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.perpetualtask.CVDataCollectionTaskService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.PerpetualTaskUnassignedReason;
import io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams;
import io.harness.perpetualtask.datacollection.K8ActivityCollectionPerpetualTaskParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecordDao;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CVDataCollectionTaskServiceImplTest extends WingsBaseTest {
  @Inject private CVDataCollectionTaskService dataCollectionTaskService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private PerpetualTaskRecordDao perpetualTaskRecordDao;
  private String accountId;
  private String cvConfigId;
  private String connectorIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String dataCollectionWorkerId;
  @Before
  public void setup() throws IllegalAccessException {
    initMocks(this);
    accountId = generateUuid();
    cvConfigId = generateUuid();
    connectorIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    dataCollectionWorkerId = generateUuid();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreateCVTask() throws InvalidProtocolBufferException {
    SecretRefData secretRefData = SecretRefData.builder().scope(Scope.ACCOUNT).identifier("secret").build();
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = AppDynamicsConnectorDTO.builder()
                                                          .accountId(accountId)
                                                          .accountname("accountName")
                                                          .username("username")
                                                          .controllerUrl("controllerUrl")
                                                          .passwordRef(secretRefData)
                                                          .build();
    DataCollectionConnectorBundle bundle = DataCollectionConnectorBundle.builder()
                                               .connectorIdentifier(connectorIdentifier)
                                               .connectorDTO(ConnectorInfoDTO.builder()
                                                                 .connectorConfig(appDynamicsConnectorDTO)
                                                                 .connectorType(ConnectorType.APP_DYNAMICS)
                                                                 .build())
                                               .dataCollectionType(DataCollectionType.CV)
                                               .dataCollectionWorkerId(generateUuid())
                                               .build();
    String taskId = dataCollectionTaskService.create(accountId, orgIdentifier, projectIdentifier, bundle);
    assertThat(taskId).isNotNull();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskService.getTaskRecord(taskId);
    PerpetualTaskClientContext perpetualTaskClientContext = perpetualTaskRecord.getClientContext();
    assertThat(perpetualTaskClientContext.getClientParams()).isNull();
    assertThat(perpetualTaskService.getPerpetualTaskType(taskId)).isEqualTo(DATA_COLLECTION_TASK);
    assertThat(perpetualTaskRecord.getIntervalSeconds()).isEqualTo(60);
    assertThat(perpetualTaskRecord.getTimeoutMillis()).isEqualTo(Duration.ofHours(3).toMillis());

    PerpetualTaskExecutionBundle perpetualTaskExecutionBundle =
        PerpetualTaskExecutionBundle.parseFrom(perpetualTaskClientContext.getExecutionBundle());
    DataCollectionPerpetualTaskParams taskParams =
        AnyUtils.unpack(perpetualTaskExecutionBundle.getTaskParams(), DataCollectionPerpetualTaskParams.class);
    assertThat(taskParams.getDataCollectionWorkerId()).isEqualTo(bundle.getDataCollectionWorkerId());
    assertThat(perpetualTaskExecutionBundle.getCapabilitiesList()).isNotEmpty();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCreateK8Task() throws InvalidProtocolBufferException {
    DataCollectionConnectorBundle bundle =
        DataCollectionConnectorBundle.builder()
            .connectorIdentifier(connectorIdentifier)
            .connectorDTO(
                ConnectorInfoDTO.builder()
                    .connectorConfig(
                        KubernetesClusterConfigDTO.builder()
                            .credential(
                                KubernetesCredentialDTO.builder()
                                    .kubernetesCredentialType(KubernetesCredentialType.MANUAL_CREDENTIALS)
                                    .config(KubernetesClusterDetailsDTO.builder()
                                                .auth(KubernetesAuthDTO.builder()
                                                          .credentials(KubernetesServiceAccountDTO.builder().build())
                                                          .build())
                                                .build())
                                    .build())
                            .build())
                    .connectorType(ConnectorType.KUBERNETES_CLUSTER)
                    .build())
            .dataCollectionType(DataCollectionType.KUBERNETES)
            .dataCollectionWorkerId(generateUuid())
            .build();
    String taskId = dataCollectionTaskService.create(accountId, orgIdentifier, projectIdentifier, bundle);
    assertThat(taskId).isNotNull();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskService.getTaskRecord(taskId);
    PerpetualTaskClientContext perpetualTaskClientContext = perpetualTaskRecord.getClientContext();
    assertThat(perpetualTaskClientContext.getClientParams()).isNull();
    assertThat(perpetualTaskService.getPerpetualTaskType(taskId)).isEqualTo(K8_ACTIVITY_COLLECTION_TASK);
    assertThat(perpetualTaskRecord.getIntervalSeconds()).isEqualTo(60);
    assertThat(perpetualTaskRecord.getTimeoutMillis()).isEqualTo(Duration.ofHours(3).toMillis());

    PerpetualTaskExecutionBundle perpetualTaskExecutionBundle =
        PerpetualTaskExecutionBundle.parseFrom(perpetualTaskClientContext.getExecutionBundle());
    K8ActivityCollectionPerpetualTaskParams taskParams =
        AnyUtils.unpack(perpetualTaskExecutionBundle.getTaskParams(), K8ActivityCollectionPerpetualTaskParams.class);
    assertThat(taskParams.getDataCollectionWorkerId()).isEqualTo(bundle.getDataCollectionWorkerId());
    assertThat(perpetualTaskExecutionBundle.getCapabilitiesList()).isNotEmpty();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testCreateMultiplePerpetualTasks_withSameDataCollectionWorkerId() {
    Map<String, String> params = new HashMap<>();
    params.put("cvConfigId", cvConfigId);
    params.put("connectorIdentifier", connectorIdentifier);
    params.put("dataCollectionWorkerId", dataCollectionWorkerId);

    String dataCollectionWorkerId = generateUuid();
    SecretRefData secretRefData = SecretRefData.builder().scope(Scope.ACCOUNT).identifier("secret").build();
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = AppDynamicsConnectorDTO.builder()
                                                          .accountId(accountId)
                                                          .accountname("accountName")
                                                          .username("username")
                                                          .controllerUrl("controllerUrl")
                                                          .passwordRef(secretRefData)
                                                          .build();
    DataCollectionConnectorBundle bundle = DataCollectionConnectorBundle.builder()
                                               .connectorIdentifier(connectorIdentifier)
                                               .connectorDTO(ConnectorInfoDTO.builder()
                                                                 .connectorConfig(appDynamicsConnectorDTO)
                                                                 .connectorType(ConnectorType.APP_DYNAMICS)
                                                                 .build())
                                               .dataCollectionWorkerId(dataCollectionWorkerId)
                                               .dataCollectionType(DataCollectionType.CV)
                                               .build();
    String taskId = dataCollectionTaskService.create(accountId, orgIdentifier, projectIdentifier, bundle);

    AppDynamicsConnectorDTO appDynamicsConnectorDTO2 = AppDynamicsConnectorDTO.builder()
                                                           .accountId(accountId)
                                                           .accountname("accountName2")
                                                           .username("username2")
                                                           .controllerUrl("controllerUrl2")
                                                           .passwordRef(secretRefData)
                                                           .build();
    DataCollectionConnectorBundle bundle2 = DataCollectionConnectorBundle.builder()
                                                .connectorIdentifier(connectorIdentifier)
                                                .connectorDTO(ConnectorInfoDTO.builder()
                                                                  .connectorConfig(appDynamicsConnectorDTO2)
                                                                  .connectorType(ConnectorType.APP_DYNAMICS)
                                                                  .build())
                                                .dataCollectionType(DataCollectionType.CV)
                                                .dataCollectionWorkerId(dataCollectionWorkerId)
                                                .build();
    String duplicateTaskId = dataCollectionTaskService.create(accountId, orgIdentifier, projectIdentifier, bundle2);
    assertThat(taskId).isEqualTo(duplicateTaskId);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDelete() {
    Map<String, String> params = new HashMap<>();
    params.put("cvConfigId", cvConfigId);
    params.put("connectorIdentifier", connectorIdentifier);
    params.put("dataCollectionWorkerId", dataCollectionWorkerId);
    SecretRefData secretRefData = SecretRefData.builder().scope(Scope.ACCOUNT).identifier("secret").build();
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = AppDynamicsConnectorDTO.builder()
                                                          .accountId(accountId)
                                                          .accountname("accountName")
                                                          .username("username")
                                                          .controllerUrl("controllerUrl")
                                                          .passwordRef(secretRefData)
                                                          .build();
    DataCollectionConnectorBundle bundle = DataCollectionConnectorBundle.builder()
                                               .connectorIdentifier(connectorIdentifier)
                                               .connectorDTO(ConnectorInfoDTO.builder()
                                                                 .connectorConfig(appDynamicsConnectorDTO)
                                                                 .connectorType(ConnectorType.APP_DYNAMICS)
                                                                 .build())
                                               .dataCollectionWorkerId(generateUuid())
                                               .dataCollectionType(DataCollectionType.CV)
                                               .build();
    String taskId = dataCollectionTaskService.create(accountId, orgIdentifier, projectIdentifier, bundle);
    assertThat(taskId).isNotNull();
    dataCollectionTaskService.delete(accountId, "some-other-id");
    assertThat(perpetualTaskService.getTaskRecord(taskId)).isNotNull();
    dataCollectionTaskService.delete(accountId, taskId);
    assertThat(perpetualTaskService.getTaskRecord(taskId)).isNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testGetCVNGPerpetualTaskDTO() {
    for (PerpetualTaskUnassignedReason reason : PerpetualTaskUnassignedReason.values()) {
      for (PerpetualTaskState perpetualTaskState : PerpetualTaskState.values()) {
        String accountId = generateUuid();
        String delegateId = generateUuid();
        String taskId = generateUuid();

        PerpetualTaskRecord perpetualTaskRecord = PerpetualTaskRecord.builder()
                                                      .uuid(taskId)
                                                      .accountId(accountId)
                                                      .perpetualTaskType(DATA_COLLECTION_TASK)
                                                      .delegateId(delegateId)
                                                      .state(perpetualTaskState)
                                                      .unassignedReason(reason)
                                                      .build();

        perpetualTaskRecordDao.save(perpetualTaskRecord);

        PerpetualTaskRecord record = perpetualTaskRecordDao.getTask(taskId);
        assertThat(record).isNotNull();
        CVNGPerpetualTaskDTO cvngPerpetualTaskDTO = dataCollectionTaskService.getCVNGPerpetualTaskDTO(taskId);
        assertThat(cvngPerpetualTaskDTO.getDelegateId()).isEqualTo(record.getDelegateId());
        assertThat(cvngPerpetualTaskDTO.getAccountId()).isEqualTo(record.getAccountId());
      }
    }
  }
}
