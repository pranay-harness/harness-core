package io.harness.data.algorithm;

import static junit.framework.TestCase.assertTrue;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class IdentifierNameTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testRandom() {
    String identifier = IdentifierName.random();
    assertTrue(Character.isLetter(identifier.charAt(0)));
  }
}
