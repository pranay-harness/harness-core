package io.harness.migrations.all;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import io.harness.migrations.Migration;

import software.wings.service.intfc.template.TemplateGalleryService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DuplicateGlobalAccountMigration implements Migration {
  @Inject private TemplateGalleryService templateGalleryService;
  @Override
  public void migrate() {
    log.info("Deleting template gallery for Account Name: Global");
    templateGalleryService.deleteAccountGalleryByName(GLOBAL_ACCOUNT_ID, "Global");
    log.info("Finished deleting template gallery for Account Name: Global");
  }
}
