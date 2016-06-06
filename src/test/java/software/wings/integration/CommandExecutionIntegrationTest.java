package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.AppContainer.AppContainerBuilder.anAppContainer;
import static software.wings.beans.Command.Builder.aCommand;
import static software.wings.beans.CommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.beans.CopyArtifactCommandUnit.Builder.aCopyArtifactCommandUnit;
import static software.wings.beans.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionCredential.HostConnectionCredentialBuilder.aHostConnectionCredential;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.ServiceInstanceBuilder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;
import static software.wings.utils.WingsUnitTestConstants.APP_ID;
import static software.wings.utils.WingsUnitTestConstants.ENV_ID;
import static software.wings.utils.WingsUnitTestConstants.SERVICE_ID;
import static software.wings.utils.WingsUnitTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsUnitTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsUnitTestConstants.TEMPLATE_NAME;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.AppContainer;
import software.wings.beans.Command;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.Host;
import software.wings.beans.HostConnectionCredential;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.impl.ServiceCommandExecutorService;
import software.wings.service.intfc.AppContainerService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 6/2/16.
 */

@Integration
@Ignore
public class CommandExecutionIntegrationTest extends WingsBaseTest {
  private static final String HOST_NAME = "192.168.1.52";
  private static final String USER = "ssh_user";
  private static final String PASSWORD = "Wings@123";
  private static final SettingAttribute HOST_CONN_ATTR_PWD =
      aSettingAttribute().withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build();
  private static final HostConnectionCredential CREDENTIAL =
      aHostConnectionCredential().withSshUser(USER).withSshPassword(PASSWORD).build();
  private static final Host HOST = aHost()
                                       .withAppId(APP_ID)
                                       .withHostName(HOST_NAME)
                                       .withHostConnAttr(HOST_CONN_ATTR_PWD)
                                       .withHostConnectionCredential(CREDENTIAL)
                                       .build();
  private static final Service SERVICE = aService().withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private static final ServiceTemplate SERVICE_TEMPLATE =
      aServiceTemplate().withUuid(TEMPLATE_ID).withName(TEMPLATE_NAME).withService(SERVICE).build();
  public static final ServiceInstance SERVICE_INSTANCE = aServiceInstance()
                                                             .withAppId(APP_ID)
                                                             .withEnvId(ENV_ID)
                                                             .withHost(HOST)
                                                             .withServiceTemplate(SERVICE_TEMPLATE)
                                                             .build();
  private static String fileId;
  @Inject ServiceCommandExecutorService serviceCommandExecutorService;
  @Inject AppContainerService appContainerService;
  @Inject WingsPersistence wingsPersistence;

  /**
   * Sets the up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    wingsPersistence.getDatastore().getCollection(AppContainer.class).drop();
    String uuid = appContainerService.save(anAppContainer().withName("jetty").build(),
        new ByteArrayInputStream("echo 'hello world'".getBytes(StandardCharsets.UTF_8)), PLATFORMS);
    fileId = wingsPersistence.get(AppContainer.class, uuid).getFileUuid();
  }

  /**
   * Should execute command.
   */
  @Test
  public void shouldExecuteCommand() {
    Command command = aCommand()
                          .withName("COPY_CONTAINER")
                          .addCommandUnits(anExecCommandUnit().withCommandString("rm -f $HOME/jetty").build(),
                              aCopyArtifactCommandUnit()
                                  .withFileBucket(PLATFORMS)
                                  .withFileId(fileId)
                                  .withDestinationFilePath("$HOME")
                                  .build(),
                              anExecCommandUnit().withCommandString("chmod +x $HOME/jetty && $HOME/jetty").build())
                          .build();

    ExecutionResult executionResult = serviceCommandExecutorService.execute(SERVICE_INSTANCE, command);
    assertThat(command.getCommandUnits().get(0).getExecutionResult()).isEqualTo(SUCCESS);
    assertThat(command.getCommandUnits().get(1).getExecutionResult()).isEqualTo(SUCCESS);
    assertThat(command.getCommandUnits().get(2).getExecutionResult()).isEqualTo(SUCCESS);
    assertThat(executionResult).isEqualTo(SUCCESS);
  }

  /**
   * Should capture failed execution command unit.
   */
  @Test
  public void shouldCaptureFailedExecutionCommandUnit() {
    Command command = aCommand()
                          .withName("COPY_CONTAINER")
                          .addCommandUnits(anExecCommandUnit().withCommandString("rm -f $HOME/jetty").build(),
                              aCopyArtifactCommandUnit()
                                  .withFileBucket(PLATFORMS)
                                  .withFileId(fileId)
                                  .withDestinationFilePath("$HOME")
                                  .build(),
                              anExecCommandUnit().withCommandString("chmod +x $HOME/jetty && $HOME/XYZ").build())
                          .build();

    ExecutionResult executionResult = serviceCommandExecutorService.execute(SERVICE_INSTANCE, command);

    assertThat(command.getCommandUnits().get(0).getExecutionResult()).isEqualTo(SUCCESS);
    assertThat(command.getCommandUnits().get(1).getExecutionResult()).isEqualTo(SUCCESS);
    assertThat(command.getCommandUnits().get(2).getExecutionResult()).isEqualTo(FAILURE);
    assertThat(executionResult).isEqualTo(FAILURE);
  }
}
