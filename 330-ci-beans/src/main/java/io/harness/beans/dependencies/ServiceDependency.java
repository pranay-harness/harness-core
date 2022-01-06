/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
