package io.harness.perpetualtask;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.Map;

@Data
@FieldNameConstants(innerTypeName = "PerpetualTaskClientContextKeys")
public class PerpetualTaskClientContext {
  private Map<String, String> clientParams;
  /* What is the purpose of the taskId field, since it is not used anywhere */
  @Deprecated private transient String taskId;
  private long lastContextUpdated;

  public PerpetualTaskClientContext(Map<String, String> clientParams) {
    this.clientParams = clientParams;
  }
}
