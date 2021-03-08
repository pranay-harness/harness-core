package io.harness.migrations;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.all.DeleteGitFileActivityAndGitFileAcitivitySummary;
import io.harness.migrations.all.RefactorTheFieldsInGitSyncError;
import io.harness.migrations.all.SyncNewFolderForConfigFiles;
import io.harness.migrations.all.TemplateLibraryYamlOnPrimaryManagerMigration;
import io.harness.migrations.gitsync.SetQueueKeyYamChangeSetMigration;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
@TargetModule(Module._390_DB_MIGRATION)
public class OnPrimaryManagerMigrationList {
  public static List<Pair<Integer, Class<? extends OnPrimaryManagerMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends OnPrimaryManagerMigration>>>()
        .add(Pair.of(1, SyncNewFolderForConfigFiles.class))
        .add(Pair.of(2, TemplateLibraryYamlOnPrimaryManagerMigration.class))
        .add(Pair.of(3, RefactorTheFieldsInGitSyncError.class))
        .add(Pair.of(4, BaseMigration.class))
        .add(Pair.of(5, SetQueueKeyYamChangeSetMigration.class))
        .add(Pair.of(6, DeleteGitFileActivityAndGitFileAcitivitySummary.class))
        .build();
  }
}
