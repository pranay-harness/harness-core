package io.harness.data.algorithm;

import static junit.framework.TestCase.assertTrue;

import org.junit.Test;

public class IdentifierNameTest {
  @Test
  public void testRandom() {
    String identifier = IdentifierName.random();
    assertTrue(Character.isLetter(identifier.charAt(0)));
  }
}
