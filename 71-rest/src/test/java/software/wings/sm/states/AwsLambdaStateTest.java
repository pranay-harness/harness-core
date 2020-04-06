package software.wings.sm.states;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.command.AwsLambdaCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.WorkflowStandardParams;

public class AwsLambdaStateTest extends CategoryTest {
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private SettingsService settingsService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ActivityService activityService;
  @Mock private LogService logService;
  @Mock private SecretManager secretManager;
  @Mock private ServiceTemplateHelper serviceTemplateHelper;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private DelegateService delegateService;

  @Spy @InjectMocks AwsLambdaState awsLambdaState;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testExecute_fail() {
    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    Application app = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();
    Environment env = anEnvironment().appId(APP_ID).uuid(ENV_ID).name(ENV_NAME).build();
    doReturn(app).when(mockContext).getApp();

    final PhaseElement phaseElement =
        PhaseElement.builder().serviceElement(ServiceElement.builder().uuid("uuid").build()).build();
    doReturn(phaseElement).when(mockContext).getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    doReturn("infraid").when(mockContext).fetchInfraMappingId();

    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    EmbeddedUser mockCurrentUser = mock(EmbeddedUser.class);
    doReturn(mockCurrentUser).when(mockParams).getCurrentUser();
    doReturn(mockParams).when(mockContext).getContextElement(ContextElementType.STANDARD);
    doReturn(app).when(mockParams).fetchRequiredApp();
    doReturn(env).when(mockParams).getEnv();
    doReturn(env).when(mockParams).getEnv();
    doReturn(mock(Service.class)).when(serviceResourceService).getWithDetails(anyString(), anyString());
    final ServiceCommand serviceCommandMock = mock(ServiceCommand.class);
    doReturn(serviceCommandMock)
        .when(serviceResourceService)
        .getCommandByName(anyString(), anyString(), anyString(), anyString());
    doReturn(mock(Command.class)).when(serviceCommandMock).getCommand();
    doReturn(null).when(infrastructureMappingService).get(anyString(), anyString());

    awsLambdaState.execute(mockContext);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecute() {
    when(infrastructureMappingService.get(anyString(), anyString()))
        .thenReturn(AwsLambdaInfraStructureMapping.builder()
                        .uuid(INFRA_MAPPING_ID)
                        .appId(APP_ID)
                        .computeProviderSettingId(SETTING_ID)
                        .envId(ENV_ID)
                        .build());
    when(settingsService.get(SETTING_ID))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().withValue(AwsConfig.builder().build()).build());

    ExecutionContextImpl mockContext = mock(ExecutionContextImpl.class);
    Application app = anApplication().uuid(APP_ID).name(APP_NAME).accountId(ACCOUNT_ID).build();
    doReturn(app).when(mockContext).getApp();

    Environment env = anEnvironment().appId(APP_ID).uuid(ENV_ID).name(ENV_NAME).build();

    final PhaseElement phaseElement =
        PhaseElement.builder().serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build()).build();
    doReturn(phaseElement).when(mockContext).getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    doReturn(INFRA_MAPPING_ID).when(mockContext).fetchInfraMappingId();

    doReturn(Service.builder().uuid(SERVICE_ID).build())
        .when(serviceResourceService)
        .getWithDetails(anyString(), anyString());

    WorkflowStandardParams mockParams = mock(WorkflowStandardParams.class);
    EmbeddedUser mockCurrentUser = mock(EmbeddedUser.class);
    doReturn(mockCurrentUser).when(mockParams).getCurrentUser();
    doReturn(mockParams).when(mockContext).getContextElement(ContextElementType.STANDARD);
    doReturn(app).when(mockParams).fetchRequiredApp();
    doReturn(env).when(mockParams).getEnv();

    ServiceCommand serviceCommand =
        aServiceCommand().withCommand(aCommand().withCommandUnits(asList(new AwsLambdaCommandUnit())).build()).build();
    doReturn(serviceCommand)
        .when(serviceResourceService)
        .getCommandByName(anyString(), anyString(), anyString(), anyString());
    when(((DeploymentExecutionContext) mockContext).getDefaultArtifactForService(SERVICE_ID))
        .thenReturn(anArtifact().withArtifactStreamId(ARTIFACT_STREAM_ID).build());
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(DockerArtifactStream.builder().build());
    when(serviceResourceService.getFlattenCommandUnitList(APP_ID, SERVICE_ID, ENV_ID, null))
        .thenReturn(asList(new AwsLambdaCommandUnit()));
    when(activityService.save(any())).thenReturn(Activity.builder().build());
    when(serviceResourceService.getLambdaSpecification(APP_ID, SERVICE_ID))
        .thenReturn(LambdaSpecification.builder()
                        .functions(asList(
                            LambdaSpecification.FunctionSpecification.builder().functionName("functionName").build()))
                        .build());
    when(mockContext.renderExpression("functionName")).thenReturn("functionName");

    awsLambdaState.execute(mockContext);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getEnvId()).isEqualTo(ENV_ID);
    assertThat(delegateTask.getInfrastructureMappingId()).isEqualTo(INFRA_MAPPING_ID);
  }
}