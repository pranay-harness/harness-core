package io.harness.encryption;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;
import lombok.experimental.FieldNameConstants;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Optional;

@FieldNameConstants(innerTypeName = "EncryptionReflectionUtilsTestKeys")
public class EncryptionReflectionUtilsTest {
  private static final String ANNOTATION_FIELD_NAME = "test_annotation";
  @Encrypted(fieldName = ANNOTATION_FIELD_NAME) private String value;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetEncryptedFieldTag() {
    Optional<Field> annotatedFieldOptional = Optional.ofNullable(
        ReflectionUtils.getFieldByName(EncryptionReflectionUtilsTest.class, EncryptionReflectionUtilsTestKeys.value));
    Field annotatedField = annotatedFieldOptional.<RuntimeException>orElseThrow(
        () -> { throw new RuntimeException("Field should not be null"); });
    String encryptedFieldTag = EncryptionReflectUtils.getEncryptedFieldTag(annotatedField);
    assertThat(encryptedFieldTag).isEqualTo(ANNOTATION_FIELD_NAME);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetFieldHavingFieldName() {
    Optional<Field> annotatedFieldOptional = Optional.ofNullable(
        ReflectionUtils.getFieldByName(EncryptionReflectionUtilsTest.class, EncryptionReflectionUtilsTestKeys.value));
    Field annotatedField = annotatedFieldOptional.<RuntimeException>orElseThrow(
        () -> { throw new RuntimeException("Field should not be null"); });
    Field field =
        EncryptionReflectUtils.getFieldHavingFieldName(Collections.singletonList(annotatedField), ANNOTATION_FIELD_NAME)
            .<RuntimeException>orElseThrow(() -> { throw new RuntimeException("Field should not be null"); });
    assertThat(field.getName()).isEqualTo(EncryptionReflectionUtilsTestKeys.value);
  }
}
