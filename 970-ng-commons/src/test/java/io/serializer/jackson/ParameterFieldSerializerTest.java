/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.serializer.jackson;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.InputSetValidatorType;
import io.harness.category.element.UnitTests;
import io.harness.multiline.MultilineStringMixin;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.validation.InputSetValidator;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class ParameterFieldSerializerTest extends CategoryTest implements MultilineStringMixin {
  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper(new YAMLFactory());
    objectMapper.registerModule(new NGHarnessJacksonModule());
  }

  @Test
  @Owner(developers = OwnerRule.GARVIT)
  @Category(UnitTests.class)
  public void testParameterFieldSerialization() throws IOException {
    SampleParams params = SampleParams.builder().build();
    ParameterField<String> field = ParameterField.createExpressionField(
        true, "<+input>", new InputSetValidator(InputSetValidatorType.ALLOWED_VALUES, "a,b,c"), true);
    params.setInner(field);
    validateSerialization(params);

    field = ParameterField.createValueField("hello");
    params.setInner(field);
    validateSerialization(params);

    SampleParamsMap paramsMap =
        SampleParamsMap.builder().innerMap(ParameterField.createValueField(new HashMap<>())).build();
    validateSerialization(paramsMap);
  }

  private void validateSerialization(SampleParamsMap sampleParamsMap) throws IOException {
    String str = objectMapper.writeValueAsString(sampleParamsMap);
    SampleParamsMap outputParamsMap = objectMapper.readValue(str, SampleParamsMap.class);
    assertThat(outputParamsMap).isNotNull();
    assertThat(outputParamsMap.getInnerMap()).isNotNull();
    assertThat(outputParamsMap.getInnerMap().getValue()).isEmpty();
  }

  private void validateSerialization(SampleParams params) throws IOException {
    String str = objectMapper.writeValueAsString(params);
    SampleParams outParams = objectMapper.readValue(str, SampleParams.class);
    assertThat(outParams).isNotNull();
    if (params.inner == null) {
      assertThat(outParams.inner).isNull();
      return;
    }

    ParameterField<String> in = params.inner;
    ParameterField<String> out = params.inner;
    assertThat(out).isNotNull();
    assertThat(out.isExpression()).isEqualTo(in.isExpression());
    assertThat(out.getExpressionValue()).isEqualTo(in.getExpressionValue());
    assertThat(out.isTypeString()).isEqualTo(in.isTypeString());
    assertThat(out.getValue()).isEqualTo(in.getValue());

    if (in.getInputSetValidator() == null) {
      assertThat(out.getInputSetValidator()).isNull();
      return;
    }

    assertThat(out.getInputSetValidator()).isNotNull();
    assertThat(out.getInputSetValidator().getValidatorType()).isEqualTo(in.getInputSetValidator().getValidatorType());
    assertThat(out.getInputSetValidator().getParameters()).isEqualTo(in.getInputSetValidator().getParameters());
  }

  @Data
  @Builder
  private static class SampleParams {
    ParameterField<String> inner;
  }

  @Data
  @Builder
  private static class SampleParamsMap {
    ParameterField<Map<String, String>> innerMap;
  }
}
