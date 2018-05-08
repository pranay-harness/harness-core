package migrations.all;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.generator.WorkflowGenerator;
import software.wings.rules.Integration;

@Integration
public class AddValidUntilToWaitInstanceTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject WorkflowGenerator workflowGenerator;
  @Inject AddValidUntilToWaitInstance migrator;

  @Test
  @Ignore
  public void shouldMigrate() {
    migrator.migrate();
  }
}