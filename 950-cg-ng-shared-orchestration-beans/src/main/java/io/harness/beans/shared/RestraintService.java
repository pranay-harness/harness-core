package io.harness.beans.shared;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;

@OwnedBy(CDC)
public interface RestraintService {
  ResourceConstraint get(String ownerId, String id);
  ResourceConstraint getByNameAndAccountId(String name, String accountId);
  ResourceConstraint save(@Valid ResourceConstraint resourceConstraint);
  List<ResourceConstraint> getConstraintsIn(Set<String> constraintIds);
}
