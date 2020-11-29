package software.wings.delegatetasks.pcf;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.ANIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.WingsBaseTest;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfCommandTaskHandler;
import software.wings.delegatetasks.pcf.pcftaskhandler.PcfSetupCommandTaskHandler;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandTaskParameters;
import software.wings.helpers.ext.pcf.request.PcfRunPluginCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.service.impl.aws.model.AwsAsgListAllNamesRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class PcfCommandTaskTest extends WingsBaseTest {
  @Mock PcfDelegateTaskHelper pcfDelegateTaskHelper;
  @InjectMocks
  private final PcfCommandTask pcfTask =
      new PcfCommandTask(DelegateTaskPackage.builder()
                             .delegateId("delegateId")
                             .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())

                             .build(),
          null, notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(pcfTask).set("pcfDelegateTaskHelper", pcfDelegateTaskHelper);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testRun() {
    PcfCommandSetupRequest pcfCommandRequest = PcfCommandSetupRequest.builder().build();
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();
    PcfCommandTaskParameters taskParameters = PcfCommandTaskParameters.builder()
                                                  .pcfCommandRequest(pcfCommandRequest)
                                                  .encryptedDataDetails(encryptedDataDetails)
                                                  .build();
    pcfTask.run(new Object[] {taskParameters});
    verify(pcfDelegateTaskHelper, times(1))
        .getPcfCommandExecutionResponse(eq(pcfCommandRequest), eq(encryptedDataDetails), eq(false));

    reset(pcfDelegateTaskHelper);
    pcfTask.run(new Object[] {pcfCommandRequest, encryptedDataDetails});
    verify(pcfDelegateTaskHelper, times(1))
        .getPcfCommandExecutionResponse(eq(pcfCommandRequest), eq(encryptedDataDetails), eq(false));

    reset(pcfDelegateTaskHelper);
    PcfRunPluginCommandRequest pluginCommandRequest =
        PcfRunPluginCommandRequest.builder().encryptedDataDetails(encryptedDataDetails).build();
    pcfTask.run(pluginCommandRequest);
    verify(pcfDelegateTaskHelper, times(1))
        .getPcfCommandExecutionResponse(eq(pluginCommandRequest), eq(encryptedDataDetails), eq(false));

    reset(pcfDelegateTaskHelper);
    assertThatThrownBy(() -> pcfTask.run(AwsAsgListAllNamesRequest.builder().build()))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testDelegateTaskHelper() {
    PcfCommandSetupRequest pcfCommandRequest =
        PcfCommandSetupRequest.builder().pcfCommandType(PcfCommandRequest.PcfCommandType.SETUP).build();
    List<EncryptedDataDetail> encryptedDataDetails = Collections.emptyList();
    Map<String, PcfCommandTaskHandler> commandTaskTypeToTaskHandlerMap = new HashMap<>();
    PcfSetupCommandTaskHandler mockHandler = mock(PcfSetupCommandTaskHandler.class);
    commandTaskTypeToTaskHandlerMap.put(PcfCommandRequest.PcfCommandType.SETUP.name(), mockHandler);

    PcfDelegateTaskHelper delegateTaskHelper = new PcfDelegateTaskHelper();
    on(delegateTaskHelper).set("commandTaskTypeToTaskHandlerMap", commandTaskTypeToTaskHandlerMap);
    delegateTaskHelper.getPcfCommandExecutionResponse(pcfCommandRequest, encryptedDataDetails, false);
    verify(mockHandler, times(1)).executeTask(eq(pcfCommandRequest), eq(encryptedDataDetails), eq(false));

    doThrow(Exception.class).when(mockHandler).executeTask(eq(pcfCommandRequest), eq(encryptedDataDetails), eq(false));
    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        delegateTaskHelper.getPcfCommandExecutionResponse(pcfCommandRequest, encryptedDataDetails, eq(false));
    assertThat(pcfCommandExecutionResponse).isNotNull();
    assertThat(pcfCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }
}
