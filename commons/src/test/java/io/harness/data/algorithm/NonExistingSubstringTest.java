package io.harness.data.algorithm;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.harness.exception.SmallAlphabetException;
import org.junit.Test;

public class NonExistingSubstringTest {
  @Test
  public void testSubstring() throws SmallAlphabetException {
    assertEquals(NonExistingSubstring.substring("", "abcd", asList("abcd-ac-ad-aa-bc")), "bd");
    assertEquals(NonExistingSubstring.substring("a", "abcd", asList("abcd-ac-ad-aa-bc")), "abb");
  }

  @Test
  public void testSelfHeadTailed() {
    assertFalse(NonExistingSubstring.selfHeadTailed(""));
    assertFalse(NonExistingSubstring.selfHeadTailed("abc"));
    assertFalse(NonExistingSubstring.selfHeadTailed("ababc"));
    assertTrue(NonExistingSubstring.selfHeadTailed("aba"));
    assertTrue(NonExistingSubstring.selfHeadTailed("abcab"));
  }

  @Test
  public void testHeadTailed() {
    assertFalse(NonExistingSubstring.headTailed("", "abc"));
    assertFalse(NonExistingSubstring.headTailed("abc", ""));
    assertFalse(NonExistingSubstring.headTailed("abc", "abc"));
    assertTrue(NonExistingSubstring.headTailed("abc", "cba"));
    assertTrue(NonExistingSubstring.headTailed("abb", "cca"));
    assertTrue(NonExistingSubstring.headTailed("abc", "dba"));
    assertTrue(NonExistingSubstring.headTailed("abcccc", "dba"));
    assertTrue(NonExistingSubstring.headTailed("abc", "ddddba"));
  }
}
