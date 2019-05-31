package software.wings.service.impl.artifact;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Base.CREATED_AT_KEY;
import static software.wings.beans.artifact.Artifact.ContentStatus.DELETED;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADED;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADING;
import static software.wings.beans.artifact.Artifact.ContentStatus.METADATA_ONLY;
import static software.wings.beans.artifact.Artifact.ContentStatus.NOT_DOWNLOADED;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.FAILED;
import static software.wings.beans.artifact.Artifact.Status.QUEUED;
import static software.wings.beans.artifact.Artifact.Status.READY;
import static software.wings.beans.artifact.Artifact.Status.REJECTED;
import static software.wings.beans.artifact.Artifact.Status.RUNNING;
import static software.wings.beans.artifact.Artifact.Status.WAITING;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.CUSTOM;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.collect.CollectEvent.Builder.aCollectEvent;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;
import static software.wings.utils.ArtifactType.DOCKER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.BasicDBObject;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import io.harness.queue.Queue;
import io.harness.scheduler.PersistentScheduler;
import io.harness.validation.Create;
import io.harness.validation.Update;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.FeatureName;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactKeys;
import software.wings.beans.artifact.Artifact.ContentStatus;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.collect.CollectEvent;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class ArtifactServiceImpl.
 */
@Singleton
@ValidateOnExecution
@Slf4j
public class ArtifactServiceImpl implements ArtifactService {
  /**
   * The Auto downloaded.
   */
  List<String> autoDownloaded =
      asList(ArtifactStreamType.DOCKER.name(), ArtifactStreamType.ECR.name(), ArtifactStreamType.GCR.name(),
          ArtifactStreamType.ACR.name(), ArtifactStreamType.AMAZON_S3.name(), ArtifactStreamType.AMI.name());

  private static final String DEFAULT_ARTIFACT_FILE_NAME = "ArtifactFile";
  private static final int ARTIFACT_RETENTION_SIZE = 25;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
  @Inject private Queue<CollectEvent> collectQueue;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private AppService appService;
  @Inject private ExecutorService executorService;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private SettingsService settingsService;
  @Inject private FeatureFlagService featureFlagService;

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Override
  public PageResponse<Artifact> listSortByBuildNo(PageRequest<Artifact> pageRequest) {
    List<Artifact> artifacts = new ArrayList<>();
    PageResponse<Artifact> pageResponse = wingsPersistence.query(Artifact.class, pageRequest);
    Map<String, List<Artifact>> groupByArtifactStream =
        pageResponse.getResponse().stream().collect(Collectors.groupingBy(Artifact::getArtifactStreamId));
    for (Entry<String, List<Artifact>> artifactStreamEntry : groupByArtifactStream.entrySet()) {
      artifacts.addAll(artifactStreamEntry.getValue().stream().sorted(new ArtifactComparator()).collect(toList()));
    }
    pageResponse.setResponse(artifacts);
    return pageResponse;
  }

  @Override
  public PageResponse<Artifact> listSortByBuildNo(String appId, String serviceId, PageRequest<Artifact> pageRequest) {
    if (GLOBAL_APP_ID.equals(appId)) {
      return listSortByBuildNo(serviceId, pageRequest);
    }

    if (serviceId != null) {
      List<String> artifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(appId, serviceId);
      if (isNotEmpty(artifactStreamIds)) {
        pageRequest.addFilter(ArtifactKeys.artifactStreamId, IN, artifactStreamIds.toArray());
      } else {
        return aPageResponse().withResponse(new ArrayList<Artifact>()).build();
      }
    }

    return listSortByBuildNo(pageRequest);
  }

  @Override
  public PageResponse<Artifact> listSortByBuildNo(String serviceId, PageRequest<Artifact> pageRequest) {
    if (serviceId != null) {
      List<String> artifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(serviceId);
      if (isNotEmpty(artifactStreamIds)) {
        pageRequest.addFilter(ArtifactKeys.artifactStreamId, IN, artifactStreamIds.toArray());
      } else {
        return aPageResponse().withResponse(new ArrayList<Artifact>()).build();
      }
    }

    return listSortByBuildNo(pageRequest);
  }

  @Override
  @ValidationGroups(Create.class)
  public Artifact create(@Valid Artifact artifact) {
    String appId = artifact.fetchAppId();
    if (appId != null && !appId.equals(GLOBAL_APP_ID) && !appService.exist(appId)) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER).addParam("args", "App does not exist: " + appId);
    }

    ArtifactStream artifactStream = artifactStreamService.get(artifact.getArtifactStreamId());
    notNullCheck("Artifact Stream", artifactStream, USER);
    artifact.setArtifactSourceName(artifactStream.getSourceName());
    setAccountId(artifact);
    setArtifactStatus(artifact, artifactStream);
    if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, artifact.getAccountId())) {
      artifact.setServiceIds(artifactStreamServiceBindingService.listServiceIds(artifactStream.getUuid()));
    }

    String key = wingsPersistence.save(artifact);
    Artifact savedArtifact = wingsPersistence.get(Artifact.class, key);
    if (savedArtifact.getStatus().equals(QUEUED)) {
      logger.info("Sending event to collect artifact {} ", savedArtifact.getUuid());
      collectQueue.send(aCollectEvent().withArtifact(savedArtifact).build());
    }
    executorService.submit(() -> deleteArtifactsWithContents(ARTIFACT_RETENTION_SIZE, artifactStream));
    return savedArtifact;
  }

  private void setAccountId(Artifact artifact) {
    if (isEmpty(artifact.getAccountId())) {
      if (artifact.fetchAppId() != null && !artifact.fetchAppId().equals(GLOBAL_APP_ID)) {
        artifact.setAccountId(appService.getAccountIdByAppId(artifact.fetchAppId()));
      } else {
        if (artifact.getSettingId() != null) {
          artifact.setAccountId(settingsService.fetchAccountIdBySettingId(artifact.getSettingId()));
        }
      }
    }
  }

  private void setArtifactStatus(Artifact artifact, ArtifactStream artifactStream) {
    if (artifactStream.isMetadataOnly() || autoDownloaded.contains(artifactStream.getArtifactStreamType())) {
      artifact.setContentStatus(METADATA_ONLY);
      artifact.setStatus(APPROVED);
      return;
    }

    String appId = artifact.fetchAppId();
    if (NEXUS.name().equals(artifactStream.getArtifactStreamType())) { // TODO: if (isNotEmpty(artifactPaths) ||not
      if (appId != null && !appId.equals(GLOBAL_APP_ID)) { // Null) ->not_downloaded
        artifact.setContentStatus(
            getArtifactType(appId, artifactStream.getUuid()).equals(DOCKER) ? METADATA_ONLY : NOT_DOWNLOADED);
        artifact.setStatus(APPROVED);
        return;
      } else {
        artifact.setContentStatus(
            RepositoryType.docker.name().equals(((NexusArtifactStream) artifactStream).getRepositoryType())
                ? METADATA_ONLY
                : NOT_DOWNLOADED);
        artifact.setStatus(APPROVED);
        return;
      }
    }

    if (ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())) {
      if (appId != null && !appId.equals(GLOBAL_APP_ID)) {
        if (getArtifactType(appId, artifactStream.getUuid()).equals(DOCKER)) {
          artifact.setContentStatus(METADATA_ONLY);
          artifact.setStatus(APPROVED);
          return;
        }
        artifact.setStatus(QUEUED);
        return;
      } else {
        if (RepositoryType.docker.name().equals(((ArtifactoryArtifactStream) artifactStream).getRepositoryType())) {
          artifact.setContentStatus(METADATA_ONLY);
          artifact.setStatus(APPROVED);
          return;
        }
        artifact.setStatus(QUEUED);
        return;
      }
    }
    artifact.setStatus(QUEUED);
  }

  // TODO: ASR: remove this method after migration
  private ArtifactType getArtifactType(String appId, String artifactStreamId) {
    return artifactStreamServiceBindingService.getService(appId, artifactStreamId, true).getArtifactType();
  }

  @Override
  @ValidationGroups(Update.class)
  public Artifact update(@Valid Artifact artifact) {
    wingsPersistence.update(wingsPersistence.createQuery(Artifact.class)
                                .filter(ArtifactKeys.accountId, artifact.getAccountId())
                                .filter(ArtifactKeys.uuid, artifact.getUuid()),
        wingsPersistence.createUpdateOperations(Artifact.class).set("displayName", artifact.getDisplayName()));
    return wingsPersistence.get(Artifact.class, artifact.getUuid());
  }

  @Override
  public void updateStatus(String artifactId, String accountId, Status status) {
    Query<Artifact> query = wingsPersistence.createQuery(Artifact.class)
                                .filter(ID_KEY, artifactId)
                                .filter(ArtifactKeys.accountId, accountId);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class);
    ops.set(ArtifactKeys.status, status);
    wingsPersistence.update(query, ops);
  }

  @Override
  public void updateStatus(String artifactId, String accountId, Status status, String errorMessage) {
    Query<Artifact> query = wingsPersistence.createQuery(Artifact.class)
                                .filter(ID_KEY, artifactId)
                                .filter(ArtifactKeys.accountId, accountId);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class);
    setUnset(ops, ArtifactKeys.status, status);
    setUnset(ops, ArtifactKeys.errorMessage, errorMessage);
    wingsPersistence.update(query, ops);
  }

  @Override
  public void updateStatus(String artifactId, String accountId, Status status, ContentStatus contentStatus) {
    Query<Artifact> query = wingsPersistence.createQuery(Artifact.class)
                                .filter(ID_KEY, artifactId)
                                .filter(ArtifactKeys.accountId, accountId);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class);
    setUnset(ops, ArtifactKeys.status, status);
    setUnset(ops, ArtifactKeys.contentStatus, contentStatus);
    wingsPersistence.update(query, ops);
  }

  @Override
  public void updateStatus(
      String artifactId, String accountId, Status status, ContentStatus contentStatus, String errorMessage) {
    Query<Artifact> query = wingsPersistence.createQuery(Artifact.class)
                                .filter(ID_KEY, artifactId)
                                .filter(ArtifactKeys.accountId, accountId);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class);
    setUnset(ops, ArtifactKeys.status, status);
    setUnset(ops, ArtifactKeys.contentStatus, contentStatus);
    setUnset(ops, ArtifactKeys.errorMessage, errorMessage);
    wingsPersistence.update(query, ops);
  }

  @Override
  public void updateArtifactSourceName(ArtifactStream artifactStream) {
    Query<Artifact> query = prepareArtifactWithMetadataQuery(artifactStream);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class);
    ops.set("artifactSourceName", artifactStream.getSourceName());
    wingsPersistence.update(query, ops);
  }

  @Override
  public void addArtifactFile(String artifactId, String accountId, List<ArtifactFile> artifactFile) {
    logger.info("Adding artifactFiles for artifactId {} and accountId {}", artifactId, accountId);
    Query<Artifact> query = wingsPersistence.createQuery(Artifact.class)
                                .filter(ArtifactKeys.accountId, accountId)
                                .filter(ArtifactKeys.uuid, artifactId);
    UpdateOperations<Artifact> ops =
        wingsPersistence.createUpdateOperations(Artifact.class).addAll(ArtifactKeys.artifactFiles, artifactFile, false);
    wingsPersistence.update(query, ops);
  }

  @Override
  public File download(String accountId, String artifactId) {
    Artifact artifact = get(accountId, artifactId);
    if (artifact == null || artifact.getStatus() != READY || isEmpty(artifact.getArtifactFiles())) {
      return null;
    }

    ArtifactFile artifactFile = artifact.getArtifactFiles().get(0);

    File tempDir = Files.createTempDir();
    String fileName = Optional.ofNullable(artifactFile.getName()).orElse(DEFAULT_ARTIFACT_FILE_NAME);

    File file = new File(tempDir, fileName);

    fileService.download(artifactFile.getFileUuid(), file, ARTIFACTS);
    return file;
  }

  @Override
  public Artifact get(String artifactId) {
    return wingsPersistence.get(Artifact.class, artifactId);
  }

  @Override
  public Artifact get(String accountId, String artifactId) {
    return wingsPersistence.createQuery(Artifact.class)
        .filter(ArtifactKeys.accountId, accountId)
        .filter(ArtifactKeys.uuid, artifactId)
        .get();
  }

  @Override
  public Artifact getWithServices(String artifactId, String appId) {
    Artifact artifact = wingsPersistence.get(Artifact.class, artifactId);
    artifact.setServices(artifactStreamServiceBindingService.listServices(appId, artifact.getArtifactStreamId()));
    return artifact;
  }

  @Override
  public boolean delete(String accountId, String artifactId) {
    Artifact artifact = get(accountId, artifactId);
    if (artifact == null) {
      return true;
    }

    if (isNotEmpty(artifact.getArtifactFiles())) {
      List<String> artifactIds = asList(artifactId);
      List<String> artifactFileUuids = collectArtifactFileIds(artifact);
      deleteArtifacts(artifactIds.toArray(), artifactFileUuids);
    } else {
      wingsPersistence.delete(accountId, Artifact.class, artifactId);
    }
    return true;
  }

  @Override
  public void deleteArtifacts(List<Artifact> artifacts) {
    List<String> artifactIds = new ArrayList<>();
    List<String> artifactIdsWithFiles = new ArrayList<>();
    List<String> artifactFileIds = new ArrayList<>();
    for (Artifact artifact : artifacts) {
      if (isNotEmpty(artifact.getArtifactFiles())) {
        artifactIdsWithFiles.add(artifact.getUuid());
        List<String> ids = collectArtifactFileIds(artifact);
        if (isNotEmpty(ids)) {
          artifactFileIds.addAll(ids);
        }
      } else {
        artifactIds.add(artifact.getUuid());
      }
    }
    if (isNotEmpty(artifactIds)) {
      wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "artifacts")
          .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactIds.toArray())));
    }
    if (isNotEmpty(artifactIdsWithFiles)) {
      deleteArtifacts(artifactIdsWithFiles.toArray(), artifactFileIds);
    }
  }

  @Override
  public void pruneByArtifactStream(String appId, String artifactStreamId) {
    // TODO: ASR: update with accountId
    deleteArtifactsByQuery(wingsPersistence.createQuery(Artifact.class, excludeAuthority)
                               .project(ArtifactKeys.artifactFiles, true)
                               .filter(ArtifactKeys.artifactStreamId, artifactStreamId));
  }

  private void deleteArtifactsByQuery(Query<Artifact> artifactQuery) {
    List<String> artifactIds = new ArrayList<>();
    List<String> artifactIdsWithFiles = new ArrayList<>();
    List<String> artifactFileIds = new ArrayList<>();
    try (HIterator<Artifact> iterator = new HIterator<>(artifactQuery.fetch())) {
      while (iterator.hasNext()) {
        Artifact artifact = iterator.next();
        if (isNotEmpty(artifact.getArtifactFiles())) {
          artifactIdsWithFiles.add(artifact.getUuid());
          List<String> ids = collectArtifactFileIds(artifact);
          if (isNotEmpty(ids)) {
            artifactFileIds.addAll(ids);
          }
        } else {
          artifactIds.add(artifact.getUuid());
        }
      }
    }
    if (isNotEmpty(artifactIds)) {
      wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "artifacts")
          .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactIds.toArray())));
    }
    if (isNotEmpty(artifactIdsWithFiles)) {
      deleteArtifacts(artifactIdsWithFiles.toArray(), artifactFileIds);
    }
  }

  private List<String> collectArtifactFileIds(Artifact artifact) {
    return artifact.getArtifactFiles()
        .stream()
        .filter(artifactFile -> artifactFile.getFileUuid() != null)
        .map(ArtifactFile::getFileUuid)
        .collect(Collectors.toList());
  }

  @Override
  public Artifact fetchLatestArtifactForArtifactStream(ArtifactStream artifactStream) {
    return getArtifact(artifactStream, asList(QUEUED, RUNNING, REJECTED, WAITING, READY, APPROVED, FAILED));
  }

  private Artifact getArtifact(ArtifactStream artifactStream, List<Status> statuses) {
    // TODO: ASR: update with accountId
    Query<Artifact> artifactQuery = wingsPersistence.createQuery(Artifact.class, excludeAuthority)
                                        .filter(ArtifactKeys.artifactStreamId, artifactStream.getUuid());
    // For the custom artifact stream name as set artifact source name. Name can be changed so, it can not be unique
    if (CUSTOM.name().equals(artifactStream.getArtifactStreamType())) {
      return artifactQuery.order("-createdAt").field(ArtifactKeys.status).hasAnyOf(statuses).get();
    }
    // For the custom artifact stream name as set artifact source name. Name can be changed so, it can not be unique
    artifactQuery.filter(ArtifactKeys.artifactSourceName, artifactStream.getSourceName());
    return artifactQuery.order("-createdAt").field(ArtifactKeys.status).hasAnyOf(statuses).get();
  }

  @Override
  public Artifact fetchLastCollectedApprovedArtifactForArtifactStream(ArtifactStream artifactStream) {
    return getArtifact(artifactStream, asList(READY, APPROVED));
  }

  @Override
  public Artifact fetchLastCollectedArtifact(ArtifactStream artifactStream) {
    return getArtifact(artifactStream, asList(READY, QUEUED, RUNNING, WAITING, APPROVED));
  }

  @Override
  public Artifact getArtifactByBuildNumber(ArtifactStream artifactStream, String buildNumber, boolean regex) {
    // TODO: ASR: update with accountId
    Query<Artifact> artifactQuery = wingsPersistence.createQuery(Artifact.class, excludeAuthority)
                                        .filter(ArtifactKeys.artifactStreamId, artifactStream.getUuid());
    if (CUSTOM.name().equals(artifactStream.getArtifactStreamType())) {
      return artifactQuery.filter("metadata.buildNo", regex ? compile(buildNumber) : buildNumber)
          .order("-createdAt")
          .disableValidation()
          .get();
    }
    artifactQuery.filter(ArtifactKeys.artifactSourceName, artifactStream.getSourceName());
    return artifactQuery.filter("metadata.buildNo", regex ? compile(buildNumber) : buildNumber)
        .order("-createdAt")
        .disableValidation()
        .get();
  }

  @Override
  public Artifact startArtifactCollection(String accountId, String artifactId) {
    logger.info("Start collecting artifact {} of accountId {}", artifactId, accountId);
    Artifact artifact = wingsPersistence.get(Artifact.class, artifactId);
    if (artifact == null) {
      throw new WingsException(
          "Artifact [" + artifactId + "] for the accountId [" + accountId + "] does not exist", USER);
    }
    if (RUNNING == artifact.getStatus() || QUEUED == artifact.getStatus()) {
      logger.info(
          "Artifact Metadata collection for artifactId {} of the accountId {} is in progress or queued. Returning.",
          artifactId, accountId);
      return artifact;
    }

    if (artifact.getContentStatus() == null && !isEmpty(artifact.getArtifactFiles())) {
      logger.info(
          "Artifact {} content status empty. It means it is already downloaded. Updating artifact content status as DOWNLOADED",
          artifactId);
      updateStatus(artifactId, artifact.getAccountId(), APPROVED, DOWNLOADED);
      return artifact;
    }

    if ((METADATA_ONLY == artifact.getContentStatus()) || (DOWNLOADING == artifact.getContentStatus())
        || (DOWNLOADED == artifact.getContentStatus())) {
      logger.info(
          "Artifact content for artifactId {} of the accountId {} is either downloaded or in progress. Returning.",
          artifactId, accountId);
      return artifact;
    }

    logger.info("Sending event to collect artifact {} ", artifact.getUuid());
    collectQueue.send(aCollectEvent().withArtifact(artifact).build());

    return artifact;
  }

  @Override
  public ContentStatus getArtifactContentStatus(Artifact artifact) {
    if (artifact.getContentStatus() != null) {
      return artifact.getContentStatus();
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getArtifactStreamId());
    if (artifactStream == null) {
      logger.info("ArtifactStream of artifact {} was deleted", artifact.getUuid());
      artifact = wingsPersistence.get(Artifact.class, artifact.getUuid());
      if (artifact == null) {
        return DELETED;
      }
      if (artifact.getContentStatus() == null) {
        if (!isEmpty(artifact.getArtifactFiles())) {
          updateStatus(artifact.getUuid(), artifact.getAccountId(), APPROVED, DOWNLOADED);
          return DOWNLOADED;
        } else {
          updateStatus(artifact.getUuid(), artifact.getAccountId(), APPROVED, METADATA_ONLY);
          return METADATA_ONLY;
        }
      }
      return artifact.getContentStatus();
    }
    setArtifactStatus(artifact, artifactStream);
    return artifact.getContentStatus();
  }

  @Override
  public void deleteArtifacts(int retentionSize) {
    try (HIterator<ArtifactStream> artifactStreams =
             new HIterator(wingsPersistence.createQuery(ArtifactStream.class)
                               .project(ArtifactStreamKeys.artifactStreamType, true)
                               .project(ArtifactStreamKeys.metadataOnly, true)
                               .fetch())) {
      while (artifactStreams.hasNext()) {
        ArtifactStream artifactStream = artifactStreams.next();
        deleteArtifactsWithContents(retentionSize, artifactStream);
      }
    }
  }

  private void deleteArtifactsWithContents(int retentionSize, ArtifactStream artifactStream) {
    if (artifactStream.isMetadataOnly() || autoDownloaded.contains(artifactStream.getArtifactStreamType())) {
      return;
    }
    // TODO: ASR: update with accountId
    List<Artifact> toBeDeletedArtifacts = wingsPersistence.createQuery(Artifact.class, excludeAuthority)
                                              .project(ArtifactKeys.artifactFiles, true)
                                              .filter(ArtifactKeys.artifactStreamId, artifactStream.getUuid())
                                              .field(ArtifactKeys.contentStatus)
                                              .hasAnyOf(asList(DOWNLOADED))
                                              .order(Sort.descending(CREATED_AT_KEY))
                                              .asList(new FindOptions().skip(retentionSize));
    if (isNotEmpty(toBeDeletedArtifacts)) {
      toBeDeletedArtifacts =
          toBeDeletedArtifacts.stream().filter(artifact -> isNotEmpty(artifact.getArtifactFiles())).collect(toList());
      logger.info("Deleting artifacts for artifactStreamId [{}] of size: [{}]", artifactStream.getUuid(),
          toBeDeletedArtifacts.size());
      deleteArtifacts(artifactStream.getUuid(), toBeDeletedArtifacts);
    }
  }

  private void deleteArtifacts(String artifactStreamId, List<Artifact> toBeDeletedArtifacts) {
    try {
      List<String> artifactFileIds = toBeDeletedArtifacts.stream()
                                         .flatMap(artifact -> artifact.getArtifactFiles().stream())
                                         .filter(artifactFile -> artifactFile.getFileUuid() != null)
                                         .map(artifactFile -> artifactFile.getFileUuid())
                                         .collect(Collectors.toList());
      if (isNotEmpty(artifactFileIds)) {
        Object[] artifactIds = toBeDeletedArtifacts.stream().map(Artifact::getUuid).toArray();
        deleteArtifacts(artifactIds, artifactFileIds);
      }
    } catch (Exception ex) {
      logger.warn(String.format("Failed to purge(delete) artifacts for artifactStreamId %s of size: %s",
                      artifactStreamId, toBeDeletedArtifacts.size()),
          ex);
    }
    logger.info("Deleting artifacts for artifactStreamId {} of size: {} success", artifactStreamId,
        toBeDeletedArtifacts.size());
  }

  private void deleteArtifacts(Object[] artifactIds, List<String> artifactFileIds) {
    logger.info("Deleting artifactIds of artifacts {}", artifactIds);
    wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "artifacts")
        .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactIds)));
    if (isNotEmpty(artifactFileIds)) {
      for (String fileId : artifactFileIds) {
        fileService.deleteFile(fileId, ARTIFACTS);
      }
    }
  }

  public Query<Artifact> prepareArtifactWithMetadataQuery(ArtifactStream artifactStream) {
    // TODO: ASR: update with accountId
    Query<Artifact> artifactQuery = wingsPersistence.createQuery(Artifact.class, excludeAuthority)
                                        .project(ArtifactKeys.metadata, true)
                                        .project(ArtifactKeys.revision, true)
                                        .filter(ArtifactKeys.artifactStreamId, artifactStream.getUuid())
                                        .field(ArtifactKeys.status)
                                        .hasAnyOf(asList(QUEUED, RUNNING, REJECTED, WAITING, READY, APPROVED, FAILED))
                                        .disableValidation();
    if (CUSTOM.name().equals(artifactStream.getArtifactStreamType())) {
      return artifactQuery;
    }
    artifactQuery.filter(ArtifactKeys.artifactSourceName, artifactStream.getSourceName());
    return artifactQuery;
  }

  @Override
  public void deleteWhenArtifactSourceNameChanged(ArtifactStream artifactStream) {
    deleteArtifactsByQuery(wingsPersistence.createQuery(Artifact.class, excludeAuthority)
                               .project(ArtifactKeys.artifactFiles, true)
                               .filter(ArtifactKeys.artifactStreamId, artifactStream.getUuid())
                               .filter(ArtifactKeys.artifactSourceName, artifactStream.getSourceName()));
  }

  @Override
  public List<Artifact> listByIds(String accountId, Collection<String> artifactIds) {
    return wingsPersistence.createQuery(Artifact.class)
        .filter(ArtifactKeys.accountId, accountId)
        .field(ArtifactKeys.uuid)
        .in(artifactIds)
        .asList();
  }

  @Override
  public List<Artifact> listByAccountId(String accountId) {
    return wingsPersistence.createQuery(Artifact.class).filter(ArtifactKeys.accountId, accountId).asList();
  }

  @Override
  public List<Artifact> listByAppId(String appId) {
    // NOTE: appId is only used for finding accountId
    if (GLOBAL_APP_ID.equals(appId)) {
      return new ArrayList<>();
    }

    return listByAccountId(appService.getAccountIdByAppId(appId));
  }
}
