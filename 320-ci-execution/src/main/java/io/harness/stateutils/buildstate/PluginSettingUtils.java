package io.harness.stateutils.buildstate;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static org.springframework.util.StringUtils.trimLeadingCharacter;
import static org.springframework.util.StringUtils.trimTrailingCharacter;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.steps.stepinfo.DockerStepInfo;
import io.harness.beans.steps.stepinfo.ECRStepInfo;
import io.harness.beans.steps.stepinfo.GCRStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheGCSStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheS3StepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheGCSStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheS3StepInfo;
import io.harness.beans.steps.stepinfo.UploadToGCSStepInfo;
import io.harness.beans.steps.stepinfo.UploadToS3StepInfo;
import io.harness.exception.InvalidArgumentsException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PluginSettingUtils {
  public static final String PLUGIN_REGISTRY = "PLUGIN_REGISTRY";
  public static final String PLUGIN_REPO = "PLUGIN_REPO";
  public static final String PLUGIN_TAGS = "PLUGIN_TAGS";
  public static final String PLUGIN_DOCKERFILE = "PLUGIN_DOCKERFILE";
  public static final String PLUGIN_CONTEXT = "PLUGIN_CONTEXT";
  public static final String PLUGIN_TARGET = "PLUGIN_TARGET";
  public static final String PLUGIN_BUILD_ARGS = "PLUGIN_BUILD_ARGS";
  public static final String PLUGIN_CUSTOM_LABELS = "PLUGIN_CUSTOM_LABELS";
  public static final String PLUGIN_PATH = "PLUGIN_PATH";
  public static final String PLUGIN_MOUNT = "PLUGIN_MOUNT";
  public static final String PLUGIN_BUCKET = "PLUGIN_BUCKET";
  public static final String PLUGIN_ENDPOINT = "PLUGIN_ENDPOINT";
  public static final String PLUGIN_REGION = "PLUGIN_REGION";
  public static final String PLUGIN_SOURCE = "PLUGIN_SOURCE";
  public static final String PLUGIN_RESTORE = "PLUGIN_RESTORE";
  public static final String PLUGIN_REBUILD = "PLUGIN_REBUILD";
  public static final String PLUGIN_FILENAME = "PLUGIN_FILENAME";
  public static final String PLUGIN_ROOT = "PLUGIN_ROOT";
  public static final String TAR = ".tar";

  public static Map<String, String> getPluginCompatibleEnvVariables(PluginCompatibleStep stepInfo, String identifier) {
    switch (stepInfo.getNonYamlInfo().getStepInfoType()) {
      case ECR:
        return getECRStepInfoEnvVariables((ECRStepInfo) stepInfo, identifier);
      case GCR:
        return getGCRStepInfoEnvVariables((GCRStepInfo) stepInfo, identifier);
      case DOCKER:
        return getDockerStepInfoEnvVariables((DockerStepInfo) stepInfo, identifier);
      case UPLOAD_GCS:
        return getUploadToGCSStepInfoEnvVariables((UploadToGCSStepInfo) stepInfo, identifier);
      case UPLOAD_S3:
        return getUploadToS3StepInfoEnvVariables((UploadToS3StepInfo) stepInfo, identifier);
      case SAVE_CACHE_GCS:
        return getSaveCacheGCSStepInfoEnvVariables((SaveCacheGCSStepInfo) stepInfo, identifier);
      case RESTORE_CACHE_GCS:
        return getRestoreCacheGCSStepInfoEnvVariables((RestoreCacheGCSStepInfo) stepInfo, identifier);
      case SAVE_CACHE_S3:
        return getSaveCacheS3StepInfoEnvVariables((SaveCacheS3StepInfo) stepInfo, identifier);
      case RESTORE_CACHE_S3:
        return getRestoreCacheS3StepInfoEnvVariables((RestoreCacheS3StepInfo) stepInfo, identifier);
      default:
        throw new IllegalStateException("Unexpected value: " + stepInfo.getNonYamlInfo().getStepInfoType());
    }
  }

  private static Map<String, String> getGCRStepInfoEnvVariables(GCRStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_REGISTRY, stepInfo.getRegistry());
    setMandatoryEnvironmentVariable(map, PLUGIN_REPO, stepInfo.getRepo());
    setMandatoryEnvironmentVariable(map, PLUGIN_TAGS, listToStringSlice(stepInfo.getTags()));

    setOptionalEnvironmentVariable(map, PLUGIN_DOCKERFILE, stepInfo.getDockerfile());
    setOptionalEnvironmentVariable(map, PLUGIN_CONTEXT, stepInfo.getContext());
    setOptionalEnvironmentVariable(map, PLUGIN_TARGET, stepInfo.getTarget());
    setOptionalEnvironmentVariable(map, PLUGIN_BUILD_ARGS, listToStringSlice(stepInfo.getBuildArgs()));
    setOptionalEnvironmentVariable(map, PLUGIN_CUSTOM_LABELS, mapToStringSlice(stepInfo.getLabels()));

    return map;
  }

  private static Map<String, String> getECRStepInfoEnvVariables(ECRStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_REGISTRY, stepInfo.getRegistry());
    setMandatoryEnvironmentVariable(map, PLUGIN_REPO, stepInfo.getRepo());
    setMandatoryEnvironmentVariable(map, PLUGIN_TAGS, listToStringSlice(stepInfo.getTags()));

    setOptionalEnvironmentVariable(map, PLUGIN_DOCKERFILE, stepInfo.getDockerfile());
    setOptionalEnvironmentVariable(map, PLUGIN_CONTEXT, stepInfo.getContext());
    setOptionalEnvironmentVariable(map, PLUGIN_TARGET, stepInfo.getTarget());
    setOptionalEnvironmentVariable(map, PLUGIN_BUILD_ARGS, listToStringSlice(stepInfo.getBuildArgs()));
    setOptionalEnvironmentVariable(map, PLUGIN_CUSTOM_LABELS, mapToStringSlice(stepInfo.getLabels()));

    return map;
  }

  private static Map<String, String> getDockerStepInfoEnvVariables(DockerStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_REPO, stepInfo.getRepo().getValue());
    setMandatoryEnvironmentVariable(map, PLUGIN_TAGS,
        listToStringSlice(
            RunTimeInputHandler.resolveListParameter("tags", "DockerHub", identifier, stepInfo.getTags(), true)));

    String dockerFile = RunTimeInputHandler.resolveStringParameter(
        "dockerfile", "DockerHub", identifier, stepInfo.getDockerfile(), false);
    if (dockerFile != null && !dockerFile.equals(RunTimeInputHandler.UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_DOCKERFILE, dockerFile);
    }

    String context =
        RunTimeInputHandler.resolveStringParameter("context", "DockerHub", identifier, stepInfo.getContext(), false);
    if (context != null && !context.equals(RunTimeInputHandler.UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_CONTEXT, context);
    }

    String target =
        RunTimeInputHandler.resolveStringParameter("target", "DockerHub", identifier, stepInfo.getTarget(), false);
    if (target != null && !target.equals(RunTimeInputHandler.UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_TARGET, target);
    }

    List<String> buildArgs =
        RunTimeInputHandler.resolveListParameter("buildArgs", "DockerHub", identifier, stepInfo.getBuildArgs(), false);

    if (isNotEmpty(buildArgs)) {
      setOptionalEnvironmentVariable(map, PLUGIN_BUILD_ARGS, listToStringSlice(buildArgs));
    }

    Map<String, String> labels =
        RunTimeInputHandler.resolveMapParameter("labels", "DockerHub", identifier, stepInfo.getLabels(), false);

    if (isNotEmpty(labels)) {
      setOptionalEnvironmentVariable(map, PLUGIN_CUSTOM_LABELS, mapToStringSlice(labels));
    }

    return map;
  }

  private static Map<String, String> getRestoreCacheGCSStepInfoEnvVariables(
      RestoreCacheGCSStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_FILENAME,
        RunTimeInputHandler.resolveStringParameter("key", "RestoreCacheGCS", identifier, stepInfo.getKey(), true)
            + TAR);
    setMandatoryEnvironmentVariable(map, PLUGIN_BUCKET,
        RunTimeInputHandler.resolveStringParameter(
            "bucket", "RestoreCacheGCS", identifier, stepInfo.getBucket(), true));

    setMandatoryEnvironmentVariable(map, PLUGIN_RESTORE, "true");

    String target = RunTimeInputHandler.resolveStringParameter(
        "target", "RestoreCacheGCS", identifier, stepInfo.getTarget(), false);
    if (target != null && !target.equals(RunTimeInputHandler.UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_PATH, target);
    }

    return map;
  }

  private static Map<String, String> getSaveCacheGCSStepInfoEnvVariables(
      SaveCacheGCSStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_FILENAME,
        RunTimeInputHandler.resolveStringParameter("key", "SaveCacheGCS", identifier, stepInfo.getKey(), true) + TAR);

    setMandatoryEnvironmentVariable(map, PLUGIN_BUCKET,
        RunTimeInputHandler.resolveStringParameter("bucket", "SaveCacheGCS", identifier, stepInfo.getBucket(), true));

    List<String> sourcePath = RunTimeInputHandler.resolveListParameter(
        "sourcePath", "SaveCacheGCS", identifier, stepInfo.getSourcePath(), true);

    setMandatoryEnvironmentVariable(map, PLUGIN_MOUNT, listToStringSlice(sourcePath));
    setMandatoryEnvironmentVariable(map, PLUGIN_REBUILD, "true");

    String target =
        RunTimeInputHandler.resolveStringParameter("target", "SaveCacheGCS", identifier, stepInfo.getTarget(), false);
    if (target != null && !target.equals(RunTimeInputHandler.UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_PATH, target);
    }

    return map;
  }

  private static Map<String, String> getRestoreCacheS3StepInfoEnvVariables(
      RestoreCacheS3StepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_FILENAME,
        RunTimeInputHandler.resolveStringParameter("key", "RestoreCacheS3", identifier, stepInfo.getKey(), true) + TAR);

    setMandatoryEnvironmentVariable(map, PLUGIN_ROOT,
        RunTimeInputHandler.resolveStringParameter("bucket", "RestoreCacheS3", identifier, stepInfo.getBucket(), true));

    setMandatoryEnvironmentVariable(map, PLUGIN_RESTORE, "true");

    String target =
        RunTimeInputHandler.resolveStringParameter("target", "RestoreCacheS3", identifier, stepInfo.getTarget(), false);
    if (target != null && !target.equals(RunTimeInputHandler.UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_PATH, target);
    }

    String endpoint = RunTimeInputHandler.resolveStringParameter(
        "endpoint", "RestoreCacheS3", identifier, stepInfo.getEndpoint(), false);
    if (endpoint != null && !endpoint.equals(RunTimeInputHandler.UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_ENDPOINT, endpoint);
    }

    return map;
  }

  private static Map<String, String> getSaveCacheS3StepInfoEnvVariables(
      SaveCacheS3StepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_FILENAME, stepInfo.getKey() + TAR);
    setMandatoryEnvironmentVariable(map, PLUGIN_ROOT, stepInfo.getBucket());
    setMandatoryEnvironmentVariable(map, PLUGIN_MOUNT, listToStringSlice(stepInfo.getSourcePath()));
    setMandatoryEnvironmentVariable(map, PLUGIN_REBUILD, "true");

    setOptionalEnvironmentVariable(map, PLUGIN_PATH, stepInfo.getTarget());
    setOptionalEnvironmentVariable(map, PLUGIN_ENDPOINT, stepInfo.getEndpoint());
    return map;
  }

  private static Map<String, String> getUploadToGCSStepInfoEnvVariables(
      UploadToGCSStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_SOURCE, stepInfo.getSourcePath());
    String target = null;
    if (isNotEmpty(stepInfo.getBucket()) && isNotEmpty(stepInfo.getTarget())) {
      target = format(
          "%s/%s", trimTrailingCharacter(stepInfo.getBucket(), '/'), trimLeadingCharacter(stepInfo.getTarget(), '/'));
    }
    setMandatoryEnvironmentVariable(map, PLUGIN_TARGET, target);

    return map;
  }

  private static Map<String, String> getUploadToS3StepInfoEnvVariables(UploadToS3StepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_BUCKET, stepInfo.getBucket());
    setMandatoryEnvironmentVariable(map, PLUGIN_SOURCE, stepInfo.getSourcePath());
    setMandatoryEnvironmentVariable(map, PLUGIN_TARGET, stepInfo.getTarget());

    setOptionalEnvironmentVariable(map, PLUGIN_ENDPOINT, stepInfo.getEndpoint());
    setOptionalEnvironmentVariable(map, PLUGIN_REGION, stepInfo.getRegion());

    return map;
  }

  // converts map "key1":"value1","key2":"value2" to string "key1=value1,key2=value2"
  private String mapToStringSlice(Map<String, String> map) {
    if (isEmpty(map)) {
      return "";
    }
    StringBuilder mapAsString = new StringBuilder();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      mapAsString.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
    }
    mapAsString.deleteCharAt(mapAsString.length() - 1);
    return mapAsString.toString();
  }

  // converts list "value1", "value2" to string "value1,value2"
  private String listToStringSlice(List<String> stringList) {
    if (isEmpty(stringList)) {
      return "";
    }
    StringBuilder listAsString = new StringBuilder();
    for (String value : stringList) {
      listAsString.append(value).append(",");
    }
    listAsString.deleteCharAt(listAsString.length() - 1);
    return listAsString.toString();
  }

  private static void setOptionalEnvironmentVariable(Map<String, String> envVarMap, String var, String value) {
    if (isEmpty(value)) {
      return;
    }
    envVarMap.put(var, value);
  }
  private static void setMandatoryEnvironmentVariable(Map<String, String> envVarMap, String var, String value) {
    if (isEmpty(value)) {
      throw new InvalidArgumentsException(format("Environment variable %s can't be empty or null", var));
    }
    envVarMap.put(var, value);
  }
}
