package io.harness.network;

import io.harness.logging.AutoLogContext;

public class UrlLogContext extends AutoLogContext {
  public static final String URL = "URL";

  public UrlLogContext(String url, OverrideBehavior behavior) {
    super(URL, url, behavior);
  }
}
