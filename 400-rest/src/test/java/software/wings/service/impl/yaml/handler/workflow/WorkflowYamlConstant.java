/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

public class WorkflowYamlConstant {
  private static final String resourcePath = "400-rest/src/test/resources/workflows/";

  public static final String BASIC_VALID_YAML_CONTENT_RESOURCE_PATH = resourcePath + "basicValidWorkflow.yaml";
  public static final String BASIC_VALID_YAML_CONTENT_TEMPLATIZED_RESOURCE_PATH =
      resourcePath + "basicValidWorkflowTemplatized.yaml";
  public static final String BASIC_VALID_YAML_CONTENT_WITH_MULTILINE_USER_INPUT_RESOURCE_PATH =
      resourcePath + "basicValidWithMultilineInput.yaml";
  public static final String BASIC_VALID_YAML_FILE_PATH_PREFIX = "Setup/Applications/APP_NAME/Workflows/";
  public static final String BASIC_INVALID_YAML_CONTENT = "envName: env1\nphaseInvalid: phase1\ntype: BASIC";
  public static final String BASIC_INVALID_YAML_FILE_PATH = "Setup/Applications/APP_NAME/WorkflowsInvalid/basic.yaml";

  // END STRING CONSTANTS FOR BASIC WORKFLOW YAML TEST

  // START STRING CONSTANTS FOR BUILD WORKFLOW YAML TEST
  public static final String BUILD_VALID_YAML_CONTENT_RESOURCE_PATH = resourcePath + "buildValidWorkflow.yaml";

  // START STRING CONSTANTS FOR BUILD WORKFLOW YAML TEST
  public static final String BUILD_VALID_YAML_CONTENT_INFRA_DEF_RESOURCE_PATH =
      resourcePath + "buildValidWorkflowWithInfra.yaml";

  public static final String BUILD_VALID_YAML_USER_GROUP_RESOURCE_PATH =
      resourcePath + "buildValidWorkflowWithUserGroup.yaml";

  public static final String BUILD_VALID_YAML_USER_GROUP_TEMPLATIZED2_RESOURCE_PATH =
      resourcePath + "buildValidWorkflowTemplatized.yaml";

  public static final String BUILD_VALID_JIRA_RESOURCE_PATH = resourcePath + "buildWorkflowJira.yaml";

  public static final String BUILD_VALID_SERVICENOW_RESOURCE_PATH = resourcePath + "buildServiceNow.yaml";

  public static final String BUILD_VALID_LINKED_SHELL_RESOURCE_PATH = resourcePath + "buildLinkedShellScript.yaml";

  public static final String BUILD_VALID_YAML_FILE_PATH = "Setup/Applications/APP_NAME/Workflows/build.yaml";
  public static final String BUILD_VALID_YAML_FILE_PATH2 = "Setup/Applications/APP_NAME/Workflows/test.yaml";
  public static final String BUILD_INVALID_YAML_CONTENT_RESOURCE_PATH = resourcePath + "buildInvalidWorkflow.yaml";
  public static final String BUILD_INVALID_YAML_FILE_PATH = "Setup/Applications/APP_NAME/WorkflowsInvalid/build.yaml";
  // END STRING CONSTANTS FOR BUILD WORKFLOW YAML TEST

  // START STRING CONSTANTS FOR CANARY WORKFLOW YAML TEST
  public static final String CANARY_VALID_YAML_CONTENT_RESOURCE_PATH = resourcePath + "canaryValidWorkflow.yaml";

  public static final String CANARY_VALID_YAML_FILE_PATH = "Setup/Applications/APP_NAME/Workflows/canary.yaml";
  public static final String CANARY_INVALID_YAML_CONTENT = "envName: env1\nphaseInvalid: phase1\ntype: CANARY";
  public static final String CANARY_INVALID_YAML_FILE_PATH = "Setup/Applications/APP_NAME/WorkflowsInvalid/canary.yaml";
  // END STRING CONSTANTS FOR CANARY WORKFLOW YAML TEST

  // START STRING CONSTANTS FOR ROLLING WORKFLOW YAML TEST
  public static final String ROLLING_VALID_YAML_CONTENT_RESOURCE_PATH = resourcePath + "rollingValidWorkflow.yaml";
  public static final String ROLLING_JENKINS_YAML_CONTENT_RESOURCE_PATH = resourcePath + "rollingJenkins.yaml";
  public static final String ROLLING_GCB_YAML_CONTENT_RESOURCE_PATH = resourcePath + "rollingGCB.yaml";
  public static final String ROLLING_BAMBOO_YAML_CONTENT_RESOURCE_PATH = resourcePath + "rollingBamboo.yaml";
  public static final String ROLLING_RESOURCE_CONSTRAINT_RESOURCE_PATH =
      resourcePath + "rollingResourceConstraint.yaml";
  public static final String ROLLING_RESOURCE_CONSTRAINT_RESOURCE_PATH2 =
      resourcePath + "rollingResourceConstraintEdited.yaml";

  public static final String ROLLING_VALID_YAML_FILE_PATH = "Setup/Applications/APP_NAME/Workflows/rolling.yaml";
  public static final String ROLLING_INVALID_YAML_CONTENT = "envName: env1\nphaseInvalid: phase1\ntype: ROLLING";
  public static final String ROLLING_INVALID_YAML_FILE_PATH =
      "Setup/Applications/APP_NAME/WorkflowsInvalid/rolling.yaml";
  // END STRING CONSTANTS FOR ROLLING WORKFLOW YAML TEST

  // START STRING CONSTANTS FOR BLUE_GREEN WORKFLOW YAML TEST
  public static final String BLUE_GREEN_VALID_YAML_CONTENT_RESOURCE_PATH = resourcePath + "blueGreenValidWorkflow.yaml";

  public static final String BLUE_GREEN_VALID_YAML_FILE_PATH = "Setup/Applications/APP_NAME/Workflows/blueGreen.yaml";
  public static final String BLUE_GREEN_INVALID_YAML_CONTENT = "envName: env1\nphaseInvalid: phase1\ntype: BLUE_GREEN";
  public static final String BLUE_GREEN_INVALID_YAML_FILE_PATH =
      "Setup/Applications/APP_NAME/WorkflowsInvalid/blueGreen.yaml";
  // END STRING CONSTANTS FOR BLUE_GREEN WORKFLOW YAML TEST

  // START STRING CONSTANTS FOR MULTI_SERVICE WORKFLOW YAML TEST
  public static final String MULTI_SERVICE_VALID_YAML_CONTENT_RESOURCE_PATH =
      resourcePath + "multiServiceValidWorkflow.yaml";
  public static final String MULTI_SERVICE_VARIABLE_OVERRIDE_RESOURCE_PATH =
      resourcePath + "multiServiceWorkflowWithVariableOverride.yaml";
  public static final String MULTI_SERVICE_VALID_YAML_FILE_PATH =
      "Setup/Applications/APP_NAME/Workflows/multiService.yaml";
  public static final String MULTI_SERVICE_INVALID_YAML_CONTENT =
      "envName: env1\nphaseInvalid: phase1\ntype: MULTI_SERVICE";
  public static final String MULTI_SERVICE_INVALID_YAML_FILE_PATH =
      "Setup/Applications/APP_NAME/WorkflowsInvalid/multiService.yaml";
  // END STRING CONSTANTS FOR MULTI_SERVICE WORKFLOW YAML TEST
}
