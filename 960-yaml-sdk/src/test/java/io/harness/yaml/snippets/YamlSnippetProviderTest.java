package io.harness.yaml.snippets;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.snippets.dto.YamlSnippetsDTO;
import io.harness.yaml.snippets.helper.YamlSnippetHelper;
import io.harness.yaml.snippets.impl.YamlSnippetProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class YamlSnippetProviderTest extends CategoryTest {
  private YamlSnippetProvider yamlSnippetProvider;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    YamlSnippetHelper yamlSnippetHelper = new YamlSnippetHelper();
    yamlSnippetProvider = new YamlSnippetProvider(yamlSnippetHelper);
    final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("testIndex.xml");
    String snippetMetaData = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    yamlSnippetHelper.preComputeTagsAndNameMap(snippetMetaData);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetYamlSnippetMetaData() {
    YamlSnippetsDTO yamlSnippetMetaData = yamlSnippetProvider.getYamlSnippetMetaData(Arrays.asList("connector", "k8s"));
    assertThat(yamlSnippetMetaData.getYamlSnippets().size()).isEqualTo(1);
    yamlSnippetMetaData = yamlSnippetProvider.getYamlSnippetMetaData(Arrays.asList("connector"));
    assertThat(yamlSnippetMetaData.getYamlSnippets().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testGetYamlSnippet() throws IOException {
    final String yamlSnippet = yamlSnippetProvider.getYamlSnippet("git-1-0");
    final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("testsnippets/git/test.yaml");
    String snippet = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
    assertThat(yamlSnippet).isEqualTo(snippet);
  }
}