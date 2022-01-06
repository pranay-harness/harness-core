/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
