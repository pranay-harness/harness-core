package migrations;

import com.google.common.collect.ImmutableList;

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
        //        .add(Pair.of(106, AddVerifyToRollbackWorkflows.class))
        .build();
  }
}
