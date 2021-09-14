/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
