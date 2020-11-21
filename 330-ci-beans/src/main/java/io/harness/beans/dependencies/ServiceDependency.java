package io.harness.beans.dependencies;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceDependency {
  public enum Status {
    SUCCESS,
    ERROR;
  }

  @NotNull String identifier;
  String name;
  @NotNull String image;
  Status status;
  String startTime;
  String endTime;
  String errorMessage;
}
