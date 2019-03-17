package software.wings.integration.migration;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.category.element.IntegrationTests;
import migrations.BaseMigration;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.rules.Integration;
import software.wings.service.intfc.MigrationService;

/**
 * @author brett on 1/24/18
 */
@Integration
@Ignore
public class RunMigrationsUtil extends WingsBaseTest {
  @Inject private MigrationService migrationService;
  @Inject private Injector injector;

  @Test
  @Category(IntegrationTests.class)
  public void runMigrations() {
    migrationService.runMigrations();
  }

  @Test
  @Category(IntegrationTests.class)
  public void runSpecificMigration() {
    // Temporarily change this to any Migration class to execute it directly
    injector.getInstance(BaseMigration.class).migrate();
  }
}
