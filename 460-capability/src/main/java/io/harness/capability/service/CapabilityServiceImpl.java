/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.capability.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.CapabilitySubjectPermissionCrudObserver;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.capability.CapabilityTaskSelectionDetails.CapabilityTaskSelectionDetailsBuilder;
import io.harness.capability.CapabilityTaskSelectionDetails.CapabilityTaskSelectionDetailsKeys;
import io.harness.capability.internal.CapabilityAttributes;
import io.harness.capability.internal.CapabilityDao;
import io.harness.delegate.beans.CapabilityProtoConverter;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.observer.Subject;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;

@Singleton
@Slf4j
public class CapabilityServiceImpl implements CapabilityService {
  @Inject CapabilityDao capabilityDao;

  @Getter
  private Subject<CapabilitySubjectPermissionCrudObserver> capSubjectPermissionTaskCrudSubject = new Subject<>();

  @Override
  public List<String> processTaskCapabilityRequirement(CapabilityRequirement capabilityRequirement,
      CapabilityTaskSelectionDetails taskSelectionDetails, List<String> assignableDelegateIds) {
    // Upsert capability record
    capabilityDao.upsert(capabilityRequirement, HPersistence.upsertReturnOldOptions);

    // Upsert capability task selection details and add missing delegate permissions, if new record was added
    CapabilityTaskSelectionDetails existingSelectionDetails =
        capabilityDao.upsert(taskSelectionDetails, HPersistence.upsertReturnOldOptions);

    List<String> missingDelegateIds = new ArrayList<>();
    if (existingSelectionDetails == null) {
      // Filter assignable delegates to identify only those that are missing and insert permission records for them
      List<String> existingPermissionDelegateIds =
          capabilityDao
              .getAllCapabilityPermissions(capabilityRequirement.getAccountId(), capabilityRequirement.getUuid(), null)
              .stream()
              .map(CapabilitySubjectPermission::getDelegateId)
              .collect(Collectors.toList());

      missingDelegateIds = assignableDelegateIds.stream()
                               .filter(delegateId -> !existingPermissionDelegateIds.contains(delegateId))
                               .collect(Collectors.toList());

      return addCapabilityPermissions(
          capabilityRequirement, missingDelegateIds, PermissionResult.UNCHECKED, taskSelectionDetails.isBlocked());
    }

    return Collections.emptyList();
  }

  @Override
  public List<String> addCapabilityPermissions(CapabilityRequirement capabilityRequirement, List<String> allDelegateIds,
      PermissionResult result, boolean isCapabilityBlocked) {
    List<String> insertedPermissionIds =
        capabilityDao.addCapabilityPermissions(capabilityRequirement, allDelegateIds, result);

    if (isCapabilityBlocked && isNotEmpty(insertedPermissionIds)) {
      for (String delegateId : allDelegateIds) {
        capSubjectPermissionTaskCrudSubject.fireInform(
            CapabilitySubjectPermissionCrudObxxxxxxxx:onBlockingPermissionsCreated, capabilityRequirement.getAccountId(),
            delegateId);
      }
    }

    return insertedPermissionIds;
  }

  @Override
  public CapabilityRequirement buildCapabilityRequirement(String accountId, ExecutionCapability agentCapability) {
    if (isBlank(accountId) || agentCapability == null) {
      return null;
    }

    CapabilityParameters capabilityParameters = CapabilityProtoConverter.toProto(agentCapability);

    if (capabilityParameters == null) {
      log.warn("Could not generate Capability Parameters for capability type {} and description {}",
          agentCapability.getCapabilityType(), agentCapability.fetchCapabilityBasis());
      return null;
    }

    String capabilityDescription = CapabilityAttributes.getCapabilityDescriptor(capabilityParameters);
    String capabilityId = generateCapabilityId(accountId, agentCapability.getCapabilityType(), capabilityDescription);

    if (isBlank(capabilityId)) {
      log.warn("Could not generate Capability Id for capability type {} and description {}",
          agentCapability.getCapabilityType(), capabilityDescription);
      return null;
    }

    return CapabilityRequirement.builder()
        .uuid(capabilityId)
        .accountId(accountId)
        .validUntil(Date.from(Instant.now().plus(Duration.ofDays(30))))
        .capabilityType(agentCapability.getCapabilityType().name())
        .capabilityParameters(capabilityParameters)
        .build();
  }

  @Override
  public CapabilityTaskSelectionDetails buildCapabilityTaskSelectionDetails(CapabilityRequirement capabilityRequirement,
      TaskGroup taskGroup, Map<String, String> taskSetupAbstractions, List<SelectorCapability> selectorCapabilities,
      List<String> assignableDelegateIds) {
    CapabilityTaskSelectionDetailsBuilder builder = CapabilityTaskSelectionDetails.builder()
                                                        .accountId(capabilityRequirement.getAccountId())
                                                        .capabilityId(capabilityRequirement.getUuid())
                                                        .taskGroup(taskGroup)
                                                        .taskSetupAbstractions(taskSetupAbstractions)
                                                        .validUntil(capabilityRequirement.getValidUntil());

    // If there are no capable delegates, mark record as blocked
    Set<String> capableDelegateIds =
        capabilityDao
            .getValidAllowedCapabilityPermissions(capabilityRequirement.getAccountId(), capabilityRequirement.getUuid())
            .stream()
            .map(CapabilitySubjectPermission::getDelegateId)
            .collect(Collectors.toSet());

    if (!CollectionUtils.containsAny(capableDelegateIds, assignableDelegateIds)) {
      builder.blocked(true);
    }

    Map<String, Set<String>> taskSelectors = new HashMap<>();
    if (selectorCapabilities != null) {
      for (SelectorCapability selectorCapability : selectorCapabilities) {
        if (taskSelectors.get(selectorCapability.getSelectorOrigin()) == null) {
          taskSelectors.put(selectorCapability.getSelectorOrigin(), selectorCapability.getSelectors());
        } else {
          taskSelectors.get(selectorCapability.getSelectorOrigin()).addAll(selectorCapability.getSelectors());
        }
      }

      builder.taskSelectors(taskSelectors);
    }

    String capabilityTaskSelectionId = generateCapabilityTaskSelectionId(capabilityRequirement.getAccountId(),
        capabilityRequirement.getUuid(), taskGroup, taskSelectors, taskSetupAbstractions);

    if (isNotBlank(capabilityTaskSelectionId)) {
      builder.uuid(capabilityTaskSelectionId);
      return builder.build();
    }

    return null;
  }

  @Override
  public String generateCapabilityTaskSelectionId(String accountId, String capabilityId, TaskGroup taskGroup,
      Map<String, Set<String>> taskSelectors, Map<String, String> taskSetupAbstractions) {
    if (isBlank(accountId) && isBlank(capabilityId) && taskGroup == null && taskSelectors == null
        && taskSetupAbstractions == null) {
      return null;
    }

    StringBuilder setupAbstractionsKey = new StringBuilder();
    if (isNotEmpty(taskSetupAbstractions)) {
      for (Map.Entry<String, String> entry : taskSetupAbstractions.entrySet()) {
        setupAbstractionsKey.append(entry.getKey()).append(entry.getValue());
      }
    }

    StringBuilder selectorsKey = new StringBuilder();
    if (isNotEmpty(taskSelectors)) {
      for (Map.Entry<String, Set<String>> entry : taskSelectors.entrySet()) {
        selectorsKey.append(entry.getKey()).append(String.join("", entry.getValue()));
      }
    }

    String recordKey = accountId + capabilityId + (taskGroup != null ? taskGroup.toString() : "")
        + setupAbstractionsKey.toString() + selectorsKey.toString();

    byte[] bytes = recordKey.getBytes();
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hashInBytes = md.digest(bytes);
      String identifier = Base64.encodeBase64URLSafeString(hashInBytes);
      log.info("Generated task selector identifier {} from key {}", identifier, recordKey);

      return identifier;
    } catch (NoSuchAlgorithmException e) {
      log.error("Unexpected exception occurred while trying to generate CapabilityRequirement Uuid.");
      return null;
    }
  }

  @Override
  public String generateCapabilityId(String accountId, CapabilityType capabilityType, String capabilityDescription) {
    if (isBlank(accountId) && capabilityType == null && isBlank(capabilityDescription)) {
      return null;
    }

    String recordKey = accountId + (capabilityType != null ? capabilityType.toString() : null) + capabilityDescription;

    byte[] bytes = recordKey.getBytes();
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] hashInBytes = md.digest(bytes);
      String identifier = Base64.encodeBase64URLSafeString(hashInBytes);
      log.info("Generated capability identifier {} from key {}", identifier, recordKey);

      return identifier;
    } catch (NoSuchAlgorithmException e) {
      log.error("Unexpected exception occurred while trying to generate CapabilityRequirement Uuid.");
      return null;
    }
  }

  @Override
  public List<CapabilitySubjectPermission> getAllDelegatePermissions(
      String accountId, String delegateId, PermissionResult permissionResultFilter) {
    return capabilityDao.getAllDelegatePermissions(accountId, delegateId, permissionResultFilter);
  }

  @Override
  public List<CapabilityTaskSelectionDetails> getAllCapabilityTaskSelectionDetails(
      String accountId, String capabilityId) {
    return capabilityDao.getAllCapabilityTaskSelectionDetails(accountId, capabilityId);
  }

  @Override
  public boolean deleteCapabilitySubjectPermission(String uuid) {
    return capabilityDao.deleteCapabilitySubjectPermission(uuid);
  }

  @Override
  public boolean deleteCapabilitySubjectPermission(String accountId, String delegateId, String capabilityId) {
    return capabilityDao.deleteCapabilitySubjectPermission(accountId, delegateId, capabilityId);
  }

  @Override
  public Set<String> getCapableDelegateIds(String accountId, List<CapabilityRequirement> capabilityRequirements) {
    Set<String> capableDelegateIds =
        capabilityDao.getValidAllowedCapabilityPermissions(accountId, capabilityRequirements.get(0).getUuid())
            .stream()
            .map(CapabilitySubjectPermission::getDelegateId)
            .collect(Collectors.toSet());

    for (int i = 1; i < capabilityRequirements.size(); i++) {
      capableDelegateIds.retainAll(
          capabilityDao.getValidAllowedCapabilityPermissions(accountId, capabilityRequirements.get(i).getUuid())
              .stream()
              .map(CapabilitySubjectPermission::getDelegateId)
              .collect(Collectors.toSet()));
    }

    return capableDelegateIds;
  }

  @Override
  public List<CapabilitySubjectPermission> getPermissionsByIds(String accountId, Set<String> permissionIds) {
    return capabilityDao.getPermissionsByIds(accountId, permissionIds);
  }

  @Override
  public List<CapabilityRequirement> getAllCapabilityRequirements(String accountId) {
    return capabilityDao.getAllCapabilityRequirements(accountId);
  }

  @Override
  public List<CapabilitySubjectPermission> getAllCapabilityPermissions(
      String accountId, String capabilityId, PermissionResult permissionResultFilter) {
    return capabilityDao.getAllCapabilityPermissions(accountId, capabilityId, permissionResultFilter);
  }

  @Override
  public void resetDelegatePermissionCheckIterations(String accountId, String delegateId) {
    Set<String> allDelegateCapabilities = capabilityDao.getAllDelegatePermissions(accountId, delegateId, null)
                                              .stream()
                                              .map(CapabilitySubjectPermission::getCapabilityId)
                                              .collect(Collectors.toSet());

    List<CapabilityTaskSelectionDetails> blockedTaskSelectionDetailsList =
        capabilityDao.getTaskSelectionDetails(accountId, allDelegateCapabilities)
            .stream()
            .filter(item -> item.isBlocked())
            .collect(Collectors.toList());

    for (CapabilityTaskSelectionDetails blockedSelectionDetails : blockedTaskSelectionDetailsList) {
      // Obtain new iterations that would schedule record for immediate execution. 10 seconds are being deducted from
      // current time in order to get the first iteration as close as possible to the current time
      blockedSelectionDetails.setBlockingCheckIterations(null);
      List<Long> newCheckIterations = blockedSelectionDetails.recalculateNextIterations(
          CapabilityTaskSelectionDetailsKeys.blockingCheckIterations, true, System.currentTimeMillis() - 10000L);

      capabilityDao.updateBlockingCheckIterations(
          accountId, blockedSelectionDetails.getUuid(), newCheckIterations, HPersistence.returnOldOptions);
    }
  }

  @Override
  public List<CapabilityTaskSelectionDetails> getBlockedTaskSelectionDetails(String accountId) {
    return capabilityDao.getBlockedTaskSelectionDetails(accountId);
  }

  @Override
  public List<CapabilitySubjectPermission> getNotDeniedCapabilityPermissions(String accountId, String capabilityId) {
    return capabilityDao.getNotDeniedCapabilityPermissions(accountId, capabilityId);
  }
}
