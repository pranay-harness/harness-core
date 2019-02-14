package io.harness.generator;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.generator.TemplateFolderGenerator.TemplateFolders.TEMPLATE_FOLDER;
import static io.harness.generator.TemplateGalleryGenerator.TemplateGalleries.HARNESS_GALLERY;
import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.VariableType.TEXT;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.harness.delegate.beans.ScriptType;
import software.wings.beans.Account;
import software.wings.beans.template.Template;
import software.wings.beans.template.Template.TemplateBuilder;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateService;

import java.util.Arrays;

@Singleton
public class TemplateGenerator {
  @Inject AccountGenerator accountGenerator;
  @Inject TemplateGalleryGenerator templateGalleryGenerator;
  @Inject TemplateFolderGenerator templateFolderGenerator;
  @Inject WingsPersistence wingsPersistence;
  @Inject TemplateService templateService;

  public enum Templates { SHELL_SCRIPT }

  public Template ensurePredefined(Randomizer.Seed seed, OwnerManager.Owners owners, Templates predefined) {
    switch (predefined) {
      case SHELL_SCRIPT:
        return ensureShellScriptTemplate(seed, owners);
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
    TemplateFolder parentFolder = templateFolderGenerator.ensurePredefined(seed, owners, TEMPLATE_FOLDER);
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
            .variables(Arrays.asList(
                aVariable().withType(TEXT).withName("var1").withMandatory(true).withValue("Hello World").build()))
            .build());
  }

  public Template ensureTemplate(Randomizer.Seed seed, OwnerManager.Owners owners, Template template) {
    EnhancedRandom random = Randomizer.instance(seed);
    TemplateGallery templateGallery = templateGalleryGenerator.ensurePredefined(seed, owners, HARNESS_GALLERY);
    TemplateFolder parentFolder = templateFolderGenerator.ensurePredefined(seed, owners, TEMPLATE_FOLDER);
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
      Account account = owners.obtainAccount(() -> accountGenerator.randomAccount());
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
      builder.variables(Arrays.asList(aVariable().withType(TEXT).withName("var1").withMandatory(true).build()));
    }
    if (template != null && template.getGalleryId() != null) {
      builder.gallery(template.getGallery());
    } else {
      builder.galleryId(templateGallery.getUuid());
    }

    Template existingTemplate = exists(builder.build());
    if (existingTemplate != null) {
      Template existing = templateService.get(existingTemplate.getUuid());
      if (existing != null) {
        return existing;
      }
    }

    return templateService.save(builder.build());
  }

  public Template exists(Template template) {
    //    return templateService.fetchTemplateIdByNameAndFolderId(
    //        template.getAccountId(), template.getName(), template.getFolderId());
    return wingsPersistence.createQuery(Template.class)
        .project(Template.NAME_KEY, true)
        .project(Template.ACCOUNT_ID_KEY, true)
        .filter(Template.ACCOUNT_ID_KEY, template.getAccountId())
        .filter(Template.NAME_KEY, template.getName())
        .filter(Template.FOLDER_ID_KEY, template.getFolderId())
        .get();
  }
}
