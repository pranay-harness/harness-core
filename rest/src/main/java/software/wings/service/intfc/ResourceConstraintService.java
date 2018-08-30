package software.wings.service.intfc;

import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintRegistry;
import io.harness.validation.Create;
import io.harness.validation.Update;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.ResourceConstraintUsage;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;

public interface ResourceConstraintService extends OwnedByAccount {
  PageResponse<ResourceConstraint> list(PageRequest<ResourceConstraint> pageRequest);

  @ValidationGroups(Create.class) ResourceConstraint save(@Valid ResourceConstraint resourceConstraint);

  @ValidationGroups(Update.class) ResourceConstraint update(@Valid ResourceConstraint resourceConstraint);

  ResourceConstraint get(String accountId, String resourceConstraintId);

  void delete(String accountId, String resourceConstraintId);

  ConstraintRegistry getRegistry();

  int getMaxOrder(String resourceConstraintId);

  Constraint createAbstraction(ResourceConstraint resourceConstraint);

  Set<String> updateActiveConstraints(String appId, String workflowExecution);

  Set<String> selectBlockedConstraints();

  void updateBlockedConstraints(Set<String> constraintIds);

  List<ResourceConstraintUsage> usage(String accountId, List<String> resourceConstraintIds);
}
