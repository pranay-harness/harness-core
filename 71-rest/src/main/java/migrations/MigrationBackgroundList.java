package migrations;

import com.google.common.collect.ImmutableList;

import migrations.all.AddInfraMappingNameToInstanceData;
import migrations.all.AddIsDefaultFlagToUserGroup;
import migrations.all.AddOrchestrationToWorkflows;
import migrations.all.AddStateMachineToWorkflowExecutions;
import migrations.all.AddValidUntilToCommandLog;
import migrations.all.AddValidUntilToWorkflowExecution;
import migrations.all.CleanupOrphanInstances;
import migrations.all.CleanupSyncStatusForDeletedEntities;
import migrations.all.DeleteAndAddInstanceSyncJobToAllAccounts;
import migrations.all.DeleteOrphanNotificationGroups;
import migrations.all.MigrateLogDataRecordsToGoogle;
import migrations.all.RemoveSupportEmailFromSalesContacts;
import migrations.all.SendInviteUrlForAllUserInvites;
import migrations.all.SetEmailToIndividualMemberFlag;
import migrations.all.SetLastLoginTimeToAllUsers;
import migrations.all.TerraformIsTemplatizedMigration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class MigrationBackgroundList {
  /**
   * Add your background migrations to the end of the list with the next sequence number.
   * Make sure your background migration is resumable and with rate limit that does not exhaust
   * the resources.
   */
  public static List<Pair<Integer, Class<? extends Migration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends Migration>>>()
        .add(Pair.of(1, AddValidUntilToCommandLog.class))
        .add(Pair.of(2, BaseMigration.class))
        .add(Pair.of(3, SetLastLoginTimeToAllUsers.class))
        .add(Pair.of(4, BaseMigration.class))
        .add(Pair.of(5, BaseMigration.class))
        .add(Pair.of(6, BaseMigration.class))
        .add(Pair.of(7, RemoveSupportEmailFromSalesContacts.class))
        .add(Pair.of(8, BaseMigration.class))
        .add(Pair.of(9, BaseMigration.class))
        .add(Pair.of(10, BaseMigration.class))
        .add(Pair.of(11, DeleteAndAddInstanceSyncJobToAllAccounts.class))
        .add(Pair.of(12, TerraformIsTemplatizedMigration.class))
        .add(Pair.of(13, AddValidUntilToWorkflowExecution.class))
        .add(Pair.of(14, SendInviteUrlForAllUserInvites.class))
        .add(Pair.of(15, BaseMigration.class))
        .add(Pair.of(16, AddOrchestrationToWorkflows.class))
        .add(Pair.of(17, CleanupOrphanInstances.class))
        .add(Pair.of(18, CleanupSyncStatusForDeletedEntities.class))
        .add(Pair.of(19, AddStateMachineToWorkflowExecutions.class))
        .add(Pair.of(20, DeleteOrphanNotificationGroups.class))
        .add(Pair.of(21, AddIsDefaultFlagToUserGroup.class))
        .add(Pair.of(22, AddInfraMappingNameToInstanceData.class))
        .add(Pair.of(23, MigrateLogDataRecordsToGoogle.class))
        .add(Pair.of(24, SetEmailToIndividualMemberFlag.class))
        .add(Pair.of(25, SetEmailToIndividualMemberFlag.class))
        .build();
  }
}
