package software.wings.service.impl.template;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.CommandCategory.Type.COMMANDS;
import static software.wings.beans.CommandCategory.Type.COPY;
import static software.wings.beans.CommandCategory.Type.SCRIPTS;
import static software.wings.beans.CommandCategory.Type.VERIFICATIONS;
import static software.wings.beans.command.CommandType.START;
import static software.wings.beans.command.CommandUnitType.COMMAND;
import static software.wings.beans.command.CommandUnitType.COPY_CONFIGS;
import static software.wings.beans.command.CommandUnitType.DOCKER_START;
import static software.wings.beans.command.CommandUnitType.DOCKER_STOP;
import static software.wings.beans.command.CommandUnitType.EXEC;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_CLEARED;
import static software.wings.beans.command.CommandUnitType.PORT_CHECK_LISTENING;
import static software.wings.beans.command.CommandUnitType.PROCESS_CHECK_RUNNING;
import static software.wings.beans.command.CommandUnitType.SCP;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.template.TemplateHelper.obtainTemplateFolderPath;
import static software.wings.beans.template.TemplateHelper.obtainTemplateName;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_V2_INSTALL_PATH;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_V3_INSTALL_PATH;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_CUSTOM_KEYWORD;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC_CHANGED;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_DEC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_NAME;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.CommandCategory;
import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.Variable;
import software.wings.beans.Variable.VariableBuilder;
import software.wings.beans.VariableType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.VersionedTemplate;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.service.intfc.template.TemplateVersionService;
import software.wings.utils.WingsTestConstants;

import java.io.IOException;
import java.util.List;
import javax.validation.ConstraintViolationException;

public class TemplateServiceTest extends TemplateBaseTest {
  private static final String MY_START_COMMAND = "My Start Command";
  private static final String MY_START_COMMAND_APP = "My Start Command App";
  private static final String ANOTHER_MY_START_COMMAND_APP = "Another My Start Command App";
  private static final String ANOTHER_MY_START_COMMAND_ANOTHER_APP = "Another My Start Command Another App";
  private static final String ANOTHER_APP_ID = "ANOTHER_APP_ID";
  @Inject private TemplateVersionService templateVersionService;

  @Test
  @Category(UnitTests.class)
  public void shouldSaveTemplateAtAccountLevel() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAccountId()).isNotEmpty();
    assertThat(savedTemplate.getGalleryId()).isNotEmpty();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords())
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), template.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);

    SshCommandTemplate savedSshCommandTemplate = (SshCommandTemplate) savedTemplate.getTemplateObject();
    assertThat(savedSshCommandTemplate).isNotNull();
    assertThat(savedSshCommandTemplate.getCommandType()).isEqualTo(START);
    assertThat(savedSshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(savedSshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("Start");
  }

  @Test(expected = ConstraintViolationException.class)
  @Category(UnitTests.class)
  public void shouldNotSaveInvalidNameTemplate() {
    Template template = getSshCommandTemplate();
    template.setName(WingsTestConstants.INVALID_NAME);
    templateService.save(template);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetTemplate() {
    Template savedTemplate = saveTemplate();
    getTemplateAndValidate(savedTemplate, GLOBAL_APP_ID);
  }

  private void getTemplateAndValidate(Template savedTemplate, String globalAppId) {
    Template template = templateService.get(savedTemplate.getUuid());
    assertThat(template).isNotNull();
    assertThat(template.getAppId()).isNotNull().isEqualTo(globalAppId);
    assertThat(template.getKeywords()).isNotEmpty();
    assertThat(template.getKeywords())
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), template.getName().toLowerCase());
    assertThat(template.getVersion()).isEqualTo(1);
    SshCommandTemplate SshCommandTemplate = (SshCommandTemplate) template.getTemplateObject();
    assertThat(SshCommandTemplate).isNotNull();
    assertThat(SshCommandTemplate.getCommandType()).isEqualTo(START);
    assertThat(SshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(SshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("Start");
  }

  private Template saveTemplate() {
    return saveTemplate(MY_START_COMMAND, GLOBAL_APP_ID);
  }

  private Template saveTemplate(String name, String appId) {
    Template template = getSshCommandTemplate(name, appId);
    Template savedTemplate = templateService.save(template);

    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(appId);
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords()).contains(template.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    SshCommandTemplate savedSshCommandTemplate = (SshCommandTemplate) savedTemplate.getTemplateObject();
    assertThat(savedSshCommandTemplate).isNotNull();
    assertThat(savedSshCommandTemplate.getCommandType()).isEqualTo(START);
    assertThat(savedSshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(savedSshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("Start");
    return savedTemplate;
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetTemplateByVersion() {
    Template savedTemplate = saveTemplate();

    Template template = templateService.get(savedTemplate.getAccountId(), savedTemplate.getUuid(), null);
    assertThat(template).isNotNull();
    assertThat(template.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(template.getKeywords()).isNotEmpty();
    assertThat(template.getKeywords())
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), template.getName().toLowerCase());
    assertThat(template.getVersion()).isEqualTo(1);

    SshCommandTemplate sshCommandTemplate = (SshCommandTemplate) template.getTemplateObject();
    assertThat(sshCommandTemplate).isNotNull();
    assertThat(sshCommandTemplate.getCommandType()).isEqualTo(START);
    assertThat(sshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(sshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("Start");

    template = templateService.get(
        savedTemplate.getAccountId(), savedTemplate.getUuid(), String.valueOf(template.getVersion()));
    assertThat(template).isNotNull();
    assertThat(template.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(template.getKeywords()).isNotEmpty();
    assertThat(template.getKeywords())
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), template.getName().toLowerCase());
    assertThat(template.getVersion()).isEqualTo(1);
    sshCommandTemplate = (SshCommandTemplate) template.getTemplateObject();
    assertThat(sshCommandTemplate).isNotNull();
    assertThat(sshCommandTemplate.getCommandType()).isEqualTo(START);
    assertThat(sshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(sshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("Start");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldList() {
    saveTemplate();

    PageRequest<Template> pageRequest = aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build();
    List<Template> templates = templateService.list(pageRequest);

    assertThat(templates).isNotEmpty();
    Template template = templates.stream().findFirst().get();

    assertThat(template).isNotNull();
    assertThat(template.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(template.getKeywords())
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), template.getName().toLowerCase());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateTemplateSame() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);

    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getKeywords())
        .isNotEmpty()
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), savedTemplate.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    SshCommandTemplate SshCommandTemplate = (SshCommandTemplate) savedTemplate.getTemplateObject();
    assertThat(SshCommandTemplate).isNotNull();
    assertThat(SshCommandTemplate.getCommandType()).isEqualTo(START);
    assertThat(SshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(SshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("Start");

    savedTemplate.setDescription(TEMPLATE_DESC_CHANGED);

    Template updatedTemplate = templateService.update(savedTemplate);
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(updatedTemplate.getKeywords())
        .isNotEmpty()
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), updatedTemplate.getName().toLowerCase());
    assertThat(updatedTemplate.getDescription()).isEqualTo(TEMPLATE_DESC_CHANGED);
    assertThat(updatedTemplate.getVersion()).isEqualTo(1L);
    assertThat(updatedTemplate.getTemplateObject()).isNotNull();
    SshCommandTemplate = (SshCommandTemplate) savedTemplate.getTemplateObject();
    assertThat(SshCommandTemplate).isNotNull();
    assertThat(SshCommandTemplate.getCommandType()).isEqualTo(START);
    assertThat(SshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(SshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("Start");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateTemplateVariables() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);

    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(savedTemplate.getKeywords())
        .isNotEmpty()
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), savedTemplate.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    SshCommandTemplate SshCommandTemplate = (SshCommandTemplate) savedTemplate.getTemplateObject();
    assertThat(SshCommandTemplate).isNotNull();
    assertThat(SshCommandTemplate.getCommandType()).isEqualTo(START);
    assertThat(SshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(SshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("Start");
    assertThat(savedTemplate.getVariables()).isNullOrEmpty();

    Variable var1 = VariableBuilder.aVariable()
                        .type(VariableType.TEXT)
                        .name("var1")
                        .mandatory(false)
                        .description("var 1 original")
                        .build();
    savedTemplate.setVariables(asList(var1));

    Template updatedTemplate = templateService.update(savedTemplate);
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getVersion()).isEqualTo(2L);
    assertThat(updatedTemplate.getVariables()).containsExactly(var1);

    var1.setDescription("var 1 updated");
    updatedTemplate.setVariables(asList(var1));

    updatedTemplate = templateService.update(updatedTemplate);
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getVersion()).isEqualTo(3L);
    assertThat(updatedTemplate.getVariables()).containsExactly(var1);

    var1.setValue("var 1");
    updatedTemplate.setVariables(asList(var1));

    updatedTemplate = templateService.update(updatedTemplate);
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getVersion()).isEqualTo(4L);
    assertThat(updatedTemplate.getVariables()).containsExactly(var1);

    var1.setDescription("var 1 with value");
    updatedTemplate.setVariables(asList(var1));

    updatedTemplate = templateService.update(updatedTemplate);
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getVersion()).isEqualTo(5L);
    assertThat(updatedTemplate.getVariables()).containsExactly(var1);
  }

  @Test(expected = ConstraintViolationException.class)
  @Category(UnitTests.class)
  public void shouldNotUpdateInvalidNameTemplate() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);

    assertThat(savedTemplate).isNotNull();

    savedTemplate.setDescription(TEMPLATE_DESC_CHANGED);
    savedTemplate.setName(WingsTestConstants.INVALID_NAME);
    templateService.update(savedTemplate);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateHttpTemplate() {
    updateHttpTemplate(GLOBAL_APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDeleteTemplate() {
    deleteTemplate(GLOBAL_APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDeleteByFolder() {
    TemplateFolder templateFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, "Harness/Tomcat Commands");

    Template httpTemplate =
        Template.builder()
            .name("Ping Response")
            .appId(GLOBAL_APP_ID)
            .accountId(GLOBAL_ACCOUNT_ID)
            .folderPath("Harness/Tomcat Commands")
            .templateObject(
                HttpTemplate.builder().assertion("200 ok").url("http://harness.io").header("header").build())
            .build();
    templateService.save(httpTemplate);

    templateService.deleteByFolder(templateFolder);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFetchTemplateUri() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    String templateUri = templateService.fetchTemplateUri(template.getUuid());
    assertTemplateUri(templateUri);
  }

  private void assertTemplateUri(String templateUri) {
    assertThat(templateUri).isNotEmpty();
    assertThat(obtainTemplateFolderPath(templateUri)).isEqualTo("Harness/Tomcat Commands");
    assertThat(obtainTemplateName(templateUri)).isEqualTo(MY_START_COMMAND);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFetchTemplateUriWhenTemplateDeleted() {
    assertThat(templateService.fetchTemplateUri(TEMPLATE_ID)).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFetchTemplateIdfromUri() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    String templateUri = templateService.fetchTemplateUri(template.getUuid());
    assertTemplateUri(templateUri);

    String templateUuid = templateService.fetchTemplateIdFromUri(GLOBAL_ACCOUNT_ID, templateUri);
    assertThat(templateUuid).isNotEmpty().isEqualTo(savedTemplate.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldConstructCommandFromSshTemplate() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();

    Object command = templateService.constructEntityFromTemplate(template.getUuid(), "1", EntityType.COMMAND);
    assertThat(command).isNotNull();
    assertThat(command instanceof Command);
    Command sshcommand = (Command) command;
    assertThat(sshcommand.getCommandUnits()).isNotEmpty();
  }

  private Template getSshCommandTemplate() {
    return getSshCommandTemplate(MY_START_COMMAND, GLOBAL_APP_ID);
  }

  private Template getSshCommandTemplate(String name, String appId) {
    SshCommandTemplate sshCommandTemplate = SshCommandTemplate.builder()
                                                .commandType(START)
                                                .commandUnits(asList(anExecCommandUnit()
                                                                         .withName("Start")
                                                                         .withCommandPath("/home/xxx/tomcat")
                                                                         .withCommandString("bin/startup.sh")
                                                                         .build()))
                                                .build();

    return Template.builder()
        .templateObject(sshCommandTemplate)
        .name(name)
        .description(TEMPLATE_DESC)
        .folderPath("Harness/Tomcat Commands")
        .keywords(asList(TEMPLATE_CUSTOM_KEYWORD))
        .gallery(HARNESS_GALLERY)
        .appId(appId)
        .accountId(GLOBAL_ACCOUNT_ID)
        .build();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldConstructHttpFromHttpTemplate() {
    Template httpTemplate =
        Template.builder()
            .name("Ping Response")
            .appId(GLOBAL_APP_ID)
            .accountId(GLOBAL_ACCOUNT_ID)
            .folderPath("Harness/Tomcat Commands")
            .templateObject(
                HttpTemplate.builder().assertion("200 ok").url("http://harness.io").header("header").build())
            .build();
    Template savedTemplate = templateService.save(httpTemplate);

    assertThat(savedTemplate).isNotNull();

    Object http = templateService.constructEntityFromTemplate(savedTemplate.getUuid(), "1", EntityType.WORKFLOW);
    assertThat(http).isNotNull();
    assertThat(http instanceof GraphNode);
    GraphNode graphNode = (GraphNode) http;
    assertThat(graphNode).isNotNull();
    assertThat(graphNode.getProperties()).isNotNull().containsKeys("url", "header");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetCommandCategoriesAtAccountLevel() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);

    SshCommandTemplate sshCommandTemplate2 = SshCommandTemplate.builder()
                                                 .commandType(START)
                                                 .commandUnits(asList(anExecCommandUnit()
                                                                          .withName("Start")
                                                                          .withCommandPath("/home/xxx/tomcat")
                                                                          .withCommandString("bin/startup.sh")
                                                                          .build()))
                                                 .build();

    Template template2 = Template.builder()
                             .templateObject(sshCommandTemplate2)
                             .name("My Install Command")
                             .description(TEMPLATE_DESC)
                             .folderPath("Harness/Tomcat Commands")
                             .gallery(HARNESS_GALLERY)
                             .appId(GLOBAL_APP_ID)
                             .accountId(GLOBAL_ACCOUNT_ID)
                             .build();

    templateService.save(template2);

    List<CommandCategory> commandTemplateCategories =
        templateService.getCommandCategories(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, savedTemplate.getUuid());
    validateCommandCategories(commandTemplateCategories);
    validateCopyTemplateCategories(commandTemplateCategories);
    validateScriptTemplateCategories(commandTemplateCategories);
    validateCommandTemplateCommandCategories(
        commandTemplateCategories, singletonList("My Install Command"), singletonList(MY_START_COMMAND));
    validateVerifyTemplateCategories(commandTemplateCategories);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetCommandCategoriesAtAppLevel() {
    // create 2 templates in app1, 1 in another app and 1 at account level
    Template template1 = getSshCommandTemplate(MY_START_COMMAND_APP, APP_ID);
    Template savedTemplate1 = templateService.save(template1);
    Template template2 = getSshCommandTemplate(ANOTHER_MY_START_COMMAND_APP, APP_ID);
    templateService.save(template2);
    Template template3 = getSshCommandTemplate(ANOTHER_MY_START_COMMAND_ANOTHER_APP, ANOTHER_APP_ID);
    templateService.save(template3);
    Template template4 = getSshCommandTemplate();
    templateService.save(template4);

    List<CommandCategory> commandTemplateCategories =
        templateService.getCommandCategories(GLOBAL_ACCOUNT_ID, APP_ID, savedTemplate1.getUuid());
    validateCommandCategories(commandTemplateCategories);
    validateCopyTemplateCategories(commandTemplateCategories);
    validateScriptTemplateCategories(commandTemplateCategories);
    validateCommandTemplateCommandCategories(commandTemplateCategories, singletonList(ANOTHER_MY_START_COMMAND_APP),
        asList(MY_START_COMMAND, ANOTHER_MY_START_COMMAND_ANOTHER_APP));
    validateVerifyTemplateCategories(commandTemplateCategories);
  }

  private void validateCommandTemplateCommandCategories(
      List<CommandCategory> commandTemplateCategories, List<String> includeList, List<String> excludeList) {
    List<CommandCategory> commandTemplateCommandCategories =
        commandTemplateCategories.stream()
            .filter(commandCategory -> commandCategory.getType().equals(COMMANDS))
            .collect(toList());
    assertThat(commandTemplateCommandCategories).extracting(CommandCategory::getCommandUnits).isNotEmpty();
    commandTemplateCommandCategories.forEach(commandCategory -> {
      assertThat(commandCategory.getType()).isEqualTo(COMMANDS);
      assertThat(commandCategory.getDisplayName()).isEqualTo(COMMANDS.getDisplayName());
      assertThat(commandCategory.getCommandUnits())
          .isNotEmpty()
          .extracting(CommandCategory.CommandUnit::getType)
          .contains(COMMAND, COMMAND);
      assertThat(commandCategory.getCommandUnits())
          .isNotEmpty()
          .extracting(CommandCategory.CommandUnit::getName)
          .contains(includeList.toArray(new String[includeList.size()]))
          .doesNotContain(excludeList.toArray(new String[excludeList.size()]));
    });
  }

  private void validateCopyTemplateCategories(List<CommandCategory> commandTemplateCategories) {
    List<CommandCategory> copyTemplateCategories =
        commandTemplateCategories.stream()
            .filter(commandCategory -> commandCategory.getType().equals(COPY))
            .collect(toList());
    assertThat(copyTemplateCategories).extracting(CommandCategory::getCommandUnits).isNotEmpty();
    copyTemplateCategories.forEach(commandCategory -> {
      assertThat(commandCategory.getType()).isEqualTo(COPY);
      assertThat(commandCategory.getDisplayName()).isEqualTo(COPY.getDisplayName());
      assertThat(commandCategory.getCommandUnits())
          .isNotEmpty()
          .extracting(CommandCategory.CommandUnit::getType)
          .contains(COPY_CONFIGS, SCP);
    });
  }

  private void validateVerifyTemplateCategories(List<CommandCategory> commandTemplateCategories) {
    List<CommandCategory> verifyTemplateCategories =
        commandTemplateCategories.stream()
            .filter(commandCategory -> commandCategory.getType().equals(VERIFICATIONS))
            .collect(toList());
    assertThat(verifyTemplateCategories).extracting(CommandCategory::getCommandUnits).isNotEmpty();
    verifyTemplateCategories.forEach(commandCategory -> {
      assertThat(commandCategory.getType()).isEqualTo(VERIFICATIONS);
      assertThat(commandCategory.getDisplayName()).isEqualTo(VERIFICATIONS.getDisplayName());
      assertThat(commandCategory.getCommandUnits())
          .isNotEmpty()
          .extracting(CommandCategory.CommandUnit::getType)
          .contains(PROCESS_CHECK_RUNNING, PORT_CHECK_CLEARED, PORT_CHECK_LISTENING);
    });
  }

  private void validateScriptTemplateCategories(List<CommandCategory> commandTemplateCategories) {
    List<CommandCategory> scriptTemplateCategories =
        commandTemplateCategories.stream()
            .filter(commandCategory -> commandCategory.getType().equals(SCRIPTS))
            .collect(toList());
    assertThat(scriptTemplateCategories).extracting(CommandCategory::getCommandUnits).isNotEmpty();
    scriptTemplateCategories.forEach(commandCategory -> {
      assertThat(commandCategory.getType()).isEqualTo(SCRIPTS);
      assertThat(commandCategory.getDisplayName()).isEqualTo(SCRIPTS.getDisplayName());
      assertThat(commandCategory.getCommandUnits())
          .isNotEmpty()
          .extracting(CommandCategory.CommandUnit::getType)
          .contains(EXEC, DOCKER_START, DOCKER_STOP);
    });
  }

  private void validateCommandCategories(List<CommandCategory> commandTemplateCategories) {
    assertThat(commandTemplateCategories).isNotEmpty();
    assertThat(commandTemplateCategories).isNotEmpty();
    assertThat(commandTemplateCategories)
        .isNotEmpty()
        .extracting(CommandCategory::getType)
        .contains(CommandCategory.Type.values());
    assertThat(commandTemplateCategories)
        .isNotEmpty()
        .extracting(CommandCategory::getDisplayName)
        .contains(
            COMMANDS.getDisplayName(), COPY.getDisplayName(), SCRIPTS.getDisplayName(), VERIFICATIONS.getDisplayName());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFetchTemplateByKeyword() {
    Template template = getSshCommandTemplate();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    Template template1 = templateService.fetchTemplateByKeyword(GLOBAL_ACCOUNT_ID, TEMPLATE_CUSTOM_KEYWORD);
    assertThat(template1.getUuid()).isEqualTo(savedTemplate.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldConvertYamlToTemplate() throws IOException {
    Template template = templateService.convertYamlToTemplate(POWER_SHELL_IIS_V2_INSTALL_PATH);
    assertThat(template).isNotNull();
    assertThat(((SshCommandTemplate) template.getTemplateObject()).getCommands().size()).isEqualTo(4);
    assertThat(((SshCommandTemplate) template.getTemplateObject()).getCommands().get(0).getName())
        .isEqualTo("Download Artifact");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldConvertYamlToTemplateIISV3() throws IOException {
    Template template = templateService.convertYamlToTemplate(POWER_SHELL_IIS_V3_INSTALL_PATH);
    assertThat(template).isNotNull();
    assertThat(((SshCommandTemplate) template.getTemplateObject()).getCommands().size()).isEqualTo(4);
    assertThat(((SshCommandTemplate) template.getTemplateObject()).getCommands().get(0).getName())
        .isEqualTo("Download Artifact");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSaveTemplateAtAppLevel() {
    Template template = getSshCommandTemplate(MY_START_COMMAND, APP_ID);

    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAccountId()).isNotEmpty();
    assertThat(savedTemplate.getGalleryId()).isNotEmpty();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(APP_ID);
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords())
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), template.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);

    SshCommandTemplate savedSshCommandTemplate = (SshCommandTemplate) savedTemplate.getTemplateObject();
    assertThat(savedSshCommandTemplate).isNotNull();
    assertThat(savedSshCommandTemplate.getCommandType()).isEqualTo(START);
    assertThat(savedSshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(savedSshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("Start");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetAppLevelTemplate() {
    Template savedTemplate = saveTemplate(MY_START_COMMAND, APP_ID);
    getTemplateAndValidate(savedTemplate, APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListAppLevelTemplates() {
    saveTemplate(MY_START_COMMAND, APP_ID);

    PageRequest<Template> pageRequest = aPageRequest().addFilter("appId", EQ, APP_ID).build();
    List<Template> templates = templateService.list(pageRequest);

    assertThat(templates).isNotEmpty();
    Template template = templates.stream().findFirst().get();

    assertThat(template).isNotNull();
    assertThat(template.getAppId()).isNotNull().isEqualTo(APP_ID);
    assertThat(template.getKeywords())
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), template.getName().toLowerCase());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAllowSameTemplateNameInAccountAndApplication() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder accountLevelFolder = templateFolderService.save(TemplateFolder.builder()
                                                                       .name(TEMPLATE_FOLDER_NAME)
                                                                       .description(TEMPLATE_FOLDER_DEC)
                                                                       .parentId(parentFolder.getUuid())
                                                                       .appId(GLOBAL_APP_ID)
                                                                       .accountId(GLOBAL_ACCOUNT_ID)
                                                                       .build());
    assertThat(accountLevelFolder).isNotNull();
    assertThat(accountLevelFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(accountLevelFolder.getAppId()).isEqualTo(GLOBAL_APP_ID);

    Template accountTemplate = templateService.save(
        Template.builder()
            .name("Ping Response")
            .appId(GLOBAL_APP_ID)
            .accountId(GLOBAL_ACCOUNT_ID)
            .folderId(accountLevelFolder.getUuid())
            .templateObject(
                HttpTemplate.builder().assertion("200 ok").url("http://harness.io").header("header").build())
            .build());
    assertThat(accountTemplate.getName()).isEqualTo("Ping Response");
    assertThat(accountTemplate.getAppId()).isEqualTo(GLOBAL_APP_ID);

    TemplateFolder appLevelFolder = templateFolderService.save(TemplateFolder.builder()
                                                                   .name(TEMPLATE_FOLDER_NAME)
                                                                   .description(TEMPLATE_FOLDER_DEC)
                                                                   .parentId(parentFolder.getUuid())
                                                                   .appId(APP_ID)
                                                                   .accountId(GLOBAL_ACCOUNT_ID)
                                                                   .build());
    assertThat(appLevelFolder).isNotNull();
    assertThat(appLevelFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(appLevelFolder.getAppId()).isEqualTo(APP_ID);

    Template appTemplate = templateService.save(
        Template.builder()
            .name("Ping Response")
            .appId(APP_ID)
            .accountId(GLOBAL_ACCOUNT_ID)
            .folderId(appLevelFolder.getUuid())
            .templateObject(
                HttpTemplate.builder().assertion("200 ok").url("http://harness.io").header("header").build())
            .build());
    assertThat(appTemplate.getName()).isEqualTo("Ping Response");
    assertThat(appTemplate.getAppId()).isEqualTo(APP_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFetchApplicationLevelTemplateByKeyword() {
    Template template = getSshCommandTemplate(MY_START_COMMAND, APP_ID);
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    Template template1 = templateService.fetchTemplateByKeyword(GLOBAL_ACCOUNT_ID, TEMPLATE_CUSTOM_KEYWORD);
    assertThat(template1).isNull();
    template1 = templateService.fetchTemplateByKeyword(GLOBAL_ACCOUNT_ID, APP_ID, TEMPLATE_CUSTOM_KEYWORD);
    assertThat(template1.getUuid()).isEqualTo(savedTemplate.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateApplicationLevelHttpTemplate() {
    updateHttpTemplate(APP_ID);
  }

  private void updateHttpTemplate(String appId) {
    Template httpTemplate =
        Template.builder()
            .name("Ping Response")
            .appId(appId)
            .accountId(GLOBAL_ACCOUNT_ID)
            .folderPath("Harness/Tomcat Commands")
            .templateObject(
                HttpTemplate.builder().assertion("200 ok").url("http://harness.io").header("header").build())
            .build();
    Template savedTemplate = templateService.save(httpTemplate);

    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(appId);
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    HttpTemplate savedHttpTemplate = (HttpTemplate) savedTemplate.getTemplateObject();
    assertThat(savedHttpTemplate).isNotNull();
    assertThat(savedHttpTemplate.getUrl()).isNotNull().isEqualTo("http://harness.io");
    assertThat(savedHttpTemplate).isNotNull();
    assertThat(savedTemplate.getKeywords()).isNotEmpty();
    assertThat(savedTemplate.getKeywords()).contains(savedTemplate.getName().toLowerCase());

    savedTemplate.setDescription(TEMPLATE_DESC_CHANGED);

    HttpTemplate updatedHttpTemplate = HttpTemplate.builder()
                                           .url("https://harness.io")
                                           .header(savedHttpTemplate.getHeader())
                                           .assertion(savedHttpTemplate.getAssertion())
                                           .build();
    savedTemplate.setTemplateObject(updatedHttpTemplate);
    savedTemplate.setName("Another Ping Response");

    Template updatedTemplate = templateService.update(savedTemplate);
    assertThat(updatedTemplate).isNotNull();
    assertThat(updatedTemplate.getAppId()).isNotNull().isEqualTo(appId);
    assertThat(updatedTemplate.getDescription()).isEqualTo(TEMPLATE_DESC_CHANGED);
    assertThat(updatedTemplate.getVersion()).isEqualTo(2L);
    assertThat(updatedTemplate.getTemplateObject()).isNotNull();
    assertThat(updatedTemplate.getKeywords()).isNotEmpty();
    assertThat(updatedTemplate.getKeywords()).contains(updatedTemplate.getName().toLowerCase());
    assertThat(updatedTemplate.getKeywords()).doesNotContain("Ping Response".toLowerCase());

    updatedHttpTemplate = (HttpTemplate) updatedTemplate.getTemplateObject();
    assertThat(updatedHttpTemplate).isNotNull();
    assertThat(updatedHttpTemplate.getUrl()).isNotNull().isEqualTo("https://harness.io");
    assertThat(updatedHttpTemplate).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDeleteApplicationLevelTemplatesByFolder() {
    TemplateFolder parentFolder = templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    TemplateFolder appLevelFolder = templateFolderService.save(TemplateFolder.builder()
                                                                   .name(TEMPLATE_FOLDER_NAME)
                                                                   .description(TEMPLATE_FOLDER_DEC)
                                                                   .parentId(parentFolder.getUuid())
                                                                   .appId(APP_ID)
                                                                   .accountId(GLOBAL_ACCOUNT_ID)
                                                                   .build());
    assertThat(appLevelFolder).isNotNull();
    assertThat(appLevelFolder.getName()).isEqualTo(TEMPLATE_FOLDER_NAME);
    assertThat(appLevelFolder.getAppId()).isEqualTo(APP_ID);

    Template httpTemplate =
        Template.builder()
            .name("Ping Response")
            .appId(APP_ID)
            .accountId(GLOBAL_ACCOUNT_ID)
            .folderPath("Harness/My Template Folder")
            .folderId(appLevelFolder.getUuid())
            .templateObject(
                HttpTemplate.builder().assertion("200 ok").url("http://harness.io").header("header").build())
            .build();
    Template savedTemplate = templateService.save(httpTemplate);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isEqualTo(APP_ID);
    assertThat(savedTemplate.getName()).isEqualTo("Ping Response");

    assertTrue(templateService.deleteByFolder(appLevelFolder));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDeleteApplicationLevelTemplate() {
    deleteTemplate(APP_ID);
  }

  private void deleteTemplate(String appId) {
    Template template;
    if (appId.equals(APP_ID)) {
      template = getSshCommandTemplate(MY_START_COMMAND, APP_ID);
    } else {
      template = getSshCommandTemplate();
    }
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getAppId()).isNotNull().isEqualTo(appId);
    assertThat(savedTemplate.getKeywords())
        .isNotEmpty()
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), savedTemplate.getName().toLowerCase());
    assertThat(savedTemplate.getVersion()).isEqualTo(1);
    SshCommandTemplate SshCommandTemplate = (SshCommandTemplate) savedTemplate.getTemplateObject();
    assertThat(SshCommandTemplate).isNotNull();
    assertThat(SshCommandTemplate.getCommandType()).isEqualTo(START);
    assertThat(SshCommandTemplate.getCommandUnits()).isNotEmpty();
    assertThat(SshCommandTemplate.getCommandUnits()).extracting(CommandUnit::getName).contains("Start");

    boolean delete = templateService.delete(savedTemplate.getAccountId(), savedTemplate.getUuid());
    assertThat(delete).isTrue();
    Template deletedTemplate;
    try {
      deletedTemplate = templateService.get(savedTemplate.getUuid());
    } catch (WingsException e) {
      deletedTemplate = null;
    }
    assertThat(deletedTemplate).isNull();

    // Verify the versioned template
    VersionedTemplate versionedTemplate = templateService.getVersionedTemplate(
        savedTemplate.getAccountId(), savedTemplate.getUuid(), savedTemplate.getVersion());
    assertThat(versionedTemplate).isNull();

    // Verify the template versions deleted
    PageRequest templateVersionPageRequest =
        aPageRequest()
            .addFilter(TemplateVersion.ACCOUNT_ID_KEY, EQ, savedTemplate.getAccountId())
            .addFilter(TemplateVersion.APP_ID_KEY, EQ, appId)
            .addFilter(TemplateVersion.TEMPLATE_UUID_KEY, EQ, savedTemplate.getUuid())
            .build();
    assertThat(templateVersionService.listTemplateVersions(templateVersionPageRequest).getResponse()).isEmpty();
  }
}
