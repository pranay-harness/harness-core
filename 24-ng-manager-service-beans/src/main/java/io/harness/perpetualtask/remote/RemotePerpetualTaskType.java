package io.harness.perpetualtask.remote;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RemotePerpetualTaskType {
  REMOTE_SAMPLE("REMOTE_SAMPLE", "ng-manager");

  private final String taskType;
  private final String ownerServiceId;
}
