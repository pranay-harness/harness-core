package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.AMAN;

import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskClientParams;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.api.PcfDeploymentInfo;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.Service;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;

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
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class PCFInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "accountId";
  private static final String TASK_ID = "taskId";
  private static final String APPLICATION_NAME = "applicationName";
  private static final String INFRA_ID = "infraId";
  private static final String APP_ID = "appId";
  @Mock PerpetualTaskService perpetualTaskService;
  @InjectMocks PCFInstanceSyncPerpetualTaskCreator pcfInstanceSyncPerpetualTaskCreator;
  @Captor ArgumentCaptor<PerpetualTaskSchedule> scheduleArgumentCaptor;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private ServiceResourceService serviceResourceService;
  private PcfInfrastructureMapping pcfInfrastructureMapping;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(perpetualTaskService.createTask(
             any(), anyString(), any(), scheduleArgumentCaptor.capture(), eq(false), anyString()))
        .thenReturn(TASK_ID);
    pcfInfrastructureMapping = PcfInfrastructureMapping.builder()
                                   .uuid(INFRASTRUCTURE_MAPPING_ID)
                                   .accountId(ACCOUNT_ID)
                                   .name(INFRASTRUCTURE_MAPPING_ID)
                                   .build();
    pcfInfrastructureMapping.setDisplayName("infraName");
    when(infrastructureMappingService.get(any(), any())).thenReturn(pcfInfrastructureMapping);
    when(environmentService.get(any(), any()))
        .thenReturn(Environment.Builder.anEnvironment().accountId(ACCOUNT_ID).appId(APP_ID).build());
    when(serviceResourceService.get(any(), any()))
        .thenReturn(Service.builder().accountId(ACCOUNT_ID).appId(APP_ID).build());
    when(appService.get(any()))
        .thenReturn(Application.Builder.anApplication().appId(APP_ID).accountId(ACCOUNT_ID).build());
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testCreate() {
    PcfInstanceSyncPerpetualTaskClientParams pcfInstanceSyncParams = getPcfInstanceSyncPerpTaskClientParams();

    String taskId = pcfInstanceSyncPerpetualTaskCreator.create(pcfInstanceSyncParams, pcfInfrastructureMapping);
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
