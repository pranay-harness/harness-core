package software.wings.helpers.ext.k8s.response;

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
}
