package migrations;

import com.google.common.collect.ImmutableList;

import lombok.experimental.UtilityClass;
import migrations.all.SyncNewFolderForConfigFiles;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@UtilityClass
public class OnPrimaryManagerMigrationList {
  public static List<Pair<Integer, Class<? extends OnPrimaryManagerMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends OnPrimaryManagerMigration>>>()
        .add(Pair.of(1, SyncNewFolderForConfigFiles.class))
        .build();
  }
}
