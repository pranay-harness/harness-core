package software.wings.service.impl;

import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AmazonS3BuildService;
import software.wings.service.intfc.ArtifactoryBuildService;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.BuildService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.service.intfc.EcrBuildService;
import software.wings.service.intfc.GcrBuildService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.NexusBuildService;

/**
 * Service locator for all the artifact streams.
 * We currently use guice based lookup but we have an issue with the current design.
 * Each config (compute provider / connector) can only be associated to one Service.
 * In our case, both ECR and S3 services use AwsConfig.
 * @author rktummala on 08/17/17
 */
public class ServiceLocator {
  public Class<? extends BuildService> getBuildServiceClass(String artifactStreamTypeStr) {
    ArtifactStreamType artifactStreamType = ArtifactStreamType.valueOf(artifactStreamTypeStr);
    switch (artifactStreamType) {
      case AMAZON_S3:
        return AmazonS3BuildService.class;
      case ARTIFACTORY:
      case ARTIFACTORYDOCKER:
        return ArtifactoryBuildService.class;
      case BAMBOO:
        return BambooBuildService.class;
      case DOCKER:
        return DockerBuildService.class;
      case ECR:
        return EcrBuildService.class;
      case GCR:
        return GcrBuildService.class;
      case JENKINS:
        return JenkinsBuildService.class;
      case NEXUS:
        return NexusBuildService.class;
      default:
        throw new WingsException("Unsupported artifact stream type: " + artifactStreamType);
    }
  }
}
