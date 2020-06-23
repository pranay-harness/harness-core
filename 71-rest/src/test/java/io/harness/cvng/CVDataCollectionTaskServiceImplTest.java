package io.harness.cvng;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.perpetualtask.PerpetualTaskType.DATA_COLLECTION_TASK;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.cvng.core.services.entities.SplunkCVConfig;
import io.harness.cvng.perpetualtask.CVDataCollectionTaskService;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

import java.util.HashMap;
import java.util.Map;

public class CVDataCollectionTaskServiceImplTest extends WingsBaseTest {
  @Inject private CVDataCollectionTaskService dataCollectionTaskService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Mock private CVConfigService cvConfigService;
  private String accountId;
  private String cvConfigId;
  private String connectorId;
  @Before
  public void setup() throws IllegalAccessException {
    initMocks(this);
    accountId = generateUuid();
    cvConfigId = generateUuid();
    connectorId = generateUuid();
    FieldUtils.writeField(dataCollectionTaskService, "cvConfigService", cvConfigService, true);
    CVConfig cvConfig = new SplunkCVConfig();
    cvConfig.setConnectorId(connectorId);
    when(cvConfigService.get(eq(cvConfigId))).thenReturn(cvConfig);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void create() {
    String taskId = dataCollectionTaskService.create(accountId, cvConfigId);
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
    assertThat(perpetualTaskRecord.getTimeoutMillis()).isEqualTo(900000);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void delete() {
    String taskId = dataCollectionTaskService.create(accountId, cvConfigId);
    assertThat(taskId).isNotNull();
    dataCollectionTaskService.delete(accountId, "some-other-id");
    assertThat(perpetualTaskService.getTaskRecord(taskId)).isNotNull();
    dataCollectionTaskService.delete(accountId, taskId);
    assertThat(perpetualTaskService.getTaskRecord(taskId)).isNull();
  }
}