/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.freeze.beans;

import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;

import com.fasterxml.jackson.annotation.JsonProperty;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = false,
    components = {HarnessModuleComponent.CDS_DEPLOYMENT_FREEZE})
public enum FreezeEntityType {
  @JsonProperty("Service") SERVICE,
  @JsonProperty("Project") PROJECT,
  @JsonProperty("Org") ORG,
  @JsonProperty("Environment") ENVIRONMENT,
  @JsonProperty("EnvType") ENV_TYPE,
  @JsonProperty("Pipeline") PIPELINE;
}
