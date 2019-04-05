package software.wings.service.impl.template;

import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.joor.Reflect.on;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.beans.command.ServiceCommand.TEMPLATE_UUID_KEY;
import static software.wings.beans.template.TemplateType.SSH;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.LATEST_TAG;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_APP_INSTALL_PATH;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_STOP_PATH;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC_CHANGED;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_NAME;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_COMMAND_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import com.mongodb.DBCursor;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.exception.WingsException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Variable;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateReference;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;

public class SshCommandTemplateProcessorTest extends TemplateBaseTest {
  private static final String MY_STOP = "MyStop";
  private static final String STANDALONE_EXEC = "Standalone Exec";
  private static final String ANOTHER_COMMAND = "AnotherCommand";
  private static final String MY_START = "MyStart";
  private static final String MY_INSTALL = "MyInstall";
  @Mock private MorphiaIterator<ServiceCommand, ServiceCommand> serviceCommandIterator;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private Query<ServiceCommand> query;
  @Mock private FieldEnd end;
  @Mock private DBCursor dbCursor;
  @Mock private ServiceResourceService serviceResourceService;

  @Inject private SshCommandTemplateProcessor sshCommandTemplateProcessor;

  @Test
  @Category(UnitTests.class)
  public void shouldLoadTomcatStandardCommands() {
    templateService.loadYaml(SSH, TOMCAT_WAR_STOP_PATH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldLoadDefaultCommandTemplates() {
    templateService.loadDefaultTemplates(SSH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldLoadIISCommands() {
    templateService.loadYaml(SSH, POWER_SHELL_IIS_APP_INSTALL_PATH, GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
  }

  @Test
  @Category(UnitTests.class)
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
  @Category(UnitTests.class)
  public void shouldUpdateCommandTemplate() {
    Template template = getSshCommandTemplate();
    Template savedTemplate = templateService.save(template);

    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getVersion()).isEqualTo(1);

    savedTemplate.setDescription(TEMPLATE_DESC_CHANGED);

    savedTemplate.setVariables(asList(aVariable().withName("MyVar").withValue("MyValue").build(),
        aVariable().withName("MySecondVar").withValue("MySecondValue").build()));
    savedTemplate.setName("Another Template");
    Template updatedTemplate = templateService.update(savedTemplate);
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getVersion()).isEqualTo(2L);
    assertThat(updatedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(updatedTemplate.getDescription()).isEqualTo(TEMPLATE_DESC_CHANGED);
    assertThat(updatedTemplate.getVariables()).isNotEmpty();
    assertThat(updatedTemplate.getKeywords()).isNotEmpty();
    assertThat(updatedTemplate.getKeywords()).doesNotContain(template.getName().toLowerCase());
    assertThat(updatedTemplate.getKeywords()).contains("Another Template".toLowerCase());

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
  @Category(UnitTests.class)
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

    when(wingsPersistence.createQuery(ServiceCommand.class, excludeAuthority)).thenReturn(query);

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

    when(query.filter(TEMPLATE_UUID_KEY, savedTemplate.getUuid())).thenReturn(query);
    when(query.fetch()).thenReturn(serviceCommandIterator);

    when(serviceCommandIterator.getCursor()).thenReturn(dbCursor);
    when(serviceCommandIterator.hasNext()).thenReturn(true).thenReturn(false);
    when(serviceCommandIterator.next()).thenReturn(serviceCommand);

    templateService.updateLinkedEntities(savedTemplate);

    verify(serviceResourceService)
        .updateCommand(serviceCommand.getAppId(), serviceCommand.getServiceId(), serviceCommand, true);
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testCRUDCommandTemplate() {
    // Create individual commands like MyStart, MyStop, MyAnotherCommand
    Template myStop = createMyStopCommand();
    Template myStart = createMyStartCommand();
    Template myAnotherCommand = createMyAnotherCommand();
    // Create command MyInstall composed of other commands from template library
    Template installCommand = createInstallCommand(myStop, myStart, myAnotherCommand);
    assertThat(installCommand).isNotNull();
    assertThat(installCommand.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(installCommand.getVersion()).isEqualTo(1);
    SshCommandTemplate savedSshCommandTemplate = (SshCommandTemplate) installCommand.getTemplateObject();
    assertThat(savedSshCommandTemplate.getCommandUnits().size()).isEqualTo(4);
    assertThat(savedSshCommandTemplate.getReferencedTemplateList().size()).isEqualTo(4);
    assertThat(
        savedSshCommandTemplate.getReferencedTemplateList().get(0).getTemplateReference().getTemplateUuid().equals(
            myStop.getUuid()));
    assertThat(
        savedSshCommandTemplate.getReferencedTemplateList().get(0).getTemplateReference().getTemplateVersion().equals(
            myStop.getVersion()));
    assertThat(savedSshCommandTemplate.getReferencedTemplateList().get(0).getVariableMapping().containsKey("V3"));
    assertThat(savedSshCommandTemplate.getReferencedTemplateList().get(0).getVariableMapping().get("V3").getValue())
        .isEqualTo("hello3");

    // update top-level variable and check if its reflected in templateVariablesList
    installCommand.setVariables(asList(aVariable().withName("V3").withValue("hello3-updated").build(),
        aVariable().withName("V4").withValue("world4").build(), aVariable().withName("V5").withValue("bye").build(),
        aVariable().withName("V2").withValue("there2").build()));
    Template updatedTemplate = templateService.update(installCommand);
    SshCommandTemplate updatedSshCommandTemplate = (SshCommandTemplate) updatedTemplate.getTemplateObject();
    assertThat(updatedSshCommandTemplate.getCommandUnits().size()).isEqualTo(4);
    assertThat(updatedSshCommandTemplate.getReferencedTemplateList().size()).isEqualTo(4);
    assertThat(
        updatedSshCommandTemplate.getReferencedTemplateList().get(0).getTemplateReference().getTemplateUuid().equals(
            myStop.getUuid()));
    assertThat(
        updatedSshCommandTemplate.getReferencedTemplateList().get(0).getTemplateReference().getTemplateVersion().equals(
            myStop.getVersion()));
    assertThat(updatedSshCommandTemplate.getReferencedTemplateList().get(0).getVariableMapping().containsKey("V3"));
    assertThat(updatedSshCommandTemplate.getReferencedTemplateList().get(0).getVariableMapping().get("V3").getValue())
        .isEqualTo("hello3-updated");
    assertThat(updatedTemplate.getVariables().size()).isEqualTo(4);
    assertThat(updatedTemplate.getVariables()).extracting("name", "value").contains(tuple("V3", "hello3-updated"));

    // try deleting my stop - should fail since its referenced in myinstall
    templateService.delete(GLOBAL_ACCOUNT_ID, myStop.getUuid());
    // delete myinstall
    deleteTemplate(installCommand);
    deleteTemplates(myStop, myStart, myAnotherCommand);
  }

  private void deleteTemplate(Template installCommand) {
    assertTrue(templateService.delete(GLOBAL_ACCOUNT_ID, installCommand.getUuid()));
  }

  private void deleteTemplates(Template myStop, Template myStart, Template myAnotherCommand) {
    deleteTemplate(myStop);
    deleteTemplate(myStart);
    deleteTemplate(myAnotherCommand);
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testCreateTemplateDuplicateVariablesDifferentFixedValues() {
    // Create individual commands like MyStart, MyStop, MyAnotherCommand
    Template myStop = createMyStopCommand();
    Template myStart = createMyStartCommand();
    Template myAnotherCommand = createMyAnotherCommand();
    // Create command MyInstall composed of other commands from template library
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    List<Variable> myStopVariables = asList(
        aVariable().withName("V3").withValue("hello3").build(), aVariable().withName("V4").withValue("world4").build());
    List<Variable> myStartVariables = asList(
        aVariable().withName("V1").withValue("${V3}").build(), aVariable().withName("V2").withValue("there2").build());
    SshCommandTemplate commandTemplate =
        SshCommandTemplate.builder()
            .commandUnits(asList(createCommand(myStop, myStopVariables, MY_STOP), createStandaloneExec(),
                createCommand(myStart, myStartVariables, MY_START),
                createCommand(
                    myAnotherCommand, asList(aVariable().withName("V3").withValue("hello4").build()), ANOTHER_COMMAND)))
            .commandType(CommandType.OTHER)
            .build();
    List<Variable> myInstallVariables = asList(aVariable().withName("V3").withValue("hello3").build(),
        aVariable().withName("V4").withValue("world4").build(), aVariable().withName("V5").withValue("bye").build(),
        aVariable().withName("V2").withValue("there2").build());
    Template template = Template.builder()
                            .name(MY_INSTALL)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .type("SSH")
                            .templateObject(commandTemplate)
                            .variables(myInstallVariables)
                            .build();
    templateService.save(template);

    deleteTemplates(myStop, myStart, myAnotherCommand);
  }

  private ExecCommandUnit createStandaloneExec() {
    return anExecCommandUnit()
        .withName(STANDALONE_EXEC)
        .withScriptType(ScriptType.BASH)
        .withCommandString("echo ${V5}")
        .withCommandPath("/tmp")
        .build();
  }

  private TemplateReference createTemplateReference(Template myStop) {
    return TemplateReference.builder().templateUuid(myStop.getUuid()).templateVersion(myStop.getVersion()).build();
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void testCreateTemplateVariableNotPassedInParent() {
    // Create individual commands like MyStart, MyStop, MyAnotherCommand
    Template myStop = createMyStopCommand();
    Template myStart = createMyStartCommand();
    Template myAnotherCommand = createMyAnotherCommand();
    // Create command MyInstall composed of other commands from template library
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    List<Variable> myStopVariables = asList(
        aVariable().withName("V3").withValue("hello3").build(), aVariable().withName("V4").withValue("world4").build());
    List<Variable> myStartVariables = asList(
        aVariable().withName("V1").withValue("hi").build(), aVariable().withName("V2").withValue("there2").build());
    List<Variable> anotherCommandVariables = asList(aVariable().withName("V3").withValue("${V1}").build());
    SshCommandTemplate commandTemplate =
        SshCommandTemplate.builder()
            .commandUnits(asList(createCommand(myStop, myStopVariables, MY_STOP), createStandaloneExec(),
                createCommand(myStart, myStartVariables, MY_START),
                createCommand(myAnotherCommand, anotherCommandVariables, ANOTHER_COMMAND)))
            .commandType(CommandType.OTHER)
            .build();
    List<Variable> installCommandVariables = asList(aVariable().withName("V3").withValue("hello3").build(),
        aVariable().withName("V4").withValue("world4").build(), aVariable().withName("V5").withValue("bye").build(),
        aVariable().withName("V2").withValue("there2").build());
    Template template = Template.builder()
                            .name(MY_INSTALL)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .type("SSH")
                            .templateObject(commandTemplate)
                            .variables(installCommandVariables)
                            .build();
    templateService.save(template);

    deleteTemplates(myStop, myStart, myAnotherCommand);
  }

  private Command createCommand(Template template, List<Variable> myStopVariables, String name) {
    return aCommand()
        .withName(name)
        .withTemplateReference(createTemplateReference(template))
        .withTemplateVariables(myStopVariables)
        .build();
  }

  private Template createMyStopCommand() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    SshCommandTemplate commandTemplate = SshCommandTemplate.builder()
                                             .commandUnits(asList(anExecCommandUnit()
                                                                      .withName("EXEC-1")
                                                                      .withCommandPath("/home/xxx/tomcat")
                                                                      .withCommandString("echo ${V3}")
                                                                      .withScriptType(ScriptType.BASH)
                                                                      .build(),
                                                 anExecCommandUnit()
                                                     .withName("EXEC-2")
                                                     .withCommandPath("/home/xxx/tomcat")
                                                     .withCommandString("echo ${V4}")
                                                     .withScriptType(ScriptType.BASH)
                                                     .build()))
                                             .build();

    Template template = Template.builder()
                            .name(MY_STOP)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .templateObject(commandTemplate)
                            .appId(GLOBAL_APP_ID)
                            .variables(asList(aVariable().withName("V3").withValue("hello").build(),
                                aVariable().withName("V4").withValue("world").build()))
                            .build();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    return savedTemplate;
  }

  private Template createMyStartCommand() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    SshCommandTemplate commandTemplate = SshCommandTemplate.builder()
                                             .commandUnits(asList(anExecCommandUnit()
                                                                      .withName("EXEC-1")
                                                                      .withCommandPath("/home/xxx/tomcat")
                                                                      .withCommandString("echo ${V1}")
                                                                      .withScriptType(ScriptType.BASH)
                                                                      .build(),
                                                 anExecCommandUnit()
                                                     .withName("EXEC-2")
                                                     .withCommandPath("/home/xxx/tomcat")
                                                     .withCommandString("echo ${V2}")
                                                     .withScriptType(ScriptType.BASH)
                                                     .build()))
                                             .build();

    Template template = Template.builder()
                            .name(MY_START)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .templateObject(commandTemplate)
                            .appId(GLOBAL_APP_ID)
                            .variables(asList(aVariable().withName("V1").withValue("hi").build(),
                                aVariable().withName("V2").withValue("world").build()))
                            .build();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    return savedTemplate;
  }

  private Template createMyAnotherCommand() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    SshCommandTemplate commandTemplate = SshCommandTemplate.builder()
                                             .commandUnits(asList(anExecCommandUnit()
                                                                      .withName("EXEC-1")
                                                                      .withCommandPath("/home/xxx/tomcat")
                                                                      .withCommandString("echo ${V3}")
                                                                      .withScriptType(ScriptType.BASH)
                                                                      .build()))
                                             .build();

    Template template = Template.builder()
                            .name(ANOTHER_COMMAND)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .templateObject(commandTemplate)
                            .variables(asList(aVariable().withName("V3").withValue("hi").build()))
                            .build();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    return savedTemplate;
  }

  private Template createInstallCommand(Template myStop, Template myStart, Template myAnotherCommand) {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    SshCommandTemplate commandTemplate =
        SshCommandTemplate.builder()
            .commandUnits(asList(createCommand(myStop,
                                     asList(aVariable().withName("V3").withValue("hello3").build(),
                                         aVariable().withName("V4").withValue("world4").build()),
                                     MY_STOP),
                createStandaloneExec(),
                createCommand(myStart,
                    asList(aVariable().withName("V1").withValue("${V3}").build(),
                        aVariable().withName("V2").withValue("there2").build()),
                    MY_START),
                createCommand(
                    myAnotherCommand, asList(aVariable().withName("V3").withValue("hello3").build()), ANOTHER_COMMAND)))
            .commandType(CommandType.OTHER)
            .build();
    Template template = Template.builder()
                            .name(MY_INSTALL)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .type("SSH")
                            .templateObject(commandTemplate)
                            .variables(asList(aVariable().withName("V3").withValue("hello3").build(),
                                aVariable().withName("V4").withValue("world4").build(),
                                aVariable().withName("V5").withValue("bye").build(),
                                aVariable().withName("V2").withValue("there2").build()))
                            .build();
    return templateService.save(template);
  }
}