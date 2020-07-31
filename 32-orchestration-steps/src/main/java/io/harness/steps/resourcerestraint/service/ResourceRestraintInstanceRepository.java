package io.harness.steps.resourcerestraint.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@OwnedBy(CDC)
@HarnessRepo
public interface ResourceRestraintInstanceRepository extends CrudRepository<ResourceRestraintInstance, String> {
  List<ResourceRestraintInstance> findByResourceUnitOrderByOrderAsc(String resourceUnit);
}
