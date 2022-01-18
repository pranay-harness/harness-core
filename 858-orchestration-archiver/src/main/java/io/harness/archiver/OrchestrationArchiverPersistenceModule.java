package io.harness.archiver;

import io.harness.gitsync.persistance.GitSyncablePersistenceConfig;
import io.harness.ng.accesscontrol.migrations.AccessControlMigrationPersistenceConfig;
import io.harness.notification.NotificationChannelPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.harness.springdata.SpringPersistenceModule;

import java.util.List;

public class OrchestrationArchiverPersistenceModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    List<Class<?>> resultClasses =
        Lists.newArrayList(ImmutableList.of(SpringPersistenceConfig.class, NotificationChannelPersistenceConfig.class,
            AccessControlMigrationPersistenceConfig.class, GitSyncablePersistenceConfig.class));
    Class<?>[] resultClassesArray = new Class<?>[ resultClasses.size() ];
    return resultClasses.toArray(resultClassesArray);
  }
}
