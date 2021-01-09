package io.harness.cdng.visitor;

import io.harness.cdng.manifest.ManifestType;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.walktree.visitor.utilities.VisitorParentPathUtils;

public interface YamlTypes {
  String PRIMARY_ARTIFACT = "primary";
  String ARTIFACT_LIST_CONFIG = "artifacts";
  String ARTIFACT_OVERRIDE_SETS = "artifactOverrideSets";
  String SIDECAR_ARTIFACT_CONFIG = "sidecar";
  String SIDECARS_ARTIFACT_CONFIG = "sidecars";
  String ENVIRONMENT_YAML = "environment";
  String INFRASTRUCTURE_DEF = "infrastructureDefinition";
  String INFRA_USE_FROM_STAGE = "useFromStage";
  String INFRA_USE_FROM_STAGE_OVERRIDES = "overrides";

  String K8S_ROLLING_ROLLBACK = StepSpecTypeConstants.K8S_ROLLING_ROLLBACK;
  String K8S_ROLLING_DEPLOY = StepSpecTypeConstants.K8S_ROLLING_DEPLOY;
  String K8S_BLUE_GREEN_DEPLOY = StepSpecTypeConstants.K8S_BLUE_GREEN_DEPLOY;
  String K8S_APPLY = StepSpecTypeConstants.K8S_APPLY;

  String MANIFEST_LIST_CONFIG = "manifests";
  String MANIFEST_CONFIG = "manifest";
  String MANIFEST_OVERRIDE_SETS = "manifestOverrideSets";
  String K8S_MANIFEST = ManifestType.K8Manifest;
  String SPEC = "spec";
  String PIPELINE_INFRASTRUCTURE = "infrastructure";
  String HTTP_STEP = StepSpecTypeConstants.HTTP;
  String SHELL_SCRIPT_STEP = StepSpecTypeConstants.SHELL_SCRIPT;
  String SERVICE_CONFIG = "service";
  String SERVICE_DEFINITION = "serviceDefinition";
  String SERVICE_SPEC = "spec";
  String SERVICE_USE_FROM_STAGE = "useFromStage";
  String SERVICE_USE_FROM_STAGE_OVERRIDES = "overrides";
  String STAGE_OVERRIDES_CONFIG = "stageOverrides";
  String PATH_CONNECTOR = VisitorParentPathUtils.PATH_CONNECTOR;
  String VARIABLE_OVERRIDE_SETS = "variableOverrideSets";
  String CONNECTOR_REF = "connectorRef";
  String TAG = "tag";
  String TAG_REGEX = "tagRegex";
  String IMAGE_PATH = "imagePath";
  String BRANCH = "branch";
  String COMMIT_ID = "commitId";
  String NAMESPACE = "namespace";
  String RELEASE_NAME = "releaseName";
  String STORE_CONFIG_WRAPPER = "store";

  String SKIP_DRY_RUN = "skipDryRun";
  String OUTPUT = "output";
}
