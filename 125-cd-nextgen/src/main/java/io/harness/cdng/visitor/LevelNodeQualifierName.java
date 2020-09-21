package io.harness.cdng.visitor;

import static io.harness.cdng.pipeline.DeploymentStage.DEPLOYMENT_NAME;

import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.pipeline.stepinfo.StepSpecType;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;

public interface LevelNodeQualifierName {
  String ARTIFACT_SPEC_WRAPPER = "primary";
  String ARTIFACT_LIST_CONFIG = "artifacts";
  String ARTIFACT_OVERRIDE_SETS = "overrideSet";
  String DOCKER_HUB_NAME = ArtifactSourceConstants.DOCKER_HUB_NAME;
  String GCR_ARTIFACT_CONFIG = ArtifactSourceConstants.GCR_NAME;
  String SIDECAR_ARTIFACT_CONFIG = "sidecar";
  String ENVIRONMENT_YAML = "environment";
  String INFRASTRUCTURE_DEF = "infrastructureDefinition";
  String INFRA_USE_FROM_STAGE = "useFromStage";
  String INFRA_USE_FROM_STAGE_OVERRIDES = "overrides";
  String KUBERNETES_DIRECT = InfrastructureKind.KUBERNETES_DIRECT;
  String K8S_ROLLING_ROLLBACK = StepSpecType.K8S_ROLLING_ROLLBACK;
  String K8S_ROLLING_DEPLOY = StepSpecType.K8S_ROLLING_DEPLOY;
  String MANIFEST_CONFIG = "manifest";
  String MANIFEST_OVERRIDE_SETS = "overrideSet";
  String STORE_CONFIG_WRAPPER = "store";
  String K8S_MANIFEST = ManifestType.K8Manifest;
  String VALUE_MANIFEST = ManifestType.VALUES;
  String CDPIPELINE = "pipeline";
  String DEPLOYMENT_STAGE = DEPLOYMENT_NAME;
  String PIPELINE_INFRASTRUCTURE = "infrastructure";
  String HTTP_STEP = StepSpecType.HTTP;
  String SHELL_SCRIPT_STEP = StepSpecType.SHELL_SCRIPT;
  String KUBERNETES_SERVICE_SPEC = "spec";
  String SERVICE_CONFIG = "service";
  String SERVICE_DEFINITION = "serviceDefinition";
  String SERVICE_USE_FROM_STAGE = "useFromStage";
  String SERVICE_USE_FROM_STAGE_OVERRIDES = "overrides";
  String STAGE_OVERRIDES_CONFIG = "stageOverrides";
  String STAGE_VARIABLES = "stageVariables";
}
