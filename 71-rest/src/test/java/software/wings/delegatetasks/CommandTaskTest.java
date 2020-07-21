package software.wings.delegatetasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SAHIL;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.connector.DockerhubConnectorConfig;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.rule.Owner;
import org.apache.commons.lang3.NotImplementedException;
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
  public void testRun() {
    ArtifactTaskParameters taskParameters =
        ArtifactTaskParameters.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .attributes(DockerArtifactSourceAttributes.builder()
                            .dockerhubConnector("CONNECTOR")
                            .imagePath("imagePath")
                            .tag("tag")
                            .build())
            .connectorConfig(
                DockerhubConnectorConfig.builder().identifier("CONNECTOR").registryUrl("CONNECTOR_URL").build())
            .build();
    assertThatExceptionOfType(NotImplementedException.class)
        .isThrownBy(() -> commandTask.run(taskParameters))
        .withMessage("not implemented");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testRunWithObjectParameters() {
    Command command = aCommand().withName("Ami-Command").withCommandType(ENABLE).build();
    CommandExecutionResult expectedCommandExecutionResult =
        CommandExecutionResult.builder()
            .status(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
            .errorMessage(null)
            .commandExecutionData(commandExecutionContextBuider.getCommandExecutionData())
            .build();
    when(serviceCommandExecutorService.execute(command, commandExecutionContextBuider))
        .thenReturn(CommandExecutionResult.CommandExecutionStatus.SUCCESS);
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
            .status(CommandExecutionResult.CommandExecutionStatus.FAILURE)
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