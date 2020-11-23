package io.harness.capability.internal;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilityRequirement.CapabilityRequirementKeys;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionKeys;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

@Slf4j
public class CapabilityDao {
  private final HPersistence persistence;

  @Inject
  public CapabilityDao(HPersistence persistence) {
    this.persistence = persistence;
  }
  // TODO: find migrations that add TTL and examples of creating batch request, move all txn to batch request

  // Adds a new capability requirement into the capability dao, and initializes all permission entities
  // for the set of delegates specified here
  public String addCapabilityRequirement(CapabilityRequirement requirement, List<String> allDelegateIds) {
    Instant creationTime = Instant.now();
    checkState(!requirement.getAccountId().isEmpty());
    requirement.setValidUntil(Date.from(Instant.now().plus(Duration.ofHours(100))));
    String capabilityId = persistence.save(requirement);
    persistence.save(allDelegateIds.stream()
                         .map(delegateId
                             -> (CapabilitySubjectPermission) CapabilitySubjectPermission.builder()
                                    .accountId(requirement.getAccountId())
                                    .capabilityId(capabilityId)
                                    .delegateId(delegateId)
                                    .validUntil(Date.from(creationTime))
                                    .permissionResult(PermissionResult.UNCHECKED)
                                    .build())
                         .collect(toList()));
    return capabilityId;
  }

  // Updates a set of permissions under a capability ID so that the permissions return true
  public boolean updateCapabilityPermission(
      String accountId, String capabilityId, List<String> delegateIds, PermissionResult result) {
    // TODO: cache this
    CapabilityRequirement requirement = persistence.createQuery(CapabilityRequirement.class)
                                            .filter(CapabilityRequirementKeys.accountId, accountId)
                                            .filter(CapabilityRequirementKeys.uuid, capabilityId)
                                            .get();

    Query<CapabilitySubjectPermission> updateQuery =
        persistence.createQuery(CapabilitySubjectPermission.class)
            .filter(CapabilitySubjectPermissionKeys.accountId, accountId)
            .filter(CapabilitySubjectPermissionKeys.capabilityId, capabilityId)
            .field(CapabilitySubjectPermissionKeys.delegateId)
            .in(delegateIds);

    UpdateOperations<CapabilitySubjectPermission> updateOperations =
        persistence.createUpdateOperations(CapabilitySubjectPermission.class)
            .set(CapabilitySubjectPermissionKeys.permissionResult, result)
            .set(CapabilitySubjectPermissionKeys.validUntil, Instant.now().plus(Duration.ofMinutes(15)));

    UpdateResults update = persistence.update(updateQuery, updateOperations);
    return update.getUpdatedCount() == delegateIds.size();
  }

  // capability requirements are generally immutable, but we keep track of when a capability is last used
  public void updateCapabilityRequirement(String accountId, String capabilityId) {
    Query<CapabilityRequirement> requirementQuery = persistence.createQuery(CapabilityRequirement.class)
                                                        .filter(CapabilityRequirementKeys.accountId, accountId)
                                                        .filter(CapabilityRequirementKeys.uuid, capabilityId);
    UpdateOperations<CapabilityRequirement> requirementUpdateOperations =
        persistence.createUpdateOperations(CapabilityRequirement.class)
            .set(CapabilityRequirementKeys.validUntil, Instant.now().plus(Duration.ofHours(100)));
    persistence.update(requirementQuery, requirementUpdateOperations);
  }

  // remove all capability permissions and the associated capability reuqirement
  public void removeCapabilityRequirement(String accountId, String capabilityId) {
    Query<CapabilityRequirement> requirementQuery = persistence.createQuery(CapabilityRequirement.class)
                                                        .filter(CapabilityRequirementKeys.accountId, accountId)
                                                        .filter(CapabilityRequirementKeys.uuid, capabilityId);
    Query<CapabilitySubjectPermission> permissionQuery =
        persistence.createQuery(CapabilitySubjectPermission.class)
            .filter(CapabilitySubjectPermissionKeys.accountId, accountId)
            .filter(CapabilitySubjectPermissionKeys.capabilityId, capabilityId);
    persistence.delete(requirementQuery);
    persistence.delete(permissionQuery);
  }

  public List<CapabilitySubjectPermission> getAllCapabilityPermission(String accountId, List<String> capabilityIds) {
    return persistence.createQuery(CapabilitySubjectPermission.class)
        .filter(CapabilitySubjectPermissionKeys.accountId, accountId)
        .field(CapabilitySubjectPermissionKeys.capabilityId)
        .in(capabilityIds)
        .asList();
  }

  public List<CapabilitySubjectPermission> getAllDelegatePermission(String accountId, List<String> delegateIds) {
    return persistence.createQuery(CapabilitySubjectPermission.class)
        .filter(CapabilitySubjectPermissionKeys.accountId, accountId)
        .field(CapabilitySubjectPermissionKeys.delegateId)
        .in(delegateIds)
        .asList();
  }

  public List<CapabilityRequirement> getAllCapabilityRequirement(String accountId) {
    return persistence.createQuery(CapabilityRequirement.class)
        .filter(CapabilityRequirementKeys.accountId, accountId)
        .asList();
  }
}
