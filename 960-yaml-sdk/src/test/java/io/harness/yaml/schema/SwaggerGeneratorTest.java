/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.TestClass;

import io.dropwizard.jackson.Jackson;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SwaggerGeneratorTest extends CategoryTest {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGenerateDefinitions() {
    SwaggerGenerator swaggerGenerator = new SwaggerGenerator(Jackson.newObjectMapper());
    final Map<String, Model> stringModelMap =
        swaggerGenerator.generateDefinitions(TestClass.ClassWhichContainsInterface.class);
    assertThat(stringModelMap.size()).isEqualTo(4);
    assertThat(stringModelMap.get("ClassWhichContainsInterface")).isInstanceOf(ModelImpl.class);
    assertThat(stringModelMap.get("ClassWithoutApiModelOverride")).isInstanceOf(ComposedModel.class);
    assertThat(stringModelMap.get("testName")).isInstanceOf(ComposedModel.class);
    assertThat(stringModelMap.get("TestInterface")).isInstanceOf(ModelImpl.class);
  }
}
