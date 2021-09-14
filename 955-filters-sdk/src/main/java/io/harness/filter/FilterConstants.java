/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.filter;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DX)
public class FilterConstants {
  public static final String CONNECTOR_FILTER = "Connector";
  public static final String PIPELINE_SETUP_FILTER = "PipelineSetup";
  public static final String TEMPLATE_FILTER = "Template";
  public static final String PIPELINE_EXECUTION_FILTER = "PipelineExecution";
  public static final String DEPLOYMENT_FILTER = "Deployment";
  public static final String AUDIT_FILTER = "Audit";
  public static final String DELEGATE_PROFILE_FILTER = "DelegateProfile";
  public static final String DELEGATE_FILTER = "Delegate";
}
