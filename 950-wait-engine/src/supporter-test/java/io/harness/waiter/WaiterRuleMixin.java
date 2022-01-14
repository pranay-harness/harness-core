package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.waiter.WaiterConfiguration.PersistenceLayer;

import java.lang.annotation.Annotation;
import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface WaiterRuleMixin {
  default PersistenceLayer obtainPersistenceLayer(List<Annotation> annotations) {
    return annotations.stream().anyMatch(SpringWaiter.class ::isInstance) ? PersistenceLayer.SPRING
                                                                          : PersistenceLayer.MORPHIA;
  };
}
