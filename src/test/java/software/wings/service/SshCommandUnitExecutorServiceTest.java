package software.wings.service;

import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.core.ssh.executors.SshExecutor.ExecutionResult.SUCCESS;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Execution;
import software.wings.beans.Host.HostBuilder;
import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;
import software.wings.rules.Integration;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.SshCommandUnitExecutorService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 5/25/16.
 */

@Integration
@Ignore
public class SshCommandUnitExecutorServiceTest extends WingsBaseTest {
  @Inject private SshCommandUnitExecutorService commandUnitExecutorService;
  @Inject private AppContainerService appContainerService;
  @Inject private FileService fileService;

  private static final String HOST_NAME = "192.168.1.13";

  @Test
  public void shouldSucessfullyExecuteExecCommandUnits() {
    //    String saveFile = fileService.saveFile(anAppContainer().withName("stratup.sh").build(), new
    //    ByteArrayInputStream("echo 'hello world'".getBytes(StandardCharsets.UTF_8)), PLATFORMS);
    //    System.out.println(saveFile);
    Execution execution = CustomCommand.CustomCommandBuilder.aCustomCommand()
                              .withUuid(getUuid())
                              .withHost(HostBuilder.aHost().withHostName(HOST_NAME).build())
                              .withSshUser("ssh_user")
                              .withSshPassword("Wings@123")
                              .build();
    ExecutionResult executionResult = commandUnitExecutorService.execute(execution);
    Assertions.assertThat(executionResult).isEqualTo(SUCCESS);
  }
}
