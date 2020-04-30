package migrations;

import com.google.common.collect.ImmutableList;

import lombok.experimental.UtilityClass;
import migrations.seedata.IISInstallCommandMigration;
import migrations.seedata.IISInstallCommandV5Migration;
import migrations.seedata.ReImportTemplatesMigration;
import migrations.seedata.TemplateGalleryDefaultTemplatesMigration;
import migrations.seedata.TomcatInstallCommandMigration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@UtilityClass
public class SeedDataMigrationList {
  public static List<Pair<Integer, Class<? extends SeedDataMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends SeedDataMigration>>>()
        .add(Pair.of(1, BaseSeedDataMigration.class))
        .add(Pair.of(2, BaseSeedDataMigration.class))
        .add(Pair.of(3, TemplateGalleryDefaultTemplatesMigration.class))
        .add(Pair.of(4, IISInstallCommandMigration.class))
        .add(Pair.of(5, BaseSeedDataMigration.class))
        .add(Pair.of(6, BaseSeedDataMigration.class))
        .add(Pair.of(7, BaseSeedDataMigration.class))
        .add(Pair.of(8, BaseSeedDataMigration.class))
        .add(Pair.of(9, BaseSeedDataMigration.class))
        .add(Pair.of(10, BaseSeedDataMigration.class))
        .add(Pair.of(11, TomcatInstallCommandMigration.class))
        .add(Pair.of(12, BaseSeedDataMigration.class))
        .add(Pair.of(13, ReImportTemplatesMigration.class))
        .add(Pair.of(14, IISInstallCommandV5Migration.class))
        .build();
  }
}
