/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml.snippets;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.TestClass;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;
import io.harness.yaml.snippets.bean.YamlSnippetMetaData;
import io.harness.yaml.snippets.helper.YamlSnippetHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class YamlSnippetHelperTest extends CategoryTest {
  YamlSnippetHelper yamlSnippetHelper;
  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("testIndex.xml");
    String snippetMetaData = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    yamlSnippetHelper = new YamlSnippetHelper();
    yamlSnippetHelper.preComputeTagsAndNameMap(snippetMetaData,
        YamlSchemaRootClass.builder()
            .clazz(TestClass.ClassWhichContainsInterface.class)
            .entityType(EntityType.CONNECTORS)
            .build());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testPreComputation() {
    assertThat(yamlSnippetHelper.getTagSnippetMap()).isNotEmpty();
    assertThat(yamlSnippetHelper.getIdentifierSnippetMap()).isNotEmpty();
    assertThat(yamlSnippetHelper.getTagSnippetMap().size()).isEqualTo(4);
    assertThat(yamlSnippetHelper.getIdentifierSnippetMap().size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetIdentifier() {
    final String test =
        yamlSnippetHelper.getIdentifier(YamlSnippetMetaData.builder().name("abc xyz").version("1.0").build());
    assertThat(test).isNotNull();
    assertThat(test).isEqualTo("abc-xyz-1-0");
  }
}
