package io.harness.data.algorithm;

import io.harness.exception.SmallAlphabetException;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class NonExistingSubstring {
  private NonExistingSubstring() {}

  public static boolean headTailed(String head, String tail) {
    int size = Math.min(head.length(), tail.length()) - 1;
    for (int i = tail.length() - size; i < tail.length(); ++i) {
      if (head.startsWith(tail.substring(i))) {
        return true;
      }
    }
    return false;
  }

  /*
   * Determines if the string is self head-tailed. Self-head-tailed string is a string that has at least
   * one of its starting substrings strings is also a ending.
   * Note this is not the same as palindromic - every palindromic string is self composed, but not
   * every self composed string is palindromic. For example: 'abcab' is not palindromic, but it is
   * self composed, because 'ab' is at the same time starting and ending substring.
   */
  public static boolean selfHeadTailed(String string) {
    return headTailed(string, string);
  }

  public static boolean coHeadTailed(String string1, String string2) {
    return headTailed(string1, string2) || headTailed(string2, string1);
  }

  public static boolean coHeadTailed(String string, List<String> list) {
    for (String item : list) {
      if (coHeadTailed(string, item)) {
        return true;
      }
    }
    return false;
  }

  private static void countSuffix(String prefix, String exclude, Map<Character, Integer> chars) {
    int last = exclude.length() - prefix.length() - 1;
    int index = -2;
    while (index < last) {
      index = exclude.indexOf(prefix, index + 1);
      if (index == -1) {
        break;
      }
      int pos = index + prefix.length();
      if (pos >= exclude.length()) {
        break;
      }
      char c = exclude.charAt(index + prefix.length());
      chars.computeIfPresent(c, (ch, count) -> count + 1);
    }
  }

  public static String substring(String prefix, String alphabet, List<String> excludes) throws SmallAlphabetException {
    String substring = prefix;

    for (;;) {
      Map<Character, Integer> chars = new HashMap<>();
      for (int i = 0; i < alphabet.length(); i++) {
        char c = alphabet.charAt(i);
        chars.put(c, 0);
      }

      for (String exclude : excludes) {
        countSuffix(substring, exclude, chars);
      }

      while (!chars.isEmpty()) {
        Entry<Character, Integer> min = Collections.min(chars.entrySet(), Comparator.comparingDouble(Entry::getValue));
        String next = substring + min.getKey();

        // We should not use self composed strings as unique replacements, because placing them next to each other
        // might misrepresents as another variable.
        if (selfHeadTailed(next) || coHeadTailed(next, excludes)) {
          chars.remove(min.getKey());
          continue;
        }

        if (min.getValue() == 0) {
          return next;
        }
        substring = next;
        break;
      }

      if (chars.isEmpty()) {
        throw new SmallAlphabetException(alphabet);
      }
    }
  }
}
