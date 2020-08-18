package io.harness.cvng;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.perpetualtask.PerpetualTaskType.DATA_COLLECTION_TASK;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.perpetualtask.CVDataCollectionTaskService;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class CVDataCollectionTaskServiceImplTest extends WingsBaseTest {
  @Inject private CVDataCollectionTaskService dataCollectionTaskService;
  @Inject private PerpetualTaskService perpetualTaskService;
  private String accountId;
  private String cvConfigId;
  private String connectorId;
  @Before
  public void setup() throws IllegalAccessException {
    initMocks(this);
    accountId = generateUuid();
    cvConfigId = generateUuid();
    connectorId = generateUuid();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void create() {
    Map<String, String> params = new HashMap<>();
    params.put("cvConfigId", cvConfigId);
    params.put("connectorId", connectorId);

    SecretRefData secretRefData = SecretRefData.builder().scope(Scope.ACCOUNT).identifier("secret").build();
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = AppDynamicsConnectorDTO.builder()
                                                          .accountId(accountId)
                                                          .accountname("accountName")
                                                          .username("username")
                                                          .controllerUrl("controllerUrl")
                                                          .passwordRef(secretRefData)
                                                          .build();
    DataCollectionConnectorBundle bundle =
        DataCollectionConnectorBundle.builder().params(params).connectorConfigDTO(appDynamicsConnectorDTO).build();
    String taskId = dataCollectionTaskService.create(accountId, bundle);
    assertThat(taskId).isNotNull();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskService.getTaskRecord(taskId);
    PerpetualTaskClientContext perpetualTaskClientContext = perpetualTaskRecord.getClientContext();
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put("accountId", accountId);
    clientParamMap.put("cvConfigId", cvConfigId);
    clientParamMap.put("connectorId", connectorId);
    assertThat(perpetualTaskClientContext.getClientParams()).isEqualTo(clientParamMap);
    assertThat(perpetualTaskService.getPerpetualTaskType(taskId)).isEqualTo(DATA_COLLECTION_TASK);
    assertThat(perpetualTaskRecord.getIntervalSeconds()).isEqualTo(60);
    assertThat(perpetualTaskRecord.getTimeoutMillis()).isEqualTo(Duration.ofHours(3).toMillis());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void delete() {
    Map<String, String> params = new HashMap<>();
    params.put("cvConfigId", cvConfigId);
    params.put("connectorId", connectorId);
    SecretRefData secretRefData = SecretRefData.builder().scope(Scope.ACCOUNT).identifier("secret").build();
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = AppDynamicsConnectorDTO.builder()
                                                          .accountId(accountId)
                                                          .accountname("accountName")
                                                          .username("username")
                                                          .controllerUrl("controllerUrl")
                                                          .passwordRef(secretRefData)
                                                          .build();
    DataCollectionConnectorBundle bundle =
        DataCollectionConnectorBundle.builder().params(params).connectorConfigDTO(appDynamicsConnectorDTO).build();
    String taskId = dataCollectionTaskService.create(accountId, bundle);
    assertThat(taskId).isNotNull();
    dataCollectionTaskService.delete(accountId, "some-other-id");
    assertThat(perpetualTaskService.getTaskRecord(taskId)).isNotNull();
    dataCollectionTaskService.delete(accountId, taskId);
    assertThat(perpetualTaskService.getTaskRecord(taskId)).isNull();
  }
}