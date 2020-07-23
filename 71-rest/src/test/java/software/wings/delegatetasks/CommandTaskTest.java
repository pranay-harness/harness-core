package software.wings.delegatetasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SAHIL;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.when;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.CommandType.ENABLE;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.PUBLIC_DNS;
import static software.wings.utils.WingsTestConstants.SSH_USER_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.infrastructure.Host;
import software.wings.service.intfc.ServiceCommandExecutorService;

public class CommandTaskTest extends WingsBaseTest {
  @Mock ServiceCommandExecutorService serviceCommandExecutorService;

  DelegateTaskPackage delegateTaskPackage =
      DelegateTaskPackage.builder()
          .delegateId("delegateid")
          .delegateTask(DelegateTask.builder()
                            .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                            .build())
          .build();

  @InjectMocks CommandTask commandTask = new CommandTask(delegateTaskPackage, null, null);

  private Host.Builder builder = aHost().withAppId(APP_ID).withHostName(HOST_NAME).withPublicDns(PUBLIC_DNS);
  private CommandExecutionContext commandExecutionContextBuider =
      aCommandExecutionContext()
          .appId(APP_ID)
          .activityId(ACTIVITY_ID)
          .runtimePath("/tmp/runtime")
          .backupPath("/tmp/backup")
          .stagingPath("/tmp/staging")
          .executionCredential(aSSHExecutionCredential().withSshUser(SSH_USER_NAME).build())
          .artifactFiles(Lists.newArrayList(anArtifactFile().withName("artifact.war").withFileUuid(FILE_ID).build()))
          .serviceVariables(ImmutableMap.of("PORT", "8080", "PASSWORD", "aSecret"))
          .safeDisplayServiceVariables(ImmutableMap.of("PORT", "8080", "PASSWORD", "*****"))
          .host(builder.build())
          .accountId(ACCOUNT_ID)
          .build();

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRunWithObjectParameters() {
    Command command = aCommand().withName("Ami-Command").withCommandType(ENABLE).build();
    CommandExecutionResult expectedCommandExecutionResult =
        CommandExecutionResult.builder()
            .status(CommandExecutionStatus.SUCCESS)
            .errorMessage(null)
            .commandExecutionData(commandExecutionContextBuider.getCommandExecutionData())
            .build();
    when(serviceCommandExecutorService.execute(command, commandExecutionContextBuider))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionResult commandExecutionResult =
        commandTask.run(new Object[] {command, commandExecutionContextBuider});
    assertEquals(expectedCommandExecutionResult, commandExecutionResult);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRunWithObjectParametersException() {
    Command command = aCommand().withName("Ami-Command").withCommandType(ENABLE).build();
    CommandExecutionResult expectedCommandExecutionResult =
        CommandExecutionResult.builder()
            .status(CommandExecutionStatus.FAILURE)
            .errorMessage("NullPointerException")
            .commandExecutionData(commandExecutionContextBuider.getCommandExecutionData())
            .build();
    when(serviceCommandExecutorService.execute(command, commandExecutionContextBuider))
        .thenThrow(new NullPointerException());
    CommandExecutionResult commandExecutionResult =
        commandTask.run(new Object[] {command, commandExecutionContextBuider});
    assertEquals(expectedCommandExecutionResult, commandExecutionResult);
  }
}