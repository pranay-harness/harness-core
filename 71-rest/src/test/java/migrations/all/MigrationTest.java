package migrations.all;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.rules.Integration;

@Integration
@Ignore
public class MigrationTest extends WingsBaseTest {
  @Inject private RemoveServiceVariablesFromActivity migration;

  @Test
  @Category(UnitTests.class)
  public void shouldMigrate() {
    migration.migrate();
  }
}
