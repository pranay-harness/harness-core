/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.template;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.MILOS;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static io.harness.shell.ScriptType.BASH;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.CommandCategory.Type.COMMANDS;
import static software.wings.beans.CommandCategory.Type.COPY;
import static software.wings.beans.CommandCategory.Type.SCRIPTS;
import static software.wings.beans.CommandCategory.Type.VERIFICATIONS;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.TEXT;
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
import static software.wings.beans.template.TemplateGallery.GalleryKey;
import static software.wings.beans.template.TemplateHelper.obtainTemplateFolderPath;
import static software.wings.beans.template.TemplateHelper.obtainTemplateName;
import static software.wings.common.TemplateConstants.HARNESS_COMMAND_LIBRARY_GALLERY;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.IMPORTED_TEMPLATE_PREFIX;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_V2_INSTALL_PATH;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_V3_INSTALL_PATH;
import static software.wings.common.TemplateConstants.SSH;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_CUSTOM_KEYWORD;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_DESC_CHANGED;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_DEC;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_FOLDER_NAME;
import static software.wings.utils.TemplateTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.beans.CommandCategory;
import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.User;
import software.wings.beans.Variable;
import software.wings.beans.Variable.VariableBuilder;
import software.wings.beans.VariableType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.template.CopiedTemplateMetadata;
import software.wings.beans.template.ImportedTemplate;
import software.wings.beans.template.ImportedTemplateMetadata;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateGalleryHelper;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.VersionedTemplate;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.beans.template.dto.HarnessImportedTemplateDetails;
import software.wings.beans.template.dto.ImportedCommand;
import software.wings.beans.template.dto.ImportedCommandVersion;
import software.wings.exception.TemplateException;
import software.wings.security.AppPermissionSummary;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript;
import software.wings.service.intfc.template.ImportedTemplateService;
import software.wings.service.intfc.template.TemplateVersionService;
import software.wings.utils.WingsTestConstants;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolationException;
import org.junit.Test;
import org.junit.Test.None;
import org.junit.experimental.categories.Category;
@OwnedBy(PL)
public class TemplateServiceTest extends TemplateBaseTestHelper {
  private static final String MY_START_COMMAND = "My Start Command";
  private static final String MY_START_COMMAND_APP = "My Start Command App";
  private static final String ANOTHER_MY_START_COMMAND_APP = "Another My Start Command App";
  private static final String ANOTHER_MY_START_COMMAND_ANOTHER_APP = "Another My Start Command Another App";
  private static final String ANOTHER_APP_ID = "ANOTHER_APP_ID";
  private static final String REF_TEMPLATE_NAME = "Ref Template";

  @Inject private TemplateVersionService templateVersionService;
  @Inject private TemplateServiceImpl templateServiceImpl;
  @Inject private TemplateGalleryHelper templateGalleryHelper;
  @Inject private ImportedTemplateService importedTemplateService;
  @Inject private HPersistence persistence;

  @Test
  @Owner(developers = SRINIVAS)
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
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldNotSaveInvalidNameTemplate() {
    Template template = getSshCommandTemplate();
    template.setName(WingsTestConstants.INVALID_NAME);
    templateService.save(template);
  }

  @Test
  @Owner(developers = SRINIVAS)
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
  @Owner(developers = SRINIVAS)
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
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldList() {
    saveTemplate();

    PageRequest<Template> pageRequest = aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build();
    List<Template> templates = templateService.list(pageRequest,
        Collections.singletonList(templateGalleryService.getAccountGalleryKey().name()), GLOBAL_ACCOUNT_ID, false);

    assertThat(templates).isNotEmpty();
    Template template = templates.stream().findFirst().get();

    assertThat(template).isNotNull();
    assertThat(template.getTemplateObject()).isNotNull();
    assertThat(template.getAppId()).isNotNull().isEqualTo(GLOBAL_APP_ID);
    assertThat(template.getKeywords())
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), template.getName().toLowerCase());
  }

  @Test
  @Owner(developers = SRINIVAS)
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
  @Owner(developers = GARVIT)
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

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void shouldNotUpdateTemplateVariables() {
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
                        .name("var-1")
                        .mandatory(false)
                        .description("variable description")
                        .build();
    savedTemplate.setVariables(asList(var1));

    templateService.update(savedTemplate);
  }

  @Test(expected = ConstraintViolationException.class)
  @Owner(developers = SRINIVAS)
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
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldUpdateHttpTemplate() {
    updateHttpTemplate(GLOBAL_APP_ID);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldDeleteTemplate() {
    deleteTemplate(GLOBAL_APP_ID);
  }

  @Test(expected = TemplateException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void shouldFailDeleteTemplateUsedInSecretManager() {
    ShellScriptTemplate shellScriptTemplate = ShellScriptTemplate.builder()
                                                  .scriptType(BASH.toString())
                                                  .scriptString("echo ${var1}\n"
                                                      + "export A=\"aaa\"\n"
                                                      + "export B=\"bbb\"")
                                                  .outputVars("A,B")
                                                  .build();
    Template template =
        Template.builder()
            .templateObject(shellScriptTemplate)
            .folderPath("Harness/Tomcat Commands")
            .gallery(HARNESS_GALLERY)
            .appId(GLOBAL_APP_ID)
            .accountId(GLOBAL_ACCOUNT_ID)
            .name("Sample Script")
            .variables(Collections.singletonList(aVariable().type(TEXT).name("var1").mandatory(true).build()))
            .build();

    Template savedTemplate = templateService.save(template);

    CustomSecretsManagerShellScript script = CustomSecretsManagerShellScript.builder()
                                                 .scriptType(CustomSecretsManagerShellScript.ScriptType.BASH)
                                                 .scriptString(UUIDGenerator.generateUuid())
                                                 .variables(new ArrayList<>())
                                                 .build();
    CustomSecretsManagerConfig config = CustomSecretsManagerConfig.builder()
                                            .name("CustomSecretsManager")
                                            .templateId(savedTemplate.getUuid())
                                            .delegateSelectors(new HashSet<>())
                                            .executeOnDelegate(true)
                                            .customSecretsManagerShellScript(script)
                                            .testVariables(new HashSet<>())
                                            .build();
    persistence.save(config);
    templateService.delete(savedTemplate.getAccountId(), savedTemplate.getUuid());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldDeleteByFolder() {
    TemplateGallery templateGallery =
        templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID, templateGalleryService.getAccountGalleryKey());
    TemplateFolder templateFolder =
        templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, "Harness/Tomcat Commands", templateGallery.getUuid());

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
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFetchTemplateUri() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    String templateUri = templateService.fetchTemplateUri(template.getUuid());
    assertTemplateUri(templateUri, "Harness/Tomcat Commands/My Start Command", "Harness/Tomcat Commands");
  }

  private void assertTemplateUri(String templateUri, String expectedUri, String expectedFolderPath) {
    assertThat(templateUri).isNotEmpty();
    assertThat(templateUri).isEqualTo(expectedUri);
    assertThat(obtainTemplateFolderPath(templateUri)).isEqualTo(expectedFolderPath);
    assertThat(obtainTemplateName(templateUri)).isEqualTo(MY_START_COMMAND);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void shouldFetchNameSpacedTemplateUri() {
    Template template = getSshCommandTemplate();

    // Case 1: Non Imported.
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    String templateUri = templateService.makeNamespacedTemplareUri(template.getUuid(), "1");
    assertTemplateUri(templateUri, "Harness/Tomcat Commands/My Start Command:1", "Harness/Tomcat Commands");

    // Case 2: Imported - harness gallery.
    Template importedTemplate = saveImportedTemplate(
        getSshCommandTemplate("Harness/" + MY_START_COMMAND, GLOBAL_APP_ID), MY_START_COMMAND, "Harness", "1.5", false);
    assertThat(importedTemplate).isNotNull();
    String templateUriImported = templateService.makeNamespacedTemplareUri(importedTemplate.getUuid(), "1");
    assertTemplateUri(
        templateUriImported, IMPORTED_TEMPLATE_PREFIX + "Harness/" + MY_START_COMMAND + ":1.5", "Harness");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFetchTemplateUriWhenTemplateDeleted() {
    assertThat(templateService.fetchTemplateUri(TEMPLATE_ID)).isNull();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFetchTemplateIdfromUri() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    String templateUri = templateService.fetchTemplateUri(template.getUuid());
    assertTemplateUri(templateUri, "Harness/Tomcat Commands/My Start Command", "Harness/Tomcat Commands");

    String templateUuid = templateService.fetchTemplateIdFromUri(GLOBAL_ACCOUNT_ID, templateUri);
    assertThat(templateUuid).isNotEmpty().isEqualTo(savedTemplate.getUuid());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldConstructCommandFromSshTemplate() {
    Template template = getSshCommandTemplate();

    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();

    Object command = templateService.constructEntityFromTemplate(template.getUuid(), "1", EntityType.COMMAND);
    assertThat(command).isNotNull();
    assertThat(command instanceof Command).isTrue();
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
        .keywords(ImmutableSet.of(TEMPLATE_CUSTOM_KEYWORD))
        .gallery(HARNESS_GALLERY)
        .appId(appId)
        .accountId(GLOBAL_ACCOUNT_ID)
        .build();
  }

  @Test
  @Owner(developers = SRINIVAS)
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
    assertThat(http instanceof GraphNode).isTrue();
    GraphNode graphNode = (GraphNode) http;
    assertThat(graphNode).isNotNull();
    assertThat(graphNode.getProperties()).isNotNull().containsKeys("url", "header");
  }

  @Test
  @Owner(developers = SRINIVAS)
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
  @Owner(developers = AADITI)
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
            .filter(commandCategory -> commandCategory.getType() == COMMANDS)
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
    List<CommandCategory> copyTemplateCategories = commandTemplateCategories.stream()
                                                       .filter(commandCategory -> commandCategory.getType() == COPY)
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
            .filter(commandCategory -> commandCategory.getType() == VERIFICATIONS)
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
            .filter(commandCategory -> commandCategory.getType() == SCRIPTS)
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
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldFetchTemplateByKeyword() {
    Template template = getSshCommandTemplate();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    Template template1 =
        templateService.fetchTemplateByKeywordForAccountGallery(GLOBAL_ACCOUNT_ID, TEMPLATE_CUSTOM_KEYWORD);
    assertThat(template1.getUuid()).isEqualTo(savedTemplate.getUuid());
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldConvertYamlToTemplate() throws IOException {
    Template template = templateService.convertYamlToTemplate(POWER_SHELL_IIS_V2_INSTALL_PATH);
    assertThat(template).isNotNull();
    assertThat(((SshCommandTemplate) template.getTemplateObject()).getCommands().size()).isEqualTo(4);
    assertThat(((SshCommandTemplate) template.getTemplateObject()).getCommands().get(0).getName())
        .isEqualTo("Download Artifact");
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldConvertYamlToTemplateIISV3() throws IOException {
    Template template = templateService.convertYamlToTemplate(POWER_SHELL_IIS_V3_INSTALL_PATH);
    assertThat(template).isNotNull();
    assertThat(((SshCommandTemplate) template.getTemplateObject()).getCommands().size()).isEqualTo(4);
    assertThat(((SshCommandTemplate) template.getTemplateObject()).getCommands().get(0).getName())
        .isEqualTo("Download Artifact");
  }

  @Test
  @Owner(developers = SRINIVAS)
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

  @Test(expected = None.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetAppLevelTemplate() {
    Template savedTemplate = saveTemplate(MY_START_COMMAND, APP_ID);
    getTemplateAndValidate(savedTemplate, APP_ID);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldListAppLevelTemplates() {
    saveTemplate(MY_START_COMMAND, APP_ID);

    PageRequest<Template> pageRequest = aPageRequest().addFilter("appId", EQ, APP_ID).build();
    List<Template> templates = templateService.list(pageRequest,
        Collections.singletonList(templateGalleryService.getAccountGalleryKey().name()), GLOBAL_ACCOUNT_ID, false);

    assertThat(templates).isNotEmpty();
    Template template = templates.stream().findFirst().get();

    assertThat(template).isNotNull();
    assertThat(template.getAppId()).isNotNull().isEqualTo(APP_ID);
    assertThat(template.getKeywords())
        .contains(TEMPLATE_CUSTOM_KEYWORD.toLowerCase(), template.getName().toLowerCase());
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldAllowSameTemplateNameInAccountAndApplication() {
    TemplateGallery templateGallery =
        templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID, templateGalleryService.getAccountGalleryKey());
    TemplateFolder parentFolder =
        templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY, templateGallery.getUuid());
    TemplateFolder accountLevelFolder = templateFolderService.save(TemplateFolder.builder()
                                                                       .name(TEMPLATE_FOLDER_NAME)
                                                                       .description(TEMPLATE_FOLDER_DEC)
                                                                       .parentId(parentFolder.getUuid())
                                                                       .appId(GLOBAL_APP_ID)
                                                                       .accountId(GLOBAL_ACCOUNT_ID)
                                                                       .build(),
        templateGallery.getUuid());
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
                                                                   .build(),
        templateGallery.getUuid());
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
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldFetchApplicationLevelTemplateByKeyword() {
    Template template = getSshCommandTemplate(MY_START_COMMAND, APP_ID);
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    Template template1 =
        templateService.fetchTemplateByKeywordForAccountGallery(GLOBAL_ACCOUNT_ID, TEMPLATE_CUSTOM_KEYWORD);
    assertThat(template1).isNull();
    template1 =
        templateService.fetchTemplateByKeywordForAccountGallery(GLOBAL_ACCOUNT_ID, APP_ID, TEMPLATE_CUSTOM_KEYWORD);
    assertThat(template1.getUuid()).isEqualTo(savedTemplate.getUuid());
  }

  @Test(expected = None.class)
  @Owner(developers = AADITI)
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
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldDeleteApplicationLevelTemplatesByFolder() {
    TemplateGallery templateGallery =
        templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID, templateGalleryService.getAccountGalleryKey());
    TemplateFolder parentFolder =
        templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY, templateGallery.getUuid());
    TemplateFolder appLevelFolder = templateFolderService.save(TemplateFolder.builder()
                                                                   .name(TEMPLATE_FOLDER_NAME)
                                                                   .description(TEMPLATE_FOLDER_DEC)
                                                                   .parentId(parentFolder.getUuid())
                                                                   .appId(APP_ID)
                                                                   .accountId(GLOBAL_ACCOUNT_ID)
                                                                   .build(),
        templateGallery.getUuid());
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

    assertThat(templateService.deleteByFolder(appLevelFolder)).isTrue();
  }

  private void setupTemplatePermissions(Set<String> templateIds) {
    User user = User.Builder.anUser().name(USER_NAME).uuid(USER_ID).build();
    user.setUserRequestContext(
        UserRequestContext.builder()
            .accountId(ACCOUNT_ID)
            .userPermissionInfo(UserPermissionInfo.builder()
                                    .appPermissionMapInternal(new HashMap<String, AppPermissionSummary>() {
                                      {
                                        put(APP_ID,
                                            AppPermissionSummary.builder()
                                                .templatePermissions(
                                                    new HashMap<PermissionAttribute.Action, Set<String>>() {
                                                      { put(PermissionAttribute.Action.DELETE, templateIds); }
                                                    })
                                                .build());
                                      }
                                    })
                                    .build())
            .build());
    UserThreadLocal.set(user);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void shouldFailDeleteByFolderIfApplicationPermissionsAreMissing() {
    TemplateGallery templateGallery =
        templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID, templateGalleryService.getAccountGalleryKey());
    TemplateFolder parentFolder =
        templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY, templateGallery.getUuid());
    TemplateFolder appLevelFolder = templateFolderService.save(TemplateFolder.builder()
                                                                   .name(TEMPLATE_FOLDER_NAME)
                                                                   .description(TEMPLATE_FOLDER_DEC)
                                                                   .parentId(parentFolder.getUuid())
                                                                   .appId(APP_ID)
                                                                   .accountId(GLOBAL_ACCOUNT_ID)
                                                                   .build(),
        templateGallery.getUuid());
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
    setupTemplatePermissions(new HashSet<>(Arrays.asList("templateId1", "templateId2")));
    assertThatThrownBy(() -> templateService.deleteByFolder(appLevelFolder))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(String.format("User not allowed to delete template folder with id %s", appLevelFolder.getUuid()));
  }

  @Test(expected = None.class)
  @Owner(developers = AADITI)
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
            .addFilter(TemplateVersion.ACCOUNT_ID_KEY2, EQ, savedTemplate.getAccountId())
            .addFilter(TemplateVersion.APP_ID_KEY2, EQ, appId)
            .addFilter(TemplateVersion.TEMPLATE_UUID_KEY, EQ, savedTemplate.getUuid())
            .build();
    assertThat(templateVersionService.listTemplateVersions(templateVersionPageRequest).getResponse()).isEmpty();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotSaveTemplateWithDuplicateVariables() {
    TemplateGallery templateGallery =
        templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID, templateGalleryService.getAccountGalleryKey());
    TemplateFolder parentFolder =
        templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY, templateGallery.getUuid());
    HttpTemplate httpTemplate =
        HttpTemplate.builder().url("{Url}").method("GET").header("Authorization:${Header}").assertion("200 ok").build();
    Template template = Template.builder()
                            .templateObject(httpTemplate)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .name("Enable Instance")
                            .variables(asList(aVariable().type(TEXT).name("Url").mandatory(true).build(),
                                aVariable().type(TEXT).name("Url").mandatory(true).build()))
                            .build();
    templateService.save(template);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotUpdateTemplateWithDuplicateVariables() {
    TemplateGallery templateGallery =
        templateGalleryService.getByAccount(GLOBAL_ACCOUNT_ID, templateGalleryService.getAccountGalleryKey());
    TemplateFolder parentFolder =
        templateFolderService.getByFolderPath(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY, templateGallery.getUuid());
    HttpTemplate httpTemplate =
        HttpTemplate.builder().url("{Url}").method("GET").header("Authorization:${Header}").assertion("200 ok").build();
    Template template = Template.builder()
                            .templateObject(httpTemplate)
                            .folderId(parentFolder.getUuid())
                            .appId(GLOBAL_APP_ID)
                            .accountId(GLOBAL_ACCOUNT_ID)
                            .name("Enable Instance")
                            .variables(asList(aVariable().type(TEXT).name("Url").mandatory(true).build(),
                                aVariable().type(TEXT).name("Method").mandatory(true).build()))
                            .build();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();
    assertThat(savedTemplate.getVariables()).isNotEmpty();
    assertThat(savedTemplate.getVariables().size()).isEqualTo(2);
    List<Variable> variables = savedTemplate.getVariables();
    for (Variable var : variables) {
      if (var.getName().equals("Method")) {
        var.setName("Url");
      }
    }
    savedTemplate.setVariables(variables);
    templateService.update(savedTemplate);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testFindByFolder() {
    Template savedTemplate = saveTemplate();
    assertThat(templateService.findByFolder(templateFolderService.get(savedTemplate.getFolderId()),
                   savedTemplate.getName(), savedTemplate.getAppId()))
        .isNotNull();
    assertThat(templateService.findByFolder(templateFolderService.get(savedTemplate.getFolderId()),
                   savedTemplate.getName() + "invalid text", savedTemplate.getAppId()))
        .isNull();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testFolderPathGeneration() {
    Template savedTemplate = saveTemplate();
    assertThat(templateService.getTemplateFolderPathString(savedTemplate)).isEqualTo("Harness/Tomcat Commands");
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void shouldFetchTemplateFromNameSpacedUri() {
    Template template = getSshCommandTemplate();
    Template savedTemplate = templateService.save(template);
    assertThat(savedTemplate).isNotNull();

    // Case 1: Account Template.
    String templateUri = templateService.fetchTemplateUri(template.getUuid());
    assertTemplateUri(templateUri, "Harness/Tomcat Commands/My Start Command", "Harness/Tomcat Commands");

    Template returnedTemplate = templateService.fetchTemplateFromUri(templateUri, GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID);
    assertThat(returnedTemplate.getVersion()).isNotNull();
    assertThat(returnedTemplate.getAccountId()).isNotNull();
    assertThat(returnedTemplate.getAppId()).isNotNull();
    assertThat(returnedTemplate.getFolderId()).isNotNull();
    assertThat(templateService.fetchTemplateIdFromUri(GLOBAL_ACCOUNT_ID, templateUri))
        .isEqualTo(savedTemplate.getUuid());

    // Case 2: Imported Template.
    Template importedTemplate = saveImportedTemplate(
        getSshCommandTemplate("Harness/" + MY_START_COMMAND, GLOBAL_APP_ID), MY_START_COMMAND, "Harness", "1.5", false);
    String importedTemplateUri = templateService.makeNamespacedTemplareUri(importedTemplate.getUuid(), "1");
    assertTemplateUri(
        importedTemplateUri, IMPORTED_TEMPLATE_PREFIX + "Harness/" + MY_START_COMMAND + ":1.5", "Harness");

    Template returnedImportedemplate =
        templateService.fetchTemplateFromUri(importedTemplateUri, GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID);
    assertThat(returnedImportedemplate.getVersion()).isNotNull();
    assertThat(returnedImportedemplate.getAccountId()).isNotNull();
    assertThat(returnedImportedemplate.getAppId()).isNotNull();
    assertThat(returnedImportedemplate.getFolderId()).isNotNull();
    assertThat(templateService.fetchTemplateIdFromUri(GLOBAL_ACCOUNT_ID, importedTemplateUri))
        .isEqualTo(importedTemplate.getUuid());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void shouldListVersionsOfImportedTemplate() {
    final String commandId = "COMMAND_ID";
    final String commandStoreId = "COMMAND_STORE_ID";
    Template template_1 = saveImportedTemplate(getSshCommandTemplate(), commandId, commandStoreId, "1.2", false);
    Template template = getSshCommandTemplate();
    template.setTemplateObject(SshCommandTemplate.builder().build());
    template.setUuid(template_1.getUuid());
    saveImportedTemplate(template, commandId, commandStoreId, "1.3", true);
    ImportedCommand templateVersions = templateVersionService.listImportedTemplateVersions(
        commandId, commandStoreId, GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID);
    List<String> versions = templateVersions.getImportedCommandVersionList()
                                .stream()
                                .map(ImportedCommandVersion::getVersion)
                                .collect(toList());
    assertThat(versions).isEqualTo(Arrays.asList("1.2", "1.3"));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void shouldGetAndListDefaultImportedTemplate() {
    final String commandId = "COMMAND_ID";
    final String commandStoreId = "COMMAND_STORE_ID";
    Template template_1 = saveImportedTemplate(getSshCommandTemplate(), commandId, commandStoreId, "1.2", false);
    Template template = getSshCommandTemplate();
    template.setTemplateObject(SshCommandTemplate.builder().build());
    template.setUuid(template_1.getUuid());
    saveImportedTemplate(template, commandId, commandStoreId, "1.3", true);
    PageRequest<Template> pageRequest = aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build();

    List<Template> templates = templateService.list(
        pageRequest, Collections.singletonList(HARNESS_COMMAND_LIBRARY_GALLERY), GLOBAL_ACCOUNT_ID, true);
    assertThat(templates.get(0).getVersion()).isEqualTo(1L);

    Template template_2 = templateService.get(template.getUuid(), "default");
    assertThat(template_2.getVersion()).isEqualTo(1L);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void shouldUpdateDefaultVersionAndGetImportedTemplate() {
    final String commandId = "COMMAND_ID";
    final String commandStoreId = "COMMAND_STORE_ID";
    Template template_1 = saveImportedTemplate(getSshCommandTemplate(), commandId, commandStoreId, "1.2", false);
    Template template = getSshCommandTemplate();
    template.setTemplateObject(SshCommandTemplate.builder().build());
    template.setUuid(template_1.getUuid());
    saveImportedTemplate(template, commandId, commandStoreId, "1.3", true);
    template.setTemplateMetadata(ImportedTemplateMetadata.builder().defaultVersion(2L).build());
    templateService.update(template);
    PageRequest<Template> pageRequest = aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build();

    List<Template> templates = templateService.list(
        pageRequest, Collections.singletonList(HARNESS_COMMAND_LIBRARY_GALLERY), GLOBAL_ACCOUNT_ID, true);
    assertThat(templates.get(0).getVersion()).isEqualTo(2L);

    Template template_2 = templateService.get(template.getUuid(), "default");
    assertThat(template_2.getVersion()).isEqualTo(2L);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void nonImportedTemplateShouldNotBeReturnedByListImportedTemplateVersions() {
    templateService.save(getSshCommandTemplate());
    ImportedCommand importedCommand = templateVersionService.listImportedTemplateVersions(
        "COMMANDNAME", "COMMANDSTORENAME", GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID);
    assertThat(importedCommand).isNull();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void shouldListLatestVersionsOfImportedTemplatesOfGivenIds() {
    final String commandName = "COMMAND_NAME";
    final String commandName_1 = "COMMAND_NAME_1";
    final String commandStoreName = "COMMAND_STORE_NAME";

    Template template = saveImportedTemplate(getSshCommandTemplate(), commandName, commandStoreName, "1", false);

    TemplateVersion newVersion = TemplateVersion.builder()
                                     .version(2L)
                                     .importedTemplateVersion("3.1")
                                     .accountId(template.getAccountId())
                                     .changeType(TemplateVersion.ChangeType.IMPORTED.name())
                                     .galleryId(template.getGalleryId())
                                     .templateUuid(template.getUuid())
                                     .templateType(SSH)
                                     .build();
    persistence.save(newVersion);
    saveImportedTemplate(getSshCommandTemplate("test", GLOBAL_APP_ID), commandName_1, commandStoreName, "2.1", false);

    List<ImportedCommand> importedTemplateLatestVersions = templateVersionService.listLatestVersionOfImportedTemplates(
        asList(commandName, commandName_1), commandStoreName, GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID);
    assertThat(importedTemplateLatestVersions.get(0).getHighestVersion()).isEqualTo("3.1");
    assertThat(importedTemplateLatestVersions.get(1).getHighestVersion()).isEqualTo("2.1");
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testCopyOfImportedTemplate() {
    final String commandName = "COMMAND_NAME";
    final String commandStoreName = "COMMAND_STORE_ID";
    final String version = "1.2";

    Template template = saveImportedTemplate(getSshCommandTemplate(), commandName, commandStoreName, version, false);
    CopiedTemplateMetadata copiedTemplateMetadata = CopiedTemplateMetadata.builder()
                                                        .parentTemplateId(template.getUuid())
                                                        .parentTemplateVersion(template.getVersion())
                                                        .parentCommandVersion(version)
                                                        .parentCommandName(commandName)
                                                        .parentCommandStoreName(commandStoreName)
                                                        .build();
    Template copiedTemplate = Template.builder()
                                  .appId(template.getAppId())
                                  .templateMetadata(copiedTemplateMetadata)
                                  .name(commandName)
                                  .accountId(template.getAccountId())
                                  .templateObject(template.getTemplateObject())
                                  .build();
    copiedTemplate = templateService.save(copiedTemplate);

    assertThat(copiedTemplate).isNotNull();
    assertThat(copiedTemplate.getName()).isEqualTo(commandName);
    assertThat(copiedTemplate.getTemplateObject()).isNotNull();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void nonImportedTemplateConnotBeCopied() {
    final String version = "1.2";
    final String copiedTemplateName = "copied";
    final String commandName = "COMMAND_NAME";
    final String commandStoreName = "COMMAND_STORE_NAME";

    Template template = saveTemplate();
    CopiedTemplateMetadata copiedTemplateMetadata = CopiedTemplateMetadata.builder()
                                                        .parentTemplateId(template.getUuid())
                                                        .parentTemplateVersion(1L)
                                                        .parentCommandVersion(version)
                                                        .parentCommandName(commandName)
                                                        .parentCommandStoreName(commandStoreName)
                                                        .build();
    Template copiedTemplate = Template.builder()
                                  .appId(GLOBAL_APP_ID)
                                  .templateMetadata(copiedTemplateMetadata)
                                  .name(copiedTemplateName)
                                  .accountId(GLOBAL_ACCOUNT_ID)
                                  .templateObject(template.getTemplateObject())
                                  .build();

    assertThatThrownBy(() -> templateService.save(copiedTemplate));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void copiedMetadataOfTemplateCannotBeUpdated() {
    final String commandName = "COMMAND_NAME";
    final String commandStoreName = "COMMAND_STORE_NAME";
    final String version = "1.2";
    final String copiedTemplateName = "copied";
    Template template = saveImportedTemplate(getSshCommandTemplate(), commandName, commandStoreName, version, false);
    CopiedTemplateMetadata copiedTemplateMetadata = CopiedTemplateMetadata.builder()
                                                        .parentTemplateId(template.getUuid())
                                                        .parentTemplateVersion(template.getVersion())
                                                        .parentCommandVersion(version)
                                                        .parentCommandName(commandName)
                                                        .parentCommandStoreName(commandStoreName)
                                                        .build();
    Template copiedTemplate = Template.builder()
                                  .appId(template.getAppId())
                                  .templateMetadata(copiedTemplateMetadata)
                                  .name(copiedTemplateName)
                                  .accountId(template.getAccountId())
                                  .templateObject(template.getTemplateObject())
                                  .build();
    copiedTemplate = templateService.save(copiedTemplate);

    CopiedTemplateMetadata newCopiedTemplateMetadata = CopiedTemplateMetadata.builder()
                                                           .parentTemplateId("random")
                                                           .parentTemplateVersion(template.getVersion())
                                                           .parentCommandVersion(version)
                                                           .build();
    copiedTemplate.setTemplateMetadata(newCopiedTemplateMetadata);
    copiedTemplate = templateService.update(copiedTemplate);

    assertThat(copiedTemplate.getTemplateMetadata()).isEqualTo(copiedTemplateMetadata);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void getCopiedTemplate() {
    final String commandName = "COMMAND_NAME";
    final String commandStoreName = "COMMAND_STORE_NAME";
    final String version = "1.2";
    final String copiedTemplateName = "copied";
    Template template = saveImportedTemplate(getSshCommandTemplate(), commandName, commandStoreName, version, false);
    CopiedTemplateMetadata copiedTemplateMetadata = CopiedTemplateMetadata.builder()
                                                        .parentTemplateId(template.getUuid())
                                                        .parentTemplateVersion(template.getVersion())
                                                        .parentCommandVersion(version)
                                                        .parentCommandName(commandName)
                                                        .parentCommandStoreName(commandStoreName)
                                                        .build();
    Template copiedTemplate = Template.builder()
                                  .appId(template.getAppId())
                                  .templateMetadata(copiedTemplateMetadata)
                                  .name(copiedTemplateName)
                                  .accountId(template.getAccountId())
                                  .templateObject(template.getTemplateObject())
                                  .build();
    copiedTemplate = templateService.save(copiedTemplate);

    Template returnedTemplate = templateService.get(copiedTemplate.getUuid());

    assertThat(returnedTemplate).isNotNull();
    assertThat(returnedTemplate.getTemplateMetadata()).isEqualTo(copiedTemplateMetadata);
    assertThat(returnedTemplate.getName()).isEqualTo(copiedTemplateName);
    assertThat(returnedTemplate.getTemplateObject()).isEqualTo(copiedTemplate.getTemplateObject());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void everyCopyOfImportedTemplateShouldHaveUniqueName() {
    final String commandName = "COMMAND_NAME";
    final String commandStoreName = "COMMAND_STORE_NAME";
    final String version = "1.2";
    final String copiedTemplateName = "copied";
    Template template = saveImportedTemplate(getSshCommandTemplate(), commandName, commandStoreName, version, false);
    CopiedTemplateMetadata copiedTemplateMetadata = CopiedTemplateMetadata.builder()
                                                        .parentTemplateId(template.getUuid())
                                                        .parentTemplateVersion(template.getVersion())
                                                        .parentCommandVersion(version)
                                                        .parentCommandName(commandName)
                                                        .parentCommandStoreName(commandStoreName)
                                                        .build();
    Template copiedTemplate = Template.builder()
                                  .appId(template.getAppId())
                                  .templateMetadata(copiedTemplateMetadata)
                                  .name(copiedTemplateName)
                                  .accountId(template.getAccountId())
                                  .templateObject(template.getTemplateObject())
                                  .build();
    copiedTemplate = templateService.save(copiedTemplate);
    Template copiedTemplate_1 = Template.builder()
                                    .appId(template.getAppId())
                                    .templateMetadata(copiedTemplateMetadata)
                                    .name(copiedTemplateName)
                                    .accountId(template.getAccountId())
                                    .templateObject(template.getTemplateObject())
                                    .build();
    templateService.save(copiedTemplate_1);

    assertThat(templateService.get(copiedTemplate_1.getUuid()).equals(copiedTemplateName + "_" + 1));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testList() {
    final String commandName = "COMMAND_NAME";
    final String commandStoreName = "COMMAND_STORE_NAME";
    Template importedTemplate =
        saveImportedTemplate(getSshCommandTemplate(), commandName, commandStoreName, "1", false);
    Template accountTemplate = saveTemplate();

    // Case 0: empty input
    PageRequest<Template> pageRequest_0 = aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build();
    List<Template> templates_0 = templateService.list(pageRequest_0, EMPTY_LIST, GLOBAL_ACCOUNT_ID, false);

    assertThat(templates_0).isNotEmpty();
    assertThat(templates_0.stream()
                   .map(Template::getUuid)
                   .filter(uuid -> uuid.equals(importedTemplate.getUuid()))
                   .findFirst()
                   .get())
        .isNotNull();
    assertThat(templates_0.stream()
                   .map(Template::getUuid)
                   .filter(uuid -> uuid.equals(accountTemplate.getUuid()))
                   .findFirst()
                   .get())
        .isNotNull();

    // Case 1: null as input
    PageRequest<Template> pageRequest_1 = aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build();
    List<Template> templates_1 = templateService.list(pageRequest_1, null, GLOBAL_ACCOUNT_ID, false);

    assertThat(templates_1).isNotEmpty();
    assertThat(templates_1.stream()
                   .map(Template::getUuid)
                   .filter(uuid -> uuid.equals(importedTemplate.getUuid()))
                   .findFirst()
                   .get())
        .isNotNull();
    assertThat(templates_1.stream()
                   .map(Template::getUuid)
                   .filter(uuid -> uuid.equals(accountTemplate.getUuid()))
                   .findFirst()
                   .get())
        .isNotNull();

    // Case 2: Single filter as input.
    PageRequest<Template> pageRequest_2 = aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build();
    List<Template> templates_2 = templateService.list(pageRequest_2,
        Collections.singletonList(templateGalleryService.getAccountGalleryKey().name()), GLOBAL_ACCOUNT_ID, false);

    assertThat(templates_2).isNotEmpty();
    assertThat(templates_2.stream()
                   .map(Template::getUuid)
                   .filter(uuid -> uuid.equals(accountTemplate.getUuid()))
                   .findFirst()
                   .get())
        .isNotNull();

    // Case 3: Multiple filter as input.
    PageRequest<Template> pageRequest_3 = aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build();
    List<Template> templates_3 = templateService.list(pageRequest_3,
        Arrays.asList(GalleryKey.HARNESS_COMMAND_LIBRARY_GALLERY.name(), GalleryKey.ACCOUNT_TEMPLATE_GALLERY.name()),
        GLOBAL_ACCOUNT_ID, false);

    assertThat(templates_3).isNotEmpty();
    assertThat(templates_3.stream()
                   .map(Template::getUuid)
                   .filter(uuid -> uuid.equals(importedTemplate.getUuid()))
                   .findFirst()
                   .get())
        .isNotNull();
    assertThat(templates_3.stream()
                   .map(Template::getUuid)
                   .filter(uuid -> uuid.equals(accountTemplate.getUuid()))
                   .findFirst()
                   .get())
        .isNotNull();

    // Case 4: inexistent galleryKey
    PageRequest<Template> pageRequest_4 = aPageRequest().addFilter("appId", EQ, GLOBAL_APP_ID).build();
    assertThatThrownBy(() -> templateService.list(pageRequest_4, Arrays.asList("random"), GLOBAL_ACCOUNT_ID, false));
  }

  private Template saveImportedTemplate(
      Template template, String commandName, String commandStoreName, String version, boolean isUpdate) {
    HarnessImportedTemplateDetails harnessImportedTemplateDetails = HarnessImportedTemplateDetails.builder()
                                                                        .commandName(commandName)
                                                                        .commandStoreName(commandStoreName)
                                                                        .commandVersion(version)
                                                                        .build();
    template.setImportedTemplateDetails(harnessImportedTemplateDetails);
    template.setGalleryId(
        templateGalleryHelper
            .getGalleryByGalleryKey(GalleryKey.HARNESS_COMMAND_LIBRARY_GALLERY.name(), GLOBAL_ACCOUNT_ID)
            .getUuid());
    if (isUpdate) {
      template = templateService.updateReferenceTemplate(template);
    } else {
      template = templateService.saveReferenceTemplate(template);
      ImportedTemplate importedTemplate = ImportedTemplate.builder()
                                              .accountId(GLOBAL_ACCOUNT_ID)
                                              .appId(GLOBAL_APP_ID)
                                              .commandName(commandName)
                                              .commandStoreName(commandStoreName)
                                              .templateId(template.getUuid())
                                              .build();
      persistence.save(importedTemplate);
    }

    return templateService.get(template.getUuid());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetYamlOfTemplate() {
    Template template = templateService.save(getSshCommandTemplate());
    assertThat(templateService.getYamlOfTemplate(template.getUuid(), template.getVersion())).isNotNull();
    assertThatThrownBy(() -> templateService.getYamlOfTemplate(template.getUuid(), template.getVersion() + 1));
  }
}
