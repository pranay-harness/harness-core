/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public class OrchestrationStepTypes {
  private OrchestrationStepTypes() {}

  public static final String RESOURCE_RESTRAINT = "RESOURCE_RESTRAINT";
  public static final String FORK = "FORK";
  public static final String SECTION = "SECTION";
  public static final String DUMMY = "DUMMY";
  public static final String SECTION_CHAIN = "SECTION_CHAIN";
  public static final String DUMMY_SECTION = "DUMMY_SECTION";
  public static final String PIPELINE_SECTION = "PIPELINE_SECTION";
  public static final String NG_SECTION = "NG_SECTION";
  public static final String NG_FORK = "NG_FORK";
  public static final String STEP_GROUP = "STEP_GROUP";
  public static final String APPROVAL_STAGE = "APPROVAL_STAGE";
  public static final String FLAG_CONFIGURATION = "FLAG_CONFIGURATION";
}
