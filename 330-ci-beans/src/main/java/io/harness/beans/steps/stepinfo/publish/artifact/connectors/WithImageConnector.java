package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import static io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector.ARTIFACTORY_PROPERTY;
import static io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector.DOCKERHUB_PROPERTY;
import static io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector.ECR_PROPERTY;
import static io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector.GCR_PROPERTY;
import static io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector.NEXUS_PROPERTY;
import static io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector.S3_PROPERTY;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface WithImageConnector {
  void setConnector(ArtifactConnector connector);

  @JsonProperty(ECR_PROPERTY)
  default void setImageConnector(EcrConnector connector) {
    setConnector(connector);
  }

  @JsonProperty(GCR_PROPERTY)
  default void setImageConnector(GcrConnector connector) {
    setConnector(connector);
  }

  @JsonProperty(ARTIFACTORY_PROPERTY)
  default void setImageConnector(ArtifactoryConnector connector) {
    setConnector(connector);
  }

  @JsonProperty(NEXUS_PROPERTY)
  default void setImageConnector(NexusConnector connector) {
    setConnector(connector);
  }

  @JsonProperty(S3_PROPERTY)
  default void setImageConnector(S3Connector connector) {
    setConnector(connector);
  }

  @JsonProperty(DOCKERHUB_PROPERTY)
  default void setImageConnector(DockerhubConnector connector) {
    setConnector(connector);
  }
}
