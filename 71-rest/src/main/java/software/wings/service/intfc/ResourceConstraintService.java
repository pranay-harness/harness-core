package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintRegistry;
import io.harness.validation.Create;
import io.harness.validation.Update;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.ResourceConstraintInstance;
import software.wings.beans.ResourceConstraintUsage;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface ResourceConstraintService extends OwnedByAccount {
  PageResponse<ResourceConstraint> list(PageRequest<ResourceConstraint> pageRequest);

  @ValidationGroups(Create.class) ResourceConstraint save(@Valid ResourceConstraint resourceConstraint);

  @ValidationGroups(Update.class) void update(@Valid ResourceConstraint resourceConstraint);

  ResourceConstraint get(String accountId, String resourceConstraintId);

  ResourceConstraint getByName(@NotNull String accountId, @NotNull String resourceConstraintName);

  ResourceConstraint ensureResourceConstraintForConcurrency(String accountId, String name);

  void delete(String accountId, String resourceConstraintId);

  ConstraintRegistry getRegistry();

  int getMaxOrder(String resourceConstraintId);

  Constraint createAbstraction(ResourceConstraint resourceConstraint);

  Set<String> updateActiveConstraints(String appId, String workflowExecution);

  boolean updateActiveConstraintForInstance(ResourceConstraintInstance instance);

  Set<String> selectBlockedConstraints();

  void updateBlockedConstraints(Set<String> constraintIds);

  List<ResourceConstraintUsage> usage(String accountId, List<String> resourceConstraintIds);

  List<ResourceConstraintInstance> fetchResourceConstraintInstancesForUnitAndEntityType(
      String appId, String resourceConstraintId, String unit, String entityType);

  static String workflowExecutionIdFromReleaseEntityId(String releaseEntityId) {
    return releaseEntityId.split("[|]")[0];
  }

  static String phaseNameFromReleaseEntityId(String releaseEntityId) {
    return releaseEntityId.split("[|]")[1];
  }

  static String releaseEntityId(String workflowExecutionId) {
    return workflowExecutionId;
  }

  static String releaseEntityId(String workflowExecutionId, String phaseName) {
    return workflowExecutionId + "|" + phaseName;
  }

  int getAllCurrentlyAcquiredPermits(String holdingScope, String releaseEntityId, String appId);
}
