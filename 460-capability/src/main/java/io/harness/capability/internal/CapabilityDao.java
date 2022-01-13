/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.capability.internal;

import static io.harness.mongo.MongoUtils.setUnset;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilityRequirement.CapabilityRequirementKeys;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionKeys;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.capability.CapabilityTaskSelectionDetails.CapabilityTaskSelectionDetailsKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
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

  public CapabilityRequirement upsert(
      CapabilityRequirement capabilityRequirement, FindAndModifyOptions findAndModifyOptions) {
    Query<CapabilityRequirement> query =
        persistence.createQuery(CapabilityRequirement.class)
            .filter(CapabilityRequirementKeys.accountId, capabilityRequirement.getAccountId())
            .filter(CapabilityRequirementKeys.uuid, capabilityRequirement.getUuid());

    UpdateOperations<CapabilityRequirement> updateOperations =
        persistence.createUpdateOperations(CapabilityRequirement.class);
    updateOperations.set(CapabilityRequirementKeys.uuid, capabilityRequirement.getUuid());
    updateOperations.set(CapabilityRequirementKeys.accountId, capabilityRequirement.getAccountId());
    updateOperations.set(CapabilityRequirementKeys.validUntil, capabilityRequirement.getValidUntil());
    updateOperations.set(CapabilityRequirementKeys.capabilityType, capabilityRequirement.getCapabilityType());
    updateOperations.set(
        CapabilityRequirementKeys.capabilityParameters, capabilityRequirement.getCapabilityParameters());

    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }

  public CapabilityTaskSelectionDetails upsert(
      CapabilityTaskSelectionDetails taskSelectionDetails, FindAndModifyOptions findAndModifyOptions) {
    Query<CapabilityTaskSelectionDetails> query =
        persistence.createQuery(CapabilityTaskSelectionDetails.class)
            .filter(CapabilityTaskSelectionDetailsKeys.accountId, taskSelectionDetails.getAccountId())
            .filter(CapabilityTaskSelectionDetailsKeys.uuid, taskSelectionDetails.getUuid());

    UpdateOperations<CapabilityTaskSelectionDetails> updateOperations =
        persistence.createUpdateOperations(CapabilityTaskSelectionDetails.class);
    updateOperations.set(CapabilityTaskSelectionDetailsKeys.uuid, taskSelectionDetails.getUuid());
    updateOperations.set(CapabilityTaskSelectionDetailsKeys.accountId, taskSelectionDetails.getAccountId());
    updateOperations.set(CapabilityTaskSelectionDetailsKeys.capabilityId, taskSelectionDetails.getCapabilityId());
    updateOperations.set(CapabilityTaskSelectionDetailsKeys.taskGroup, taskSelectionDetails.getTaskGroup());
    updateOperations.set(CapabilityTaskSelectionDetailsKeys.taskSelectors, taskSelectionDetails.getTaskSelectors());
    updateOperations.set(
        CapabilityTaskSelectionDetailsKeys.taskSetupAbstractions, taskSelectionDetails.getTaskSetupAbstractions());
    updateOperations.set(CapabilityTaskSelectionDetailsKeys.validUntil, taskSelectionDetails.getValidUntil());
    updateOperations.set(CapabilityTaskSelectionDetailsKeys.blocked, taskSelectionDetails.isBlocked());

    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }

  public List<CapabilitySubjectPermission> getAllDelegatePermissions(
      String accountId, String delegateId, PermissionResult permissionResultFilter) {
    Query<CapabilitySubjectPermission> query = persistence.createQuery(CapabilitySubjectPermission.class)
                                                   .filter(CapabilitySubjectPermissionKeys.accountId, accountId)
                                                   .filter(CapabilitySubjectPermissionKeys.delegateId, delegateId);

    if (permissionResultFilter != null) {
      query.filter(CapabilitySubjectPermissionKeys.permissionResult, permissionResultFilter);
    }

    return query.asList();
  }

  public List<String> addCapabilityPermissions(
      CapabilityRequirement requirement, List<String> allDelegateIds, PermissionResult result) {
    return persistence.saveBatch(allDelegateIds.stream()
                                     .map(delegateId
                                         -> CapabilitySubjectPermission.builder()
                                                .accountId(requirement.getAccountId())
                                                .capabilityId(requirement.getUuid())
                                                .delegateId(delegateId)
                                                .maxValidUntil(0)
                                                .revalidateAfter(0)
                                                .validUntil(requirement.getValidUntil())
                                                .permissionResult(result)
                                                .build())
                                     .collect(toList()));
  }

  public List<CapabilitySubjectPermission> getAllCapabilityPermissions(
      String accountId, String capabilityId, PermissionResult permissionResultFilter) {
    Query<CapabilitySubjectPermission> query = persistence.createQuery(CapabilitySubjectPermission.class)
                                                   .filter(CapabilitySubjectPermissionKeys.accountId, accountId)
                                                   .filter(CapabilitySubjectPermissionKeys.capabilityId, capabilityId);

    if (permissionResultFilter != null) {
      query.filter(CapabilitySubjectPermissionKeys.permissionResult, permissionResultFilter);
    }

    return query.asList();
  }

  public List<CapabilitySubjectPermission> getValidAllowedCapabilityPermissions(String accountId, String capabilityId) {
    Query<CapabilitySubjectPermission> query =
        persistence.createQuery(CapabilitySubjectPermission.class)
            .filter(CapabilitySubjectPermissionKeys.accountId, accountId)
            .filter(CapabilitySubjectPermissionKeys.capabilityId, capabilityId)
            .filter(CapabilitySubjectPermissionKeys.permissionResult, PermissionResult.ALLOWED)
            .field(CapabilitySubjectPermissionKeys.maxValidUntil)
            .greaterThan(System.currentTimeMillis());

    return query.asList();
  }

  public List<CapabilitySubjectPermission> getPermissionsByIds(String accountId, Set<String> permissionIds) {
    Query<CapabilitySubjectPermission> query = persistence.createQuery(CapabilitySubjectPermission.class)
                                                   .filter(CapabilitySubjectPermissionKeys.accountId, accountId)
                                                   .field(CapabilitySubjectPermissionKeys.uuid)
                                                   .in(permissionIds);

    return query.asList();
  }

  public List<CapabilityTaskSelectionDetails> getAllCapabilityTaskSelectionDetails(
      String accountId, String capabilityId) {
    return persistence.createQuery(CapabilityTaskSelectionDetails.class)
        .filter(CapabilityTaskSelectionDetailsKeys.accountId, accountId)
        .filter(CapabilityTaskSelectionDetailsKeys.capabilityId, capabilityId)
        .asList();
  }

  public List<CapabilityTaskSelectionDetails> getTaskSelectionDetails(String accountId, Set<String> capabilityIds) {
    return persistence.createQuery(CapabilityTaskSelectionDetails.class)
        .filter(CapabilityTaskSelectionDetailsKeys.accountId, accountId)
        .field(CapabilityTaskSelectionDetailsKeys.capabilityId)
        .in(capabilityIds)
        .asList();
  }

  public List<CapabilityTaskSelectionDetails> getBlockedTaskSelectionDetails(String accountId) {
    return persistence.createQuery(CapabilityTaskSelectionDetails.class)
        .filter(CapabilityTaskSelectionDetailsKeys.accountId, accountId)
        .filter(CapabilityTaskSelectionDetailsKeys.blocked, true)
        .asList();
  }

  public boolean deleteCapabilitySubjectPermission(String uuid) {
    return persistence.delete(CapabilitySubjectPermission.class, uuid);
  }

  public boolean deleteCapabilitySubjectPermission(String accountId, String delegateId, String capabilityId) {
    return persistence.delete(persistence.createQuery(CapabilitySubjectPermission.class)
                                  .filter(CapabilitySubjectPermissionKeys.accountId, accountId)
                                  .filter(CapabilitySubjectPermissionKeys.delegateId, delegateId)
                                  .filter(CapabilitySubjectPermissionKeys.capabilityId, capabilityId));
  }

  public CapabilitySubjectPermission getDelegateCapabilityPermission(
      String accountId, String delegateId, String capabilityId) {
    return persistence.createQuery(CapabilitySubjectPermission.class)
        .filter(CapabilitySubjectPermissionKeys.accountId, accountId)
        .filter(CapabilitySubjectPermissionKeys.delegateId, delegateId)
        .filter(CapabilitySubjectPermissionKeys.capabilityId, capabilityId)
        .get();
  }

  public CapabilityTaskSelectionDetails updateBlockingCheckIterations(String accountId, String taskSelectionDetailsId,
      List<Long> blockingCheckIterations, FindAndModifyOptions findAndModifyOptions) {
    Query<CapabilityTaskSelectionDetails> query =
        persistence.createQuery(CapabilityTaskSelectionDetails.class)
            .filter(CapabilityTaskSelectionDetailsKeys.accountId, accountId)
            .filter(CapabilityTaskSelectionDetailsKeys.uuid, taskSelectionDetailsId)
            .filter(CapabilityTaskSelectionDetailsKeys.blocked, true);

    UpdateOperations<CapabilityTaskSelectionDetails> updateOperations =
        persistence.createUpdateOperations(CapabilityTaskSelectionDetails.class);
    setUnset(updateOperations, CapabilityTaskSelectionDetailsKeys.blockingCheckIterations, blockingCheckIterations);

    return persistence.findAndModify(query, updateOperations, findAndModifyOptions);
  }

  public List<CapabilitySubjectPermission> getNotDeniedCapabilityPermissions(String accountId, String capabilityId) {
    Query<CapabilitySubjectPermission> query = persistence.createQuery(CapabilitySubjectPermission.class)
                                                   .filter(CapabilitySubjectPermissionKeys.accountId, accountId)
                                                   .filter(CapabilitySubjectPermissionKeys.capabilityId, capabilityId)
                                                   .field(CapabilitySubjectPermissionKeys.permissionResult)
                                                   .notEqual(PermissionResult.DENIED);

    return query.asList();
  }

  // TODO: find migrations that add TTL and examples of creating batch request, move all txn to batch request

  // Adds a new capability requirement into the capability dao, and initializes all permission entities
  // for the set of delegates specified here
  public String addCapabilityRequirement(CapabilityRequirement requirement, List<String> allDelegateIds) {
    Instant creationTime = Instant.now();
    checkState(!requirement.getAccountId().isEmpty());
    requirement.setValidUntil(Date.from(Instant.now().plus(Duration.ofHours(100))));
    String capabilityId = persistence.save(requirement);
    persistence.saveBatch(allDelegateIds.stream()
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

  public List<CapabilityRequirement> getAllCapabilityRequirements(String accountId) {
    return persistence.createQuery(CapabilityRequirement.class)
        .filter(CapabilityRequirementKeys.accountId, accountId)
        .asList();
  }
}
