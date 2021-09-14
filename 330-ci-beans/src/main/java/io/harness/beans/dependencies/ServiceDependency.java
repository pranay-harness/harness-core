/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.dependencies;

import java.util.List;
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
    SUCCESS("Success"),
    ERROR("Failed");
    String displayName;
    Status(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  @NotNull String identifier;
  String name;
  @NotNull String image;
  String status;
  String startTime;
  String endTime;
  String errorMessage;
  List<String> logKeys;
}
