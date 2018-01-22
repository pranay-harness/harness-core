package io.harness.exception;

public class SmallAlphabetException extends Exception {
  public SmallAlphabetException(String alphabet) {
    super("The alphabet \"" + alphabet + "\" is unsufficient");
  }
}
