package io.harness.beans.steps;

import io.harness.EntityType;
import io.harness.beans.steps.stepinfo.DockerStepInfo;
import io.harness.beans.steps.stepinfo.ECRStepInfo;
import io.harness.beans.steps.stepinfo.GCRStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheGCSStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheS3StepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheGCSStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheS3StepInfo;
import io.harness.beans.steps.stepinfo.UploadToArtifactoryStepInfo;
import io.harness.beans.steps.stepinfo.UploadToGCSStepInfo;
import io.harness.beans.steps.stepinfo.UploadToS3StepInfo;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.yaml.core.StepSpecType;
import io.harness.yaml.schema.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import java.time.Duration;
import java.util.List;

@ApiModel(subTypes = {DockerStepInfo.class, ECRStepInfo.class, GCRStepInfo.class, PluginStepInfo.class,
              RestoreCacheGCSStepInfo.class, RestoreCacheS3StepInfo.class, RunStepInfo.class,
              SaveCacheGCSStepInfo.class, SaveCacheS3StepInfo.class, UploadToGCSStepInfo.class,
              UploadToS3StepInfo.class, UploadToArtifactoryStepInfo.class})
@YamlSchemaRoot(EntityType.INTEGRATION_STEPS)
public interface CIStepInfo extends StepSpecType, GenericStepInfo {
  int MIN_RETRY = 0;
  int MAX_RETRY = 5;
  long DEFAULT_TIMEOUT = Duration.ofHours(2).getSeconds();

  @JsonIgnore TypeInfo getNonYamlInfo();
  int getRetry();
  String getName();

  @JsonIgnore
  default long getDefaultTimeout() {
    return DEFAULT_TIMEOUT;
  }

  // TODO: implement this when we support graph section in yaml
  @JsonIgnore
  default List<String> getDependencies() {
    return null;
  }
}
