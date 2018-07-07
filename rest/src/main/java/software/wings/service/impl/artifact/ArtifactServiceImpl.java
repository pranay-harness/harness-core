package software.wings.service.impl.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.CREATED_AT_KEY;
import static software.wings.beans.artifact.Artifact.ARTIFACT_STREAM_ID_KEY;
import static software.wings.beans.artifact.Artifact.CONTENT_STATUS_KEY;
import static software.wings.beans.artifact.Artifact.ContentStatus.DELETED;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADED;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADING;
import static software.wings.beans.artifact.Artifact.ContentStatus.METADATA_ONLY;
import static software.wings.beans.artifact.Artifact.ContentStatus.NOT_DOWNLOADED;
import static software.wings.beans.artifact.Artifact.ERROR_MSG_KEY;
import static software.wings.beans.artifact.Artifact.STATUS_KEY;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.FAILED;
import static software.wings.beans.artifact.Artifact.Status.QUEUED;
import static software.wings.beans.artifact.Artifact.Status.READY;
import static software.wings.beans.artifact.Artifact.Status.REJECTED;
import static software.wings.beans.artifact.Artifact.Status.RUNNING;
import static software.wings.beans.artifact.Artifact.Status.WAITING;
import static software.wings.beans.artifact.ArtifactStream.ARTIFACT_STREAM_TYPE_KEY;
import static software.wings.beans.artifact.ArtifactStream.METADATA_ONLY_KEY;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.collect.CollectEvent.Builder.aCollectEvent;
import static software.wings.common.Constants.autoDownloaded;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.exception.WingsException.USER;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;
import static software.wings.utils.ArtifactType.DOCKER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ErrorCode;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ContentStatus;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.collect.CollectEvent;
import software.wings.core.queue.Queue;
import software.wings.dl.HIterator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class ArtifactServiceImpl.
 */
@Singleton
@ValidateOnExecution
public class ArtifactServiceImpl implements ArtifactService {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactServiceImpl.class);

  private static final String DEFAULT_ARTIFACT_FILE_NAME = "ArtifactFile";
  private static final int ARTIFACT_RETENTION_SIZE = 25;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
  @Inject private Queue<CollectEvent> collectQueue;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ExecutorService executorService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Artifact> list(PageRequest<Artifact> pageRequest, boolean withServices) {
    PageResponse<Artifact> pageResponse = wingsPersistence.query(Artifact.class, pageRequest);
    if (withServices) {
      if (pageResponse != null && pageResponse.getResponse() != null) {
        for (Artifact artifact : pageResponse.getResponse()) {
          try {
            artifact.setServices(artifact.getServiceIds()
                                     .stream()
                                     .map(serviceId -> serviceResourceService.get(artifact.getAppId(), serviceId))
                                     .collect(toList()));
          } catch (Exception e) {
            logger.error("Failed to set services for artifact {} ", artifact, e);
          }
        }
      }
    }
    return pageResponse;
  }

  @Override
  public PageResponse<Artifact> listSortByBuildNo(PageRequest<Artifact> pageRequest) {
    PageResponse<Artifact> pageResponse = wingsPersistence.query(Artifact.class, pageRequest);
    Map<String, List<Artifact>> groupByArtifactStream =
        pageResponse.getResponse().stream().collect(Collectors.groupingBy(Artifact::getArtifactStreamId));
    List<Artifact> artifacts = new ArrayList<>();
    for (Entry<String, List<Artifact>> artifactStreamEntry : groupByArtifactStream.entrySet()) {
      artifacts.addAll(artifactStreamEntry.getValue().stream().sorted(new ArtifactComparator()).collect(toList()));
    }
    pageResponse.setResponse(artifacts);
    return pageResponse;
  }

  @Override
  @ValidationGroups(Create.class)
  public Artifact create(@Valid Artifact artifact) {
    if (!appService.exist(artifact.getAppId())) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
          .addParam("args", "App does not exist: " + artifact.getAppId());
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
    notNullCheck("Artifact Stream", artifactStream, USER);

    artifact.setArtifactSourceName(artifactStream.getSourceName());
    artifact.setServiceIds(asList(artifactStream.getServiceId()));

    setArtifactStatus(artifact, artifactStream);

    String key = wingsPersistence.save(artifact);

    Artifact savedArtifact = wingsPersistence.get(Artifact.class, artifact.getAppId(), key);
    if (savedArtifact.getStatus().equals(QUEUED)) {
      logger.info("Sending event to collect artifact {} ", savedArtifact.getUuid());
      collectQueue.send(aCollectEvent().withArtifact(savedArtifact).build());
    }
    executorService.submit(() -> deleteArtifactsWithContents(ARTIFACT_RETENTION_SIZE, artifactStream));
    return savedArtifact;
  }

  private void setArtifactStatus(Artifact artifact, ArtifactStream artifactStream) {
    if (artifactStream.isMetadataOnly() || autoDownloaded.contains(artifactStream.getArtifactStreamType())) {
      artifact.setContentStatus(METADATA_ONLY);
      artifact.setStatus(APPROVED);
      return;
    }
    if (NEXUS.name().equals(artifactStream.getArtifactStreamType())) {
      artifact.setContentStatus(getArtifactType(artifactStream).equals(DOCKER) ? METADATA_ONLY : NOT_DOWNLOADED);
      artifact.setStatus(APPROVED);
      return;
    }

    if (ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())) {
      if (getArtifactType(artifactStream).equals(DOCKER)) {
        artifact.setContentStatus(METADATA_ONLY);
        artifact.setStatus(APPROVED);
        return;
      }
      artifact.setStatus(QUEUED);
      return;
    }
    artifact.setStatus(QUEUED);
  }

  private ArtifactType getArtifactType(ArtifactStream artifactStream) {
    return serviceResourceService.get(artifactStream.getAppId(), artifactStream.getServiceId(), false)
        .getArtifactType();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#update(software.wings.beans.artifact.Artifact)
   */
  @Override
  @ValidationGroups(Update.class)
  public Artifact update(@Valid Artifact artifact) {
    wingsPersistence.update(wingsPersistence.createQuery(Artifact.class)
                                .filter(APP_ID_KEY, artifact.getAppId())
                                .filter(ID_KEY, artifact.getUuid()),
        wingsPersistence.createUpdateOperations(Artifact.class).set("displayName", artifact.getDisplayName()));
    return wingsPersistence.get(Artifact.class, artifact.getAppId(), artifact.getUuid());
  }

  @Override
  public void updateStatus(String artifactId, String appId, Status status) {
    Query<Artifact> query =
        wingsPersistence.createQuery(Artifact.class).filter(ID_KEY, artifactId).filter(APP_ID_KEY, appId);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class);
    ops.set("status", status);
    wingsPersistence.update(query, ops);
  }

  @Override
  public void updateStatus(String artifactId, String appId, Status status, String errorMessage) {
    Query<Artifact> query =
        wingsPersistence.createQuery(Artifact.class).filter(ID_KEY, artifactId).filter(APP_ID_KEY, appId);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class);
    setUnset(ops, STATUS_KEY, status);
    setUnset(ops, ERROR_MSG_KEY, errorMessage);
    wingsPersistence.update(query, ops);
  }

  @Override
  public void updateStatus(String artifactId, String appId, Status status, ContentStatus contentStatus) {
    Query<Artifact> query =
        wingsPersistence.createQuery(Artifact.class).filter(ID_KEY, artifactId).filter(APP_ID_KEY, appId);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class);
    setUnset(ops, STATUS_KEY, status);
    setUnset(ops, CONTENT_STATUS_KEY, contentStatus);
    wingsPersistence.update(query, ops);
  }

  @Override
  public void updateStatus(
      String artifactId, String appId, Status status, ContentStatus contentStatus, String errorMessage) {
    Query<Artifact> query =
        wingsPersistence.createQuery(Artifact.class).filter(ID_KEY, artifactId).filter(APP_ID_KEY, appId);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class);
    setUnset(ops, STATUS_KEY, status);
    setUnset(ops, CONTENT_STATUS_KEY, contentStatus);
    setUnset(ops, ERROR_MSG_KEY, errorMessage);
    wingsPersistence.update(query, ops);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#addArtifactFile(java.lang.String, java.lang.String,
   * java.util.List)
   */
  @Override
  public void addArtifactFile(String artifactId, String appId, List<ArtifactFile> artifactFile) {
    logger.info("Adding artifactFiles for artifactId {} and appId {}", artifactId, appId);
    Query<Artifact> query =
        wingsPersistence.createQuery(Artifact.class).filter(ID_KEY, artifactId).filter(APP_ID_KEY, appId);
    UpdateOperations<Artifact> ops =
        wingsPersistence.createUpdateOperations(Artifact.class).addAll("artifactFiles", artifactFile, false);
    wingsPersistence.update(query, ops);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#download(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public File download(String appId, String artifactId) {
    Artifact artifact = wingsPersistence.get(Artifact.class, appId, artifactId);
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

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#get(java.lang.String, java.lang.String)
   */
  @Override
  public Artifact get(String appId, String artifactId) {
    return get(appId, artifactId, false);
  }

  @Override
  public Artifact get(String appId, String artifactId, boolean withServices) {
    Artifact artifact = wingsPersistence.get(Artifact.class, appId, artifactId);
    if (withServices) {
      List<Service> services = artifact.getServiceIds()
                                   .stream()
                                   .map(serviceId -> serviceResourceService.get(artifact.getAppId(), serviceId))
                                   .collect(toList());
      artifact.setServices(services);
    }
    return artifact;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#softDelete(java.lang.String, java.lang.String)
   */
  @Override
  public boolean delete(String appId, String artifactId) {
    Artifact artifact = get(appId, artifactId);
    if (artifact == null) {
      return true;
    }

    if (isNotEmpty(artifact.getArtifactFiles())) {
      List<String> artifactIds = asList(artifactId);
      List<ObjectId> artifactFileUuids = artifact.getArtifactFiles()
                                             .stream()
                                             .filter(artifactFile -> artifactFile.getFileUuid() != null)
                                             .map(artifactFile -> new ObjectId(artifactFile.getFileUuid()))
                                             .collect(Collectors.toList());
      deleteArtifacts(artifactIds.toArray(), artifactFileUuids);
    } else {
      wingsPersistence.delete(Artifact.class, appId, artifactId);
    }
    return true;
  }

  public boolean prune(String appId, String artifactId) {
    return wingsPersistence.delete(Artifact.class, appId, artifactId);
  }

  @Override
  public void pruneByArtifactStream(String appId, String artifactStreamId) {
    List<String> artifactIds = new ArrayList<>();
    List<String> artifactIdsWithFiles = new ArrayList<>();
    List<ObjectId> artifactFileUuids = new ArrayList<>();
    try (HIterator<Artifact> iterator = new HIterator<>(wingsPersistence.createQuery(Artifact.class)
                                                            .filter(APP_ID_KEY, appId)
                                                            .project("artifactFiles", true)
                                                            .filter(APP_ID_KEY, appId)
                                                            .filter(ARTIFACT_STREAM_ID_KEY, artifactStreamId)
                                                            .fetch())) {
      while (iterator.hasNext()) {
        Artifact artifact = iterator.next();
        if (isNotEmpty(artifact.getArtifactFiles())) {
          artifactIdsWithFiles.add(artifact.getUuid());
          List<ObjectId> ids = artifact.getArtifactFiles()
                                   .stream()
                                   .filter(artifactFile -> artifactFile.getFileUuid() != null)
                                   .map(artifactFile -> new ObjectId(artifactFile.getFileUuid()))
                                   .collect(Collectors.toList());
          if (isNotEmpty(ids)) {
            artifactFileUuids.addAll(ids);
          }
        } else {
          artifactIds.add(artifact.getUuid());
        }
      }
    }
    if (isNotEmpty(artifactIds)) {
      wingsPersistence.getCollection("artifacts")
          .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactIds.toArray())));
    }
    if (isNotEmpty(artifactIdsWithFiles)) {
      deleteArtifacts(artifactIdsWithFiles.toArray(), artifactFileUuids);
    }
  }

  @Override
  public Artifact fetchLatestArtifactForArtifactStream(
      String appId, String artifactStreamId, String artifactSourceName) {
    return getArtifact(appId, artifactStreamId, artifactSourceName,
        asList(QUEUED, RUNNING, REJECTED, WAITING, READY, APPROVED, FAILED));
  }

  private Artifact getArtifact(
      String appId, String artifactStreamId, String artifactSourceName, List<Status> statuses) {
    return wingsPersistence.createQuery(Artifact.class)
        .filter("appId", appId)
        .filter("artifactStreamId", artifactStreamId)
        .filter("artifactSourceName", artifactSourceName)
        .order("-createdAt")
        .field("status")
        .hasAnyOf(statuses)
        .get();
  }

  @Override
  public Artifact fetchLastCollectedArtifactForArtifactStream(
      String appId, String artifactStreamId, String artifactSourceName) {
    return getArtifact(appId, artifactStreamId, artifactSourceName, asList(READY, APPROVED));
  }

  @Override
  public Artifact getArtifactByBuildNumber(String appId, String artifactStreamId, String buildNumber) {
    return wingsPersistence.createQuery(Artifact.class)
        .filter("appId", appId)
        .filter("artifactStreamId", artifactStreamId)
        .filter("metadata.buildNo", buildNumber)
        .get();
  }

  @Override
  public Artifact getArtifactByBuildNumber(
      String appId, String artifactStreamId, String artifactSource, String buildNumber, boolean regex) {
    return wingsPersistence.createQuery(Artifact.class)
        .filter("appId", appId)
        .filter("artifactStreamId", artifactStreamId)
        .filter("artifactSourceName", artifactSource)
        .filter("metadata.buildNo", regex ? compile(buildNumber) : buildNumber)
        .order("-createdAt")
        .disableValidation()
        .get();
  }

  @Override
  public Artifact startArtifactCollection(String appId, String artifactId) {
    logger.info("Start collecting artifact {} of appId {}", artifactId, appId);
    Artifact artifact = wingsPersistence.get(Artifact.class, appId, artifactId);
    if (artifact == null) {
      throw new WingsException("Artifact [" + artifactId + "] for the appId [" + appId + "] does not exist", USER);
    }
    if (RUNNING == artifact.getStatus() || QUEUED == artifact.getStatus()) {
      logger.info("Artifact Metadata collection for artifactId {} of the appId {} is in progress or queued. Returning.",
          artifactId, appId);
      return artifact;
    }

    if (artifact.getContentStatus() == null && !isEmpty(artifact.getArtifactFiles())) {
      logger.info(
          "Artifact {} content status empty. It means it is already downloaded. Updating artifact content status as DOWNLOADED",
          artifactId);
      updateStatus(artifactId, appId, APPROVED, DOWNLOADED);
      return artifact;
    }

    if ((METADATA_ONLY == artifact.getContentStatus()) || (DOWNLOADING == artifact.getContentStatus())
        || (DOWNLOADED == artifact.getContentStatus())) {
      logger.info("Artifact content for artifactId {} of the appId {} is either downloaded or in progress. Returning.",
          artifactId, appId);
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
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
    if (artifactStream == null) {
      logger.info("ArtifactStream of artifact {} was deleted", artifact.getUuid());
      artifact = wingsPersistence.get(Artifact.class, artifact.getAppId(), artifact.getUuid());
      if (artifact == null) {
        return DELETED;
      }
      if (artifact.getContentStatus() == null) {
        if (!isEmpty(artifact.getArtifactFiles())) {
          updateStatus(artifact.getUuid(), artifact.getAppId(), APPROVED, DOWNLOADED);
          return DOWNLOADED;
        } else {
          updateStatus(artifact.getUuid(), artifact.getAppId(), APPROVED, METADATA_ONLY);
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
    try (HIterator<ArtifactStream> artifactStreams = new HIterator(wingsPersistence.createQuery(ArtifactStream.class)
                                                                       .project(ARTIFACT_STREAM_TYPE_KEY, true)
                                                                       .project(APP_ID_KEY, true)
                                                                       .project(METADATA_ONLY_KEY, true)
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
    List<Artifact> toBeDeletedArtifacts = wingsPersistence.createQuery(Artifact.class)
                                              .project("artifactFiles", true)
                                              .filter(APP_ID_KEY, artifactStream.getAppId())
                                              .filter(ARTIFACT_STREAM_ID_KEY, artifactStream.getUuid())
                                              .field(CONTENT_STATUS_KEY)
                                              .hasAnyOf(asList(DOWNLOADED))
                                              .order(Sort.descending(CREATED_AT_KEY))
                                              .asList(new FindOptions().skip(retentionSize));
    if (isNotEmpty(toBeDeletedArtifacts)) {
      toBeDeletedArtifacts =
          toBeDeletedArtifacts.stream().filter(artifact -> isNotEmpty(artifact.getArtifactFiles())).collect(toList());
      logger.info("Deleting artifacts for artifactStreamId [{}]  of size: [{}] for appId [{}]",
          artifactStream.getUuid(), toBeDeletedArtifacts.size(), artifactStream.getAppId());
      deleteArtifacts(artifactStream.getAppId(), artifactStream.getUuid(), toBeDeletedArtifacts);
    }
  }

  private void deleteArtifacts(String appId, String artifactStreamId, List<Artifact> toBeDeletedArtifacts) {
    try {
      List<ObjectId> artifactFileUuids = toBeDeletedArtifacts.stream()
                                             .flatMap(artifact -> artifact.getArtifactFiles().stream())
                                             .filter(artifactFile -> artifactFile.getFileUuid() != null)
                                             .map(artifactFile -> new ObjectId(artifactFile.getFileUuid()))
                                             .collect(Collectors.toList());
      if (isNotEmpty(artifactFileUuids)) {
        Object[] artifactIds = toBeDeletedArtifacts.stream().map(Artifact::getUuid).toArray();
        deleteArtifacts(artifactIds, artifactFileUuids);
      }
    } catch (Exception ex) {
      logger.warn(format("Failed to purge(delete) artifacts for artifactStreamId %s of size: %s for appId %s",
                      artifactStreamId, toBeDeletedArtifacts.size(), appId),
          ex);
    }
    logger.info("Deleting artifacts for artifactStreamId {}  of size: {} for appId {} success", artifactStreamId,
        toBeDeletedArtifacts.size(), appId);
  }

  private void deleteArtifacts(Object[] artifactIds, List<ObjectId> artifactFileUuids) {
    logger.info("Deleting artifactIds of artifacts {}", artifactIds);
    wingsPersistence.getCollection("artifacts").remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactIds)));
    if (isNotEmpty(artifactFileUuids)) {
      wingsPersistence.getCollection("artifacts.files")
          .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactFileUuids.toArray())));
      wingsPersistence.getCollection("artifacts.chunks")
          .remove(new BasicDBObject("files_id", new BasicDBObject("$in", artifactFileUuids.toArray())));
    }
  }

  @Override
  public void deleteArtifactFiles() {
    try {
      List<String> fileUuids = wingsPersistence.getCollection("artifacts").distinct("artifactFiles.fileUuid");
      logger.info("Artifact with files size {} and uuids {} ", fileUuids.size(), fileUuids);
      List<ObjectId> artifactFileUuids = wingsPersistence.getCollection("artifacts.files").distinct("_id");
      logger.info("Artifact files size {} and  uuids {}", artifactFileUuids.size(), artifactFileUuids);
      List<ObjectId> fileObjectUuids = fileUuids.stream().map((String s) -> new ObjectId(s)).collect(toList());
      artifactFileUuids.removeAll(fileObjectUuids);
      logger.info("Dangling artifact files size {} and  uuids {}", artifactFileUuids.size(), artifactFileUuids);
      wingsPersistence.getCollection("artifacts.files")
          .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactFileUuids.toArray())));
      wingsPersistence.getCollection("artifacts.chunks")
          .remove(new BasicDBObject("files_id", new BasicDBObject("$in", artifactFileUuids.toArray())));
    } catch (Exception ex) {
      logger.warn("Failed to purge (delete) the artifact files", ex);
    }
  }

  @Override
  public List<Artifact> fetchArtifacts(String appId, Set<String> artifactUuids) {
    return wingsPersistence.createQuery(Artifact.class)
        .filter(APP_ID_KEY, appId)
        .field(ID_KEY)
        .in(artifactUuids)
        .asList();
  }
}
