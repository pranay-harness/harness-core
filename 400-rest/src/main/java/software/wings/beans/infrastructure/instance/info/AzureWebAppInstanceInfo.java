/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.infrastructure.instance.info;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class AzureWebAppInstanceInfo extends InstanceInfo {
  private String instanceId;
  private String appName;
  private String slotName;
  private String slotId;
  private String appServicePlanId;
  private String host;
  private String state;
  private String instanceType;
}
