/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.generator.TemplateFolderGenerator.TemplateFolders.HARNESS_COMMAND_LIBRARY;
import static io.harness.generator.TemplateFolderGenerator.TemplateFolders.TEMPLATE_FOLDER_PCF_COMMANDS;
import static io.harness.generator.TemplateFolderGenerator.TemplateFolders.TEMPLATE_FOLDER_SERVICE_COMMANDS;
import static io.harness.generator.TemplateFolderGenerator.TemplateFolders.TEMPLATE_FOLDER_SHELL_SCRIPTS;
import static io.harness.generator.TemplateGalleryGenerator.TemplateGalleries.HARNESS_GALLERY;
import static io.harness.govern.Switch.unhandled;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.ARTIFACT;
import static software.wings.beans.VariableType.TEXT;
import static software.wings.beans.command.CommandType.OTHER;
import static software.wings.beans.command.CommandUnitType.SCP;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ScpCommandUnit.Builder.aScpCommandUnit;
import static software.wings.beans.command.ScpCommandUnit.ScpFileCategory.ARTIFACTS;

import static java.util.Arrays.asList;

import io.harness.shell.ScriptType;

import software.wings.beans.Account;
import software.wings.beans.Variable;
import software.wings.beans.command.Command;
import software.wings.beans.template.Template;
import software.wings.beans.template.Template.TemplateBuilder;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateReference;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.PcfCommandTemplate;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.benas.randombeans.api.EnhancedRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class TemplateGenerator {
  @Inject AccountGenerator accountGenerator;
  @Inject TemplateGalleryGenerator templateGalleryGenerator;
  @Inject TemplateFolderGenerator templateFolderGenerator;
  @Inject WingsPersistence wingsPersistence;
  @Inject TemplateService templateService;

  public enum Templates {
    SHELL_SCRIPT,
    SERVICE_COMMAND_1,
    SERVICE_COMMAND_2,
    MULTI_ARTIFACT_COMMAND_TEMPLATE,
    PCF_COMMAND_TEMPLATE_1
  }

  public Template ensurePredefined(Randomizer.Seed seed, OwnerManager.Owners owners, Templates predefined) {
    switch (predefined) {
      case SHELL_SCRIPT:
        return ensureShellScriptTemplate(seed, owners);
      case SERVICE_COMMAND_1:
      case SERVICE_COMMAND_2:
        return ensureServiceCommandTemplate(seed, owners, predefined);
      case MULTI_ARTIFACT_COMMAND_TEMPLATE:
        return ensureMultiArtifactCommandTemplate(seed, owners);
      case PCF_COMMAND_TEMPLATE_1:
        return ensurePcfCommandTemplate(seed, owners);
      default:
        unhandled(predefined);
    }
    return null;
  }

  private Template ensureShellScriptTemplate(Randomizer.Seed seed, OwnerManager.Owners owners) {
    Account account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }
    TemplateFolder parentFolder =
        templateFolderGenerator.ensurePredefined(seed, owners, TEMPLATE_FOLDER_SHELL_SCRIPTS, GLOBAL_APP_ID);
    ShellScriptTemplate shellScriptTemplate = ShellScriptTemplate.builder()
                                                  .scriptType(ScriptType.BASH.name())
                                                  .scriptString("echo ${var1}\n"
                                                      + "export A=\"aaa\"\n"
                                                      + "export B=\"bbb\"")
                                                  .outputVars("A,B")
                                                  .build();

    return ensureTemplate(seed, owners,
        Template.builder()
            .type(TemplateType.SHELL_SCRIPT.name())
            .accountId(account.getUuid())
            .name("Sample Shell Script")
            .templateObject(shellScriptTemplate)
            .folderId(parentFolder.getUuid())
            .appId(GLOBAL_APP_ID)
            .variables(Arrays.asList(aVariable().type(TEXT).name("var1").mandatory(true).value("Hello World").build()))
            .build());
  }

  Template ensureServiceCommandTemplate(Randomizer.Seed seed, OwnerManager.Owners owners, Templates templates) {
    Account account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }
    TemplateFolder parentFolder =
        templateFolderGenerator.ensurePredefined(seed, owners, TEMPLATE_FOLDER_SERVICE_COMMANDS, GLOBAL_APP_ID);
    if (templates == Templates.SERVICE_COMMAND_1) {
      SshCommandTemplate command1 =
          SshCommandTemplate.builder()
              .commandType(OTHER)
              .commandUnits(asList(anExecCommandUnit()
                                       .withName("MyExec-1")
                                       .withCommandPath("/tmp")
                                       //                              .withCommandUnitType(EXEC)
                                       .withCommandString("mkdir -p \"$WINGS_RUNTIME_PATH\"\n"
                                           + "echo ${var1}\n"
                                           + "echo ${t_artifact1.buildNo}")
                                       .build(),
                  aScpCommandUnit()
                      .withName("MyCopy-1")
                      .withFileCategory(ARTIFACTS)
                      .withCommandUnitType(SCP)
                      .withArtifactVariableName("t_artifact1")
                      .withDestinationDirectoryPath("$WINGS_RUNTIME_PATH")
                      .build()))
              .build();

      return ensureTemplate(seed, owners,
          Template.builder()
              .type(TemplateType.SSH.name())
              .accountId(account.getUuid())
              .name("MyCommand-1")
              .templateObject(command1)
              .folderId(parentFolder.getUuid())
              .appId(GLOBAL_APP_ID)
              .variables(Arrays.asList(aVariable().type(TEXT).name("var1").mandatory(true).build(),
                  aVariable().type(ARTIFACT).name("t_artifact1").mandatory(true).build()))
              .build());
    } else if (templates == Templates.SERVICE_COMMAND_2) {
      SshCommandTemplate command2 =
          SshCommandTemplate.builder()
              .commandType(OTHER)
              .commandUnits(asList(anExecCommandUnit()
                                       .withName("MyExec-2")
                                       //                              .withCommandUnitType(EXEC)
                                       .withCommandPath("/tmp")
                                       .withCommandString("mkdir -p \"$WINGS_RUNTIME_PATH\"\n"
                                           + "echo ${var2}\n"
                                           + "echo ${t_artifact2.buildNo}")
                                       .build(),
                  aScpCommandUnit()
                      .withName("MyCopy-2")
                      .withFileCategory(ARTIFACTS)
                      .withCommandUnitType(SCP)
                      .withArtifactVariableName("t_artifact2")
                      .withDestinationDirectoryPath("$WINGS_RUNTIME_PATH")
                      .build()))
              .build();

      return ensureTemplate(seed, owners,
          Template.builder()
              .type(TemplateType.SSH.name())
              .accountId(account.getUuid())
              .name("MyCommand-2")
              .templateObject(command2)
              .folderId(parentFolder.getUuid())
              .appId(GLOBAL_APP_ID)
              .variables(Arrays.asList(aVariable().type(TEXT).name("var2").mandatory(true).build(),
                  aVariable().type(ARTIFACT).name("t_artifact2").mandatory(true).build()))
              .build());
    }
    return null;
  }

  private Template ensureMultiArtifactCommandTemplate(Randomizer.Seed seed, OwnerManager.Owners owners) {
    Account account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }
    TemplateFolder parentFolder =
        templateFolderGenerator.ensurePredefined(seed, owners, TEMPLATE_FOLDER_SERVICE_COMMANDS, GLOBAL_APP_ID);

    Template commandTemplate1 = ensureServiceCommandTemplate(seed, owners, Templates.SERVICE_COMMAND_1);
    Template commandTemplate2 = ensureServiceCommandTemplate(seed, owners, Templates.SERVICE_COMMAND_2);

    SshCommandTemplate command =
        SshCommandTemplate.builder()
            .commandType(OTHER)
            .commandUnits(asList(
                Command.Builder.aCommand()
                    .withName("MyCommand-1")
                    .withTemplateReference(TemplateReference.builder()
                                               .templateUuid(commandTemplate1.getUuid())
                                               .templateVersion(commandTemplate1.getVersion())
                                               .build())
                    .withTemplateVariables(asList(aVariable().type(TEXT).name("var1").value("Hello Jane Doe").build(),
                        aVariable().type(ARTIFACT).name("t_artifact1").value("${m_artifact1}").build()))
                    .build(),
                Command.Builder.aCommand()
                    .withName("MyCommand-2")
                    .withTemplateReference(TemplateReference.builder()
                                               .templateUuid(commandTemplate2.getUuid())
                                               .templateVersion(commandTemplate2.getVersion())
                                               .build())
                    .withTemplateVariables(asList(aVariable().type(TEXT).name("var2").value("Hello John Doe").build(),
                        aVariable().type(ARTIFACT).name("t_artifact2").value("${m_artifact2}").build()))
                    .build()))
            .build();

    return ensureTemplate(seed, owners,
        Template.builder()
            .type(TemplateType.SSH.name())
            .accountId(account.getUuid())
            .name("MyCommand")
            .templateObject(command)
            .folderId(parentFolder.getUuid())
            .appId(GLOBAL_APP_ID)
            .variables(Arrays.asList(aVariable().type(TEXT).name("var1").value("Hello Jane Doe").build(),
                aVariable().type(ARTIFACT).name("m_artifact1").build(),
                aVariable().type(TEXT).name("var2").value("Hello John Doe").build(),
                aVariable().type(ARTIFACT).name("m_artifact2").build()))
            .build());
  }

  private Template ensurePcfCommandTemplate(Randomizer.Seed seed, OwnerManager.Owners owners) {
    Account account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }
    TemplateFolder parentFolder =
        templateFolderGenerator.ensurePredefined(seed, owners, TEMPLATE_FOLDER_PCF_COMMANDS, GLOBAL_APP_ID);
    PcfCommandTemplate pcfCommandTemplate =
        PcfCommandTemplate.builder()
            .scriptString(
                "cf ${commandName} \n cat ${service.manifest}/manifest.yml \n cat ${service.manifest.repoRoot}/pcf-app1/vars.yml")
            .timeoutIntervalInMinutes(4)
            .build();

    return ensureTemplate(seed, owners,
        Template.builder()
            .type(TemplateType.PCF_PLUGIN.name())
            .accountId(account.getUuid())
            .name("Pcf Command Test - " + System.currentTimeMillis())
            .templateObject(pcfCommandTemplate)
            .folderId(parentFolder.getUuid())
            .appId(GLOBAL_APP_ID)
            .variables(Arrays.asList(aVariable().type(TEXT).name("commandName").mandatory(true).value("apps").build()))
            .build());
  }

  public Template ensureCustomDeploymentTemplate(Randomizer.Seed seed, OwnerManager.Owners owners,
      CustomDeploymentTypeTemplate customDeploymentTypeTemplate, String templateName,
      List<Variable> secretInfraVariables) {
    Account account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }
    TemplateFolder parentFolder =
        templateFolderGenerator.ensurePredefined(seed, owners, HARNESS_COMMAND_LIBRARY, GLOBAL_APP_ID);

    List<Variable> infraVariables = new ArrayList<>();
    infraVariables.add(aVariable()
                           .type(TEXT)
                           .name("url")
                           .mandatory(true)
                           .value("app.harness.io")
                           .description("harness app url")
                           .build());
    infraVariables.add(aVariable().type(TEXT).name("username").mandatory(true).value("").build());
    infraVariables.add(aVariable()
                           .type(TEXT)
                           .name("deploymentName")
                           .mandatory(true)
                           .value("nginx")
                           .description("name of the deployment")
                           .build());
    infraVariables.add(aVariable()
                           .type(TEXT)
                           .name("namespace")
                           .mandatory(true)
                           .value("default")
                           .description("namespace to deploy pods")
                           .build());
    infraVariables.add(aVariable()
                           .type(TEXT)
                           .name("cluster")
                           .mandatory(true)
                           .value(null)
                           .description("cluster used for deployment")
                           .build());
    infraVariables.add(aVariable().type(TEXT).name("repoName").mandatory(true).value("").description("").build());
    if (!secretInfraVariables.isEmpty()) {
      infraVariables.addAll(secretInfraVariables);
    }

    return ensureTemplate(seed, owners,
        Template.builder()
            .type(TemplateType.CUSTOM_DEPLOYMENT_TYPE.name())
            .accountId(account.getUuid())
            .name(templateName)
            .templateObject(customDeploymentTypeTemplate)
            .folderId(parentFolder.getUuid())
            .appId(GLOBAL_APP_ID)
            .variables(infraVariables)
            .build());
  }

  public Template ensureTemplate(Randomizer.Seed seed, OwnerManager.Owners owners, Template template) {
    EnhancedRandom random = Randomizer.instance(seed);
    TemplateGallery templateGallery = templateGalleryGenerator.ensurePredefined(seed, owners, HARNESS_GALLERY);
    TemplateFolder parentFolder =
        templateFolderGenerator.ensurePredefined(seed, owners, TEMPLATE_FOLDER_SHELL_SCRIPTS, GLOBAL_APP_ID);
    ShellScriptTemplate shellScriptTemplate = ShellScriptTemplate.builder()
                                                  .scriptType(ScriptType.BASH.name())
                                                  .scriptString("echo ${var1}\n"
                                                      + "export A=\"aaa\"\n"
                                                      + "export B=\"bbb\"")
                                                  .outputVars("A,B")
                                                  .build();

    TemplateBuilder builder = Template.builder();

    if (template != null && template.getAccountId() != null) {
      builder.accountId(template.getAccountId());
    } else {
      Account account = owners.obtainAccount(() -> accountGenerator.ensureRandom(seed, owners));
      builder.accountId(account.getUuid());
    }

    if (template != null && template.getName() != null) {
      builder.name(template.getName());
    } else {
      builder.name(random.nextObject(String.class));
    }

    builder.appId(GLOBAL_APP_ID);

    if (template != null && template.getTemplateObject() != null) {
      builder.templateObject(template.getTemplateObject());
    } else {
      builder.templateObject(shellScriptTemplate);
    }

    if (template != null && template.getFolderId() != null) {
      builder.folderId(template.getFolderId());
    } else {
      builder.folderId(parentFolder.getUuid());
    }
    if (template != null && isNotEmpty(template.getVariables())) {
      builder.variables(template.getVariables());
    } else {
      builder.variables(Arrays.asList(aVariable().type(TEXT).name("var1").mandatory(true).build()));
    }
    if (template != null && template.getGalleryId() != null) {
      builder.gallery(template.getGallery());
    } else {
      builder.galleryId(templateGallery.getUuid());
    }
    if (template != null && template.getType() != null) {
      builder.type(template.getType());
    }

    Template existingTemplate = exists(builder.build());
    if (existingTemplate != null) {
      Template existing = templateService.get(existingTemplate.getUuid());
      if (existing != null) {
        return existing;
      }
    }

    final Template finalTemplate = builder.build();

    return GeneratorUtils.suppressDuplicateException(
        () -> templateService.save(finalTemplate), () -> exists(finalTemplate));
  }

  public Template exists(Template template) {
    //    return templateService.fetchTemplateIdByNameAndFolderId(
    //        template.getAccountId(), template.getName(), template.getFolderId());
    return wingsPersistence.createQuery(Template.class)
        .project(Template.NAME_KEY, true)
        .project(Template.ACCOUNT_ID_KEY2, true)
        .filter(Template.ACCOUNT_ID_KEY2, template.getAccountId())
        .filter(Template.NAME_KEY, template.getName())
        .filter(Template.FOLDER_ID_KEY, template.getFolderId())
        .get();
  }
}
