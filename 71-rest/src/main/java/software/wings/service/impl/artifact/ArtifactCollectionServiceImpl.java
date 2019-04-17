package software.wings.service.impl.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.beans.artifact.ArtifactStreamType.ACR;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.beans.artifact.ArtifactStreamType.SFTP;
import static software.wings.beans.artifact.ArtifactStreamType.SMB;
import static software.wings.common.Constants.BUILD_NO;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.artifact.CustomBuildSourceService;
import software.wings.utils.ArtifactType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/***
 * Service responsible to glue all artifact
 */
@Singleton
@Slf4j
public class ArtifactCollectionServiceImpl implements ArtifactCollectionService {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private BuildSourceService buildSourceService;
  @Inject private ArtifactService artifactService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactCollectionUtil artifactCollectionUtil;
  @Inject private CustomBuildSourceService customBuildSourceService;

  public static final Duration timeout = Duration.ofMinutes(10);

  private static final List<String> metadataOnlyStreams = Collections.unmodifiableList(asList(DOCKER.name(), ECR.name(),
      GCR.name(), NEXUS.name(), AMI.name(), ACR.name(), SMB.name(), SFTP.name(), CUSTOM.name()));

  @Override
  public Artifact collectArtifact(String appId, String artifactStreamId, BuildDetails buildDetails) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (artifactStream == null) {
      throw new WingsException("Artifact Stream was deleted", USER);
    }
    return artifactService.create(artifactCollectionUtil.getArtifact(artifactStream, buildDetails));
  }

  @Override
  public Artifact collectArtifact(String artifactStreamId, BuildDetails buildDetails) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (artifactStream == null) {
      throw new WingsException("Artifact Stream was deleted", USER);
    }
    return artifactService.create(artifactCollectionUtil.getArtifact(artifactStream, buildDetails));
  }

  @Override
  public void collectNewArtifactsAsync(String appId, ArtifactStream artifactStream, String permitId) {}

  @Override
  public void collectNewArtifactsAsync(ArtifactStream artifactStream, String permitId) {}

  @Override
  public Artifact collectNewArtifacts(String appId, ArtifactStream artifactStream, String buildNumber) {
    List<BuildDetails> builds =
        buildSourceService.getBuilds(appId, artifactStream.getUuid(), artifactStream.getSettingId());
    if (EmptyPredicate.isNotEmpty(builds)) {
      Optional<BuildDetails> buildDetails =
          builds.stream().filter(build -> buildNumber.equals(build.getNumber())).findFirst();
      if (buildDetails.isPresent()) {
        return collectArtifact(appId, artifactStream.getUuid(), buildDetails.get());
      }
    }
    return null;
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

      logger.info("Collecting build details for artifact stream id {} type {} and source name {} ",
          artifactStream.getUuid(), artifactStream.getArtifactStreamType(), artifactStream.getSourceName());

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
    List<BuildDetails> builds;
    if (CUSTOM.name().equals(artifactStream.getArtifactStreamType())) {
      logger.info("Collecting custom repository build details for artifact stream id {}", artifactStream.getUuid());
      builds = customBuildSourceService.getBuilds(artifactStream.getAppId(), artifactStream.getUuid());
      logger.info("Collected custom repository build details for artifact stream id {}", artifactStream.getUuid());
    } else {
      builds = buildSourceService.getBuilds(
          artifactStream.getAppId(), artifactStream.getUuid(), artifactStream.getSettingId());
    }

    if (!isEmpty(builds)) {
      Set<String> newBuildNumbers = getNewBuildNumbers(artifactStream, builds);
      builds.forEach((BuildDetails buildDetails1) -> {
        if (newBuildNumbers.contains(buildDetails1.getNumber())) {
          logger.info("New Artifact version [{}] found for Artifact stream [type: {}, uuid: {}]. "
                  + "Add entry in Artifact collection",
              buildDetails1.getNumber(), artifactStream.getArtifactStreamType(), artifactStream.getUuid());
          Artifact newArtifact = artifactCollectionUtil.getArtifact(artifactStream, buildDetails1);
          newArtifacts.add(artifactService.create(newArtifact));
        }
      });
    }
  }

  private void collectLatestArtifact(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    logger.debug("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStream.getUuid(),
        artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
    BuildDetails lastSuccessfulBuild = getLastSuccessfulBuild(appId, artifactStream, artifactStream.getUuid());
    Artifact lastCollectedArtifact = artifactService.fetchLatestArtifactForArtifactStream(artifactStream);
    int buildNo = (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(BUILD_NO) != null)
        ? parseInt(lastCollectedArtifact.getMetadata().get(BUILD_NO))
        : 0;
    if (lastSuccessfulBuild != null && parseInt(lastSuccessfulBuild.getNumber()) > buildNo) {
      logger.info("Existing build no {} is older than new build number {}. Collect new Artifact for ArtifactStream {}",
          buildNo, lastSuccessfulBuild.getNumber(), artifactStream.getUuid());
      newArtifacts.add(artifactService.create(artifactCollectionUtil.getArtifact(artifactStream, lastSuccessfulBuild)));
    }
  }

  private void collectArtifactoryArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    if (getService(appId, artifactStream).getArtifactType().equals(ArtifactType.DOCKER)) {
      collectMetaDataOnlyArtifacts(artifactStream, newArtifacts);
    } else if (artifactStream.fetchArtifactStreamAttributes().getRepositoryType() == null
        || artifactStream.fetchArtifactStreamAttributes().getRepositoryType().equals("any")) {
      collectGenericArtifacts(appId, artifactStream, newArtifacts);
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
          newArtifacts.add(artifactService.create(artifactCollectionUtil.getArtifact(artifactStream, buildDetails)));
        }
      });
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
    // Filter out the duplicated entries
    Map<String, BuildDetails> buildNoDetails =
        builds.parallelStream().collect(Collectors.toMap(BuildDetails::getNumber, Function.identity()));
    try (HIterator<Artifact> iterator =
             new HIterator(artifactService.prepareArtifactWithMetadataQuery(artifactStream).fetch())) {
      while (iterator.hasNext()) {
        buildNoDetails.remove(iterator.next().getBuildNo());
      }
    }
    return buildNoDetails.keySet();
  }

  private Set<String> getNewArtifactPaths(ArtifactStream artifactStream, List<BuildDetails> builds) {
    Map<String, BuildDetails> buildArtifactPathDetails =
        builds.parallelStream().collect(Collectors.toMap(BuildDetails::getArtifactPath, Function.identity()));
    try (HIterator<Artifact> iterator =
             new HIterator(artifactService.prepareArtifactWithMetadataQuery(artifactStream).fetch())) {
      while (iterator.hasNext()) {
        buildArtifactPathDetails.remove(iterator.next().getArtifactPath());
      }
    }
    return buildArtifactPathDetails.keySet();
  }

  private Service getService(String appId, ArtifactStream artifactStream) {
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId(), false);
    if (service == null) {
      artifactStreamService.delete(appId, artifactStream.getUuid());
      throw new WingsException(ErrorCode.GENERAL_ERROR, USER)
          .addParam("message", format("Artifact stream %s is a zombie.", artifactStream.getUuid()));
    }
    return service;
  }
}
