package io.harness.execution.export;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.export.request.ExportExecutionsRequest;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

@OwnedBy(CDC)
public class ExportExecutionsRequestLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.calculateLogKeyForId(ExportExecutionsRequest.class);

  public ExportExecutionsRequestLogContext(String requestId, OverrideBehavior behavior) {
    super(ID, requestId, behavior);
  }
}
