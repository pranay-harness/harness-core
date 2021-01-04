package io.harness.yaml.snippets;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
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
    yamlSnippetHelper.preComputeTagsAndNameMap(snippetMetaData);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testPreComputation() {
    assertThat(yamlSnippetHelper.getTagSnippetMap()).isNotEmpty();
    assertThat(yamlSnippetHelper.getIdentifierSnippetMap()).isNotEmpty();
    assertThat(yamlSnippetHelper.getTagSnippetMap().size()).isEqualTo(3);
    assertThat(yamlSnippetHelper.getIdentifierSnippetMap().size()).isEqualTo(2);
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