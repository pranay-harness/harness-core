package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Consumer;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@OwnedBy(CDC)
@HarnessRepo
public interface ResourceRestraintInstanceRepository extends CrudRepository<ResourceRestraintInstance, String> {
  Optional<ResourceRestraintInstance> findFirstByResourceRestraintIdOrderByOrderDesc(String resourceRestraintId);
  List<ResourceRestraintInstance> findByReleaseEntityTypeAndReleaseEntityId(
      String releaseEntityType, String releaseEntityId);
  Optional<ResourceRestraintInstance> findByUuidAndResourceUnitAndStateIn(
      String uuid, String resourceUnit, List<Consumer.State> states);
}
