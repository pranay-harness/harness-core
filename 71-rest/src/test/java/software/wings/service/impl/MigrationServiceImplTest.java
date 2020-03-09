package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import migrations.OnPrimaryManagerMigration;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Schema;

import java.util.HashMap;
import java.util.Map;

public class MigrationServiceImplTest extends WingsBaseTest {
  @Inject MigrationServiceImpl migrationService;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testMigrationsOnPrimary() {
    testMigrationUpgrade();
    testMigrationDowngrade();
  }

  private void testMigrationUpgrade() {
    Schema schema = Schema.builder()
                        .version(0)
                        .backgroundVersion(0)
                        .seedDataVersion(0)
                        .timescaleDbVersion(0)
                        .timescaleDBDataVersion(0)
                        .onPrimaryManagerVersion(0)
                        .build();
    wingsPersistence.save(schema);

    Map<Integer, Class<? extends OnPrimaryManagerMigration>> onPrimaryManagerMigrationMap = new HashMap<>();
    onPrimaryManagerMigrationMap.put(1, TestClass.class);
    migrationService.runMigrationWhenNewManagerIsPrimary(1, onPrimaryManagerMigrationMap);
    Query<Schema> query = wingsPersistence.createQuery(Schema.class);
    assertThat(query.get().getOnPrimaryManagerVersion()).isEqualTo(1);
  }

  private void testMigrationDowngrade() {
    Schema schema = Schema.builder()
                        .version(0)
                        .backgroundVersion(0)
                        .seedDataVersion(0)
                        .timescaleDbVersion(0)
                        .timescaleDBDataVersion(0)
                        .onPrimaryManagerVersion(2)
                        .build();
    wingsPersistence.save(schema);

    Map<Integer, Class<? extends OnPrimaryManagerMigration>> onPrimaryManagerMigrationMap = new HashMap<>();
    onPrimaryManagerMigrationMap.put(1, TestClass.class);
    migrationService.scheduleOnPrimaryMigrations(schema, 1, onPrimaryManagerMigrationMap);
    Query<Schema> query = wingsPersistence.createQuery(Schema.class);
    assertThat(query.get().getOnPrimaryManagerVersion()).isEqualTo(1);
  }

  public static class TestClass implements OnPrimaryManagerMigration {
    @Override
    public void migrate() {
      // do nothing.
    }
  }
}