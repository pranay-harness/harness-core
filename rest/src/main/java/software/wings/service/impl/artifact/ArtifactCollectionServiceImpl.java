package software.wings.service.impl.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.FAILED;
import static software.wings.beans.artifact.Artifact.Status.QUEUED;
import static software.wings.beans.artifact.Artifact.Status.READY;
import static software.wings.beans.artifact.Artifact.Status.REJECTED;
import static software.wings.beans.artifact.Artifact.Status.RUNNING;
import static software.wings.beans.artifact.Artifact.Status.WAITING;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.service.impl.artifact.ArtifactCollectionUtil.getArtifact;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;
import software.wings.utils.MavenVersionCompareUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/***
 * Service responsible to glue all artifact
 */
@Singleton
public class ArtifactCollectionServiceImpl implements ArtifactCollectionService {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private BuildSourceService buildSourceService;
  @Inject private ArtifactService artifactService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ArtifactStreamService artifactStreamService;

  public static final Duration timeout = Duration.ofMinutes(10);
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionServiceImpl.class);

  private static final List<String> metadataOnlyStreams =
      Collections.unmodifiableList(asList(DOCKER.name(), ECR.name(), GCR.name(), NEXUS.name(), AMI.name(), ACR.name()));

  @Override
  public Artifact collectArtifact(String appId, String artifactStreamId, BuildDetails buildDetails) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (artifactStream == null) {
      throw new WingsException("Artifact Stream was deleted", USER);
    }
    return artifactService.create(getArtifact(artifactStream, buildDetails));
  }

  @Override
  public List<Artifact> collectNewArtifacts(String appId, String artifactStreamId) {
    try (AcquiredLock ignored = persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, timeout)) {
      List<Artifact> newArtifacts = new ArrayList<>();
      ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
      if (artifactStream == null) {
        logger.info("Artifact Stream {} does not exist. Returning", artifactStreamId);
        return newArtifacts;
      }
      String artifactStreamType = artifactStream.getArtifactStreamType();
      if (metadataOnlyStreams.contains(artifactStreamType)) {
        collectMetaDataOnlyArtifacts(artifactStream, newArtifacts);
      } else if (ARTIFACTORY.name().equals(artifactStreamType)) {
        collectArtifactoryArtifacts(appId, artifactStream, newArtifacts);
      } else if (AMAZON_S3.name().equals(artifactStreamType) || GCS.name().equals(artifactStreamType)) {
        collectGenericArtifacts(appId, artifactStream, newArtifacts);
      } else {
        // Jenkins or Bamboo case
        collectLatestArtifact(appId, artifactStream, newArtifacts);
      }
      return newArtifacts;
    }
  }

  private void collectMetaDataOnlyArtifacts(ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    logger.debug("Collecting build details for artifact stream id {} type {} and source name {} ",
        artifactStream.getUuid(), artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
    List<BuildDetails> builds = buildSourceService.getBuilds(
        artifactStream.getAppId(), artifactStream.getUuid(), artifactStream.getSettingId());
    if (!isEmpty(builds)) {
      Set<String> newBuildNumbers = getNewBuildNumbers(artifactStream, builds);
      builds.forEach((BuildDetails buildDetails1) -> {
        if (newBuildNumbers.contains(buildDetails1.getNumber())) {
          logger.info("New Artifact version [{}] found for Artifact stream [type: {}, uuid: {}]. "
                  + "Add entry in Artifact collection",
              buildDetails1.getNumber(), artifactStream.getArtifactStreamType(), artifactStream.getUuid());
          Artifact newArtifact = getArtifact(artifactStream, buildDetails1);
          newArtifacts.add(artifactService.create(newArtifact));
        }
      });
    }
  }

  private void collectLatestArtifact(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    logger.debug("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStream.getUuid(),
        artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
    BuildDetails lastSuccessfulBuild = getLastSuccessfulBuild(appId, artifactStream, artifactStream.getUuid());
    Artifact lastCollectedArtifact = artifactService.fetchLatestArtifactForArtifactStream(
        appId, artifactStream.getUuid(), artifactStream.getSourceName());
    int buildNo = (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(BUILD_NO) != null)
        ? parseInt(lastCollectedArtifact.getMetadata().get(BUILD_NO))
        : 0;
    if (lastSuccessfulBuild != null && parseInt(lastSuccessfulBuild.getNumber()) > buildNo) {
      logger.info("Existing build no {} is older than new build number {}. Collect new Artifact for ArtifactStream {}",
          buildNo, lastSuccessfulBuild.getNumber(), artifactStream.getUuid());
      newArtifacts.add(artifactService.create(getArtifact(artifactStream, lastSuccessfulBuild)));
    }
  }

  private void collectArtifactoryArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    if (getService(appId, artifactStream).getArtifactType().equals(ArtifactType.DOCKER)) {
      collectMetaDataOnlyArtifacts(artifactStream, newArtifacts);
    } else if (artifactStream.getArtifactStreamAttributes().getRepositoryType() == null
        || !artifactStream.getArtifactStreamAttributes().getRepositoryType().equals("maven")) {
      collectGenericArtifacts(appId, artifactStream, newArtifacts);
    } else {
      collectMavenArtifacts(appId, artifactStream, newArtifacts);
    }
  }

  private void collectGenericArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    String artifactStreamType = artifactStream.getArtifactStreamType();
    logger.debug("Collecting Artifacts for artifact stream id {} type {} and source name {} ", artifactStreamId,
        artifactStreamType, artifactStream.getSourceName());
    List<BuildDetails> builds;
    if (ARTIFACTORY.name().equals(artifactStreamType)) {
      builds = buildSourceService.getBuilds(appId, artifactStreamId, artifactStream.getSettingId(), 25);
    } else {
      builds = buildSourceService.getBuilds(appId, artifactStreamId, artifactStream.getSettingId());
    }
    if (!isEmpty(builds)) {
      Set<String> newArtifactPaths = getNewArtifactPaths(artifactStream, builds);
      builds.forEach(buildDetails -> {
        if (newArtifactPaths.contains(buildDetails.getArtifactPath())) {
          newArtifacts.add(artifactService.create(getArtifact(artifactStream, buildDetails)));
        }
      });
    }
  }

  private void collectMavenArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    BuildDetails latestVersion = getLastSuccessfulBuild(appId, artifactStream, artifactStreamId);
    if (latestVersion == null) {
      return;
    }
    logger.info("Latest version in artifactory server {}", latestVersion);
    Artifact lastCollectedArtifact =
        artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId, artifactStream.getSourceName());
    String buildNo = (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(BUILD_NO) != null)
        ? lastCollectedArtifact.getMetadata().get(BUILD_NO)
        : "";
    logger.info("Last collected artifactory maven artifact version {} ", buildNo);
    if (buildNo.isEmpty() || versionCompare(latestVersion.getNumber(), buildNo) > 0) {
      logger.info(
          "Existing version no {} is older than new version number {}. Collect new Artifact for ArtifactStream {}",
          buildNo, latestVersion.getNumber(), artifactStreamId);
      newArtifacts.add(artifactService.create(getArtifact(artifactStream, latestVersion)));
    }
  }

  private BuildDetails getLastSuccessfulBuild(String appId, ArtifactStream artifactStream, String artifactStreamId) {
    logger.debug("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
        artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
    BuildDetails latestVersion =
        buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());
    if (latestVersion == null) {
      return null;
    }
    return latestVersion;
  }

  /**
   * Gets all  existing artifacts for the given artifact stream, and compares with artifact source data
   */
  private Set<String> getNewBuildNumbers(ArtifactStream artifactStream, List<BuildDetails> builds) {
    Map<String, BuildDetails> buildNoDetails =
        builds.parallelStream().collect(Collectors.toMap(BuildDetails::getNumber, Function.identity()));
    try (HIterator<Artifact> iterator = new HIterator(getArtifactQuery(artifactStream).fetch())) {
      while (iterator.hasNext()) {
        buildNoDetails.remove(iterator.next().getBuildNo());
      }
    }
    return buildNoDetails.keySet();
  }

  private Set<String> getNewArtifactPaths(ArtifactStream artifactStream, List<BuildDetails> builds) {
    Map<String, BuildDetails> buildArtifactPathDetails =
        builds.parallelStream().collect(Collectors.toMap(BuildDetails::getArtifactPath, Function.identity()));
    try (HIterator<Artifact> iterator = new HIterator<>(getArtifactQuery(artifactStream).fetch())) {
      while (iterator.hasNext()) {
        buildArtifactPathDetails.remove(iterator.next().getArtifactPath());
      }
    }
    return buildArtifactPathDetails.keySet();
  }

  private Query<Artifact> getArtifactQuery(ArtifactStream artifactStream) {
    return wingsPersistence.createQuery(Artifact.class)
        .project("metadata", true)
        .filter(Artifact.APP_ID_KEY, artifactStream.getAppId())
        .filter("artifactStreamId", artifactStream.getUuid())
        .filter("artifactSourceName", artifactStream.getSourceName())
        .field("status")
        .hasAnyOf(asList(QUEUED, RUNNING, REJECTED, WAITING, READY, APPROVED, FAILED))
        .disableValidation();
  }

  private Service getService(String appId, ArtifactStream artifactStream) {
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId(), false);
    if (service == null) {
      artifactStreamService.delete(appId, artifactStream.getUuid());
      throw new WingsException(ErrorCode.GENERAL_ERROR)
          .addParam("message", format("Artifact stream %s is a zombie.", artifactStream.getUuid()));
    }
    return service;
  }

  /**
   * Compares two maven format version strings.
   *
   */
  public static int versionCompare(String str1, String str2) {
    return MavenVersionCompareUtil.compare(str1).with(str2);
  }
}
