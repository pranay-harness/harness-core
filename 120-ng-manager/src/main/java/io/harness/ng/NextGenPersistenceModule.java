package io.harness.ng;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.migrations.AccessControlMigrationPersistenceConfig;
import io.harness.pms.sdk.PmsSdkPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.springdata.SpringPersistenceModule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;

@OwnedBy(HarnessTeam.PL)
public class NextGenPersistenceModule extends SpringPersistenceModule {
  private final boolean withPMS;
  private final boolean withAccessControlMigration;

  public NextGenPersistenceModule(boolean withPMS, boolean withAccessControlMigration) {
    this.withPMS = withPMS;
    this.withAccessControlMigration = withAccessControlMigration;
  }

  @Override
  protected Class<?>[] getConfigClasses() {
    List<Class<?>> resultClasses = Lists.newArrayList(ImmutableList.of(SpringPersistenceConfig.class));
    if (withPMS) {
      resultClasses.add(PmsSdkPersistenceConfig.class);
    }
    if (withAccessControlMigration) {
      resultClasses.add(AccessControlMigrationPersistenceConfig.class);
    }
    Class<?>[] resultClassesArray = new Class<?>[ resultClasses.size() ];
    return resultClasses.toArray(resultClassesArray);
  }
}
