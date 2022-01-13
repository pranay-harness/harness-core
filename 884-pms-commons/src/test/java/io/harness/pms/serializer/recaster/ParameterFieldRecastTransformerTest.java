/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.serializer.recaster;

import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.InputSetValidatorType;
import io.harness.category.element.UnitTests;
import io.harness.core.Recaster;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.ParameterFieldValueWrapper;
import io.harness.pms.yaml.validation.InputSetValidator;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ParameterFieldRecastTransformerTest extends CategoryTest {
  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithObsoleteStructure() {
    Map<String, Object> map = new HashMap<>();
    map.put(Recaster.RECAST_CLASS_KEY, ParameterField.class.getName());
    map.put("expressionValue", "someValue");
    map.put("expression", false);
    map.put("typeString", false);
    ParameterField recasted = RecastOrchestrationUtils.fromMap(map, ParameterField.class);
    assertThat(recasted.getExpressionValue()).isEqualTo("someValue");
    assertThat(recasted.isExpression()).isFalse();
    assertThat(recasted.isTypeString()).isFalse();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithValueField() {
    ParameterField<Object> parameterField = ParameterField.createValueField("Value");
    Map<String, Object> map = RecastOrchestrationUtils.toMap(parameterField);
    ParameterField recasted = RecastOrchestrationUtils.fromMap(map, ParameterField.class);
    assertThat(recasted).isEqualTo(parameterField);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithExpressionField() {
    ParameterField<Object> parameterField =
        ParameterField.createExpressionField(true, "<+json.object(httpResponseBody).metaData>", null, true);
    Map<String, Object> map = RecastOrchestrationUtils.toMap(parameterField);
    ParameterField recasted = RecastOrchestrationUtils.fromMap(map, ParameterField.class);
    assertThat(recasted).isEqualTo(parameterField);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithJsonResponseField() {
    ParameterField<Object> parameterField = ParameterField.createJsonResponseField("{response: \"success\"}");
    Map<String, Object> map = RecastOrchestrationUtils.toMap(parameterField);
    ParameterField recasted = RecastOrchestrationUtils.fromMap(map, ParameterField.class);
    assertThat(recasted).isEqualTo(parameterField);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithInputSetValidator() {
    ParameterField<Object> parameterField = ParameterField.createValueFieldWithInputSetValidator(
        "Value", new InputSetValidator(InputSetValidatorType.REGEX, "*"), true);
    Map<String, Object> map = RecastOrchestrationUtils.toMap(parameterField);
    ParameterField recasted = RecastOrchestrationUtils.fromMap(map, ParameterField.class);
    assertThat(recasted).isEqualTo(parameterField);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldTestParameterFieldWithComplexObjects() {
    DummyB dummyB = DummyB.builder()
                        .strVal("a")
                        .intVal(1)
                        .listVal(Collections.singletonList("b"))
                        .mapVal(Collections.singletonMap("c", 3))
                        .build();
    ParameterFieldValueWrapper<List<DummyB>> l = new ParameterFieldValueWrapper<>(Collections.singletonList(dummyB));
    Map<String, Object> mapB = RecastOrchestrationUtils.toMap(l);
    ParameterFieldValueWrapper recastedB = RecastOrchestrationUtils.fromMap(mapB, ParameterFieldValueWrapper.class);
    assertThat(recastedB).isNotNull();
    assertThat(recastedB).isEqualTo(l);

    DummyA dummyA = DummyA.builder().pf(ParameterField.createValueField(Collections.singletonList(dummyB))).build();
    Map<String, Object> map = RecastOrchestrationUtils.toMap(dummyA);
    DummyA recasted = RecastOrchestrationUtils.fromMap(map, DummyA.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(dummyA);
  }

  @Data
  @Builder
  public static class DummyA {
    private ParameterField<List<DummyB>> pf;
  }

  @Data
  @Builder
  public static class DummyB {
    private String strVal;
    private int intVal;
    private List<String> listVal;
    private Map<String, Integer> mapVal;
  }
}
