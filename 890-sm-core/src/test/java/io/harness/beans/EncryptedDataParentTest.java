/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.encryption.Encrypted;
import io.harness.rule.Owner;

import software.wings.settings.SettingVariableTypes;

import lombok.experimental.FieldNameConstants;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@FieldNameConstants(innerTypeName = "EncryptedDataParentTestKeys")
public class EncryptedDataParentTest extends CategoryTest {
  private static final String ANNOTATION_FIELD_NAME = "test_annotation";
  @Encrypted(fieldName = ANNOTATION_FIELD_NAME) private String value;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetEncryptedFieldTag() {
    String dummyUuid = "dummyUuid";
    SettingVariableTypes type = SettingVariableTypes.GCP_KMS;
    EncryptedDataParent encryptedDataParent = EncryptedDataParent.createParentRef(
        dummyUuid, EncryptedDataParentTest.class, EncryptedDataParentTestKeys.value, type);
    assertThat(encryptedDataParent).isNotNull();
    assertThat(encryptedDataParent.getId()).isEqualTo(dummyUuid);
    assertThat(encryptedDataParent.getFieldName()).isEqualTo(ANNOTATION_FIELD_NAME);
    assertThat(encryptedDataParent.getType()).isEqualTo(type);
  }
}
