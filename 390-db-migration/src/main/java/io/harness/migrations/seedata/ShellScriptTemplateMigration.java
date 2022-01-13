/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.seedata;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.PATH_DELIMITER;
import static software.wings.common.TemplateConstants.SHELL_SCRIPTS;
import static software.wings.common.TemplateConstants.SHELL_SCRIPT_EXAMPLE;

import static java.util.Arrays.asList;

import io.harness.migrations.SeedDataMigration;

import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShellScriptTemplateMigration implements SeedDataMigration {
  @Inject private TemplateGalleryService templateGalleryService;
  private TemplateService templateService;
  private TemplateFolderService templateFolderService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Migration: Loading new shell script templates..");
    // 1. Create Shell Scripts folder in Global account
    if (addShellScriptFolderInGlobalAccount()) {
      // 2. Add the new shell script templates to Global account
      loadNewShellScriptTemplatesToAccount();
      // 3. Create Shell Script folder in each account and copy templates
      templateGalleryService.copyNewFolderAndTemplatesFromGlobalToAccounts(
          SHELL_SCRIPTS, TemplateType.SHELL_SCRIPT, asList(SHELL_SCRIPT_EXAMPLE));
    }
  }

  private boolean addShellScriptFolderInGlobalAccount() {
    boolean success = false;
    // Check if harness template gallery exists
    TemplateGallery templateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    if (templateGallery != null) {
      // check if template folder exists
      TemplateFolder templateFolder =
          templateFolderService.getRootLevelFolder(GLOBAL_ACCOUNT_ID, templateGallery.getUuid());
      if (templateFolder != null) {
        TemplateFolder shellScriptsTemplateFolder = constructTemplateBuilder(templateFolder, SHELL_SCRIPTS);
        templateFolderService.save(shellScriptsTemplateFolder, templateGallery.getUuid());
        success = true;
      } else {
        log.error("Folder [{}] does not exist for account [{}]", HARNESS_GALLERY, GLOBAL_ACCOUNT_ID);
      }
    } else {
      log.error("Harness gallery does not exist for account [{}].", GLOBAL_ACCOUNT_ID);
    }
    return success;
  }

  public void loadNewShellScriptTemplatesToAccount() {
    templateService.loadDefaultTemplates(asList(SHELL_SCRIPT_EXAMPLE), GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
  }

  private TemplateFolder constructTemplateBuilder(TemplateFolder parentFolder, String folderName) {
    String pathId = parentFolder.getPathId() == null
        ? parentFolder.getUuid()
        : parentFolder.getPathId() + PATH_DELIMITER + parentFolder.getUuid();
    return TemplateFolder.builder()
        .name(folderName)
        .parentId(parentFolder.getUuid())
        .galleryId(parentFolder.getGalleryId())
        .pathId(pathId)
        .appId(GLOBAL_APP_ID)
        .accountId(GLOBAL_ACCOUNT_ID)
        .build();
  }
}
