package software.wings.integration;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.CommandUnitType.SCP;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.AppContainer;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceCommandExecutorService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by anubhaw on 6/2/16.
 */
@Integration
@Ignore
public class CommandExecutionIntegrationTest extends WingsBaseTest {
  private static final String HOST_NAME = "192.168.1.53";
  private static final String USER = "ssh_user";
  private static final char[] PASSWORD = "Wings@123".toCharArray();
  private static final SettingAttribute HOST_CONN_ATTR_PWD =
      aSettingAttribute().withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build();
  private static final Host HOST = aHost()
                                       .withAppId(APP_ID)
                                       .withEnvId(ENV_ID)
                                       .withHostName(HOST_NAME)
                                       .withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid())
                                       .build();
  private static final ServiceTemplate SERVICE_TEMPLATE =
      aServiceTemplate().withUuid(TEMPLATE_ID).withName(TEMPLATE_NAME).withServiceId(SERVICE_ID).build();
  /**
   * The constant SERVICE_INSTANCE.
   */
  public static final ServiceInstance SERVICE_INSTANCE = aServiceInstance()
                                                             .withAppId(APP_ID)
                                                             .withEnvId(ENV_ID)
                                                             .withHost(HOST)
                                                             .withServiceTemplate(SERVICE_TEMPLATE)
                                                             .build();
  /**
   * The Service command executor service.
   */
  @Inject ServiceCommandExecutorService serviceCommandExecutorService;
  /**
   * The File service.
   */
  @Inject FileService fileService;
  /**
   * The Wings persistence.
   */
  @Inject WingsPersistence wingsPersistence;
  private CommandExecutionContext context =
      CommandExecutionContext.Builder.aCommandExecutionContext()
          .withActivityId(ACTIVITY_ID)
          .withRuntimePath("$HOME/apps")
          .withExecutionCredential(aSSHExecutionCredential().withSshUser(USER).withSshPassword(PASSWORD).build())
          .build();

  private Command command =
      aCommand()
          .withName("INSTALL")
          .addCommandUnits(
              anExecCommandUnit().withName("Delete start and stop script").withCommandString("rm -f ./bin/*").build(),
              anExecCommandUnit()
                  .withName("Create service startup file")
                  .withCommandString("mkdir -p bin && echo 'sh service && echo \"service started\" ' > ./bin/start.sh")
                  .build(),
              anExecCommandUnit()
                  .withName("Create stop file")
                  .withCommandString("echo 'echo \"service successfully stopped\"'  > ./bin/stop.sh")
                  .build(),
              anExecCommandUnit()
                  .withName("Make start/stop script executable")
                  .withCommandString("chmod +x ./bin/*")
                  .build(),
              anExecCommandUnit().withName("Exec").withCommandString("./bin/stop.sh").build(),
              ScpCommandUnit.Builder.aScpCommandUnit()
                  .withName("Copy_ARTIFACT")
                  .withCommandUnitType(SCP)
                  .withFileCategory(ScpFileCategory.ARTIFACTS)
                  .build(),
              anExecCommandUnit().withName("EXEC").withCommandString("./bin/start.sh").build())
          .build();

  /**
   * Sets the up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    wingsPersistence.getDatastore().getCollection(AppContainer.class).drop();
    String uuid = fileService.saveFile(anArtifactFile().withName("app").build(),
        new ByteArrayInputStream("echo 'hello world'".getBytes(StandardCharsets.UTF_8)), ARTIFACTS);
    ArtifactFile artifactFile = anArtifactFile().withFileUuid(uuid).withName("service").build();
    context.setArtifactFiles(asList(artifactFile));
  }

  /**
   * Should execute command.
   */
  @Test
  public void shouldExecuteCommand() {
    CommandExecutionStatus commandExecutionStatus = serviceCommandExecutorService.execute(command, context);
    command.getCommandUnits().forEach(
        commandUnit -> assertThat(commandUnit.getCommandExecutionStatus()).isEqualTo(SUCCESS));
    assertThat(commandExecutionStatus).isEqualTo(SUCCESS);
  }

  /**
   * Should capture failed execution command unit.
   */
  @Test
  public void shouldCaptureFailedExecutionCommandUnit() {
    ((ExecCommandUnit) command.getCommandUnits().get(6)).setCommandString("INVALID_COMMAND");
    CommandExecutionStatus commandExecutionStatus = serviceCommandExecutorService.execute(command, context);
    for (int i = 0; i < command.getCommandUnits().size() - 1; i++) {
      assertThat(command.getCommandUnits().get(i).getCommandExecutionStatus()).isEqualTo(SUCCESS);
    }
    assertThat(command.getCommandUnits().get(6).getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(commandExecutionStatus).isEqualTo(FAILURE);
  }
}
