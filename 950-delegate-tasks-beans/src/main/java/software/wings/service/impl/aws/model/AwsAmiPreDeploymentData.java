/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.Collections.emptyList;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class AwsAmiPreDeploymentData {
  private String oldAsgName;
  private Integer minCapacity;
  private Integer desiredCapacity;
  private List<String> scalingPolicyJSON;
  private List<String> scheduledActionJSONs;

  public static final int DEFAULT_DESIRED_COUNT = 10;

  public int getPreDeploymentMinCapacity() {
    return minCapacity == null ? 0 : minCapacity;
  }

  public int getPreDeploymentDesiredCapacity() {
    return desiredCapacity == null ? DEFAULT_DESIRED_COUNT : desiredCapacity;
  }

  public boolean hasAsgReachedPreDeploymentCount(int desiredCount) {
    if (desiredCapacity == null) {
      return false;
    }

    return desiredCapacity.intValue() == desiredCount;
  }

  public List<String> getPreDeploymenyScalingPolicyJSON() {
    return isNotEmpty(scalingPolicyJSON) ? scalingPolicyJSON : emptyList();
  }
}
