/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api.instancedetails;

import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class InstanceApiResponse {
  private List<String> instances;
  @Getter(AccessLevel.NONE) private Integer newInstanceTrafficPercent;
  private boolean skipVerification;

  public Optional<Integer> getNewInstanceTrafficPercent() {
    return Optional.ofNullable(newInstanceTrafficPercent);
  }
}
