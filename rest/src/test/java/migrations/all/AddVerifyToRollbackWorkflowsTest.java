package migrations.all;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.generator.WorkflowGenerator;

public class AddVerifyToRollbackWorkflowsTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject WorkflowGenerator workflowGenerator;
  @Inject AddVerifyToRollbackWorkflows migrator;

  @Test
  public void shouldMigrate() {
    migrator.migrate();
  }
}
