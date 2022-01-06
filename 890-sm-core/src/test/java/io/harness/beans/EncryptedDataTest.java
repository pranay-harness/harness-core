/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.rule.OwnerRule.VIKAS;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.SMCoreTestBase;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;

import software.wings.settings.SettingVariableTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;
import org.powermock.reflect.Whitebox;

public class EncryptedDataTest extends SMCoreTestBase {
  private static final Random random = new Random();
  private EncryptedData encryptedData;
  private SettingVariableTypes encryptedDataType;

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{true}, {false}});
  }

  @Before
  public void setup() throws Exception {
    encryptedDataType = SettingVariableTypes.AWS;
    encryptedData = EncryptedData.builder().accountId("accontId").name("name").type(encryptedDataType).build();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testAddParent() {
    String id = "id";
    SettingVariableTypes type = SettingVariableTypes.GCP_KMS;
    String fieldName = "fieldName";
    EncryptedDataParent encryptedDataParent = new EncryptedDataParent(id, type, fieldName);
    encryptedData.addParent(encryptedDataParent);
    Set<EncryptedDataParent> parents = encryptedData.getParents();
    assertThat(parents).hasSize(1);
    EncryptedDataParent returnedParent = parents.iterator().next();
    assertThat(returnedParent.getId()).isEqualTo(id);
    assertThat(returnedParent.getFieldName()).isEqualTo(fieldName);
    assertThat(returnedParent.getType()).isEqualTo(type);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void removeParent() {
    List<EncryptedDataParent> addedParents = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      String id = UUIDGenerator.generateUuid();
      SettingVariableTypes type = SettingVariableTypes.AWS;
      String fieldName = UUIDGenerator.generateUuid();
      EncryptedDataParent encryptedDataParent = new EncryptedDataParent(id, type, fieldName);
      encryptedData.addParent(encryptedDataParent);
      addedParents.add(encryptedDataParent);
    }

    assertThat(encryptedData.getParents()).hasSize(2);
    assertThat(encryptedData.containsParent(addedParents.get(0).getId(), addedParents.get(0).getType())).isTrue();
    assertThat(encryptedData.containsParent(addedParents.get(1).getId(), addedParents.get(1).getType())).isTrue();

    encryptedData.removeParent(addedParents.get(1));
    assertThat(encryptedData.getParents()).hasSize(1);
    assertThat(encryptedData.containsParent(addedParents.get(0).getId(), addedParents.get(0).getType())).isTrue();
    assertThat(encryptedData.containsParent(addedParents.get(1).getId(), addedParents.get(1).getType())).isFalse();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testEncryptedDataMigrationIteration() throws IllegalAccessException {
    long nextMigrationIteration = random.nextLong();
    FieldUtils.writeField(encryptedData, EncryptedDataKeys.nextMigrationIteration, nextMigrationIteration, true);
    assertThat(encryptedData.obtainNextIteration(EncryptedDataKeys.nextMigrationIteration))
        .isEqualTo(nextMigrationIteration);

    nextMigrationIteration = random.nextLong();
    encryptedData.updateNextIteration(EncryptedDataKeys.nextMigrationIteration, nextMigrationIteration);
    assertThat(encryptedData.obtainNextIteration(EncryptedDataKeys.nextMigrationIteration))
        .isEqualTo(nextMigrationIteration);

    try {
      encryptedData.updateNextIteration(generateUuid(), random.nextLong());
      fail("Did not throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      encryptedData.obtainNextIteration(generateUuid());
      fail("Did not throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testUpdateNextIterationForAwsToGcpKmsMigrationField() {
    long oldNextMigrationIterationValue = random.nextLong();
    Whitebox.setInternalState(
        encryptedData, EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration, oldNextMigrationIterationValue);
    long newNextMigrationIterationValue = random.nextLong();
    encryptedData.updateNextIteration(
        EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration, newNextMigrationIterationValue);
    assertThat(encryptedData.obtainNextIteration(EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration))
        .isEqualTo(newNextMigrationIterationValue);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testObtainNextIterationForAwsToGcpKmsMigrationField() {
    long nextMigrationIterationValue = random.nextLong();
    Whitebox.setInternalState(
        encryptedData, EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration, nextMigrationIterationValue);
    assertThat(encryptedData.obtainNextIteration(EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration))
        .isEqualTo(nextMigrationIterationValue);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateNextIterationForLocalToGcpKmsMigrationField() {
    long oldNextMigrationIterationValue = random.nextLong();
    Whitebox.setInternalState(
        encryptedData, EncryptedDataKeys.nextLocalToGcpKmsMigrationIteration, oldNextMigrationIterationValue);
    long newNextMigrationIterationValue = random.nextLong();
    encryptedData.updateNextIteration(
        EncryptedDataKeys.nextLocalToGcpKmsMigrationIteration, newNextMigrationIterationValue);
    assertThat(encryptedData.obtainNextIteration(EncryptedDataKeys.nextLocalToGcpKmsMigrationIteration))
        .isEqualTo(newNextMigrationIterationValue);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testObtainNextIterationForLocalToGcpKmsMigrationField() {
    long nextMigrationIterationValue = random.nextLong();
    Whitebox.setInternalState(
        encryptedData, EncryptedDataKeys.nextLocalToGcpKmsMigrationIteration, nextMigrationIterationValue);
    assertThat(encryptedData.obtainNextIteration(EncryptedDataKeys.nextLocalToGcpKmsMigrationIteration))
        .isEqualTo(nextMigrationIterationValue);
  }
}
