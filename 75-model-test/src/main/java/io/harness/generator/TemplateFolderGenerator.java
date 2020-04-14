package io.harness.generator;

import static io.harness.generator.TemplateGalleryGenerator.TemplateGalleries.HARNESS_GALLERY;
import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Account;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateFolder.TemplateFolderBuilder;
import software.wings.beans.template.TemplateGallery;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateFolderService;

@Singleton
public class TemplateFolderGenerator {
  @Inject AccountGenerator accountGenerator;
  @Inject TemplateGalleryGenerator templateGalleryGenerator;
  @Inject TemplateFolderService templateFolderService;
  @Inject WingsPersistence wingsPersistence;

  public enum TemplateFolders {
    TEMPLATE_FOLDER_SHELL_SCRIPTS,
    TEMPLATE_FOLDER_SERVICE_COMMANDS,
    APP_FOLDER_SHELL_SCRIPTS,
    TEMPLATE_FOLDER_PCF_COMMANDS
  }

  public TemplateFolder ensurePredefined(
      Randomizer.Seed seed, OwnerManager.Owners owners, TemplateFolders predefined, String appId) {
    switch (predefined) {
      case TEMPLATE_FOLDER_SHELL_SCRIPTS:
        return ensureTemplateFolder(
            seed, owners, TemplateFolder.builder().name("Functional Test - Shell Scripts").appId(appId).build());
      case TEMPLATE_FOLDER_SERVICE_COMMANDS:
        return ensureTemplateFolder(
            seed, owners, TemplateFolder.builder().name("Functional Test - Service Commands").appId(appId).build());
      case APP_FOLDER_SHELL_SCRIPTS:
        return ensureTemplateFolder(seed, owners,
            TemplateFolder.builder()
                .name("App-Functional Test Folder - " + System.currentTimeMillis())
                .appId(appId)
                .build());
      case TEMPLATE_FOLDER_PCF_COMMANDS:
        return ensureTemplateFolder(
            seed, owners, TemplateFolder.builder().name("Functional Test - PCF Commands").appId(appId).build());
      default:
        unhandled(predefined);
    }
    return null;
  }

  private TemplateFolder ensureTemplateFolder(
      Randomizer.Seed seed, OwnerManager.Owners owners, TemplateFolder templateFolder) {
    Account account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }

    EnhancedRandom random = Randomizer.instance(seed);
    TemplateFolderBuilder builder = TemplateFolder.builder();
    if (templateFolder != null && templateFolder.getAccountId() != null) {
      builder.accountId(templateFolder.getAccountId());
    } else {
      builder.accountId(account.getUuid());
    }

    if (templateFolder != null && templateFolder.getName() != null) {
      builder.name(templateFolder.getName());
    } else {
      builder.name(random.nextObject(String.class));
    }

    if (templateFolder != null && templateFolder.getGalleryId() != null) {
      builder.galleryId(templateFolder.getGalleryId());
    } else {
      TemplateGallery templateGallery = templateGalleryGenerator.ensurePredefined(seed, owners, HARNESS_GALLERY);
      if (templateGallery != null) {
        builder.galleryId(templateGallery.getUuid());
      }
    }

    if (templateFolder != null && templateFolder.getParentId() != null) {
      builder.parentId(templateFolder.getParentId());
    } else {
      String accId = (templateFolder != null && templateFolder.getAccountId() != null) ? templateFolder.getAccountId()
                                                                                       : account.getUuid();
      TemplateFolder harnessFolder = exists(TemplateFolder.builder().name("Harness").accountId(accId).build());
      if (harnessFolder != null) {
        builder.parentId(harnessFolder.getUuid());
      }
    }

    if (templateFolder != null && templateFolder.getPathId() != null) {
      builder.pathId(templateFolder.getPathId());
    } else {
      String accId = (templateFolder != null && templateFolder.getAccountId() != null) ? templateFolder.getAccountId()
                                                                                       : account.getUuid();
      TemplateFolder harnessFolder = exists(TemplateFolder.builder().name("Harness").accountId(accId).build());
      if (harnessFolder != null) {
        builder.pathId(harnessFolder.getUuid());
      }
    }

    if (templateFolder != null && templateFolder.getAppId() != null) {
      builder.appId(templateFolder.getAppId());
    } else {
      builder.appId(GLOBAL_APP_ID);
    }

    TemplateFolder existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    final TemplateFolder finalTemplateFolder = builder.build();

    return GeneratorUtils.suppressDuplicateException(
        ()
            -> templateFolderService.save(finalTemplateFolder, templateFolder.getGalleryId()),
        () -> exists(finalTemplateFolder));
  }

  public TemplateFolder exists(TemplateFolder template) {
    return wingsPersistence.createQuery(TemplateFolder.class)
        .filter(TemplateFolder.ACCOUNT_ID_KEY, template.getAccountId())
        .filter(TemplateFolder.NAME_KEY, template.getName())
        .get();
  }
}
