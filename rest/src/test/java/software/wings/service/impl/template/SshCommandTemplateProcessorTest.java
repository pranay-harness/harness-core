package software.wings.service.impl.template;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.beans.command.ServiceCommand.TEMPATE_UUID_KEY;
import static software.wings.beans.template.TemplateType.SSH;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.LATEST_TAG;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_STOP_PATH;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC_CHANGED;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_COMMAND_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import com.mongodb.DBCursor;
import org.junit.Test;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Variable;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;

public class SshCommandTemplateProcessorTest extends TemplateBaseTest {
  @Mock private MorphiaIterator<ServiceCommand, ServiceCommand> serviceCommandIterator;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private Query<ServiceCommand> query;
  @Mock private FieldEnd end;
  @Mock private DBCursor dbCursor;
  @Mock private ServiceResourceService serviceResourceService;

  @Inject private SshCommandTemplateProcessor sshCommandTemplateProcessor;

  @Test
  public void shouldLoadTomcatStandardCommands() {
    templateService.loadYaml(SSH, TOMCAT_WAR_STOP_PATH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
  }

  @Test
  public void shouldLoadDefaultCommandTemplates() {
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
  }

  @Test
  public void shouldAddCommandTemplate() {
    Template template = getSshCommandTemplate();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords()).contains(template.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    assertThat(savedTemplate.getVariables()).isNotEmpty();
    assertThat(savedTemplate.getVariables()).extracting("name").contains("MyVar");
    Variable savedVariable = savedTemplate.getVariables()
                                 .stream()
                                 .filter(variable -> variable.getValue().equals("MyValue"))
                                 .findFirst()
                                 .orElse(null);
    assertThat(savedVariable.isMandatory()).isFalse();

    SshCommandTemplate savedSshCommandTemplate = (SshCommandTemplate) savedTemplate.getTemplateObject();
    assertThat(savedSshCommandTemplate).isNotNull();
    assertThat(savedSshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(savedSshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("EXEC");
  }

  private Template getSshCommandTemplate() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    SshCommandTemplate commandTemplate =
        SshCommandTemplate.builder()
            .commandUnits(asList(anExecCommandUnit()
                                     .withName("EXEC")
                                     .withCommandPath("/home/xxx/tomcat")
                                     .withCommandString("${artifact.description}/${MyVar}")
                                     .build()))
            .build();

    return Template.builder()
        .name(TEMPLATE_NAME)
        .folderId(parentFolder.getUuid())
        .appId(GLOBAL_APP_ID)
        .accountId(GLOBAL_ACCOUNT_ID)
        .templateObject(commandTemplate)
        .appId(GLOBAL_APP_ID)
        .variables(asList(aVariable().withName("MyVar").withValue("MyValue").build()))
        .build();
  }

  @Test
  public void shouldUpdateCommandTemplate() {
    Template template = getSshCommandTemplate();
    Template savedTemplate = templateService.save(template);

    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getVersion()).isEqualTo(1);

    savedTemplate.setDescription(TEMPLATE_DESC_CHANGED);

    savedTemplate.setVariables(asList(aVariable().withName("MyVar").withValue("MyValue").build(),
        aVariable().withName("MySecondVar").withValue("MySecondValue").build()));

    Template updatedTemplate = templateService.update(savedTemplate);
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getVersion()).isEqualTo(2L);
    assertThat(updatedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(updatedTemplate.getDescription()).isEqualTo(TEMPLATE_DESC_CHANGED);
    assertThat(updatedTemplate.getVariables()).isNotEmpty();

    Variable firstVariable = updatedTemplate.getVariables()
                                 .stream()
                                 .filter(variable -> variable.getName().equals("MyVar"))
                                 .findFirst()
                                 .orElse(null);
    assertThat(firstVariable).isNotNull();
    assertThat(firstVariable.getValue()).isEqualTo("MyValue");

    Variable secondVariable = updatedTemplate.getVariables()
                                  .stream()
                                  .filter(variable -> variable.getName().equals("MySecondVar"))
                                  .findFirst()
                                  .orElse(null);
    assertThat(secondVariable).isNotNull();
    assertThat(secondVariable.getValue()).isEqualTo("MySecondValue");
  }

  @Test
  public void shouldUpdateCommandsLinked() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getVersion()).isEqualTo(1);

    savedTemplate.setDescription(TEMPLATE_DESC_CHANGED);

    savedTemplate.setVariables(asList(aVariable().withName("MyVar").withValue("MyValue").build(),
        aVariable().withName("MySecondVar").withValue("MySecondValue").build()));

    on(sshCommandTemplateProcessor).set("wingsPersistence", wingsPersistence);
    on(sshCommandTemplateProcessor).set("serviceResourceService", serviceResourceService);

    when(wingsPersistence.createQuery(ServiceCommand.class)).thenReturn(query);

    Command command =
        aCommand()
            .withName("START")
            .addCommandUnits(
                anExecCommandUnit().withCommandPath("/home/xxx/tomcat").withCommandString("bin/startup.sh").build())
            .build();

    ServiceCommand serviceCommand = aServiceCommand()
                                        .withServiceId(SERVICE_ID)
                                        .withUuid(SERVICE_COMMAND_ID)
                                        .withDefaultVersion(1)
                                        .withAppId(APP_ID)
                                        .withName("START")
                                        .withCommand(command)
                                        .withTemplateUuid(savedTemplate.getUuid())
                                        .withTemplateVersion(LATEST_TAG)
                                        .build();

    when(query.filter(TEMPATE_UUID_KEY, savedTemplate.getUuid())).thenReturn(query);
    when(query.fetch()).thenReturn(serviceCommandIterator);

    when(serviceCommandIterator.getCursor()).thenReturn(dbCursor);
    when(serviceCommandIterator.hasNext()).thenReturn(true).thenReturn(false);
    when(serviceCommandIterator.next()).thenReturn(serviceCommand);

    templateService.updateLinkedEntities(savedTemplate);

    verify(serviceResourceService)
        .updateCommand(serviceCommand.getAppId(), serviceCommand.getServiceId(), serviceCommand, true);
  }
}