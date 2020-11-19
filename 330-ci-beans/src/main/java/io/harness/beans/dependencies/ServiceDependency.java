package io.harness.beans.dependencies;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

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
