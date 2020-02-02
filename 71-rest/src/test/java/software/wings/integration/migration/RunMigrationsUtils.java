package software.wings.integration.migration;

import static io.harness.rule.OwnerRule.BRETT;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;
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
public class RunMigrationsUtils extends WingsBaseTest {
  @Inject private MigrationService migrationService;
  @Inject private Injector injector;

  @Test
  @Owner(developers = BRETT)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void runMigrations() {
    migrationService.runMigrations();
  }

  @Test
  @Owner(developers = BRETT)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void runSpecificMigration() {
    // Temporarily change this to any Migration class to execute it directly
    injector.getInstance(BaseMigration.class).migrate();
  }
}
