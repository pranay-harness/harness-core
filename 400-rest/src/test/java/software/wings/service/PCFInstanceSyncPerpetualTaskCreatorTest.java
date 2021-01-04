package software.wings.service;

import static io.harness.rule.OwnerRule.AMAN;

import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskClientParams;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.api.PcfDeploymentInfo;
import software.wings.beans.PcfInfrastructureMapping;

import com.google.protobuf.util.Durations;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class PCFInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "accountId";
  private static final String TASK_ID = "taskId";
  private static final String APPLICATION_NAME = "applicationName";
  private static final String INFRA_ID = "infraId";
  @Mock PerpetualTaskService perpetualTaskService;
  @InjectMocks PCFInstanceSyncPerpetualTaskCreator pcfInstanceSyncPerpetualTaskCreator;
  @Captor ArgumentCaptor<PerpetualTaskSchedule> scheduleArgumentCaptor;

  @Before
  public void setUp() throws Exception {
    when(perpetualTaskService.createTask(
             any(), anyString(), any(), scheduleArgumentCaptor.capture(), eq(false), anyString()))
        .thenReturn(TASK_ID);
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testCreate() {
    PcfInstanceSyncPerpetualTaskClientParams pcfInstanceSyncParams = getPcfInstanceSyncPerpTaskClientParams();

    String taskId = pcfInstanceSyncPerpetualTaskCreator.create(ACCOUNT_ID, pcfInstanceSyncParams);
    PerpetualTaskSchedule taskSchedule = scheduleArgumentCaptor.getValue();

    assertThat(taskId).isEqualTo(TASK_ID);
    assertThat(taskSchedule.getInterval()).isEqualTo(Durations.fromMinutes(INTERVAL_MINUTES));
    assertThat(taskSchedule.getTimeout()).isEqualTo(Durations.fromSeconds(TIMEOUT_SECONDS));
    Mockito.verify(perpetualTaskService, Mockito.times(1))
        .createTask(any(), anyString(), any(), any(), eq(false), anyString());
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
  public void createPerpetualTaskForNewDeployment() {
    DeploymentSummary deploymentSummary =
        DeploymentSummary.builder()
            .appId("appId")
            .accountId("accountId")
            .infraMappingId("infraId")
            .deploymentInfo(
                PcfDeploymentInfo.builder().applicationGuild("guid").applicationName("applicationName").build())
            .build();
    List<String> tasks = pcfInstanceSyncPerpetualTaskCreator.createPerpetualTasksForNewDeployment(
        Collections.singletonList(deploymentSummary), Collections.emptyList(), new PcfInfrastructureMapping());
    assertEquals(1, tasks.size());
  }
}
