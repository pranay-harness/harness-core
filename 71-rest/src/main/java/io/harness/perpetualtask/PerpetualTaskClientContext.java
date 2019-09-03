package io.harness.perpetualtask;

import lombok.Data;

import java.util.Map;

@Data
public class PerpetualTaskClientContext {
  private Map<String, String> clientParams;
  private transient String taskId;

  public PerpetualTaskClientContext(Map<String, String> clientParams) {
    this.clientParams = clientParams;
  }
}
