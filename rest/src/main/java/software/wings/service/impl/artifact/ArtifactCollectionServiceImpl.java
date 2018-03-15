package software.wings.service.impl.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
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
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.exception.WingsException.ReportTarget.USER;
import static software.wings.service.impl.artifact.ArtifactCollectionUtil.getArtifact;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
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

  public static final

      List<String> metadataOnlyStreams =
          asList(DOCKER.name(), ECR.name(), GCR.name(), ACR.name(), NEXUS.name(), AMI.name());

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
    List<Artifact> newArtifacts = new ArrayList<>();
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (artifactStream == null) {
      logger.info("Artifact Stream {} does not exist. Returning", artifactStreamId);
      return newArtifacts;
    }
    if (metadataOnlyStreams.contains(artifactStream.getArtifactStreamType())) {
      collectMetaDataOnlyArtifacts(artifactStream, newArtifacts);
    } else if (artifactStream.getArtifactStreamType().equals(ARTIFACTORY.name())) {
      collectArtifactoryArtifacts(appId, artifactStream, newArtifacts);
    } else if (artifactStream.getArtifactStreamType().equals(AMAZON_S3.name())) {
      collectGenericArtifacts(appId, artifactStream, newArtifacts);
    } else {
      // Jenkins or Bamboo case
      collectLatestArtifact(appId, artifactStream, newArtifacts);
    }
    return newArtifacts;
  }

  private void collectMetaDataOnlyArtifacts(ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    String appId = artifactStream.getAppId();
    try (AcquiredLock lock = persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, timeout)) {
      logger.info("Collecting build details for artifact stream id {} type {} and source name {} ", artifactStreamId,
          artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
      List<BuildDetails> builds = buildSourceService.getBuilds(appId, artifactStreamId, artifactStream.getSettingId());
      if (!isEmpty(builds)) {
        Set<String> newBuildNumbers = getNewBuildNumbers(artifactStream, builds);
        builds.forEach((BuildDetails buildDetails1) -> {
          if (newBuildNumbers.contains(buildDetails1.getNumber())) {
            logger.info("New Artifact version [{}] found for Artifact stream [type: {}, uuid: {}]. "
                    + "Add entry in Artifact collection",
                buildDetails1.getNumber(), artifactStream.getArtifactStreamType(), artifactStreamId);
            Artifact newArtifact = getArtifact(artifactStream, buildDetails1);
            newArtifacts.add(artifactService.create(newArtifact));
          }
        });
      }
    }
  }

  private void collectLatestArtifact(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    try (AcquiredLock lock = persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, timeout)) {
      logger.info("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
          artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
      BuildDetails lastSuccessfulBuild =
          buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());
      if (lastSuccessfulBuild == null) {
        return;
      }
      Artifact lastCollectedArtifact =
          artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId, artifactStream.getSourceName());
      int buildNo = (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(BUILD_NO) != null)
          ? Integer.parseInt(lastCollectedArtifact.getMetadata().get(BUILD_NO))
          : 0;
      if (Integer.parseInt(lastSuccessfulBuild.getNumber()) > buildNo) {
        logger.info(
            "Existing build no {} is older than new build number {}. Collect new Artifact for ArtifactStream {}",
            buildNo, lastSuccessfulBuild.getNumber(), artifactStreamId);
        newArtifacts.add(artifactService.create(getArtifact(artifactStream, lastSuccessfulBuild)));
      } else {
        logger.info("Artifact of the version {} already collected.", buildNo);
      }
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
    try (AcquiredLock lock = persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, timeout)) {
      logger.info("Collecting Artifacts for artifact stream id {} type {} and source name {} ", artifactStreamId,
          artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
      List<BuildDetails> builds;
      if (ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())) {
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
  }

  private void collectMavenArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    try (AcquiredLock lock = persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, timeout)) {
      logger.info("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
          artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
      BuildDetails latestVersion =
          buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());
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
  }

  /**
   * Gets all  existing artifacts for the given artifact stream, and compares with artifact source data
   * @param artifactStream
   * @param builds
   * @return
   */
  private Set<String> getNewBuildNumbers(ArtifactStream artifactStream, List<BuildDetails> builds) {
    Map<String, BuildDetails> buildDetails =
        builds.parallelStream().collect(Collectors.toMap(BuildDetails::getNumber, Function.identity()));
    final MorphiaIterator<Artifact, Artifact> iterator = getArtifactQuery(artifactStream).fetch();
    while (iterator.hasNext()) {
      buildDetails.remove(iterator.next().getBuildNo());
    }
    return buildDetails.keySet();
  }

  private Set<String> getNewArtifactPaths(ArtifactStream artifactStream, List<BuildDetails> builds) {
    Map<String, BuildDetails> buildDetails =
        builds.parallelStream().collect(Collectors.toMap(BuildDetails::getArtifactPath, Function.identity()));
    final MorphiaIterator<Artifact, Artifact> iterator = getArtifactQuery(artifactStream).fetch();
    while (iterator.hasNext()) {
      buildDetails.remove(iterator.next().getArtifactPath());
    }
    return buildDetails.keySet();
  }

  private Query getArtifactQuery(ArtifactStream artifactStream) {
    return wingsPersistence.createQuery(Artifact.class)
        .project("metadata", true)
        .field(Artifact.APP_ID_KEY)
        .equal(artifactStream.getAppId())
        .field("artifactStreamId")
        .equal(artifactStream.getUuid())
        .field("artifactSourceName")
        .equal(artifactStream.getSourceName())
        .field("status")
        .hasAnyOf(asList(QUEUED, RUNNING, REJECTED, WAITING, READY, APPROVED, FAILED))
        .disableValidation();
  }

  private Service getService(String appId, ArtifactStream artifactStream) {
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId(), false);
    if (service == null) {
      artifactStreamService.delete(appId, artifactStream.getUuid());
      throw new WingsException(ErrorCode.GENERAL_ERROR)
          .addParam("args", String.format("Artifact stream %s is a zombie.", artifactStream.getUuid()));
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
