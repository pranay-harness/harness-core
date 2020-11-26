package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.activityhistory.entity.ConnectivityCheckDetail;
import io.harness.ng.core.activityhistory.entity.EntityUsageActivityDetail;
import io.harness.ng.core.activityhistory.entity.NGActivity;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.service.entity.ServiceEntity;

import java.util.Set;

public class NGCoreMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Environment.class);
    set.add(ServiceEntity.class);
    set.add(NGActivity.class);
    set.add(EntitySetupUsage.class);
    set.add(ConnectivityCheckDetail.class);
    set.add(EntityUsageActivityDetail.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
