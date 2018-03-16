package migrations;

import com.google.common.collect.ImmutableList;

import migrations.all.AddArtifactCheck;
import migrations.all.AddIsDefaultToExistingNotificationGroups;
import migrations.all.AddVerifyToRollbackWorkflows;
import migrations.all.CreateDefaultUserGroupsAndAddToExistingUsers;
import migrations.all.SetDaemonSetInWorkflowPhase;
import migrations.all.TrimYamlMigration;
import migrations.all.VerifyStepWorkflowOrder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class MigrationList {
  /**
   * Add your migrations to the end of the list with the next sequence number. After it has been in production for a few
   * releases it can be deleted, but keep at least one item in this list with the latest sequence number. You can use
   * BaseMigration.class if there are no migrations left.
   */
  public static List<Pair<Integer, Class<? extends Migration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends Migration>>>()
        .add(Pair.of(105, BaseMigration.class))
        .add(Pair.of(106, AddVerifyToRollbackWorkflows.class))
        .add(Pair.of(107, VerifyStepWorkflowOrder.class))
        .add(Pair.of(108, AddIsDefaultToExistingNotificationGroups.class))
        .add(Pair.of(109, TrimYamlMigration.class))
        .add(Pair.of(110, SetDaemonSetInWorkflowPhase.class))
        .add(Pair.of(111, AddArtifactCheck.class))
        .add(Pair.of(112, CreateDefaultUserGroupsAndAddToExistingUsers.class))
        .build();
  }
}
