/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.infra.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.pipeline.NGStepType.SHELL_SCRIPT;
import static io.harness.cdng.pipeline.NGStepType.TERRAFORM_APPLY;
import static io.harness.cdng.pipeline.NGStepType.TERRAFORM_DESTROY;
import static io.harness.cdng.pipeline.NGStepType.TERRAFORM_PLAN;
import static io.harness.cdng.pipeline.NGStepType.TERRAFORM_ROLLBACK;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.NGStepType;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.Getter;

@OwnedBy(CDP)
public enum ProvisionerType {
  TERRAFORM("Terraform"),
  CLOUD_FORMATION("CloudFormation"),
  AZURE_ARM("ARM"),
  SHELL_SCRIPT_PROVISIONER("Script");

  private static final Set<ProvisionerType> supportedTypes = ImmutableSet.of(TERRAFORM);
  private static final List<NGStepType> supportedSteps =
      Arrays.asList(TERRAFORM_APPLY, TERRAFORM_PLAN, TERRAFORM_DESTROY, TERRAFORM_ROLLBACK, SHELL_SCRIPT);

  @Getter private final String displayName;
  ProvisionerType(String displayName) {
    this.displayName = displayName;
  }

  public static boolean isSupported(ProvisionerType type) {
    return supportedTypes.contains(type);
  }
  public static List<NGStepType> getSupportedSteps() {
    return supportedSteps;
  }
}
