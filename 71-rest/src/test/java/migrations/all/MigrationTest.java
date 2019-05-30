package migrations.all;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.BypassRuleMixin.Bypass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.rules.Integration;

@Integration
public class MigrationTest extends WingsBaseTest {
  @Inject private RemoveServiceVariablesFromActivity migration;

  @Test
  @Category(UnitTests.class)
  @Bypass
  public void shouldMigrate() {
    migration.migrate();
  }
}
