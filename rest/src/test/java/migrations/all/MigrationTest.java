package migrations.all;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.rules.Integration;

@Integration
@Ignore
public class MigrationTest extends WingsBaseTest {
  @Inject private AddValidUntilToDelegateTask migration;

  @Test
  public void shouldMigrate() {
    migration.migrate();
  }
}
