package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;

import software.wings.service.intfc.template.TemplateGalleryService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class SystemTemplateGalleryMigration implements Migration {
  @Inject private TemplateGalleryService templateGalleryService;

  @Override
  public void migrate() {
    log.info("Migrating Harness Inc Gallery");
    templateGalleryService.loadHarnessGallery();
    templateGalleryService.copyHarnessTemplates();
  }
}
