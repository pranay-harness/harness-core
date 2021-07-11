package io.harness.migrations;

import io.harness.migrations.seedata.IISInstallCommandMigration;
import io.harness.migrations.seedata.ReImportTemplatesMigration;
import io.harness.migrations.seedata.TemplateGalleryDefaultTemplatesMigration;
import io.harness.migrations.seedata.TomcatInstallCommandMigration;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
public class SeedDataMigrationList {
  public static List<Pair<Integer, Class<? extends io.harness.migrations.SeedDataMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends SeedDataMigration>>>()
        .add(Pair.of(1, io.harness.migrations.BaseSeedDataMigration.class))
        .add(Pair.of(2, io.harness.migrations.BaseSeedDataMigration.class))
        .add(Pair.of(3, TemplateGalleryDefaultTemplatesMigration.class))
        .add(Pair.of(4, IISInstallCommandMigration.class))
        .add(Pair.of(5, io.harness.migrations.BaseSeedDataMigration.class))
        .add(Pair.of(6, io.harness.migrations.BaseSeedDataMigration.class))
        .add(Pair.of(7, io.harness.migrations.BaseSeedDataMigration.class))
        .add(Pair.of(8, io.harness.migrations.BaseSeedDataMigration.class))
        .add(Pair.of(9, io.harness.migrations.BaseSeedDataMigration.class))
        .add(Pair.of(10, io.harness.migrations.BaseSeedDataMigration.class))
        .add(Pair.of(11, TomcatInstallCommandMigration.class))
        .add(Pair.of(12, io.harness.migrations.BaseSeedDataMigration.class))
        .add(Pair.of(13, ReImportTemplatesMigration.class))
        .add(Pair.of(14, BaseSeedDataMigration.class))
        .build();
  }
}
