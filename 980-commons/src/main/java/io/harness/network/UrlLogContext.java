/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.network;

import io.harness.logging.AutoLogContext;

public class UrlLogContext extends AutoLogContext {
  public static final String URL = "URL";

  public UrlLogContext(String url, OverrideBehavior behavior) {
    super(URL, url, behavior);
  }
}
