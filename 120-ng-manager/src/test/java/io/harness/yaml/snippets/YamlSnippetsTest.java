package io.harness.yaml.snippets;

import static io.harness.rule.OwnerRule.ABHINAV;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.YamlSdkConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class YamlSnippetsTest implements AbstractSnippetChecker {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSnippetsForTags() throws IOException {
    testIconTagsAreInTags();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSnippetsForCorrectResource() throws IOException {
    testSnippetHasCorrectResourceFileSpecified();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testEnumAndXmlInSync() throws IOException {
    testTagsEnumAndXmlInSync();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSnippetsMatchSchema() throws IOException {
    testSnippetsMatchSchema(log);
  }

  @Override
  public String getIndexResourceFileContent() throws IOException {
    final InputStream resourceAsStream =
        YamlSdkConstants.class.getClassLoader().getResourceAsStream(YamlSdkConstants.snippetsResourceFile);
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

  @Override
  public String getSchemaBasePath() {
    return YamlSdkConstants.schemaBasePath;
  }
}