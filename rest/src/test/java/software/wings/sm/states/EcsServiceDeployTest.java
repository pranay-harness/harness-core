package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.EcsServiceElement.EcsServiceElementBuilder.anEcsServiceElement;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.sm.states.EcsServiceDeploy.EcsServiceDeployBuilder.anEcsServiceDeploy;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ECS_SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.STATE_NAME;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepSubWorkflowExecutionData;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.common.VariableProcessor;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.Date;

/**
 * Created by rishi on 2/27/17.
 */
public class EcsServiceDeployTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;
  @Mock private DelegateService delegateService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ActivityService activityService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private AwsClusterService awsClusterService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;
  @Mock private VariableProcessor variableProcessor;

  private WorkflowStandardParams workflowStandardParams = aWorkflowStandardParams()
                                                              .withAppId(APP_ID)
                                                              .withEnvId(ENV_ID)
                                                              .withArtifactIds(Lists.newArrayList(ARTIFACT_ID))
                                                              .build();
  private ServiceElement serviceElement = aServiceElement().withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private PhaseElement phaseElement = aPhaseElement()
                                          .withUuid(getUuid())
                                          .withServiceElement(serviceElement)
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .build();
  private StateExecutionInstance stateExecutionInstance =
      aStateExecutionInstance()
          .withStateName(STATE_NAME)
          .addContextElement(workflowStandardParams)
          .addContextElement(phaseElement)
          .addContextElement(anEcsServiceElement()
                                 .withUuid(serviceElement.getUuid())
                                 .withClusterName(CLUSTER_NAME)
                                 .withName(ECS_SERVICE_NAME)
                                 .build())
          .addStateExecutionData(new PhaseStepSubWorkflowExecutionData())
          .build();
  private ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build();
  private Service service = aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private SettingAttribute computeProvider = aSettingAttribute().build();

  private EcsServiceDeploy ecsServiceDeploy =
      anEcsServiceDeploy(STATE_NAME).withCommandName(COMMAND_NAME).withInstanceCount(1).build();

  /**
   * Set up.
   */
  @Before
  public void setup() {
    when(appService.get(APP_ID)).thenReturn(app);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(env);

    ServiceCommand serviceCommand =
        aServiceCommand()
            .withCommand(aCommand().withCommandType(CommandType.RESIZE).withName(COMMAND_NAME).build())
            .build();
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, ENV_ID, COMMAND_NAME)).thenReturn(serviceCommand);
    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);

    on(context).set("variableProcessor", variableProcessor);

    on(ecsServiceDeploy).set("settingsService", settingsService);
    on(ecsServiceDeploy).set("delegateService", delegateService);
    on(ecsServiceDeploy).set("serviceResourceService", serviceResourceService);
    on(ecsServiceDeploy).set("activityService", activityService);
    on(ecsServiceDeploy).set("infrastructureMappingService", infrastructureMappingService);
    on(ecsServiceDeploy).set("awsClusterService", awsClusterService);

    InfrastructureMapping infrastructureMapping = anEcsInfrastructureMapping()
                                                      .withClusterName(CLUSTER_NAME)
                                                      .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                      .build();
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);

    Activity activity = Activity.Builder.anActivity().withUuid(getUuid()).build();
    when(activityService.save(any(Activity.class))).thenReturn(activity);

    com.amazonaws.services.ecs.model.Service ecsService = new com.amazonaws.services.ecs.model.Service();
    ecsService.setServiceName(ECS_SERVICE_NAME);
    ecsService.setCreatedAt(new Date());
    ecsService.setDesiredCount(0);
    when(awsClusterService.getServices(computeProvider, CLUSTER_NAME)).thenReturn(Lists.newArrayList(ecsService));

    when(settingsService.get(APP_ID, COMPUTE_PROVIDER_ID)).thenReturn(computeProvider);
  }

  @Test
  public void shouldExecute() {
    ExecutionResponse response = ecsServiceDeploy.execute(context);
    assertThat(response).isNotNull();
    verify(activityService).save(any(Activity.class));
    verify(delegateService).queueTask(any(DelegateTask.class));
  }
}
