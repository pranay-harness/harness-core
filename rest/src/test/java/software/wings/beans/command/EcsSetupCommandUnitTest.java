package software.wings.beans.command;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder.anEcsSetupParams;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TASK_FAMILY;
import static software.wings.utils.WingsTestConstants.TASK_REVISION;

import com.google.common.collect.Lists;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.container.ImageDetails;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.utils.EcsConvention;

import java.util.Collections;
import java.util.Date;

public class EcsSetupCommandUnitTest extends WingsBaseTest {
  @Mock private AwsClusterService awsClusterService;

  @InjectMocks private EcsSetupCommandUnit ecsSetupCommandUnit = new EcsSetupCommandUnit();

  private EcsSetupParams setupParams =
      anEcsSetupParams()
          .withAppName(APP_NAME)
          .withEnvName(ENV_NAME)
          .withServiceName(SERVICE_NAME)
          .withImageDetails(
              ImageDetails.builder().registryUrl("ecr").sourceName("ECR").name("todolist").tag("v1").build())
          .withInfraMappingId(INFRA_MAPPING_ID)
          .withRegion(Regions.US_EAST_1.getName())
          .withRoleArn("roleArn")
          .withTargetGroupArn("targetGroupArn")
          .withTaskFamily(TASK_FAMILY)
          .withUseLoadBalancer(false)
          .withClusterName("cluster")
          .build();
  private SettingAttribute computeProvider = aSettingAttribute().withValue(GcpConfig.builder().build()).build();
  private CommandExecutionContext context = aCommandExecutionContext()
                                                .withCloudProviderSetting(computeProvider)
                                                .withContainerSetupParams(setupParams)
                                                .withCloudProviderCredentials(emptyList())
                                                .build();
  private TaskDefinition taskDefinition;

  /**
   * Set up.
   */
  @Before
  public void setup() {
    taskDefinition = new TaskDefinition();
    taskDefinition.setFamily(TASK_FAMILY);
    taskDefinition.setRevision(TASK_REVISION);

    when(awsClusterService.createTask(eq(Regions.US_EAST_1.getName()), any(SettingAttribute.class), any(),
             any(RegisterTaskDefinitionRequest.class)))
        .thenReturn(taskDefinition);
  }

  @Test
  public void shouldExecuteWithLastService() {
    com.amazonaws.services.ecs.model.Service ecsService = new com.amazonaws.services.ecs.model.Service();
    ecsService.setServiceName(EcsConvention.getServiceName(taskDefinition.getFamily(), taskDefinition.getRevision()));
    ecsService.setCreatedAt(new Date());

    when(awsClusterService.getServices(
             Regions.US_EAST_1.getName(), computeProvider, Collections.emptyList(), CLUSTER_NAME))
        .thenReturn(Lists.newArrayList(ecsService));
    CommandExecutionStatus status = ecsSetupCommandUnit.execute(context);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(awsClusterService)
        .createTask(eq(Regions.US_EAST_1.getName()), any(SettingAttribute.class), any(),
            any(RegisterTaskDefinitionRequest.class));
    verify(awsClusterService)
        .createService(
            eq(Regions.US_EAST_1.getName()), any(SettingAttribute.class), any(), any(CreateServiceRequest.class));
  }
}
