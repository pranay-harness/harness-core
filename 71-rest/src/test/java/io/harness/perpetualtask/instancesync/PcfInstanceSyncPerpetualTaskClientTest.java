package io.harness.perpetualtask.instancesync;

import static io.harness.rule.OwnerRule.AMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.TaskType.PCF_COMMAND_TASK;
import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
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
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PcfInstanceSyncPerpetualTaskClientTest extends WingsBaseTest {
  private static final String ACCOUNT_ID = "accountId";
  private static final String TASK_ID = "taskId";
  private static final String APPLICATION_NAME = "applicationName";
  private static final String INFRA_ID = "infraId";

  @Mock PerpetualTaskService perpetualTaskService;
  @Mock SettingsService settingsService;
  @Mock SecretManager secretsManager;
  @Mock InfrastructureMappingService infraMappingService;
  @InjectMocks PcfInstanceSyncPerpetualTaskClient pcfInstanceSyncPerpetualTaskClient;

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
        .thenReturn(PcfInfrastructureMapping.builder().appId(HARNESS_APPLICATION_ID).uuid(INFRA_ID).build());
    when(settingsService.get(anyString()))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().withValue(pcfConfig).build());
    when(perpetualTaskService.getTaskRecord(TASK_ID))
        .thenReturn(PerpetualTaskRecord.builder().accountId(ACCOUNT_ID).build());
    when(secretsManager.getEncryptionDetails(pcfConfig, null, null)).thenReturn(Collections.emptyList());
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testCreate() {
    PcfInstanceSyncPerpetualTaskClientParams pcfInstanceSyncParams = getPcfInstanceSyncPerpTaskClientParams();

    String taskId = pcfInstanceSyncPerpetualTaskClient.create(ACCOUNT_ID, pcfInstanceSyncParams);
    PerpetualTaskSchedule taskSchedule = scheduleArgumentCaptor.getValue();

    assertThat(taskId).isEqualTo(TASK_ID);
    assertThat(taskSchedule.getInterval()).isEqualTo(Durations.fromMinutes(INTERVAL_MINUTES));
    assertThat(taskSchedule.getTimeout()).isEqualTo(Durations.fromSeconds(TIMEOUT_SECONDS));
    Mockito.verify(perpetualTaskService, Mockito.times(1)).createTask(any(), anyString(), any(), any(), eq(false));
  }

  private PcfInstanceSyncPerpetualTaskClientParams getPcfInstanceSyncPerpTaskClientParams() {
    return PcfInstanceSyncPerpetualTaskClientParams.builder()
        .appId(HARNESS_APPLICATION_ID)
        .inframappingId(INFRA_ID)
        .applicationName(APPLICATION_NAME)
        .build();
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testReset() {
    pcfInstanceSyncPerpetualTaskClient.reset(ACCOUNT_ID, TASK_ID);
    Mockito.verify(perpetualTaskService, Mockito.times(1)).resetTask(ACCOUNT_ID, TASK_ID);
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testDelete() {
    pcfInstanceSyncPerpetualTaskClient.delete(ACCOUNT_ID, TASK_ID);
    Mockito.verify(perpetualTaskService, Mockito.times(1)).deleteTask(ACCOUNT_ID, TASK_ID);
  }

  private PerpetualTaskClientContext getPerpetualTaskClientContext() {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(INFRASTRUCTURE_MAPPING_ID, INFRA_ID);
    clientParamMap.put(HARNESS_APPLICATION_ID, HARNESS_APPLICATION_ID);
    clientParamMap.put(PcfInstanceSyncPerpetualTaskClient.PCF_APPLICATION_NAME, APPLICATION_NAME);
    return new PerpetualTaskClientContext(clientParamMap);
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void getValidationTask() {
    PerpetualTaskClientContext clientContext = getPerpetualTaskClientContext();
    DelegateTask validationTask = pcfInstanceSyncPerpetualTaskClient.getValidationTask(clientContext, ACCOUNT_ID);

    assertThat(validationTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(validationTask.getData().getTaskType()).isEqualTo(PCF_COMMAND_TASK.name());
  }
}