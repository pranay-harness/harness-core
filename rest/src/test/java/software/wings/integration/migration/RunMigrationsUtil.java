package software.wings.integration.migration;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
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

  @Test
  public void runMigrations() {
    migrationService.runMigrations();
  }
}
