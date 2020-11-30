package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ArtifactoryConnector.class, name = "ARTIFACTORY")
  , @JsonSubTypes.Type(value = DockerhubConnector.class, name = "DOCKERHUB"),
      @JsonSubTypes.Type(value = NexusConnector.class, name = "NEXUS"),
      @JsonSubTypes.Type(value = GcrConnector.class, name = "GCR"),
      @JsonSubTypes.Type(value = S3Connector.class, name = "S3"),
      @JsonSubTypes.Type(value = EcrConnector.class, name = "ECR")
})
public interface ArtifactConnector {
  String ECR_PROPERTY = "ecr";
  String GCR_PROPERTY = "gcr";
  String ARTIFACTORY_PROPERTY = "artifactory";
  String NEXUS_PROPERTY = "nexus";
  String S3_PROPERTY = "s3";
  String DOCKERHUB_PROPERTY = "dockerhub";

  enum Type {
    ECR(ECR_PROPERTY),
    GCR(GCR_PROPERTY),
    ARTIFACTORY(ARTIFACTORY_PROPERTY),
    NEXUS(NEXUS_PROPERTY),
    S3(S3_PROPERTY),
    DOCKERHUB(DOCKERHUB_PROPERTY);

    @Getter private final String propertyName;

    Type(String propertyName) {
      this.propertyName = propertyName;
    }
  }

  String getConnectorRef();

  Type getType();
}
