package software.wings.sm.states;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;

import io.harness.CategoryTest;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.WorkflowStandardParams;

public class AwsLambdaStateTest extends CategoryTest {
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private InfrastructureMappingService infrastructureMappingService;

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
}