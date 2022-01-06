/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml.validator;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.TestClass;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

public class YamlSchemaValidatorTest extends CategoryTest {
  YamlSchemaValidator yamlSchemaValidator;
  @Before
  public void setup() throws IOException {
    initMocks(this);
    final List<YamlSchemaRootClass> yamlSchemaRootClasses =
        Collections.singletonList(YamlSchemaRootClass.builder()
                                      .entityType(EntityType.CONNECTORS)
                                      .clazz(TestClass.ClassWhichContainsInterface.class)
                                      .build());
    yamlSchemaValidator = Mockito.spy(new YamlSchemaValidator(yamlSchemaRootClasses));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testValidate() throws IOException {
    final String type1 = getYamlResource("validator/testyamltype1.yaml");
    final String type2 = getYamlResource("validator/testyamltype2.yaml");
    final String type1Incorrect = getYamlResource("validator/testType1Incorrect.yaml");
    final String type2Incorrect = getYamlResource("validator/testYamlType2Incorrect.yaml");
    String schema = getYamlResource("validator/schema.json");
    ObjectMapper objectMapper = new ObjectMapper();

    yamlSchemaValidator.populateSchemaInStaticMap(objectMapper.readTree(schema), EntityType.CONNECTORS);

    final Set<String> type1Val = yamlSchemaValidator.validate(type1, EntityType.CONNECTORS);
    assertThat(type1Val).isEmpty();

    final Set<String> type2Val = yamlSchemaValidator.validate(type2, EntityType.CONNECTORS);
    assertThat(type2Val).isEmpty();

    final Set<String> type1IncorrectVal = yamlSchemaValidator.validate(type1Incorrect, EntityType.CONNECTORS);
    assertThat(type1IncorrectVal).isNotEmpty();

    final Set<String> type2IncorrectVal = yamlSchemaValidator.validate(type2Incorrect, EntityType.CONNECTORS);
    assertThat(type2IncorrectVal).isNotEmpty();
  }

  private String getYamlResource(String resource) throws IOException {
    return IOUtils.resourceToString(resource, StandardCharsets.UTF_8, YamlSchemaValidatorTest.class.getClassLoader());
  }
}
