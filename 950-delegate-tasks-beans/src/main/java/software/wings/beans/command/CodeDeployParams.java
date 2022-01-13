/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.command;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Code deploy params.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeDeployParams {
  private String applicationName;
  private String deploymentConfigurationName;
  private String deploymentGroupName;
  private String region;
  private String bucket;
  private String key;
  private String bundleType;
  private boolean ignoreApplicationStopFailures;
  private boolean enableAutoRollback;
  private List<String> autoRollbackConfigurations;
  private String fileExistsBehavior;
  private int timeout;
}
