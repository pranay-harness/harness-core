package io.harness.mongo;

import io.harness.logging.AutoLogContext;

public class IndexLogContext extends AutoLogContext {
  public static final String ID = "indexName";

  public IndexLogContext(String indexName, OverrideBehavior behavior) {
    super(ID, indexName, behavior);
  }
}
