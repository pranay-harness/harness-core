/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.app;

import io.harness.mongo.index.migrator.ApiKeysNameUniqueInAccountMigration;
import io.harness.mongo.index.migrator.DelegateProfileNameUniqueInAccountMigration;
import io.harness.mongo.index.migrator.DelegateScopeNameUniqueInAccountMigration;
import io.harness.mongo.index.migrator.Migrator;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class IndexMigratorModule extends AbstractModule {
  @Override
  protected void configure() {
    MapBinder<String, Migrator> indexMigrators = MapBinder.newMapBinder(binder(), String.class, Migrator.class);
    indexMigrators.addBinding("delegateProfiles.uniqueName").to(DelegateProfileNameUniqueInAccountMigration.class);
    indexMigrators.addBinding("apiKeys.uniqueName").to(ApiKeysNameUniqueInAccountMigration.class);
    indexMigrators.addBinding("delegateScopes.uniqueName").to(DelegateScopeNameUniqueInAccountMigration.class);
  }
}
