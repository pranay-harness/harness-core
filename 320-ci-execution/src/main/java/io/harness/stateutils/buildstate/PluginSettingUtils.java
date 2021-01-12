package io.harness.stateutils.buildstate;

import static io.harness.beans.serializer.RunTimeInputHandler.UNRESOLVED_PARAMETER;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveListParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveMapParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;
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
  public static final String ECR_REGISTRY_PATTERN = "%s.dkr.ecr.%s.amazonaws.com";

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

    String host = resolveStringParameter("host", "BuildAndPushGCR", identifier, stepInfo.getHost(), true);
    String projectID =
        resolveStringParameter("projectID", "BuildAndPushGCR", identifier, stepInfo.getProjectID(), true);
    String registry = null;
    if (isNotEmpty(host) && isNotEmpty(projectID)) {
      registry = format("%s/%s", trimTrailingCharacter(host, '/'), trimLeadingCharacter(projectID, '/'));
    }
    setMandatoryEnvironmentVariable(map, PLUGIN_REGISTRY, registry);

    setMandatoryEnvironmentVariable(map, PLUGIN_REPO,
        resolveStringParameter("imageName", "BuildAndPushGCR", identifier, stepInfo.getImageName(), true));

    setMandatoryEnvironmentVariable(map, PLUGIN_TAGS,
        listToStringSlice(resolveListParameter("tags", "BuildAndPushGCR", identifier, stepInfo.getTags(), true)));

    String dockerfile =
        resolveStringParameter("dockerfile", "BuildAndPushGCR", identifier, stepInfo.getDockerfile(), false);
    if (dockerfile != null && !dockerfile.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_DOCKERFILE, dockerfile);
    }

    String context = resolveStringParameter("context", "BuildAndPushGCR", identifier, stepInfo.getContext(), false);
    if (context != null && !context.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_CONTEXT, context);
    }

    String target = resolveStringParameter("target", "BuildAndPushGCR", identifier, stepInfo.getTarget(), false);
    if (target != null && !target.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_TARGET, target);
    }

    List<String> buildArgs =
        resolveListParameter("buildArgs", "BuildAndPushGCR", identifier, stepInfo.getBuildArgs(), false);
    if (isNotEmpty(buildArgs)) {
      setOptionalEnvironmentVariable(map, PLUGIN_BUILD_ARGS, listToStringSlice(buildArgs));
    }

    Map<String, String> labels =
        resolveMapParameter("labels", "BuildAndPushGCR", identifier, stepInfo.getLabels(), false);
    if (isNotEmpty(labels)) {
      setOptionalEnvironmentVariable(map, PLUGIN_CUSTOM_LABELS, mapToStringSlice(labels));
    }
    return map;
  }

  private static Map<String, String> getECRStepInfoEnvVariables(ECRStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();
    String account = resolveStringParameter("account", "BuildAndPushECR", identifier, stepInfo.getAccount(), true);
    String region = resolveStringParameter("region", "BuildAndPushECR", identifier, stepInfo.getRegion(), true);
    String registry = null;
    if (isNotEmpty(account) && isNotEmpty(region)) {
      registry = format(ECR_REGISTRY_PATTERN, account, region);
    }

    setMandatoryEnvironmentVariable(map, PLUGIN_REGISTRY, registry);

    setMandatoryEnvironmentVariable(map, PLUGIN_REPO,
        resolveStringParameter("imageName", "BuildAndPushECR", identifier, stepInfo.getImageName(), true));
    setMandatoryEnvironmentVariable(map, PLUGIN_TAGS,
        listToStringSlice(resolveListParameter("tags", "BuildAndPushECR", identifier, stepInfo.getTags(), true)));

    String dockerfile =
        resolveStringParameter("dockerfile", "BuildAndPushECR", identifier, stepInfo.getDockerfile(), false);
    if (dockerfile != null && !dockerfile.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_DOCKERFILE, dockerfile);
    }

    String context = resolveStringParameter("context", "BuildAndPushECR", identifier, stepInfo.getContext(), false);
    if (context != null && !context.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_CONTEXT, context);
    }

    String target = resolveStringParameter("target", "BuildAndPushECR", identifier, stepInfo.getTarget(), false);
    if (target != null && !target.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_TARGET, target);
    }

    List<String> buildArgs =
        resolveListParameter("buildArgs", "BuildAndPushECR", identifier, stepInfo.getBuildArgs(), false);
    if (isNotEmpty(buildArgs)) {
      setOptionalEnvironmentVariable(map, PLUGIN_BUILD_ARGS, listToStringSlice(buildArgs));
    }

    Map<String, String> labels =
        resolveMapParameter("labels", "BuildAndPushECR", identifier, stepInfo.getLabels(), false);
    if (isNotEmpty(labels)) {
      setOptionalEnvironmentVariable(map, PLUGIN_CUSTOM_LABELS, mapToStringSlice(labels));
    }
    return map;
  }

  private static Map<String, String> getDockerStepInfoEnvVariables(DockerStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(
        map, PLUGIN_REPO, resolveStringParameter("repo", "DockerHub", identifier, stepInfo.getRepo(), true));
    setMandatoryEnvironmentVariable(map, PLUGIN_TAGS,
        listToStringSlice(resolveListParameter("tags", "DockerHub", identifier, stepInfo.getTags(), true)));

    String dockerFile = resolveStringParameter("dockerfile", "DockerHub", identifier, stepInfo.getDockerfile(), false);
    if (dockerFile != null && !dockerFile.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_DOCKERFILE, dockerFile);
    }

    String context = resolveStringParameter("context", "DockerHub", identifier, stepInfo.getContext(), false);
    if (context != null && !context.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_CONTEXT, context);
    }

    String target = resolveStringParameter("target", "DockerHub", identifier, stepInfo.getTarget(), false);
    if (target != null && !target.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_TARGET, target);
    }

    List<String> buildArgs = resolveListParameter("buildArgs", "DockerHub", identifier, stepInfo.getBuildArgs(), false);
    if (isNotEmpty(buildArgs)) {
      setOptionalEnvironmentVariable(map, PLUGIN_BUILD_ARGS, listToStringSlice(buildArgs));
    }

    Map<String, String> labels = resolveMapParameter("labels", "DockerHub", identifier, stepInfo.getLabels(), false);
    if (isNotEmpty(labels)) {
      setOptionalEnvironmentVariable(map, PLUGIN_CUSTOM_LABELS, mapToStringSlice(labels));
    }

    return map;
  }

  private static Map<String, String> getRestoreCacheGCSStepInfoEnvVariables(
      RestoreCacheGCSStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_FILENAME,
        resolveStringParameter("key", "RestoreCacheGCS", identifier, stepInfo.getKey(), true) + TAR);
    setMandatoryEnvironmentVariable(map, PLUGIN_BUCKET,
        resolveStringParameter("bucket", "RestoreCacheGCS", identifier, stepInfo.getBucket(), true));

    setMandatoryEnvironmentVariable(map, PLUGIN_RESTORE, "true");

    String target = resolveStringParameter("target", "RestoreCacheGCS", identifier, stepInfo.getTarget(), false);
    if (target != null && !target.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_PATH, target);
    }

    return map;
  }

  private static Map<String, String> getSaveCacheGCSStepInfoEnvVariables(
      SaveCacheGCSStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(
        map, PLUGIN_FILENAME, resolveStringParameter("key", "SaveCacheGCS", identifier, stepInfo.getKey(), true) + TAR);

    setMandatoryEnvironmentVariable(
        map, PLUGIN_BUCKET, resolveStringParameter("bucket", "SaveCacheGCS", identifier, stepInfo.getBucket(), true));

    List<String> sourcePaths =
        resolveListParameter("sourcePaths", "SaveCacheGCS", identifier, stepInfo.getSourcePaths(), true);

    setMandatoryEnvironmentVariable(map, PLUGIN_MOUNT, listToStringSlice(sourcePaths));
    setMandatoryEnvironmentVariable(map, PLUGIN_REBUILD, "true");

    String target = resolveStringParameter("target", "SaveCacheGCS", identifier, stepInfo.getTarget(), false);
    if (target != null && !target.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_PATH, target);
    }

    return map;
  }

  private static Map<String, String> getRestoreCacheS3StepInfoEnvVariables(
      RestoreCacheS3StepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_FILENAME,
        resolveStringParameter("key", "RestoreCacheS3", identifier, stepInfo.getKey(), true) + TAR);

    setMandatoryEnvironmentVariable(
        map, PLUGIN_ROOT, resolveStringParameter("bucket", "RestoreCacheS3", identifier, stepInfo.getBucket(), true));

    setMandatoryEnvironmentVariable(map, PLUGIN_RESTORE, "true");

    String target = resolveStringParameter("target", "RestoreCacheS3", identifier, stepInfo.getTarget(), false);
    if (target != null && !target.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_PATH, target);
    }

    String endpoint = resolveStringParameter("endpoint", "RestoreCacheS3", identifier, stepInfo.getEndpoint(), false);
    if (endpoint != null && !endpoint.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_ENDPOINT, endpoint);
    }

    String region = resolveStringParameter("region", "RestoreCacheS3", identifier, stepInfo.getRegion(), true);
    if (region != null && !region.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_REGION, region);
    }

    return map;
  }

  private static Map<String, String> getSaveCacheS3StepInfoEnvVariables(
      SaveCacheS3StepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(
        map, PLUGIN_FILENAME, resolveStringParameter("key", "SaveCacheS3", identifier, stepInfo.getKey(), true) + TAR);
    setMandatoryEnvironmentVariable(
        map, PLUGIN_ROOT, resolveStringParameter("bucket", "SaveCacheS3", identifier, stepInfo.getBucket(), true));
    setMandatoryEnvironmentVariable(map, PLUGIN_MOUNT,
        listToStringSlice(
            resolveListParameter("sourcePaths", "SaveCacheS3", identifier, stepInfo.getSourcePaths(), true)));
    setMandatoryEnvironmentVariable(map, PLUGIN_REBUILD, "true");

    String target = resolveStringParameter("target", "SaveCacheS3", identifier, stepInfo.getTarget(), false);
    if (target != null && !target.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_PATH, target);
    }
    String endpoint = resolveStringParameter("endpoint", "SaveCacheS3", identifier, stepInfo.getEndpoint(), false);
    if (endpoint != null && !endpoint.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_ENDPOINT, endpoint);
    }

    String region = resolveStringParameter("region", "SaveCacheS3", identifier, stepInfo.getRegion(), true);
    if (region != null && !region.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_REGION, region);
    }

    return map;
  }

  private static Map<String, String> getUploadToGCSStepInfoEnvVariables(
      UploadToGCSStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(map, PLUGIN_SOURCE,
        resolveStringParameter("sourcePaths", "GCSUpload", identifier, stepInfo.getSourcePath(), true));
    String target = null;
    String stepInfoBucket = resolveStringParameter("bucket", "GCSUpload", identifier, stepInfo.getBucket(), true);
    String stepInfoTarget = resolveStringParameter("target", "GCSUpload", identifier, stepInfo.getTarget(), true);
    if (isNotEmpty(stepInfoBucket) && isNotEmpty(stepInfoTarget)) {
      target = format("%s/%s", trimTrailingCharacter(stepInfoBucket, '/'), trimLeadingCharacter(stepInfoTarget, '/'));
    }
    setMandatoryEnvironmentVariable(map, PLUGIN_TARGET, target);

    return map;
  }

  private static Map<String, String> getUploadToS3StepInfoEnvVariables(UploadToS3StepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();

    setMandatoryEnvironmentVariable(
        map, PLUGIN_BUCKET, resolveStringParameter("bucket", "S3Upload", identifier, stepInfo.getBucket(), true));
    setMandatoryEnvironmentVariable(map, PLUGIN_SOURCE,
        resolveStringParameter("sourcePaths", "S3Upload", identifier, stepInfo.getSourcePath(), true));
    setMandatoryEnvironmentVariable(
        map, PLUGIN_TARGET, resolveStringParameter("target", "S3Upload", identifier, stepInfo.getTarget(), true));

    String endpoint = resolveStringParameter("endpoint", "S3Upload", identifier, stepInfo.getEndpoint(), false);
    if (endpoint != null && !endpoint.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_ENDPOINT, endpoint);
    }

    String region = resolveStringParameter("region", "S3Upload", identifier, stepInfo.getRegion(), true);
    if (region != null && !region.equals(UNRESOLVED_PARAMETER)) {
      setOptionalEnvironmentVariable(map, PLUGIN_REGION, region);
    }
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
