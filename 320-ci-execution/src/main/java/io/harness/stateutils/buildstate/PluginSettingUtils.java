package io.harness.stateutils.buildstate;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static org.springframework.util.StringUtils.trimLeadingCharacter;
import static org.springframework.util.StringUtils.trimTrailingCharacter;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
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

  public static Map<String, String> getPluginCompatibleEnvVariables(PluginCompatibleStep stepInfo) {
    switch (stepInfo.getNonYamlInfo().getStepInfoType()) {
      case ECR:
        return getECRStepInfoEnvVariables((ECRStepInfo) stepInfo);
      case GCR:
        return getGCRStepInfoEnvVariables((GCRStepInfo) stepInfo);
      case DOCKER:
        return getDockerStepInfoEnvVariables((DockerStepInfo) stepInfo);
      case UPLOAD_GCS:
        return getUploadToGCSStepInfoEnvVariables((UploadToGCSStepInfo) stepInfo);
      case UPLOAD_S3:
        return getUploadToS3StepInfoEnvVariables((UploadToS3StepInfo) stepInfo);
      case SAVE_CACHE_GCS:
        return getSaveCacheGCSStepInfoEnvVariables((SaveCacheGCSStepInfo) stepInfo);
      case RESTORE_CACHE_GCS:
        return getRestoreCacheGCSStepInfoEnvVariables((RestoreCacheGCSStepInfo) stepInfo);
      case SAVE_CACHE_S3:
        return getSaveCacheS3StepInfoEnvVariables((SaveCacheS3StepInfo) stepInfo);
      case RESTORE_CACHE_S3:
        return getRestoreCacheS3StepInfoEnvVariables((RestoreCacheS3StepInfo) stepInfo);
      default:
        throw new IllegalStateException("Unexpected value: " + stepInfo.getNonYamlInfo().getStepInfoType());
    }
  }

  private static Map<String, String> getGCRStepInfoEnvVariables(GCRStepInfo stepInfo) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_REGISTRY, stepInfo.getRegistry());
    setMandatoryEnvironmentVariable(map, PLUGIN_REPO, stepInfo.getRepo());
    setMandatoryEnvironmentVariable(map, PLUGIN_TAGS, listToStringSlice(stepInfo.getTags()));

    setOptionalEnvironmentVariable(map, PLUGIN_DOCKERFILE, stepInfo.getDockerFile());
    setOptionalEnvironmentVariable(map, PLUGIN_CONTEXT, stepInfo.getContext());
    setOptionalEnvironmentVariable(map, PLUGIN_TARGET, stepInfo.getTarget());
    setOptionalEnvironmentVariable(map, PLUGIN_BUILD_ARGS, mapToStringSlice(stepInfo.getBuildArgs()));
    setOptionalEnvironmentVariable(map, PLUGIN_CUSTOM_LABELS, mapToStringSlice(stepInfo.getLabels()));

    return map;
  }

  private static Map<String, String> getECRStepInfoEnvVariables(ECRStepInfo stepInfo) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_REGISTRY, stepInfo.getRegistry());
    setMandatoryEnvironmentVariable(map, PLUGIN_REPO, stepInfo.getRepo());
    setMandatoryEnvironmentVariable(map, PLUGIN_TAGS, listToStringSlice(stepInfo.getTags()));

    setOptionalEnvironmentVariable(map, PLUGIN_DOCKERFILE, stepInfo.getDockerFile());
    setOptionalEnvironmentVariable(map, PLUGIN_CONTEXT, stepInfo.getContext());
    setOptionalEnvironmentVariable(map, PLUGIN_TARGET, stepInfo.getTarget());
    setOptionalEnvironmentVariable(map, PLUGIN_BUILD_ARGS, mapToStringSlice(stepInfo.getBuildArgs()));
    setOptionalEnvironmentVariable(map, PLUGIN_CUSTOM_LABELS, mapToStringSlice(stepInfo.getLabels()));

    return map;
  }

  private static Map<String, String> getDockerStepInfoEnvVariables(DockerStepInfo stepInfo) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_REPO, stepInfo.getRepo());
    setMandatoryEnvironmentVariable(map, PLUGIN_TAGS, listToStringSlice(stepInfo.getTags()));

    setOptionalEnvironmentVariable(map, PLUGIN_DOCKERFILE, stepInfo.getDockerFile());
    setOptionalEnvironmentVariable(map, PLUGIN_CONTEXT, stepInfo.getContext());
    setOptionalEnvironmentVariable(map, PLUGIN_TARGET, stepInfo.getTarget());
    setOptionalEnvironmentVariable(map, PLUGIN_BUILD_ARGS, mapToStringSlice(stepInfo.getBuildArgs()));
    setOptionalEnvironmentVariable(map, PLUGIN_CUSTOM_LABELS, mapToStringSlice(stepInfo.getLabels()));
    return map;
  }

  private static Map<String, String> getRestoreCacheGCSStepInfoEnvVariables(RestoreCacheGCSStepInfo stepInfo) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_FILENAME, stepInfo.getKey() + TAR);
    setMandatoryEnvironmentVariable(map, PLUGIN_BUCKET, stepInfo.getBucket());
    setMandatoryEnvironmentVariable(map, PLUGIN_RESTORE, "true");

    setOptionalEnvironmentVariable(map, PLUGIN_PATH, stepInfo.getTarget());

    return map;
  }

  private static Map<String, String> getSaveCacheGCSStepInfoEnvVariables(SaveCacheGCSStepInfo stepInfo) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_FILENAME, stepInfo.getKey() + TAR);
    setMandatoryEnvironmentVariable(map, PLUGIN_BUCKET, stepInfo.getBucket());
    setMandatoryEnvironmentVariable(map, PLUGIN_MOUNT, listToStringSlice(stepInfo.getSourcePath()));
    setMandatoryEnvironmentVariable(map, PLUGIN_REBUILD, "true");

    setOptionalEnvironmentVariable(map, PLUGIN_PATH, stepInfo.getTarget());

    return map;
  }

  private static Map<String, String> getRestoreCacheS3StepInfoEnvVariables(RestoreCacheS3StepInfo stepInfo) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_FILENAME, stepInfo.getKey() + TAR);
    setMandatoryEnvironmentVariable(map, PLUGIN_ROOT, stepInfo.getBucket());
    setMandatoryEnvironmentVariable(map, PLUGIN_RESTORE, "true");

    setOptionalEnvironmentVariable(map, PLUGIN_PATH, stepInfo.getTarget());
    setOptionalEnvironmentVariable(map, PLUGIN_ENDPOINT, stepInfo.getEndpoint());

    return map;
  }

  private static Map<String, String> getSaveCacheS3StepInfoEnvVariables(SaveCacheS3StepInfo stepInfo) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_FILENAME, stepInfo.getKey() + TAR);
    setMandatoryEnvironmentVariable(map, PLUGIN_ROOT, stepInfo.getBucket());
    setMandatoryEnvironmentVariable(map, PLUGIN_MOUNT, listToStringSlice(stepInfo.getSourcePath()));
    setMandatoryEnvironmentVariable(map, PLUGIN_REBUILD, "true");

    setOptionalEnvironmentVariable(map, PLUGIN_PATH, stepInfo.getTarget());
    setOptionalEnvironmentVariable(map, PLUGIN_ENDPOINT, stepInfo.getEndpoint());
    return map;
  }

  private static Map<String, String> getUploadToGCSStepInfoEnvVariables(UploadToGCSStepInfo stepInfo) {
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

  private static Map<String, String> getUploadToS3StepInfoEnvVariables(UploadToS3StepInfo stepInfo) {
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
