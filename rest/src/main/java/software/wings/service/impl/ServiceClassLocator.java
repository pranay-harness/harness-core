package software.wings.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AmazonS3BuildService;
import software.wings.service.intfc.AmiBuildService;
import software.wings.service.intfc.BambooBuildService;
import software.wings.service.intfc.BuildService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.service.intfc.EcrBuildService;
import software.wings.service.intfc.GcrBuildService;
import software.wings.service.intfc.GcsBuildService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.NexusBuildService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class locator for all the artifact streams.
 * We currently use guice based lookup but we have an issue with the current design.
 * Each config (compute provider / connector) can only be associated to one Service.
 * In our case, both ECR and S3 services use AwsConfig.
 * @author rktummala on 08/17/17
 */
public class ServiceClassLocator {
  private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

  public Class<? extends BuildService> getBuildServiceClass(String artifactStreamTypeStr) {
    ArtifactStreamType artifactStreamType = ArtifactStreamType.valueOf(artifactStreamTypeStr);
    switch (artifactStreamType) {
      case AMAZON_S3:
        return AmazonS3BuildService.class;
      case ARTIFACTORY:
      case BAMBOO:
        return BambooBuildService.class;
      case DOCKER:
        return DockerBuildService.class;
      case ECR:
        return EcrBuildService.class;
      case GCR:
        return GcrBuildService.class;
      case GCS:
        return GcsBuildService.class;
      case JENKINS:
        return JenkinsBuildService.class;
      case NEXUS:
        return NexusBuildService.class;
      case AMI:
        return AmiBuildService.class;
      default:
        throw new WingsException("Unsupported artifact stream type: " + artifactStreamType);
    }
  }

  public static <T> List<T> descendingServices(Object inst, Class cls, Class<T> intr) {
    List<T> descendings = new ArrayList<>();

    synchronized (cls) {
      for (Field field : cls.getDeclaredFields()) {
        boolean originalAccessible = field.isAccessible();
        if (!originalAccessible) {
          field.setAccessible(true);
        }
        try {
          Object obj = field.get(inst);
          if (intr.isInstance(obj)) {
            T descending = (T) obj;
            descendings.add(descending);
          }
        } catch (IllegalAccessException e) {
          logger.error("", e);
        } finally {
          if (!originalAccessible) {
            field.setAccessible(false);
          }
        }
      }
    }

    return descendings;
  }
}
