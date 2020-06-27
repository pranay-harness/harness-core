package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import io.harness.mongo.index.migrator.DelegateProfileNameUniqueInAccountMigration;
import io.harness.mongo.index.migrator.Migrator;

public class IndexMigratorModule extends AbstractModule {
  @Override
  protected void configure() {
    MapBinder<String, Migrator> indexMigrators = MapBinder.newMapBinder(binder(), String.class, Migrator.class);
    indexMigrators.addBinding("delegateProfiles.uniqueName").to(DelegateProfileNameUniqueInAccountMigration.class);
  }
}
