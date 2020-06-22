package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import static io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector.ARTIFACTORY_PROPERTY;
import static io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector.NEXUS_PROPERTY;
import static io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector.S3_PROPERTY;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface WithFileConnector {
  void setConnector(ArtifactConnector connector);

  @JsonProperty(ARTIFACTORY_PROPERTY)
  default void setFileConnector(ArtifactoryConnector connector) {
    setConnector(connector);
  }

  @JsonProperty(NEXUS_PROPERTY)
  default void setFileConnector(NexusConnector connector) {
    setConnector(connector);
  }

  @JsonProperty(S3_PROPERTY)
  default void setFileConnector(S3Connector connector) {
    setConnector(connector);
  }
}
