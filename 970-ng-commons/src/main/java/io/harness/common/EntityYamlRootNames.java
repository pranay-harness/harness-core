package io.harness.common;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

/**
 * Name of the top element in yaml.
 * For example:
 * <p>connector:
 * name: testname
 * projectIdentifier: projectId
 * </p>
 * In this the top element is <b>connector</b>
 */
@OwnedBy(PL)
public class EntityYamlRootNames {
  public static String PROJECT = "project";
  public static String PIPELINE = "pipeline";
  public static String PIPELINE_STEP = "pipelineStep";
  public static String CONNECTOR = "connector";
  public static String SECRET = "secret";
  public static String SERVICE = "service";
  public static String ENVIRONMENT = "environment";
  public static String INPUT_SET = "inputSet";
  public static String CV_CONFIG = "cvConfig";
  public static String DELEGATE = "delegate";
  public static String DELEGATE_CONFIGURATION = "delegateConfigurations";
  public static String CV_VERIFICATION_JOB = "cvVerificationJob";
  public static String INTEGRATION_STAGE = "integrationStage";
  public static String INTEGRATION_STEP = "integrationSteps";
  public static String CV_KUBERNETES_ACTIVITY_SOURCE = "cvKubernetesActivitySource";
  public static String DEPLOYMENT_STEP = "deploymentSteps";
  public static String DEPLOYMENT_STAGE = "deploymentStage";
  public static String APPROVAL_STAGE = "approvalStage";
  public static String TRIGGERS = "trigger";
}
