package software.wings.beans.command;

import static io.harness.rule.OwnerRule.BRETT;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder.anEcsSetupParams;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TASK_FAMILY;
import static software.wings.utils.WingsTestConstants.TASK_REVISION;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.k8s.model.ImageDetails;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsSetupCommandHandler;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsSetupCommandTaskHelper;
import software.wings.utils.EcsConvention;
import software.wings.utils.WingsTestConstants;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class EcsSetupCommandUnitTest extends WingsBaseTest {
  public static final String CLUSTER_NAME = "clusterName";
  @Mock private AwsClusterService awsClusterService;
  @InjectMocks @Inject private EcsSetupCommandTaskHelper ecsSetupCommandTaskHelper;
  @InjectMocks @Inject private EcsSetupCommandHandler ecsSetupCommandHandler;
  @InjectMocks @Inject private EcsSetupCommandUnit ecsSetupCommandUnit;

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
  private SettingAttribute computeProvider = aSettingAttribute().withValue(AwsConfig.builder().build()).build();
  private CommandExecutionContext context = aCommandExecutionContext()
                                                .cloudProviderSetting(computeProvider)
                                                .containerSetupParams(setupParams)
                                                .cloudProviderCredentials(emptyList())
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
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldExecuteWithLastService() {
    com.amazonaws.services.ecs.model.Service ecsService = new com.amazonaws.services.ecs.model.Service();
    ecsService.setServiceName(EcsConvention.getServiceName(taskDefinition.getFamily(), taskDefinition.getRevision()));
    ecsService.setCreatedAt(new Date());

    when(awsClusterService.createTask(anyString(), any(), anyList(), any())).thenReturn(new TaskDefinition());
    when(awsClusterService.getServices(
             Regions.US_EAST_1.getName(), computeProvider, Collections.emptyList(), WingsTestConstants.CLUSTER_NAME))
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
