package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.RUNTIME_PATH;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.ServiceCommandExecutorServiceImpl;
import software.wings.service.impl.SshCommandUnitExecutorServiceImpl;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.utils.WingsTestConstants;

import java.util.Map;

/**
 * Created by anubhaw on 6/7/16.
 */
@Ignore // TODO: move to delegate
public class ServiceCommandExecutorServiceTest extends WingsBaseTest {
  @Mock private Map<String, CommandUnitExecutorService> commandUnitExecutorServiceMap;
  @Mock private SshCommandUnitExecutorServiceImpl sshCommandUnitExecutorService;

  @InjectMocks private ServiceCommandExecutorService cmdExecutorService = new ServiceCommandExecutorServiceImpl();

  private SettingAttribute hostConnAttrPwd =
      aSettingAttribute().withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build();
  private ExecutionCredential credential =
      aSSHExecutionCredential().withSshUser(USER_NAME).withSshPassword(WingsTestConstants.USER_PASSWORD).build();
  private Host host =
      aHost().withAppId(APP_ID).withHostName(HOST_NAME).withHostConnAttr(hostConnAttrPwd.getUuid()).build();
  private AbstractCommandUnit commandUnit =
      anExecCommandUnit().withName(COMMAND_UNIT_NAME).withCommandString("rm -f $HOME/jetty").build();
  private Command command = aCommand().withName(COMMAND_NAME).addCommandUnits(commandUnit).build();

  private CommandExecutionContext context = CommandExecutionContext.Builder.aCommandExecutionContext()
                                                .withAppId(APP_ID)
                                                .withActivityId(ACTIVITY_ID)
                                                .withRuntimePath(RUNTIME_PATH)
                                                .withExecutionCredential(credential)
                                                .withServiceTemplateId(TEMPLATE_ID)
                                                .withHost(host)
                                                .build();

  /**
   * Should execute command for service instance.
   */
  @Test
  public void shouldExecuteCommandForServiceInstance() {
    when(commandUnitExecutorServiceMap.get(DeploymentType.SSH.name())).thenReturn(sshCommandUnitExecutorService);
    when(sshCommandUnitExecutorService.execute(eq(host), any(AbstractCommandUnit.class), eq(context)))
        .thenReturn(SUCCESS);
    CommandExecutionStatus commandExecutionStatus = cmdExecutorService.execute(command, context);
    assertThat(commandExecutionStatus).isEqualTo(SUCCESS);
  }

  /**
   * Should execute nested command for service instance.
   */
  @Test
  public void shouldExecuteNestedCommandForServiceInstance() {
    Command nestedCommand = aCommand().withName("NESTED_CMD").addCommandUnits(command).build();
    when(commandUnitExecutorServiceMap.get(DeploymentType.SSH.name())).thenReturn(sshCommandUnitExecutorService);
    when(sshCommandUnitExecutorService.execute(eq(host), any(AbstractCommandUnit.class), eq(context)))
        .thenReturn(SUCCESS);
    CommandExecutionStatus commandExecutionStatus = cmdExecutorService.execute(nestedCommand, context);
    assertThat(commandExecutionStatus).isEqualTo(SUCCESS);
  }
}
