package io.harness.cdng.pipeline.steps;

public class NGStepTypes {
  private NGStepTypes() {}

  public static final String SERVICE_STEP = "SERVICE_STEP";
  public static final String ARTIFACT_STEP = "ARTIFACT_STEP";
  public static final String ARTIFACT_FORK_STEP = "ARTIFACT_FORK_STEP";
  public static final String MANIFEST_STEP = "MANIFEST_STEP";
  public static final String MANIFEST_FETCH = "MANIFEST_FETCH";
  public static final String PIPELINE_SETUP = "PIPELINE_SETUP";
  public static final String INFRASTRUCTURE_SECTION = "INFRASTRUCTURE_SECTION";
  public static final String ENVIRONMENT = "ENVIRONMENT";
  public static final String INFRASTRUCTURE = "INFRASTRUCTURE";
  public static final String DEPLOYMENT_STAGE_STEP = "DEPLOYMENT_STAGE_STEP";
  public static final String K8S_ROLLING = "K8S_ROLLING";
  public static final String K8S_ROLLBACK_ROLLING = "K8S_ROLLBACK_ROLLING";
}
