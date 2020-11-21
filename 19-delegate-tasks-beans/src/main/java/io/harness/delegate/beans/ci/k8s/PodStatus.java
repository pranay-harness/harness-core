package io.harness.delegate.beans.ci.k8s;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

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
