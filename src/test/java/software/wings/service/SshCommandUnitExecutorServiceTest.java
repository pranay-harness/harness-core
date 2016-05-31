package software.wings.service;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
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
  private static final String HOST_NAME = "192.168.1.52";
  @Inject private SshCommandUnitExecutorService commandUnitExecutorService;
  @Inject private AppContainerService appContainerService;
  @Inject private FileService fileService;

  @Test
  public void shouldSucessfullyExecuteExecCommandUnits() {
    //        String saveFile =
    //        fileService.saveFile(AppContainer.AppContainerBuilder.anAppContainer().withName("stratup.sh").build(),
    //            new ByteArrayInputStream("echo 'hello world'".getBytes(StandardCharsets.UTF_8)),
    //            FileBucket.PLATFORMS);
    //        System.out.println(saveFile);
    //   Execution execution =
    //   CustomCommand.CustomCommandBuilder.aCustomCommand().withUuid("UUID").withHost(HostBuilder.aHost().withHostName(HOST_NAME).build())
    //        .withSshUser("ssh_user").withSshPassword("Wings@123").build();
    //    execution = commandUnitExecutorService.execute(execution);
    //   Assertions.assertThat(execution.getExecutionResult()).isEqualTo(ExecutionResult.SUCCESS);
  }
}
