package software.wings.sm.states;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SweepingOutput.Scope.PIPELINE;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.waiter.ErrorNotifyResponseData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.api.ScriptStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SweepingOutput;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.expression.ShellScriptFunctor;
import software.wings.service.impl.ActivityHelperService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

import java.util.HashMap;
import java.util.Map;

public class ShellScriptStateTest {
  private static final Activity ACTIVITY_WITH_ID = Activity.builder().build();

  static {
    ACTIVITY_WITH_ID.setUuid(ACTIVITY_ID);
  }
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ActivityHelperService activityHelperService;
  @Mock private ExecutionContextImpl executionContext;
  @Mock private SweepingOutputService sweepingOutputService;
  @InjectMocks private ShellScriptFunctor shellScriptFunctor;

  @InjectMocks private ShellScriptState shellScriptState = new ShellScriptState("ShellScript");

  @Before
  public void setUp() throws Exception {
    shellScriptState.setSweepingOutputName("test");
    shellScriptState.setSweepingOutputScope(PIPELINE);
    when(executionContext.getApp()).thenReturn(anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).build());
    when(executionContext.getEnv()).thenReturn(anEnvironment().withUuid(ENV_ID).withAppId(APP_ID).build());
    HostConnectionAttributes hostConnectionAttributes = aHostConnectionAttributes()
                                                            .withAccessType(HostConnectionAttributes.AccessType.KEY)
                                                            .withAccountId(UUIDGenerator.generateUuid())
                                                            .withConnectionType(SSH)
                                                            .withKey("Test Private Key".toCharArray())
                                                            .withKeyless(false)
                                                            .withUserName("TestUser")
                                                            .build();
    when(executionContext.getGlobalSettingValue(ACCOUNT_ID, SETTING_ID)).thenReturn(hostConnectionAttributes);
    when(executionContext.renderExpression(anyString()))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, String.class));
    when(activityHelperService.createAndSaveActivity(executionContext, Type.Verification, shellScriptState.getName(),
             shellScriptState.getStateType(),
             asList(aCommand().withName(ShellScriptParameters.CommandUnit).withCommandType(CommandType.OTHER).build())))
        .thenReturn(ACTIVITY_WITH_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldHandleAsyncResponseOnShellScriptSuccessAndSaveSweepingOutput() {
    when(executionContext.getStateExecutionData())
        .thenReturn(ScriptStateExecutionData.builder().activityId(ACTIVITY_ID).build());
    when(executionContext.prepareSweepingOutputBuilder(any(SweepingOutput.Scope.class)))
        .thenReturn(SweepingOutput.builder());

    Map<String, String> map = new HashMap<>();
    map.put("A", "aaa");
    ExecutionResponse executionResponse = shellScriptState.handleAsyncResponse(executionContext,
        ImmutableMap.of(ACTIVITY_ID,
            CommandExecutionResult.builder()
                .status(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
                .commandExecutionData(ShellExecutionData.builder().sweepingOutputEnvVariables(map).build())
                .build()));
    verify(activityHelperService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
    verify(sweepingOutputService, times(1)).save(any(SweepingOutput.class));
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(
        ((ScriptStateExecutionData) (executionResponse.getStateExecutionData())).getSweepingOutputEnvVariables().size())
        .isEqualTo(1);
    assertThat(((ScriptStateExecutionData) (executionResponse.getStateExecutionData()))
                   .getSweepingOutputEnvVariables()
                   .containsKey("A"));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldHandleAsyncResponseOnShellScriptFailureAndNotSaveSweepingOutput() {
    when(executionContext.getStateExecutionData())
        .thenReturn(ScriptStateExecutionData.builder().activityId(ACTIVITY_ID).build());
    Map<String, String> map = new HashMap<>();
    map.put("A", "aaa");
    ExecutionResponse executionResponse = shellScriptState.handleAsyncResponse(executionContext,
        ImmutableMap.of(ACTIVITY_ID,
            CommandExecutionResult.builder()
                .status(CommandExecutionResult.CommandExecutionStatus.FAILURE)
                .commandExecutionData(ShellExecutionData.builder().sweepingOutputEnvVariables(map).build())
                .build()));
    verify(activityHelperService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
    verify(sweepingOutputService, times(0)).save(any(SweepingOutput.class));
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(((ScriptStateExecutionData) (executionResponse.getStateExecutionData())).getSweepingOutputEnvVariables())
        .isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFailShellScriptStateOnErrorResponse() {
    ExecutionResponse executionResponse = shellScriptState.handleAsyncResponse(executionContext,
        ImmutableMap.of(ACTIVITY_ID, ErrorNotifyResponseData.builder().errorMessage("Failed").build()));
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Category(UnitTests.class)
  public void testEscapifyBash() {
    assertThat(shellScriptFunctor.escapeString("a'b\"c`d$e~f!g@h#i%j^k&l*m(n)o-p_r{s}t[]|;:u,v.w/x?y")
                   .equals("a\\'b\\\"c\\`d\\$e~f!g@h#i%j^k\\&l*m\\(n\\)o-p_r{s}t[]\\|\\;:u,v.w/x?y"));
  }

  @Test
  @Category(UnitTests.class)
  public void testEscapifyPowershell() {
    assertThat(shellScriptFunctor.escapeString("a'b\"c`d$e~f!g@h#i%j^k&l*m(n)o-p_r{s}t[]|;:u,v.w/x?y")
                   .equals("a\\'b\\\"c\\`d\\$e~f!g@h#i%j^k\\&l*m\\(n\\)o-p_r{s}t[]\\|\\;:u,v.w/x?y"));
  }
}
