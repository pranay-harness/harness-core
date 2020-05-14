package software.wings.sm.states.provision;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner;
import software.wings.beans.shellscript.provisioner.ShellScriptProvisionParameters;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.sm.ExecutionContextImpl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ShellScriptProvisionStateTest extends WingsBaseTest {
  @Mock private DelegateService delegateService;
  @Mock private ActivityService activityService;
  @Mock private InfrastructureProvisionerService infrastructureProvisionerService;

  @InjectMocks
  private ShellScriptProvisionState state =
      new ShellScriptProvisionState(InfrastructureProvisionerType.SHELL_SCRIPT.name());

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testParseOutput() {
    assertThat(state.parseOutput(null)).isEqualTo(Collections.emptyMap());
    assertThat(state.parseOutput("")).isEqualTo(Collections.emptyMap());

    String json = "{\n"
        + "\t\"key1\":\"val1\",\n"
        + "\t\"key2\":\"val2\"\n"
        + "}";
    Map<String, Object> expectedMap = new LinkedHashMap<>();
    expectedMap.put("key1", "val1");
    expectedMap.put("key2", "val2");
    assertThat(state.parseOutput(json)).isEqualTo(expectedMap);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testValidation() {
    assertThat(state.validateFields().size()).isNotEqualTo(0);
    state.setProvisionerId("test provisioner");
    assertThat(state.validateFields().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void shouldPopulateWorkflowExecutionIdParamFromExecutionContext() {
    ExecutionContextImpl executionContext = mock(ExecutionContextImpl.class);
    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    when(activityService.save(any())).thenReturn(mock(Activity.class));
    when(executionContext.getApp()).thenReturn(mock(Application.class));
    when(executionContext.getEnv()).thenReturn(mock(Environment.class));
    when(infrastructureProvisionerService.getShellScriptProvisioner(anyString(), anyString()))
        .thenReturn(mock(ShellScriptInfrastructureProvisioner.class));
    when(executionContext.getWorkflowExecutionId()).thenReturn("workflow-execution-id");
    state.execute(executionContext);

    Mockito.verify(delegateService).queueTask(delegateTaskArgumentCaptor.capture());
    ShellScriptProvisionParameters populatedParameters =
        (ShellScriptProvisionParameters) delegateTaskArgumentCaptor.getValue().getData().getParameters()[0];
    assertThat(populatedParameters.getWorkflowExecutionId()).isEqualTo("workflow-execution-id");
  }
}
