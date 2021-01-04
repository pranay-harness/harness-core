package io.harness.yaml.snippets;

import static io.harness.rule.OwnerRule.ABHINAV;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class YamlSnippetsTest extends CategoryTest implements AbstractSnippetChecker {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSnippets() throws IOException {
    snippetTests(log);
  }
}