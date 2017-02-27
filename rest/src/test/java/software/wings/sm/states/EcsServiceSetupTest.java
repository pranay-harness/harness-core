package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
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
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.DockerArtifactStream.Builder.aDockerArtifactStream;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.common.collect.Lists;

import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.TaskDefinition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseStepSubWorkflowExecutionData;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.ECSConvention;

import java.util.Date;

/**
 * Created by rishi on 2/27/17.
 */
public class EcsServiceSetupTest extends WingsBaseTest {
  private static final String STATE_NAME = "STATE_NAME";
  private static final String DOCKER_IMAGE = "DOCKER_IMAGE";
  private static final String TASK_FAMILY = "TASK_FAMILY";
  private static final Integer TASK_REVISION = 100;
  @Mock private AwsClusterService awsClusterService;
  @Mock private SettingsService settingsService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactService artifactService;
  @Mock private AppService appService;
  @Mock private EnvironmentService environmentService;

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
          .addContextElement(anEcsServiceElement().withUuid(serviceElement.getUuid()).build())
          .addStateExecutionData(new PhaseStepSubWorkflowExecutionData())
          .build();
  private ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);

  private EcsServiceSetup ecsServiceSetup = new EcsServiceSetup(STATE_NAME);

  private Artifact artifact = anArtifact()
                                  .withAppId(APP_ID)
                                  .withServiceIds(Lists.newArrayList(SERVICE_ID))
                                  .withUuid(ARTIFACT_ID)
                                  .withArtifactStreamId(ARTIFACT_STREAM_ID)
                                  .build();
  private ArtifactStream artifactStream = aDockerArtifactStream().withImageName(DOCKER_IMAGE).build();
  private Application app = anApplication().withUuid(APP_ID).withName(APP_NAME).build();
  private Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).build();
  private Service service = aService().withAppId(APP_ID).withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private SettingAttribute computeProvider = aSettingAttribute().build();
  private TaskDefinition taskDefinition;

  /**
   * Set up.
   */
  @Before
  public void setup() {
    when(appService.get(APP_ID)).thenReturn(app);
    when(environmentService.get(APP_ID, ENV_ID, false)).thenReturn(env);
    when(artifactService.get(APP_ID, ARTIFACT_ID)).thenReturn(artifact);
    on(workflowStandardParams).set("artifactService", artifactService);
    on(workflowStandardParams).set("appService", appService);
    on(workflowStandardParams).set("environmentService", environmentService);

    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactStream);
    on(ecsServiceSetup).set("awsClusterService", awsClusterService);
    on(ecsServiceSetup).set("settingsService", settingsService);
    on(ecsServiceSetup).set("serviceResourceService", serviceResourceService);
    on(ecsServiceSetup).set("infrastructureMappingService", infrastructureMappingService);
    on(ecsServiceSetup).set("artifactStreamService", artifactStreamService);

    when(settingsService.get(APP_ID, COMPUTE_PROVIDER_ID)).thenReturn(computeProvider);

    taskDefinition = new TaskDefinition();
    taskDefinition.setFamily(TASK_FAMILY);
    taskDefinition.setRevision(TASK_REVISION);

    when(awsClusterService.createTask(any(SettingAttribute.class), any(RegisterTaskDefinitionRequest.class)))
        .thenReturn(taskDefinition);
  }

  @Test
  public void shouldThrowInvalidRequest() {
    try {
      ecsServiceSetup.execute(context);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.INVALID_REQUEST.getCode());
      assertThat(exception.getParams()).hasSize(1);
      assertThat(exception.getParams()).containsKey("message");
      assertThat(exception.getParams().get("message")).asString().contains("Invalid infrastructure type");
    }
  }

  @Test
  public void shouldExecuteWithLastService() {
    InfrastructureMapping infrastructureMapping = anEcsInfrastructureMapping()
                                                      .withClusterName(CLUSTER_NAME)
                                                      .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                      .build();
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(infrastructureMapping);
    on(ecsServiceSetup).set("infrastructureMappingService", infrastructureMappingService);

    com.amazonaws.services.ecs.model.Service ecsService = new com.amazonaws.services.ecs.model.Service();
    ecsService.setServiceName(ECSConvention.getServiceName(taskDefinition.getFamily(), taskDefinition.getRevision()));
    ecsService.setCreatedAt(new Date());

    when(awsClusterService.getServices(computeProvider, CLUSTER_NAME)).thenReturn(Lists.newArrayList(ecsService));
    ExecutionResponse response = ecsServiceSetup.execute(context);
    assertThat(response).isNotNull();
    verify(awsClusterService).createTask(any(SettingAttribute.class), any(RegisterTaskDefinitionRequest.class));
    verify(awsClusterService).createService(any(SettingAttribute.class), any(CreateServiceRequest.class));
  }
}
