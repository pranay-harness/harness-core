/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
