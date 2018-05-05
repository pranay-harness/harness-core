package migrations;

import com.google.common.collect.ImmutableList;

import migrations.all.AddArtifactCheck;
import migrations.all.AddIsDefaultToExistingNotificationGroups;
import migrations.all.AddValidUntilToStateExecutionInstance;
import migrations.all.AddValidUntilToWaitInstance;
import migrations.all.AddVerifyToRollbackWorkflows;
import migrations.all.CreateDefaultUserGroupsAndAddToExistingUsers;
import migrations.all.CreateSupportUserGroupsAndRenameAccountAdmin;
import migrations.all.DirectKubernetesToCloudProvider;
import migrations.all.FixInstanceData;
import migrations.all.FixInstanceDataForAwsSSH;
import migrations.all.FixMaxInstancesFieldInContainerSetup;
import migrations.all.GitSyncToAllAccounts;
import migrations.all.LogAnalysisExperimentalRecordsMigration;
import migrations.all.LogAnalysisRecordsMigration;
import migrations.all.LogDataRecordsMigration;
import migrations.all.LogFeedbackRecordsMigration;
import migrations.all.NewRelicMetricNameCronRemoval;
import migrations.all.ServiceKeywordsMigration;
import migrations.all.ServiceVariableReferentialIntegrity;
import migrations.all.SetDaemonSetInWorkflowPhase;
import migrations.all.SetRollbackFlagToWorkflows;
import migrations.all.StateExecutionInstanceDisplayName;
import migrations.all.TrimYamlMigration;
import migrations.all.VerifyStepWorkflowOrder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class MigrationList {
  /**
   * Add your migrations to the end of the list with the next sequence number. After it has been in production for a few
   * releases it can be deleted, but keep at least one item in this list with the latest sequence number. You can use
   * BaseMigration.class as a placeholder for any removed class.
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
        .add(Pair.of(113, CreateSupportUserGroupsAndRenameAccountAdmin.class))
        .add(Pair.of(114, GitSyncToAllAccounts.class))
        .add(Pair.of(115, AddValidUntilToStateExecutionInstance.class))
        .add(Pair.of(116, BaseMigration.class))
        .add(Pair.of(117, StateExecutionInstanceDisplayName.class))
        .add(Pair.of(118, ServiceKeywordsMigration.class))
        .add(Pair.of(119, FixMaxInstancesFieldInContainerSetup.class))
        .add(Pair.of(120, GitSyncToAllAccounts.class))
        .add(Pair.of(121, SetRollbackFlagToWorkflows.class))
        .add(Pair.of(122, AddValidUntilToWaitInstance.class))
        .add(Pair.of(123, NewRelicMetricNameCronRemoval.class))
        .add(Pair.of(124, BaseMigration.class))
        .add(Pair.of(125, ServiceVariableReferentialIntegrity.class))
        .add(Pair.of(126, DirectKubernetesToCloudProvider.class))
        .add(Pair.of(127, LogDataRecordsMigration.class))
        .add(Pair.of(128, LogAnalysisRecordsMigration.class))
        .add(Pair.of(129, LogFeedbackRecordsMigration.class))
        .add(Pair.of(130, FixInstanceData.class))
        .add(Pair.of(131, LogAnalysisExperimentalRecordsMigration.class))
        .add(Pair.of(132, FixInstanceDataForAwsSSH.class))
        .build();
  }
}
