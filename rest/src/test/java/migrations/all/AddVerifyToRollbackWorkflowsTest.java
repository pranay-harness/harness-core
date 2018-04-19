package migrations.all;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.WingsBaseTest;

public class AddVerifyToRollbackWorkflowsTest extends WingsBaseTest {
  @Inject AddVerifyToRollbackWorkflows migrator;

  @Test
  public void shouldMigrate() {
    migrator.migrate();
  }
}
