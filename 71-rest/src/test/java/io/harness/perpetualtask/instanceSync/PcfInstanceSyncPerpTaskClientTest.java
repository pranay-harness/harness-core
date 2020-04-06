package io.harness.perpetualtask.instanceSync;

import static io.harness.rule.OwnerRule.AMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.beans.TaskType.PCF_COMMAND_TASK;
import static software.wings.service.PcfInstanceSyncConstants.APPLICATION_ID;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.service.PcfInstanceSyncConstants;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.Map;

public class PcfInstanceSyncPerpTaskClientTest extends WingsBaseTest {
  private static final String ACCOUNT_ID = "accountId";
  private static final String TASK_ID = "taskId";
  private static final String APPLICATION_NAME = "applicationName";
  private static final String INFRA_ID = "infraId";
  private static final int EXPECTED_SCHEDULED_INTERVAL = 600;
  private static final String STATE_CHANGE_ACCOUNT_ID = "stateChangeAccountId";

  @Mock PerpetualTaskService perpetualTaskService;
  @Mock SettingsService settingsService;
  @Mock InfrastructureMappingService infraMappingService;
  @Mock SecretManager secretManager;
  @InjectMocks PcfInstanceSyncPerpTaskClient pcfInstanceSyncPerpTaskClient;

  @Captor ArgumentCaptor<PerpetualTaskSchedule> scheduleArgumentCaptor;
  @Captor ArgumentCaptor<String> stringCaptor;

  @Before
  public void setUp() throws Exception {
    PcfConfig pcfConfig = PcfConfig.builder().accountId(ACCOUNT_ID).build();
    when(perpetualTaskService.deleteTask(any(), any())).thenReturn(true);
    when(perpetualTaskService.resetTask(stringCaptor.capture(), any())).thenReturn(true);
    when(perpetualTaskService.createTask(any(), anyString(), any(), scheduleArgumentCaptor.capture(), anyBoolean()))
        .thenReturn(TASK_ID);
    when(infraMappingService.get(anyString(), anyString()))
        .thenReturn(PcfInfrastructureMapping.builder().appId(APPLICATION_ID).uuid(INFRA_ID).build());
    when(settingsService.get(anyString()))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().withValue(pcfConfig).build());
    when(perpetualTaskService.getTaskRecord(TASK_ID))
        .thenReturn(PerpetualTaskRecord.builder().accountId(STATE_CHANGE_ACCOUNT_ID).build());
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testCreate() {
    PcfInstanceSyncPerpTaskClientParams pcfInstanceSyncParams = getPcfInstanceSyncPerpTaskClientParams();

    String taskId = pcfInstanceSyncPerpTaskClient.create(ACCOUNT_ID, pcfInstanceSyncParams);
    PerpetualTaskSchedule taskSchedule = scheduleArgumentCaptor.getValue();

    assertThat(taskId).isEqualTo(TASK_ID);
    assertThat(taskSchedule.getInterval().getSeconds()).isEqualTo(EXPECTED_SCHEDULED_INTERVAL);
    assertThat(taskSchedule.getTimeout().getSeconds()).isEqualTo(600);
    Mockito.verify(perpetualTaskService, Mockito.times(1)).createTask(any(), anyString(), any(), any(), eq(false));
  }

  private PcfInstanceSyncPerpTaskClientParams getPcfInstanceSyncPerpTaskClientParams() {
    return PcfInstanceSyncPerpTaskClientParams.builder()
        .appId(APPLICATION_ID)
        .inframappingId(INFRA_ID)
        .applicationName(APPLICATION_NAME)
        .build();
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testReset() {
    pcfInstanceSyncPerpTaskClient.reset(ACCOUNT_ID, TASK_ID);
    Mockito.verify(perpetualTaskService, Mockito.times(1)).resetTask(ACCOUNT_ID, TASK_ID);
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testDelete() {
    pcfInstanceSyncPerpTaskClient.delete(ACCOUNT_ID, TASK_ID);
    Mockito.verify(perpetualTaskService, Mockito.times(1)).deleteTask(ACCOUNT_ID, TASK_ID);
  }

  private PerpetualTaskClientContext getPerpetualTaskClientContext() {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(ID_KEY, INFRA_ID);
    clientParamMap.put(APPLICATION_ID, APPLICATION_ID);
    clientParamMap.put(PcfInstanceSyncConstants.APPLICATION_NAME, APPLICATION_NAME);
    return new PerpetualTaskClientContext(clientParamMap);
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void getValidationTask() {
    PerpetualTaskClientContext clientContext = getPerpetualTaskClientContext();
    DelegateTask validationTask = pcfInstanceSyncPerpTaskClient.getValidationTask(clientContext, ACCOUNT_ID);

    assertThat(validationTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(validationTask.getData().getTaskType()).isEqualTo(PCF_COMMAND_TASK.name());
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testOnTaskStateChange() {
    PerpetualTaskResponse perpetualTaskResponse = PerpetualTaskResponse.builder()
                                                      .responseCode(200)
                                                      .perpetualTaskState(PerpetualTaskState.TASK_RUN_FAILED)
                                                      .build();
    pcfInstanceSyncPerpTaskClient.onTaskStateChange(TASK_ID, perpetualTaskResponse, perpetualTaskResponse);
    Mockito.verify(perpetualTaskService, Mockito.times(1)).resetTask(STATE_CHANGE_ACCOUNT_ID, TASK_ID);
    assertThat(stringCaptor.getValue()).isEqualTo(STATE_CHANGE_ACCOUNT_ID);
  }
}