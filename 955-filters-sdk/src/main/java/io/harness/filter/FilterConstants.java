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
  public static final String IDENTIFIER_LIST =
      "This is the list of the Entity Identifiers on which the filter will be applied.";
  public static final String TYPE_LIST = "This is the list of the ENTITY types on which the filter will be applied.";
  public static final String SEARCH_TERM = "Text to search/filter the Entity.";
  public static final String IGNORE_CASE =
      "Boolean value to indicate if the case of the searched term should be ignored while filtering the Entity";
}
