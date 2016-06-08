package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Command.Builder.aCommand;
import static software.wings.beans.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.beans.CommandUnitType.EXEC;
import static software.wings.beans.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionCredential.HostConnectionCredentialBuilder.aHostConnectionCredential;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.ServiceInstance.ServiceInstanceBuilder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity.Builder;
import software.wings.beans.Command;
import software.wings.beans.CommandUnit;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.Host;
import software.wings.beans.HostConnectionCredential;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.utils.WingsTestConstants;

import javax.inject.Inject;

/**
 * Created by anubhaw on 6/7/16.
 */
public class ServiceCommandExecutorServiceTest extends WingsBaseTest {
  @Mock CommandUnitExecutorService commandUnitExecutorService;
  @Mock ActivityService activityService;
  @Inject @InjectMocks ServiceCommandExecutorService cmdExecutorService;

  private SettingAttribute hostConnAttrPwd =
      aSettingAttribute().withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build();
  private HostConnectionCredential credential =
      aHostConnectionCredential().withSshUser(USER_NAME).withSshPassword(WingsTestConstants.USER_PASSWORD).build();
  private Host host = aHost()
                          .withAppId(APP_ID)
                          .withHostName(HOST_NAME)
                          .withHostConnAttr(hostConnAttrPwd)
                          .withHostConnectionCredential(credential)
                          .build();
  private Service service = aService().withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  private ServiceTemplate serviceTemplate =
      aServiceTemplate().withUuid(TEMPLATE_ID).withName(TEMPLATE_NAME).withService(service).build();
  private ServiceInstance serviceInstance = aServiceInstance()
                                                .withAppId(APP_ID)
                                                .withEnvId(ENV_ID)
                                                .withHost(host)
                                                .withServiceTemplate(serviceTemplate)
                                                .build();
  private CommandUnit commandUnit = anExecCommandUnit()
                                        .withName(COMMAND_UNIT_NAME)
                                        .withCommandUnitType(EXEC)
                                        .withCommandString("rm -f $HOME/jetty")
                                        .build();
  private Command command = aCommand().withName(COMMAND_NAME).addCommandUnits(commandUnit).build();
  private Builder activityBuilder = anActivity()
                                        .withAppId(APP_ID)
                                        .withEnvironmentId(ENV_ID)
                                        .withServiceTemplateId(TEMPLATE_ID)
                                        .withServiceTemplateName(TEMPLATE_NAME)
                                        .withServiceId(SERVICE_ID)
                                        .withServiceName(SERVICE_NAME)
                                        .withCommandName(COMMAND_NAME)
                                        .withCommandType(EXEC.name())
                                        .withHostName(HOST_NAME);

  @Test
  public void shouldExecuteCommandForServiceInstance() {
    when(activityService.save(activityBuilder.build())).thenReturn(activityBuilder.withUuid(ACTIVITY_ID).build());
    when(commandUnitExecutorService.execute(host, commandUnit, ACTIVITY_ID)).thenReturn(SUCCESS);

    ExecutionResult executionResult = cmdExecutorService.execute(serviceInstance, command);

    assertThat(executionResult).isEqualTo(SUCCESS);
  }
}
