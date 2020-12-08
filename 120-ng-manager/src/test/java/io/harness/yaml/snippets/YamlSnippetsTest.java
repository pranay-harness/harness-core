package io.harness.yaml.snippets;

import static io.harness.rule.OwnerRule.ABHINAV;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.schema.AbstractSnippetTestBase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class YamlSnippetsTest implements AbstractSnippetTestBase {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSnippets() throws IOException {
    testIconTagsAreInTags();
    testSnippetHasCorrectResourceFileSpecified();
    testTagsEnumAndXmlInSync();
  }

  @Override
  public String getIndexResourceFileContent() throws IOException {
    final InputStream resourceAsStream =
        SnippetConstants.class.getClassLoader().getResourceAsStream(SnippetConstants.resourceFile);
    return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8.name());
  }

  @Override
  public Class getTagsEnum() {
    return SnippetTag.class;
  }

  @Override
  public long getTotalTagsInEnum() {
    return SnippetTag.values().length;
  }
}