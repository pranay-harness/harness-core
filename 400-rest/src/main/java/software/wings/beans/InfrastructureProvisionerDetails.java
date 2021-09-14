/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.model.ARMResourceType;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class InfrastructureProvisionerDetails {
  private String uuid;
  private String name;
  private String description;
  private String infrastructureProvisionerType;
  private String repository;
  private Map<String, String> services;
  private String cloudFormationSourceType;
  private ARMResourceType azureARMResourceType;
  private transient List<HarnessTagLink> tagLinks;
}
