/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.utils.RequestField;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RequestFieldDeserializerTest extends CategoryTest implements MultilineStringMixin {
  private ObjectMapper objectMapper;

  @Before
  public void setUp() throws Exception {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new HarnessJacksonModule());
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_RequestField_Deserialization() throws IOException {
    final Input input = deserialiseTo(Input.class);
    assertThat(input.toString()).isNotBlank();
    assertThat(input.intField).isEqualTo(8);
    assertThat(input.strField).isEqualTo("strField_value");
    assertThat(input.listField).isEqualTo(Arrays.asList("val1", "val2"));
    testRequiredFiled(input.field1, true, "field1_value");
    testRequiredFiled(input.field2, true, null);
    testRequiredFiled(input.field3, true, null);
    assertThat(input.field4.isPresent()).isTrue();
    final Inner inner = input.field4.getValue().get();
    testRequiredFiled(inner.inner1, true, "inner1_value");
    testRequiredFiled(inner.inner2, true, null);
    testRequiredFiled(inner.inner3, true, null);
  }

  private <T> void testRequiredFiled(RequestField<T> requestField, boolean expectedHasBeenSet, T expectedValue) {
    assertThat(requestField.isPresent()).isEqualTo(expectedHasBeenSet);
    assertThat(requestField.getValue().orElse(null)).isEqualTo(expectedValue);
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_RequestField_Deserialization_witoutbuilder() throws IOException {
    final NewInput newInput = deserialiseTo(NewInput.class);
    assertThat(newInput.toString()).isNotBlank();
    assertThat(newInput.intField).isEqualTo(8);
    assertThat(newInput.strField).isEqualTo("strField_value");
    assertThat(newInput.listField).isEqualTo(Arrays.asList("val1", "val2"));
    testRequiredFiled(newInput.field1, true, "field1_value");
    testRequiredFiled(newInput.field2, true, null);
    assertThat(newInput.field3).isNull();
    assertThat(newInput.field4.isPresent()).isTrue();
    final NewInner newInner = newInput.field4.getValue().get();
    testRequiredFiled(newInner.inner1, true, "inner1_value");
    testRequiredFiled(newInner.inner2, true, null);
    assertThat(newInner.inner3).isNull();
  }
  private <T> T deserialiseTo(Class<T> clazz) throws IOException {
    String json = $GQL(/*
{
"intField":8,
"field1":"field1_value",
"field2":null,
"strField":"strField_value",
"listField":["val1","val2"],
"field4":{
  "inner1":"inner1_value",
  "inner2":null
}
}
*/);
    return objectMapper.readValue(json, clazz);
  }

  @Data
  @Builder
  private static class Input {
    private RequestField<String> field1;
    private RequestField<String> field2;
    private RequestField<String> field3;
    private RequestField<Inner> field4;
    private RequestField<Inner> field5;
    private int intField;
    private String strField;
    private List<String> listField;
  }

  @Data
  @Builder
  private static class Inner {
    private RequestField<String> inner1;
    private RequestField<String> inner2;
    private RequestField<String> inner3;
  }

  @Data
  private static class NewInput {
    private RequestField<String> field1;
    private RequestField<String> field2;
    private RequestField<String> field3;
    private RequestField<NewInner> field4;
    private RequestField<NewInner> field5;
    private int intField;
    private String strField;
    private List<String> listField;
  }

  @Data
  private static class NewInner {
    private RequestField<String> inner1;
    private RequestField<String> inner2;
    private RequestField<String> inner3;
  }
}
