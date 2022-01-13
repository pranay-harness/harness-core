/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.capability.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CapabilityTestBase;
import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.CapabilitySubjectPermissionCrudObserver;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.capability.internal.CapabilityDao;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.observer.Subject;
import io.harness.rule.Owner;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.FindAndModifyOptions;

public class CapabilityServiceImplTest extends CapabilityTestBase {
  @Mock Subject<CapabilitySubjectPermissionCrudObserver> capSubjectPermissionTaskCrudSubject;
  @Mock CapabilityDao capabilityDao;
  @InjectMocks private CapabilityServiceImpl capabilityService;

  @Before
  public void setup() throws IllegalAccessException {
    FieldUtils.writeField(
        capabilityService, "capSubjectPermissionTaskCrudSubject", capSubjectPermissionTaskCrudSubject, true);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testProcessTaskCapabilityRequirementWithExistingSelectionDetails() {
    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();
    CapabilityTaskSelectionDetails taskSelectionDetails = buildCapabilityTaskSelectionDetails();
    taskSelectionDetails.setAccountId(capabilityRequirement.getAccountId());
    taskSelectionDetails.setCapabilityId(capabilityRequirement.getUuid());

    when(capabilityDao.upsert(eq(taskSelectionDetails), any(FindAndModifyOptions.class)))
        .thenReturn(taskSelectionDetails);

    List<String> insertedPermissionIds = capabilityService.processTaskCapabilityRequirement(
        capabilityRequirement, taskSelectionDetails, Collections.emptyList());

    verify(capabilityDao).upsert(eq(capabilityRequirement), any(FindAndModifyOptions.class));
    assertThat(insertedPermissionIds).isEmpty();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testProcessTaskCapabilityRequirementWithNewSelectionDetails() {
    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();
    CapabilityTaskSelectionDetails taskSelectionDetails = buildCapabilityTaskSelectionDetails();
    taskSelectionDetails.setAccountId(capabilityRequirement.getAccountId());
    taskSelectionDetails.setCapabilityId(capabilityRequirement.getUuid());

    when(capabilityDao.upsert(eq(taskSelectionDetails), any(FindAndModifyOptions.class))).thenReturn(null);

    CapabilitySubjectPermission existingPermission =
        buildCapabilitySubjectPermission(capabilityRequirement.getAccountId(), generateUuid(),
            capabilityRequirement.getUuid(), PermissionResult.ALLOWED);

    when(capabilityDao.getAllCapabilityPermissions(
             capabilityRequirement.getAccountId(), capabilityRequirement.getUuid(), null))
        .thenReturn(Collections.singletonList(existingPermission));

    List<String> insertedPermissionIds = Collections.singletonList("permissionId");
    when(capabilityDao.addCapabilityPermissions(
             eq(capabilityRequirement), any(List.class), eq(PermissionResult.UNCHECKED)))
        .thenReturn(insertedPermissionIds);

    List<String> outputPermissionIds = capabilityService.processTaskCapabilityRequirement(
        capabilityRequirement, taskSelectionDetails, Collections.singletonList("delegateId"));

    verify(capabilityDao).upsert(eq(capabilityRequirement), any(FindAndModifyOptions.class));
    assertThat(outputPermissionIds).hasSize(1);
    verify(capSubjectPermissionTaskCrudSubject)
        .fireInform(any(), eq(capabilityRequirement.getAccountId()), eq("delegateId"));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testAddCapabilityPermissionsShouldNotInformObservers() {
    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();
    List<String> delegateIds = Arrays.asList("delegate1", "delegate2");

    List<String> insertedPermissionIds = Arrays.asList("permissionId1", "permissionId2");

    when(capabilityDao.addCapabilityPermissions(
             eq(capabilityRequirement), eq(delegateIds), eq(PermissionResult.UNCHECKED)))
        .thenReturn(insertedPermissionIds);

    // Test with blocking false and inserted permissions
    List<String> outputPermissionIds = capabilityService.addCapabilityPermissions(
        capabilityRequirement, delegateIds, PermissionResult.UNCHECKED, false);

    assertThat(outputPermissionIds).hasSize(2);
    assertThat(outputPermissionIds).containsExactlyInAnyOrder("permissionId1", "permissionId2");
    verify(capSubjectPermissionTaskCrudSubject, never()).fireInform(any(), any(), any());

    // Test with blocking true and no inserted permissions
    when(capabilityDao.addCapabilityPermissions(
             eq(capabilityRequirement), eq(delegateIds), eq(PermissionResult.UNCHECKED)))
        .thenReturn(Collections.emptyList());
    outputPermissionIds = capabilityService.addCapabilityPermissions(
        capabilityRequirement, delegateIds, PermissionResult.UNCHECKED, true);

    assertThat(outputPermissionIds).hasSize(0);
    verify(capSubjectPermissionTaskCrudSubject, never()).fireInform(any(), any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testAddCapabilityPermissionsShouldInformObservers() {
    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();
    List<String> delegateIds = Arrays.asList("delegate1", "delegate2");

    List<String> insertedPermissionIds = Arrays.asList("permissionId1", "permissionId2");

    when(capabilityDao.addCapabilityPermissions(
             eq(capabilityRequirement), eq(delegateIds), eq(PermissionResult.UNCHECKED)))
        .thenReturn(insertedPermissionIds);

    // Test with blocking false and inserted permissions
    List<String> outputPermissionIds = capabilityService.addCapabilityPermissions(
        capabilityRequirement, delegateIds, PermissionResult.UNCHECKED, true);

    assertThat(outputPermissionIds).hasSize(2);
    assertThat(outputPermissionIds).containsExactlyInAnyOrder("permissionId1", "permissionId2");
    verify(capSubjectPermissionTaskCrudSubject)
        .fireInform(any(), eq(capabilityRequirement.getAccountId()), eq("delegate1"));
    verify(capSubjectPermissionTaskCrudSubject)
        .fireInform(any(), eq(capabilityRequirement.getAccountId()), eq("delegate2"));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGenerateCapabilityId() {
    assertThat(capabilityService.generateCapabilityId("accountId", CapabilityType.HTTP, "capabilityDescription"))
        .isNotBlank();
    assertThat(capabilityService.generateCapabilityId("accountId", CapabilityType.HTTP, null)).isNotBlank();
    assertThat(capabilityService.generateCapabilityId("accountId", null, null)).isNotBlank();
    assertThat(capabilityService.generateCapabilityId(null, null, null)).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGenerateCapabilityTaskSelectionId() {
    Map<String, Set<String>> taskSelectors = new HashMap<>();
    taskSelectors.put("key5", Collections.singleton("value5"));

    Map<String, String> taskSetupAbstractions = new HashMap<>();
    taskSetupAbstractions.put("key6", "value6");

    assertThat(capabilityService.generateCapabilityTaskSelectionId(
                   "accountId", "capabilityId", TaskGroup.HTTP, taskSelectors, taskSetupAbstractions))
        .isNotBlank();
    assertThat(capabilityService.generateCapabilityTaskSelectionId(
                   "accountId", "capabilityId", TaskGroup.HTTP, taskSelectors, null))
        .isNotBlank();
    assertThat(
        capabilityService.generateCapabilityTaskSelectionId("accountId", "capabilityId", TaskGroup.HTTP, null, null))
        .isNotBlank();
    assertThat(capabilityService.generateCapabilityTaskSelectionId("accountId", "capabilityId", null, null, null))
        .isNotBlank();
    assertThat(capabilityService.generateCapabilityTaskSelectionId("accountId", null, null, null, null)).isNotBlank();
    assertThat(capabilityService.generateCapabilityTaskSelectionId(null, null, null, null, null)).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testBuildCapabilityRequirement() {
    String accountId = generateUuid();

    ExecutionCapability executionCapability =
        SelectorCapability.builder().selectors(Stream.of("a").collect(Collectors.toSet())).build();

    assertThat(capabilityService.buildCapabilityRequirement(null, executionCapability)).isNull();
    assertThat(capabilityService.buildCapabilityRequirement(accountId, null)).isNull();
    assertThat(capabilityService.buildCapabilityRequirement(accountId, executionCapability)).isNull();

    ExecutionCapability agentCapability = HttpConnectionExecutionCapability.builder().url("https://google.com").build();

    CapabilityRequirement capabilityRequirement =
        capabilityService.buildCapabilityRequirement(accountId, agentCapability);
    assertThat(capabilityRequirement).isNotNull();
    assertThat(capabilityRequirement.getAccountId()).isEqualTo(accountId);
    assertThat(capabilityRequirement.getUuid()).isNotBlank();
    assertThat(capabilityRequirement.getValidUntil()).isNotNull();
    assertThat(capabilityRequirement.getCapabilityType()).isEqualTo(agentCapability.getCapabilityType().name());
    assertThat(capabilityRequirement.getCapabilityParameters()).isNotNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testBuildCapabilityTaskSelectionDetails() {
    String delegateId = generateUuid();

    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();

    List<SelectorCapability> selectorCapabilities =
        Arrays.asList(SelectorCapability.builder()
                          .selectorOrigin("TASK_SELECTORS")
                          .selectors(Stream.of("a").collect(Collectors.toSet()))
                          .build(),
            SelectorCapability.builder()
                .selectorOrigin("TASK_SELECTORS")
                .selectors(Stream.of("b").collect(Collectors.toSet()))
                .build());

    Map<String, Set<String>> capabilityTaskSelectors = new HashMap<>();
    capabilityTaskSelectors.put("TASK_SELECTORS", Stream.of("a", "b").collect(Collectors.toSet()));

    Map<String, String> taskSetupAbstractions = new HashMap<>();
    taskSetupAbstractions.put("key6", "value6");

    List<CapabilitySubjectPermission> validAllowedPermissions =
        Collections.singletonList(CapabilitySubjectPermission.builder().delegateId(generateUuid()).build());
    when(capabilityDao.getValidAllowedCapabilityPermissions(
             capabilityRequirement.getAccountId(), capabilityRequirement.getUuid()))
        .thenReturn(validAllowedPermissions);

    // Test with all arguments
    CapabilityTaskSelectionDetails taskSelectionDetails = capabilityService.buildCapabilityTaskSelectionDetails(
        capabilityRequirement, TaskGroup.HTTP, taskSetupAbstractions, selectorCapabilities, Arrays.asList(delegateId));

    assertThat(taskSelectionDetails).isNotNull();
    assertThat(taskSelectionDetails.getAccountId()).isEqualTo(capabilityRequirement.getAccountId());
    assertThat(taskSelectionDetails.getUuid()).isNotBlank();
    assertThat(taskSelectionDetails.getCapabilityId()).isEqualTo(capabilityRequirement.getUuid());
    assertThat(taskSelectionDetails.getTaskGroup()).isEqualTo(TaskGroup.HTTP);
    assertThat(taskSelectionDetails.getTaskSetupAbstractions()).isEqualTo(taskSetupAbstractions);
    assertThat(taskSelectionDetails.getValidUntil()).isEqualTo(capabilityRequirement.getValidUntil());
    assertThat(taskSelectionDetails.isBlocked()).isEqualTo(true);
    assertThat(taskSelectionDetails.getTaskSelectors()).isEqualTo(capabilityTaskSelectors);

    // Test with partial arguments
    taskSelectionDetails = capabilityService.buildCapabilityTaskSelectionDetails(
        capabilityRequirement, TaskGroup.HTTP, null, null, Collections.emptyList());

    assertThat(taskSelectionDetails).isNotNull();
    assertThat(taskSelectionDetails.getAccountId()).isEqualTo(capabilityRequirement.getAccountId());
    assertThat(taskSelectionDetails.getUuid()).isNotBlank();
    assertThat(taskSelectionDetails.getCapabilityId()).isEqualTo(capabilityRequirement.getUuid());
    assertThat(taskSelectionDetails.getTaskGroup()).isEqualTo(TaskGroup.HTTP);
    assertThat(taskSelectionDetails.getTaskSetupAbstractions()).isEqualTo(null);
    assertThat(taskSelectionDetails.getValidUntil()).isEqualTo(capabilityRequirement.getValidUntil());
    assertThat(taskSelectionDetails.isBlocked()).isEqualTo(true);
    assertThat(taskSelectionDetails.getTaskSelectors()).isEqualTo(null);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetAllDelegatePermissions() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    PermissionResult permissionResultFilter = PermissionResult.ALLOWED;

    List<CapabilitySubjectPermission> permissionList =
        Collections.singletonList(CapabilitySubjectPermission.builder().build());
    when(capabilityDao.getAllDelegatePermissions(accountId, delegateId, permissionResultFilter))
        .thenReturn(permissionList);

    assertThat(capabilityService.getAllDelegatePermissions(accountId, delegateId, permissionResultFilter))
        .isEqualTo(permissionList);
    verify(capabilityDao).getAllDelegatePermissions(accountId, delegateId, permissionResultFilter);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetAllCapabilityTaskSelectionDetails() {
    String accountId = generateUuid();
    String capabilityId = generateUuid();

    List<CapabilityTaskSelectionDetails> taskSelectionDetailsList =
        Collections.singletonList(CapabilityTaskSelectionDetails.builder().build());
    when(capabilityDao.getAllCapabilityTaskSelectionDetails(accountId, capabilityId))
        .thenReturn(taskSelectionDetailsList);

    assertThat(capabilityService.getAllCapabilityTaskSelectionDetails(accountId, capabilityId))
        .isEqualTo(taskSelectionDetailsList);
    verify(capabilityDao).getAllCapabilityTaskSelectionDetails(accountId, capabilityId);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDeleteCapabilitySubjectPermissionById() {
    String permissionId = generateUuid();
    when(capabilityDao.deleteCapabilitySubjectPermission(permissionId)).thenReturn(true);
    assertThat(capabilityService.deleteCapabilitySubjectPermission(permissionId)).isTrue();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDeleteCapabilitySubjectPermission() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    String capabilityId = generateUuid();

    when(capabilityDao.deleteCapabilitySubjectPermission(accountId, delegateId, capabilityId)).thenReturn(true);
    assertThat(capabilityService.deleteCapabilitySubjectPermission(accountId, delegateId, capabilityId)).isTrue();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetPermissionsByIds() {
    String accountId = generateUuid();
    String permissionId1 = generateUuid();
    Set<String> permissionIds = Collections.singleton(permissionId1);

    List<CapabilitySubjectPermission> permissionList =
        Collections.singletonList(CapabilitySubjectPermission.builder().build());
    when(capabilityDao.getPermissionsByIds(accountId, permissionIds)).thenReturn(permissionList);

    assertThat(capabilityService.getPermissionsByIds(accountId, permissionIds)).isEqualTo(permissionList);
    verify(capabilityDao).getPermissionsByIds(accountId, permissionIds);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetAllCapabilityRequirements() {
    String accountId = generateUuid();

    List<CapabilityRequirement> requirementList = Collections.singletonList(CapabilityRequirement.builder().build());
    when(capabilityDao.getAllCapabilityRequirements(accountId)).thenReturn(requirementList);

    assertThat(capabilityService.getAllCapabilityRequirements(accountId)).isEqualTo(requirementList);
    verify(capabilityDao).getAllCapabilityRequirements(accountId);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetAllCapabilityPermissions() {
    String accountId = generateUuid();
    String capabilityId = generateUuid();
    PermissionResult permissionResultFilter = PermissionResult.ALLOWED;

    List<CapabilitySubjectPermission> permissionList =
        Collections.singletonList(CapabilitySubjectPermission.builder().build());
    when(capabilityDao.getAllCapabilityPermissions(accountId, capabilityId, permissionResultFilter))
        .thenReturn(permissionList);

    assertThat(capabilityService.getAllCapabilityPermissions(accountId, capabilityId, permissionResultFilter))
        .isEqualTo(permissionList);
    verify(capabilityDao).getAllCapabilityPermissions(accountId, capabilityId, permissionResultFilter);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetBlockedTaskSelectionDetails() {
    String accountId = generateUuid();

    List<CapabilityTaskSelectionDetails> taskSelectionDetailsList =
        Collections.singletonList(CapabilityTaskSelectionDetails.builder().build());
    when(capabilityDao.getBlockedTaskSelectionDetails(accountId)).thenReturn(taskSelectionDetailsList);

    assertThat(capabilityService.getBlockedTaskSelectionDetails(accountId)).isEqualTo(taskSelectionDetailsList);
    verify(capabilityDao).getBlockedTaskSelectionDetails(accountId);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetNotDeniedCapabilityPermissions() {
    String accountId = generateUuid();
    String capabilityId = generateUuid();

    List<CapabilitySubjectPermission> permissionList =
        Collections.singletonList(CapabilitySubjectPermission.builder().build());
    when(capabilityDao.getNotDeniedCapabilityPermissions(accountId, capabilityId)).thenReturn(permissionList);

    assertThat(capabilityService.getNotDeniedCapabilityPermissions(accountId, capabilityId)).isEqualTo(permissionList);
    verify(capabilityDao).getNotDeniedCapabilityPermissions(accountId, capabilityId);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetCapableDelegateIds() {
    String accountId = generateUuid();
    String delegateId1 = generateUuid();
    String delegateId2 = generateUuid();

    CapabilityRequirement capabilityRequirement1 = buildCapabilityRequirement();
    capabilityRequirement1.setAccountId(accountId);
    CapabilityRequirement capabilityRequirement2 = buildCapabilityRequirement();
    capabilityRequirement2.setAccountId(accountId);

    List<CapabilitySubjectPermission> validAllowedPermissions1 =
        Collections.singletonList(CapabilitySubjectPermission.builder().delegateId(delegateId1).build());
    when(capabilityDao.getValidAllowedCapabilityPermissions(accountId, capabilityRequirement1.getUuid()))
        .thenReturn(validAllowedPermissions1);

    List<CapabilitySubjectPermission> validAllowedPermissions2 =
        Arrays.asList(CapabilitySubjectPermission.builder().delegateId(delegateId1).build(),
            CapabilitySubjectPermission.builder().delegateId(delegateId2).build());
    when(capabilityDao.getValidAllowedCapabilityPermissions(accountId, capabilityRequirement2.getUuid()))
        .thenReturn(validAllowedPermissions2);

    Set<String> capableDelegateIds = capabilityService.getCapableDelegateIds(
        accountId, Arrays.asList(capabilityRequirement1, capabilityRequirement2));

    assertThat(capableDelegateIds).containsExactlyInAnyOrder(delegateId1);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testResetDelegatePermissionCheckIterations() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    List<CapabilitySubjectPermission> allDelegatePermissions =
        Collections.singletonList(CapabilitySubjectPermission.builder().capabilityId(generateUuid()).build());
    when(capabilityDao.getAllDelegatePermissions(accountId, delegateId, null)).thenReturn(allDelegatePermissions);

    List<CapabilityTaskSelectionDetails> taskSelectionDetailsList =
        Arrays.asList(CapabilityTaskSelectionDetails.builder().uuid("blockedId").blocked(true).build(),
            CapabilityTaskSelectionDetails.builder().uuid("nonBlockedId").blocked(false).build());
    when(capabilityDao.getTaskSelectionDetails(eq(accountId), any(Set.class))).thenReturn(taskSelectionDetailsList);

    capabilityService.resetDelegatePermissionCheckIterations(accountId, delegateId);

    verify(capabilityDao)
        .updateBlockingCheckIterations(
            eq(accountId), eq("blockedId"), any(List.class), any(FindAndModifyOptions.class));
  }

  private CapabilityRequirement buildCapabilityRequirement() {
    return CapabilityRequirement.builder()
        .accountId(generateUuid())
        .uuid(generateUuid())
        .validUntil(Date.from(Instant.now().plus(Duration.ofDays(30))))
        .capabilityType(CapabilityType.HTTP.name())
        .capabilityParameters(CapabilityParameters.getDefaultInstance())
        .build();
  }

  private CapabilityTaskSelectionDetails buildCapabilityTaskSelectionDetails() {
    Map<String, Set<String>> taskSelectors = new HashMap<>();
    taskSelectors.put("key1", Collections.singleton("value1"));

    Map<String, String> taskSetupAbstractions = new HashMap<>();
    taskSetupAbstractions.put("key2", "value2");

    return CapabilityTaskSelectionDetails.builder()
        .uuid(generateUuid())
        .accountId(generateUuid())
        .capabilityId(generateUuid())
        .taskGroup(TaskGroup.HTTP)
        .taskSelectors(taskSelectors)
        .taskSetupAbstractions(taskSetupAbstractions)
        .blocked(true)
        .validUntil(Date.from(Instant.now().plus(Duration.ofDays(30))))
        .build();
  }

  private CapabilitySubjectPermission buildCapabilitySubjectPermission(
      String accountId, String delegateId, String capabilityId, PermissionResult permissionResult) {
    return CapabilitySubjectPermission.builder()
        .uuid(generateUuid())
        .accountId(accountId)
        .delegateId(delegateId)
        .capabilityId(capabilityId)
        .maxValidUntil(120000)
        .revalidateAfter(180000)
        .validUntil(Date.from(Instant.now().plus(Duration.ofDays(30))))
        .permissionResult(permissionResult)
        .build();
  }
}
