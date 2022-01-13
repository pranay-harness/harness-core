/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.seedata;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.POWER_SHELL_COMMANDS;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_APP_INSTALL_PATH;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_WEBSITE_INSTALL_PATH;

import static java.util.Arrays.asList;

import io.harness.exception.WingsException;
import io.harness.migrations.SeedDataMigration;

import software.wings.beans.template.TemplateType;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IISInstallCommandMigration implements SeedDataMigration {
  private static final String INSTALL_IIS_APPLICATION_TEMPLATE_NAME = "Install IIS Application";
  private static final String INSTALL_IIS_WEBSITE_TEMPLATE_NAME = "Install IIS Website";
  @Inject private TemplateService templateService;
  @Inject private TemplateGalleryService templateGalleryService;

  @Override
  public void migrate() {
    log.info("Migrating Install Command for IIS");
    loadNewIISTemplatesToAccounts();
  }

  public void loadNewIISTemplatesToAccounts() {
    try {
      templateService.loadDefaultTemplates(
          asList(POWER_SHELL_IIS_WEBSITE_INSTALL_PATH, POWER_SHELL_IIS_APP_INSTALL_PATH), GLOBAL_ACCOUNT_ID,
          HARNESS_GALLERY);
    } catch (WingsException e) {
      log.info("Default Template already exists in global gallery", e);
    }

    templateGalleryService.copyHarnessTemplateFromGalleryToAccounts(POWER_SHELL_COMMANDS, TemplateType.SSH,
        INSTALL_IIS_APPLICATION_TEMPLATE_NAME, POWER_SHELL_IIS_APP_INSTALL_PATH);
    templateGalleryService.copyHarnessTemplateFromGalleryToAccounts(POWER_SHELL_COMMANDS, TemplateType.SSH,
        INSTALL_IIS_WEBSITE_TEMPLATE_NAME, POWER_SHELL_IIS_WEBSITE_INSTALL_PATH);
  }
}
