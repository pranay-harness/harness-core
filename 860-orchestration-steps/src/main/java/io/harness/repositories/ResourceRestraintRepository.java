package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.repository.CrudRepository;

@OwnedBy(PIPELINE)
@HarnessRepo
public interface ResourceRestraintRepository extends CrudRepository<ResourceRestraint, String> {
  List<ResourceRestraint> findByUuidIn(Set<String> resourceRestraintUuids);
  Optional<ResourceRestraint> findByNameAndAccountId(String name, String accountId);
}
