/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml.schema;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.yaml.TestClass;
import io.harness.yaml.schema.beans.OneOfMapping;
import io.harness.yaml.schema.beans.OneOfSetMapping;
import io.harness.yaml.schema.beans.SwaggerDefinitionsMetaInfo;

import io.dropwizard.jackson.Jackson;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DX)
public class JacksonClassHelperTest extends CategoryTest {
  JacksonClassHelper jacksonSubtypeHelper = new JacksonClassHelper(Jackson.newObjectMapper());

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetSubtypeMapping() {
    Map<String, SwaggerDefinitionsMetaInfo> stringModelSet = new HashMap<>();
    jacksonSubtypeHelper.getRequiredMappings(
        io.harness.yaml.TestClass.ClassWhichContainsInterface.class, stringModelSet);
    assertThat(stringModelSet).isNotEmpty();
    assertThat(stringModelSet.size()).isEqualTo(4);
    assertThat(stringModelSet.get("ClassWhichContainsInterface").getSubtypeClassMap().size()).isEqualTo(1);
    assertThat(stringModelSet.get("testName").getOneOfMappings().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetOneOfMappingsForClass() {
    final Set<OneOfMapping> oneOfMappingsForClass =
        jacksonSubtypeHelper.getOneOfMappingsForClass(io.harness.yaml.TestClass.ClassWithApiModelOverride.class);
    final OneOfMapping oneOfMapping_1 =
        OneOfMapping.builder().oneOfFieldNames(new HashSet<>(Arrays.asList("a", "b"))).nullable(false).build();
    final OneOfMapping oneOfMapping_2 =
        OneOfMapping.builder()
            .oneOfFieldNames(new HashSet<>(Arrays.asList("jsontypeinfo", "apimodelproperty")))
            .nullable(false)
            .build();
    assertThat(oneOfMappingsForClass).containsExactlyInAnyOrder(oneOfMapping_1, oneOfMapping_2);

    final Set<OneOfMapping> oneOfMappingsForClass_1 =
        jacksonSubtypeHelper.getOneOfMappingsForClass(TestClass.ClassWithoutApiModelOverride.class);
    final OneOfMapping oneOfMapping_1_1 =
        OneOfMapping.builder().oneOfFieldNames(new HashSet<>(Arrays.asList("x", "y"))).nullable(false).build();
    assertThat(oneOfMappingsForClass_1).containsExactlyInAnyOrder(oneOfMapping_1_1);
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testGetOneOfSetMappingsForClass() {
    final OneOfSetMapping oneOfSetMappingsForClass =
        jacksonSubtypeHelper.getOneOfSetMappingsForClass(io.harness.yaml.TestClass.ClassWithOneOfSetAnnotation.class);
    assertThat(oneOfSetMappingsForClass).isNotNull();
    assertThat(oneOfSetMappingsForClass.getOneOfSets()).hasSize(3);
    assertThat(oneOfSetMappingsForClass.getOneOfSets())
        .containsExactlyInAnyOrder(new HashSet<>(Arrays.asList("type", "spec")),
            new HashSet<>(Arrays.asList("b", "apimodelproperty")),
            new HashSet<>(Collections.singletonList("testString")));
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testOneOfSetAnnotationWithOneOfFieldAnnotationForClass() {
    assertThatThrownBy(
        ()
            -> jacksonSubtypeHelper.getRequiredMappings(
                io.harness.yaml.TestClass.ClassWithBothOneOfSetAndOneOfFieldAnnotation.class, new HashMap<>()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            "Class ClassWithBothOneOfSetAndOneOfFieldAnnotation cannot have both OneOfField and OneOfSet annotation");
  }
}
