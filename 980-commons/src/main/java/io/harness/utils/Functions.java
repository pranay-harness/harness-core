package io.harness.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Functions {
  public <T> void doNothing(T instance) {
    /*This method is used as a Blank Consumer<T>*/
  }

  public boolean staticTruth() {
    return true;
  }
}
