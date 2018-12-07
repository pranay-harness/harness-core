package migrations;

import com.google.common.collect.ImmutableList;

import migrations.seedata.GlobalAccountMigration;
import migrations.seedata.IISInstallCommandMigration;
import migrations.seedata.IISInstallCommandV2Migration;
import migrations.seedata.TemplateGalleryDefaultTemplatesMigration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class SeedDataMigrationList {
  public static List<Pair<Integer, Class<? extends SeedDataMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends SeedDataMigration>>>()
        .add(Pair.of(1, BaseSeedDataMigration.class))
        .add(Pair.of(2, BaseSeedDataMigration.class))
        .add(Pair.of(3, TemplateGalleryDefaultTemplatesMigration.class))
        .add(Pair.of(4, IISInstallCommandMigration.class))
        .add(Pair.of(5, GlobalAccountMigration.class))
        .add(Pair.of(6, IISInstallCommandV2Migration.class))
        .build();
  }
}
