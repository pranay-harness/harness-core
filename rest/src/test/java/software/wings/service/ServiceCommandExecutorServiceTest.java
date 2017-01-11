package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.AbstractCommandUnit.ExecutionResult.SUCCESS;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.infrastructure.ApplicationHost.Builder.anApplicationHost;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.RUNTIME_PATH;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.AbstractCommandUnit.ExecutionResult;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.ServiceCommandExecutorServiceImpl;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.utils.WingsTestConstants;

/**
 * Created by anubhaw on 6/7/16.
 */
public class ServiceCommandExecutorServiceTest extends WingsBaseTest {
  /**
   * The Command unit executor service.
   */
  @Mock private CommandUnitExecutorService commandUnitExecutorService;
  /**
   * The Cmd executor service.
   */
  @InjectMocks private ServiceCommandExecutorService cmdExecutorService = new ServiceCommandExecutorServiceImpl();

  private SettingAttribute hostConnAttrPwd =
      aSettingAttribute().withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build();
  private ExecutionCredential credential =
      aSSHExecutionCredential().withSshUser(USER_NAME).withSshPassword(WingsTestConstants.USER_PASSWORD).build();
  private Host host = aHost().withAppId(APP_ID).withHostName(HOST_NAME).withHostConnAttr(hostConnAttrPwd).build();
  private Service service = aService().withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private ServiceTemplate serviceTemplate =
      aServiceTemplate().withUuid(TEMPLATE_ID).withName(TEMPLATE_NAME).withEnvId(ENV_ID).withService(service).build();
  private ServiceInstance serviceInstance =
      aServiceInstance()
          .withAppId(APP_ID)
          .withServiceId(SERVICE_ID)
          .withEnvId(ENV_ID)
          .withHost(anApplicationHost().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(HOST_ID).withHost(host).build())
          .withServiceTemplate(serviceTemplate)
          .build();
  private AbstractCommandUnit commandUnit =
      anExecCommandUnit().withName(COMMAND_UNIT_NAME).withCommandString("rm -f $HOME/jetty").build();
  private Command command = aCommand().withName(COMMAND_NAME).addCommandUnits(commandUnit).build();

  private CommandExecutionContext context = CommandExecutionContext.Builder.aCommandExecutionContext()
                                                .withAppId(APP_ID)
                                                .withActivityId(ACTIVITY_ID)
                                                .withRuntimePath(RUNTIME_PATH)
                                                .withExecutionCredential(credential)
                                                .withServiceTemplate(serviceTemplate)
                                                .withHost(host)
                                                .build();

  /**
   * Should execute command for service instance.
   */
  @Test
  public void shouldExecuteCommandForServiceInstance() {
    when(commandUnitExecutorService.execute(eq(host), any(AbstractCommandUnit.class), eq(context))).thenReturn(SUCCESS);
    ExecutionResult executionResult = cmdExecutorService.execute(command, context);
    assertThat(executionResult).isEqualTo(SUCCESS);
  }

  /**
   * Should execute nested command for service instance.
   */
  @Test
  public void shouldExecuteNestedCommandForServiceInstance() {
    Command nestedCommand = aCommand().withName("NESTED_CMD").addCommandUnits(command).build();
    when(commandUnitExecutorService.execute(eq(host), any(AbstractCommandUnit.class), eq(context))).thenReturn(SUCCESS);
    ExecutionResult executionResult = cmdExecutorService.execute(nestedCommand, context);
    assertThat(executionResult).isEqualTo(SUCCESS);
  }
}
