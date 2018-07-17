package migrations.all;

import static org.slf4j.LoggerFactory.getLogger;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import software.wings.service.intfc.template.TemplateGalleryService;

public class SystemTemplateGalleryMigration implements Migration {
  private static final Logger logger = getLogger(SystemTemplateGalleryMigration.class);
  @Inject private TemplateGalleryService templateGalleryService;

  @Override
  public void migrate() {
    logger.info("Migrating Harness Inc Gallery");
    templateGalleryService.loadHarnessGallery();
  }
}
