/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resource;

import io.harness.exception.LoadResourceException;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.Charset;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Loader {
  public static String load(String resource) {
    try {
      return Resources.toString(Resources.getResource(resource), Charset.defaultCharset());
    } catch (IOException e) {
      throw new LoadResourceException(resource, e);
    }
  }
}
