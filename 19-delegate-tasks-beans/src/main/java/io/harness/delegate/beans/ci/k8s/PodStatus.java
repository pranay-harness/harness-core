package io.harness.delegate.beans.ci.k8s;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PodStatus {
  public enum Status {
    RUNNING,
    PENDING,
    ERROR;
  }
  Status status;
  String errorMessage;
  List<CIContainerStatus> ciContainerStatusList;
}
