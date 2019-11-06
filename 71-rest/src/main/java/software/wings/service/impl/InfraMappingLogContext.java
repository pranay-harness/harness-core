package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class InfraMappingLogContext extends AutoLogContext {
  public static final String ID = "infraMappingId";

  public InfraMappingLogContext(String infraMappingId, OverrideBehavior behavior) {
    super(ID, infraMappingId, behavior);
  }
}
