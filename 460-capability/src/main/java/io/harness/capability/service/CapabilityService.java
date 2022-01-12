/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.capability.service;

import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CapabilityService {
  /**
   * Does the processing of {@link CapabilityRequirement} instance, by inserting or updating records in {@link
   * CapabilityRequirement}, {@link CapabilityTaskSelectionDetails} and {@link CapabilitySubjectPermission} collections
   *
   * @return list of ids of newly inserted {@link CapabilitySubjectPermission} records, requiring immediate capability
   *     check
   */
  List<String> processTaskCapabilityRequirement(CapabilityRequirement capabilityRequirement,
      CapabilityTaskSelectionDetails taskSelectionDetails, List<String> assignableDelegateIds);

  /**
   * Creates {@link CapabilityRequirement} instance based on provided delegate task related capability, scoping and
   * selector information
   */
  CapabilityRequirement buildCapabilityRequirement(String accountId, ExecutionCapability agentCapability);

  /**
   * Creates {@link CapabilityTaskSelectionDetails} instance based on provided capability and delegate task related
   * scoping and selector information.
   */
  CapabilityTaskSelectionDetails buildCapabilityTaskSelectionDetails(CapabilityRequirement capabilityRequirement,
      TaskGroup taskGroup, Map<String, String> taskSetupAbstractions, List<SelectorCapability> selectorCapabilities,
      List<String> assignableDelegateIds);

  /**
   * Generates task selection details identifier based on provided capability identifier and delegate task related
   * scoping and selector information. Generated identifier will represent unique combination of the all provided
   * information.
   */
  String generateCapabilityTaskSelectionId(String accountId, String capabilityId, TaskGroup taskGroup,
      Map<String, Set<String>> taskSelectors, Map<String, String> taskSetupAbstractions);

  /**
   * Generates capability identifier based on provided capability details. Generated identifier will represent unique
   * combination of the all provided information.
   */
  String generateCapabilityId(String accountId, CapabilityType capabilityType, String capabilityDescription);

  List<CapabilitySubjectPermission> getAllDelegatePermissions(
      String accountId, String delegateId, PermissionResult permissionResultFilter);

  List<CapabilityTaskSelectionDetails> getAllCapabilityTaskSelectionDetails(String accountId, String capabilityId);

  boolean deleteCapabilitySubjectPermission(String uuid);

  boolean deleteCapabilitySubjectPermission(String accountId, String delegateId, String capabilityId);

  Set<String> getCapableDelegateIds(String accountId, List<CapabilityRequirement> capabilityRequirements);

  List<CapabilitySubjectPermission> getPermissionsByIds(String accountId, Set<String> permissionIds);

  List<String> addCapabilityPermissions(CapabilityRequirement capabilityRequirement, List<String> allDelegateIds,
      PermissionResult result, boolean isCapabilityBlocked);

  List<CapabilityRequirement> getAllCapabilityRequirements(String accountId);

  List<CapabilitySubjectPermission> getAllCapabilityPermissions(
      String accountId, String capabilityId, PermissionResult permissionResultFilter);

  void resetDelegatePermissionCheckIterations(String accountId, String delegateId);

  List<CapabilityTaskSelectionDetails> getBlockedTaskSelectionDetails(String accountId);

  List<CapabilitySubjectPermission> getNotDeniedCapabilityPermissions(String accountId, String capabilityId);
}
