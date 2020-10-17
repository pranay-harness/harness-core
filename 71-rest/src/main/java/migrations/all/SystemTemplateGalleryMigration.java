package migrations.all;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.service.intfc.template.TemplateGalleryService;

@Slf4j
public class SystemTemplateGalleryMigration implements Migration {
  @Inject private TemplateGalleryService templateGalleryService;

  @Override
  public void migrate() {
    logger.info("Migrating Harness Inc Gallery");
    templateGalleryService.loadHarnessGallery();
    templateGalleryService.copyHarnessTemplates();
  }
}
