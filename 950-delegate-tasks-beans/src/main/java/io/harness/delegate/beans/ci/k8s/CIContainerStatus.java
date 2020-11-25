package io.harness.delegate.beans.ci.k8s;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CIContainerStatus {
  public enum Status {
    SUCCESS,
    ERROR;
  }

  String name;
  String image;
  String startTime;
  String endTime;
  Status status;
  String errorMsg;
}
