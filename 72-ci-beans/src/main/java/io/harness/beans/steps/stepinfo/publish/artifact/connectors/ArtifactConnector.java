package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import lombok.Getter;

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

  Type getType();
}
