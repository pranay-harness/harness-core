package io.harness.Utils;

import org.apache.commons.lang3.RandomStringUtils;

public class TestUtils {
  public String generateRandomString(int charLen) {
    int length = charLen;
    boolean useLetters = true;
    boolean useNumbers = false;
    String generatedString = RandomStringUtils.random(length, useLetters, useNumbers);
    System.out.println(generatedString);
    return generatedString;
  }
}
