/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.capability.internal;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HPersistence.returnNewOptions;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.MATT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CapabilityTestBase;
import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class CapabilityDaoTest extends CapabilityTestBase {
  @InjectMocks @Inject private CapabilityDao capabilityDao;

  @Inject private HPersistence persistence;

  private final String ACCOUNT_ID = "test-account-id";
  private final String DELEGATE_ID_1 = "delegate-id-1";
  private final String DELEGATE_ID_2 = "delegate-id-2";
  private final String DELEGATE_ID_3 = "delegate-id-3";
  private final String CAPABILITY_ID_1 = "capability-id-1";
  private final String CAPABILITY_ID_2 = "capability-id-2";

  @Before
  public void setUpCapabilities() {
    capabilityDao.addCapabilityRequirement(CapabilityRequirement.builder()
                                               .accountId(ACCOUNT_ID)
                                               .uuid(CAPABILITY_ID_1)
                                               .capabilityParameters(CapabilityParameters.getDefaultInstance())
                                               .build(),
        Arrays.asList(DELEGATE_ID_1, DELEGATE_ID_2, DELEGATE_ID_3));
    capabilityDao.addCapabilityRequirement(CapabilityRequirement.builder()
                                               .accountId(ACCOUNT_ID)
                                               .uuid(CAPABILITY_ID_2)
                                               .capabilityParameters(CapabilityParameters.getDefaultInstance())
                                               .build(),
        Arrays.asList(DELEGATE_ID_1, DELEGATE_ID_2, DELEGATE_ID_3));
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testInitializedCapabilities() {
    assertThat(capabilityDao.getAllCapabilityRequirements(ACCOUNT_ID)).hasSize(2);
    List<CapabilitySubjectPermission> subjectPermissions =
        capabilityDao.getAllDelegatePermission(ACCOUNT_ID, Arrays.asList(DELEGATE_ID_1, DELEGATE_ID_2, DELEGATE_ID_3));
    assertThat(subjectPermissions).hasSize(6);
    for (CapabilitySubjectPermission permission : subjectPermissions) {
      assertThat(permission.getPermissionResult()).isEqualTo(PermissionResult.UNCHECKED);
    }
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testValidateCapabilities() {
    capabilityDao.updateCapabilityPermission(
        ACCOUNT_ID, CAPABILITY_ID_1, Arrays.asList(DELEGATE_ID_1, DELEGATE_ID_2), PermissionResult.ALLOWED);
    List<CapabilitySubjectPermission> subjectPermissions =
        capabilityDao.getAllCapabilityPermission(ACCOUNT_ID, Arrays.asList(CAPABILITY_ID_1));
    CapabilitySubjectPermission permission1 = CapabilitySubjectPermission.builder().build();
    CapabilitySubjectPermission permission3 = CapabilitySubjectPermission.builder().build();
    for (CapabilitySubjectPermission permission : subjectPermissions) {
      if (permission.getDelegateId().equals(DELEGATE_ID_1)) {
        permission1 = permission;
      }
      if (permission.getDelegateId().equals(DELEGATE_ID_3)) {
        permission3 = permission;
      }
    }
    assertThat(permission1.getValidUntil().toInstant().toEpochMilli())
        .isGreaterThan(permission3.getValidUntil().toInstant().toEpochMilli());
    assertThat(permission1.getPermissionResult()).isEqualTo(PermissionResult.ALLOWED);
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testUpdateRequirement() {
    CapabilityRequirement testedRequirement = CapabilityRequirement.builder().build();
    for (CapabilityRequirement requirement : capabilityDao.getAllCapabilityRequirements(ACCOUNT_ID)) {
      if (requirement.getUuid().equals(CAPABILITY_ID_1)) {
        testedRequirement = requirement;
        break;
      }
    }
    Instant validUntil = testedRequirement.getValidUntil().toInstant();
    capabilityDao.updateCapabilityRequirement(ACCOUNT_ID, CAPABILITY_ID_1);

    for (CapabilityRequirement requirement : capabilityDao.getAllCapabilityRequirements(ACCOUNT_ID)) {
      if (requirement.getUuid().equals(CAPABILITY_ID_1)) {
        testedRequirement = requirement;
        break;
      }
    }
    assertThat(testedRequirement.getValidUntil().toInstant().toEpochMilli()).isGreaterThan(validUntil.toEpochMilli());
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testRemoveCapability() {
    capabilityDao.removeCapabilityRequirement(ACCOUNT_ID, CAPABILITY_ID_1);

    List<CapabilityRequirement> capabilityRequirements = capabilityDao.getAllCapabilityRequirements(ACCOUNT_ID);
    assertThat(capabilityRequirements.get(0).getUuid()).isEqualTo(CAPABILITY_ID_2);

    List<CapabilitySubjectPermission> capabilityPermissions =
        capabilityDao.getAllCapabilityPermission(ACCOUNT_ID, Arrays.asList(CAPABILITY_ID_1, CAPABILITY_ID_2));
    assertThat(capabilityPermissions).hasSize(3);
    for (CapabilitySubjectPermission permission : capabilityPermissions) {
      assertThat(permission.getCapabilityId()).isEqualTo(CAPABILITY_ID_2);
    }
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testUpsertCapability() {
    // Test insert
    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();
    CapabilityRequirement insertedCapability = capabilityDao.upsert(capabilityRequirement, upsertReturnNewOptions);
    assertThat(insertedCapability).isNotNull();
    assertThat(insertedCapability.getAccountId()).isEqualTo(capabilityRequirement.getAccountId());
    assertThat(insertedCapability.getUuid()).isEqualTo(capabilityRequirement.getUuid());
    assertThat(insertedCapability.getValidUntil()).isEqualTo(capabilityRequirement.getValidUntil());
    assertThat(insertedCapability.getCapabilityType()).isEqualTo(capabilityRequirement.getCapabilityType());
    assertThat(insertedCapability.getCapabilityParameters()).isEqualTo(capabilityRequirement.getCapabilityParameters());

    // Test update
    insertedCapability.setValidUntil(Date.from(Instant.now().plus(Duration.ofDays(10))));
    insertedCapability.setCapabilityType(CapabilityType.SFTP.name());
    insertedCapability.setCapabilityParameters(CapabilityParameters.getDefaultInstance());

    CapabilityRequirement updatedCapability = capabilityDao.upsert(insertedCapability, upsertReturnNewOptions);
    assertThat(updatedCapability.getAccountId()).isEqualTo(capabilityRequirement.getAccountId());
    assertThat(updatedCapability.getUuid()).isEqualTo(capabilityRequirement.getUuid());
    assertThat(updatedCapability.getValidUntil()).isEqualTo(insertedCapability.getValidUntil());
    assertThat(updatedCapability.getCapabilityType()).isEqualTo(insertedCapability.getCapabilityType());
    assertThat(updatedCapability.getCapabilityParameters()).isEqualTo(insertedCapability.getCapabilityParameters());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testUpsertTaskSelectionDetails() {
    // Test insert
    CapabilityTaskSelectionDetails taskSelectionDetails = buildCapabilityTaskSelectionDetails();
    CapabilityTaskSelectionDetails insertedTaskSelectionDetails =
        capabilityDao.upsert(taskSelectionDetails, upsertReturnNewOptions);
    assertThat(insertedTaskSelectionDetails).isNotNull();
    assertThat(insertedTaskSelectionDetails.getUuid()).isEqualTo(taskSelectionDetails.getUuid());
    assertThat(insertedTaskSelectionDetails.getAccountId()).isEqualTo(taskSelectionDetails.getAccountId());
    assertThat(insertedTaskSelectionDetails.getCapabilityId()).isEqualTo(taskSelectionDetails.getCapabilityId());
    assertThat(insertedTaskSelectionDetails.getTaskGroup()).isEqualTo(taskSelectionDetails.getTaskGroup());
    assertThat(insertedTaskSelectionDetails.getTaskSelectors()).isEqualTo(taskSelectionDetails.getTaskSelectors());
    assertThat(insertedTaskSelectionDetails.getTaskSetupAbstractions())
        .isEqualTo(taskSelectionDetails.getTaskSetupAbstractions());
    assertThat(insertedTaskSelectionDetails.isBlocked()).isEqualTo(taskSelectionDetails.isBlocked());
    assertThat(insertedTaskSelectionDetails.getValidUntil()).isEqualTo(taskSelectionDetails.getValidUntil());

    // Test update
    Map<String, Set<String>> updatedTaskSelectors = new HashMap<>();
    updatedTaskSelectors.put("key5", Collections.singleton("value5"));

    Map<String, String> updatedTaskSetupAbstractions = new HashMap<>();
    updatedTaskSetupAbstractions.put("key6", "value6");

    insertedTaskSelectionDetails.setCapabilityId(generateUuid());
    insertedTaskSelectionDetails.setTaskGroup(TaskGroup.SFTP);
    insertedTaskSelectionDetails.setTaskSelectors(updatedTaskSelectors);
    insertedTaskSelectionDetails.setTaskSetupAbstractions(updatedTaskSetupAbstractions);
    insertedTaskSelectionDetails.setBlocked(false);
    insertedTaskSelectionDetails.setValidUntil(Date.from(Instant.now().plus(Duration.ofDays(10))));

    CapabilityTaskSelectionDetails updatedTaskSelectionDetails =
        capabilityDao.upsert(insertedTaskSelectionDetails, upsertReturnNewOptions);
    assertThat(updatedTaskSelectionDetails.getUuid()).isEqualTo(taskSelectionDetails.getUuid());
    assertThat(updatedTaskSelectionDetails.getAccountId()).isEqualTo(taskSelectionDetails.getAccountId());
    assertThat(updatedTaskSelectionDetails.getCapabilityId()).isEqualTo(insertedTaskSelectionDetails.getCapabilityId());
    assertThat(updatedTaskSelectionDetails.getTaskGroup()).isEqualTo(insertedTaskSelectionDetails.getTaskGroup());
    assertThat(updatedTaskSelectionDetails.getTaskSelectors())
        .isEqualTo(insertedTaskSelectionDetails.getTaskSelectors());
    assertThat(updatedTaskSelectionDetails.getTaskSetupAbstractions())
        .isEqualTo(insertedTaskSelectionDetails.getTaskSetupAbstractions());
    assertThat(updatedTaskSelectionDetails.isBlocked()).isEqualTo(insertedTaskSelectionDetails.isBlocked());
    assertThat(updatedTaskSelectionDetails.getValidUntil()).isEqualTo(insertedTaskSelectionDetails.getValidUntil());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetAllDelegatePermissions() {
    String accountId = generateUuid();
    String delegateId = generateUuid();

    CapabilitySubjectPermission permission1 =
        buildCapabilitySubjectPermission(accountId, delegateId, generateUuid(), PermissionResult.UNCHECKED);
    CapabilitySubjectPermission permission2 =
        buildCapabilitySubjectPermission(accountId, delegateId, generateUuid(), PermissionResult.ALLOWED);
    persistence.save(permission1);
    persistence.save(permission2);

    // Test with permission filter
    List<CapabilitySubjectPermission> filteredpermissions =
        capabilityDao.getAllDelegatePermissions(accountId, delegateId, PermissionResult.ALLOWED);
    assertThat(filteredpermissions).hasSize(1);
    assertThat(filteredpermissions.get(0)).isEqualTo(permission2);

    // Test without permission filter
    filteredpermissions = capabilityDao.getAllDelegatePermissions(accountId, delegateId, null);
    assertThat(filteredpermissions).hasSize(2);
    assertThat(filteredpermissions).containsExactlyInAnyOrder(permission1, permission2);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testAddCapabilityPermissions() {
    CapabilityRequirement capabilityRequirement = buildCapabilityRequirement();
    List<String> insertedPermissionIds = capabilityDao.addCapabilityPermissions(
        capabilityRequirement, Arrays.asList("delegate1", "delegate2"), PermissionResult.UNCHECKED);
    assertThat(insertedPermissionIds).hasSize(2);

    CapabilitySubjectPermission permission1 =
        persistence.get(CapabilitySubjectPermission.class, insertedPermissionIds.get(0));
    assertThat(permission1).isNotNull();
    assertThat(permission1.getAccountId()).isEqualTo(capabilityRequirement.getAccountId());
    assertThat(permission1.getCapabilityId()).isEqualTo(capabilityRequirement.getUuid());
    assertThat(permission1.getDelegateId()).isIn("delegate1", "delegate2");
    assertThat(permission1.getMaxValidUntil()).isEqualTo(0);
    assertThat(permission1.getRevalidateAfter()).isEqualTo(0);
    assertThat(permission1.getValidUntil()).isEqualTo(capabilityRequirement.getValidUntil());
    assertThat(permission1.getPermissionResult()).isEqualTo(PermissionResult.UNCHECKED);

    CapabilitySubjectPermission permission2 =
        persistence.get(CapabilitySubjectPermission.class, insertedPermissionIds.get(1));
    assertThat(permission2).isNotNull();
    assertThat(permission2.getAccountId()).isEqualTo(capabilityRequirement.getAccountId());
    assertThat(permission2.getCapabilityId()).isEqualTo(capabilityRequirement.getUuid());
    assertThat(permission2.getDelegateId()).isIn("delegate1", "delegate2");
    assertThat(permission2.getMaxValidUntil()).isEqualTo(0);
    assertThat(permission2.getRevalidateAfter()).isEqualTo(0);
    assertThat(permission2.getValidUntil()).isEqualTo(capabilityRequirement.getValidUntil());
    assertThat(permission2.getPermissionResult()).isEqualTo(PermissionResult.UNCHECKED);

    assertThat(permission1.getDelegateId()).isNotEqualTo(permission2.getDelegateId());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetAllCapabilityPermissions() {
    String accountId = generateUuid();
    String capabilityId = generateUuid();

    CapabilitySubjectPermission permission1 =
        buildCapabilitySubjectPermission(accountId, generateUuid(), capabilityId, PermissionResult.UNCHECKED);
    CapabilitySubjectPermission permission2 =
        buildCapabilitySubjectPermission(accountId, generateUuid(), capabilityId, PermissionResult.ALLOWED);
    persistence.save(permission1);
    persistence.save(permission2);

    // Test with permission filter
    List<CapabilitySubjectPermission> filteredpermissions =
        capabilityDao.getAllCapabilityPermissions(accountId, capabilityId, PermissionResult.ALLOWED);
    assertThat(filteredpermissions).hasSize(1);
    assertThat(filteredpermissions.get(0)).isEqualTo(permission2);

    // Test without permission filter
    filteredpermissions = capabilityDao.getAllCapabilityPermissions(accountId, capabilityId, null);
    assertThat(filteredpermissions).hasSize(2);
    assertThat(filteredpermissions).containsExactlyInAnyOrder(permission1, permission2);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetValidAllowedCapabilityPermissions() {
    String accountId = generateUuid();
    String capabilityId = generateUuid();

    CapabilitySubjectPermission permission1 =
        buildCapabilitySubjectPermission(accountId, generateUuid(), capabilityId, PermissionResult.UNCHECKED);
    CapabilitySubjectPermission permission2 =
        buildCapabilitySubjectPermission(accountId, generateUuid(), capabilityId, PermissionResult.ALLOWED);
    CapabilitySubjectPermission permission3 =
        buildCapabilitySubjectPermission(accountId, generateUuid(), capabilityId, PermissionResult.ALLOWED);
    permission3.setMaxValidUntil(System.currentTimeMillis() + 60000);

    persistence.save(permission1);
    persistence.save(permission2);
    persistence.save(permission3);

    List<CapabilitySubjectPermission> validAllowedCapabilityPermissions =
        capabilityDao.getValidAllowedCapabilityPermissions(accountId, capabilityId);
    assertThat(validAllowedCapabilityPermissions).hasSize(1);
    assertThat(validAllowedCapabilityPermissions.get(0)).isEqualTo(permission3);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetPermissionsByIds() {
    String accountId = generateUuid();

    CapabilitySubjectPermission permission1 =
        buildCapabilitySubjectPermission(accountId, generateUuid(), generateUuid(), PermissionResult.UNCHECKED);
    CapabilitySubjectPermission permission2 =
        buildCapabilitySubjectPermission(accountId, generateUuid(), generateUuid(), PermissionResult.ALLOWED);

    persistence.save(permission1);
    persistence.save(permission2);

    Set<String> permissionIds = new HashSet();
    permissionIds.add(permission1.getUuid());
    permissionIds.add(permission2.getUuid());

    List<CapabilitySubjectPermission> permissions = capabilityDao.getPermissionsByIds(accountId, permissionIds);
    assertThat(permissions).hasSize(2);
    assertThat(permissions).containsExactlyInAnyOrder(permission1, permission2);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetAllCapabilityTaskSelectionDetails() {
    CapabilityTaskSelectionDetails taskSelectionDetails = buildCapabilityTaskSelectionDetails();

    persistence.save(taskSelectionDetails);

    List<CapabilityTaskSelectionDetails> fetchedSelectionDetails = capabilityDao.getAllCapabilityTaskSelectionDetails(
        taskSelectionDetails.getAccountId(), taskSelectionDetails.getCapabilityId());
    assertThat(fetchedSelectionDetails).hasSize(1);
    assertThat(fetchedSelectionDetails.get(0)).isEqualTo(taskSelectionDetails);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetTaskSelectionDetails() {
    String accountId = generateUuid();
    CapabilityTaskSelectionDetails taskSelectionDetails1 = buildCapabilityTaskSelectionDetails();
    taskSelectionDetails1.setAccountId(accountId);
    CapabilityTaskSelectionDetails taskSelectionDetails2 = buildCapabilityTaskSelectionDetails();
    taskSelectionDetails2.setAccountId(accountId);

    persistence.save(taskSelectionDetails1);
    persistence.save(taskSelectionDetails2);

    Set<String> capabilityIds = new HashSet();
    capabilityIds.add(taskSelectionDetails1.getCapabilityId());
    capabilityIds.add(taskSelectionDetails2.getCapabilityId());

    List<CapabilityTaskSelectionDetails> fetchedSelectionDetails =
        capabilityDao.getTaskSelectionDetails(accountId, capabilityIds);
    assertThat(fetchedSelectionDetails).hasSize(2);
    assertThat(fetchedSelectionDetails).containsExactlyInAnyOrder(taskSelectionDetails1, taskSelectionDetails2);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetBlockedTaskSelectionDetails() {
    String accountId = generateUuid();
    CapabilityTaskSelectionDetails taskSelectionDetails1 = buildCapabilityTaskSelectionDetails();
    taskSelectionDetails1.setAccountId(accountId);
    CapabilityTaskSelectionDetails taskSelectionDetails2 = buildCapabilityTaskSelectionDetails();
    taskSelectionDetails2.setAccountId(accountId);
    taskSelectionDetails2.setBlocked(false);

    persistence.save(taskSelectionDetails1);
    persistence.save(taskSelectionDetails2);

    List<CapabilityTaskSelectionDetails> blockedSelectionDetails =
        capabilityDao.getBlockedTaskSelectionDetails(accountId);
    assertThat(blockedSelectionDetails).hasSize(1);
    assertThat(blockedSelectionDetails.get(0)).isEqualTo(taskSelectionDetails1);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDeleteCapabilitySubjectPermissionById() {
    CapabilitySubjectPermission permission =
        buildCapabilitySubjectPermission(generateUuid(), generateUuid(), generateUuid(), PermissionResult.UNCHECKED);
    persistence.save(permission);
    assertThat(persistence.get(CapabilitySubjectPermission.class, permission.getUuid())).isNotNull();

    assertThat(capabilityDao.deleteCapabilitySubjectPermission(permission.getUuid())).isTrue();
    assertThat(persistence.get(CapabilitySubjectPermission.class, permission.getUuid())).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDeleteCapabilitySubjectPermission() {
    CapabilitySubjectPermission permission =
        buildCapabilitySubjectPermission(generateUuid(), generateUuid(), generateUuid(), PermissionResult.UNCHECKED);
    persistence.save(permission);
    assertThat(persistence.get(CapabilitySubjectPermission.class, permission.getUuid())).isNotNull();

    assertThat(capabilityDao.deleteCapabilitySubjectPermission(
                   permission.getAccountId(), permission.getDelegateId(), permission.getCapabilityId()))
        .isTrue();
    assertThat(persistence.get(CapabilitySubjectPermission.class, permission.getUuid())).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetDelegateCapabilityPermission() {
    CapabilitySubjectPermission permission =
        buildCapabilitySubjectPermission(generateUuid(), generateUuid(), generateUuid(), PermissionResult.UNCHECKED);
    persistence.save(permission);

    CapabilitySubjectPermission fetchedPermission = capabilityDao.getDelegateCapabilityPermission(
        permission.getAccountId(), permission.getDelegateId(), permission.getCapabilityId());
    assertThat(fetchedPermission).isNotNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testUpdateBlockingCheckIterations() {
    CapabilityTaskSelectionDetails taskSelectionDetails = buildCapabilityTaskSelectionDetails();
    taskSelectionDetails.setBlockingCheckIterations(Arrays.asList(10L, 20L));
    persistence.save(taskSelectionDetails);

    List<Long> updatedIterations = Arrays.asList(40L, 50L);
    CapabilityTaskSelectionDetails updatedTaskSelectionDetails = capabilityDao.updateBlockingCheckIterations(
        taskSelectionDetails.getAccountId(), taskSelectionDetails.getUuid(), updatedIterations, returnNewOptions);
    assertThat(updatedTaskSelectionDetails.getBlockingCheckIterations()).isEqualTo(updatedIterations);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testGetNotDeniedCapabilityPermissions() {
    String accountId = generateUuid();
    String capabilityId = generateUuid();

    CapabilitySubjectPermission permission1 =
        buildCapabilitySubjectPermission(accountId, generateUuid(), capabilityId, PermissionResult.UNCHECKED);
    CapabilitySubjectPermission permission2 =
        buildCapabilitySubjectPermission(accountId, generateUuid(), capabilityId, PermissionResult.ALLOWED);
    CapabilitySubjectPermission permission3 =
        buildCapabilitySubjectPermission(accountId, generateUuid(), capabilityId, PermissionResult.DENIED);

    persistence.save(permission1);
    persistence.save(permission2);
    persistence.save(permission3);

    List<CapabilitySubjectPermission> notDeniedCapabilityPermissions =
        capabilityDao.getNotDeniedCapabilityPermissions(accountId, capabilityId);
    assertThat(notDeniedCapabilityPermissions).hasSize(2);
    assertThat(notDeniedCapabilityPermissions).containsExactlyInAnyOrder(permission1, permission2);
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
