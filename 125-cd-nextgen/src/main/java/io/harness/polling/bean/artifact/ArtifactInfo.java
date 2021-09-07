package io.harness.polling.bean.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.polling.bean.PollingInfo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@OwnedBy(HarnessTeam.CDC)
@JsonTypeName("ARTIFACT")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public interface ArtifactInfo extends PollingInfo {
  ArtifactSourceType getType();
  ArtifactConfig toArtifactConfig();
}
