package io.harness.beans.dependencies;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("serviceDependency")
public class ServiceDependency {
  @TypeAlias("serviceDependency_status")
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
