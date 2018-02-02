package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.FAILED;
import static software.wings.beans.artifact.Artifact.Status.QUEUED;
import static software.wings.beans.artifact.Artifact.Status.READY;
import static software.wings.beans.artifact.Artifact.Status.REJECTED;
import static software.wings.beans.artifact.Artifact.Status.RUNNING;
import static software.wings.beans.artifact.Artifact.Status.WAITING;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.collect.CollectEvent.Builder.aCollectEvent;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Application;
import software.wings.beans.ErrorCode;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.collect.CollectEvent;
import software.wings.core.queue.Queue;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.PruneFileJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;
import software.wings.utils.Validator;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
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
public class ArtifactServiceImpl implements ArtifactService {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactServiceImpl.class);

  private static final String DEFAULT_ARTIFACT_FILE_NAME = "ArtifactFile";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
  @Inject private Queue<CollectEvent> collectQueue;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ExecutorService executorService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  private final DateFormat dateFormat = new SimpleDateFormat("HHMMSS");

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
                                     .collect(Collectors.toList()));
          } catch (Exception e) {
            logger.error("Failed to set services for artifact {} ", artifact, e);
          }
        }
      }
    }
    return pageResponse;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#create(software.wings.beans.artifact.Artifact)
   */
  @Override
  @ValidationGroups(Create.class)
  public Artifact create(@Valid Artifact artifact) {
    return create(artifact, null);
  }

  @Override
  @ValidationGroups(Create.class)
  public Artifact create(@Valid Artifact artifact, ArtifactType artifactType) {
    if (!appService.exist(artifact.getAppId())) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT);
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
    Validator.notNullCheck("Artifact Stream", artifactStream);

    artifact.setArtifactSourceName(artifactStream.getSourceName());
    artifact.setServiceIds(asList(artifactStream.getServiceId()));
    Status status = getArtifactStatus(artifactStream);
    if (artifactType != null) {
      status = getArtifactStatus(artifactStream, artifactType);
    }
    artifact.setStatus(status);

    String key = wingsPersistence.save(artifact);

    Artifact savedArtifact = wingsPersistence.get(Artifact.class, artifact.getAppId(), key);
    if (status.equals(QUEUED)) {
      logger.info("Sending event to collect artifact {} ", savedArtifact);
      collectQueue.send(aCollectEvent().withArtifact(savedArtifact).build());
    }
    //    else {
    //      logger.info("Artifact stream {} set as Meta-data Only. Not collecting artifact", artifactStream);
    //      logger.info("Triggering deployment trigger  on post artifact collection if any");
    //      artifactStreamService.triggerStreamActionPostArtifactCollectionAsync(savedArtifact);
    //    }

    return savedArtifact;
  }

  private Status getArtifactStatus(ArtifactStream artifactStream) {
    if (artifactStream.isMetadataOnly()) {
      return APPROVED;
    }
    return (DOCKER.name().equals(artifactStream.getArtifactStreamType())
               || ECR.name().equals(artifactStream.getArtifactStreamType())
               || GCR.name().equals(artifactStream.getArtifactStreamType())
               || ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType()))
        ? (artifactStream.isAutoApproveForProduction() ? APPROVED : READY)
        : QUEUED;
  }

  private Status getArtifactStatus(ArtifactStream artifactStream, ArtifactType artifactType) {
    if (artifactStream.isMetadataOnly()) {
      return APPROVED;
    }
    if (ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType())
        || NEXUS.name().equals(artifactStream.getArtifactStreamType())) {
      if (artifactType.equals(ArtifactType.DOCKER)) {
        return APPROVED;
      }
    }
    return QUEUED;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#update(software.wings.beans.artifact.Artifact)
   */
  @Override
  @ValidationGroups(Update.class)
  public Artifact update(@Valid Artifact artifact) {
    wingsPersistence.update(wingsPersistence.createQuery(Artifact.class)
                                .field("appId")
                                .equal(artifact.getAppId())
                                .field(ID_KEY)
                                .equal(artifact.getUuid()),
        wingsPersistence.createUpdateOperations(Artifact.class).set("displayName", artifact.getDisplayName()));
    return wingsPersistence.get(Artifact.class, artifact.getAppId(), artifact.getUuid());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#updateStatus(java.lang.String, java.lang.String,
   * software.wings.beans.artifact.Artifact.Status)
   */
  @Override
  public void updateStatus(String artifactId, String appId, Status status) {
    Query<Artifact> query =
        wingsPersistence.createQuery(Artifact.class).field(ID_KEY).equal(artifactId).field("appId").equal(appId);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class).set("status", status);
    wingsPersistence.update(query, ops);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#updateStatus(java.lang.String, java.lang.String,
   * software.wings.beans.artifact.Artifact.Status)
   */
  @Override
  public void updateStatus(String artifactId, String appId, Status status, String errorMessage) {
    Query<Artifact> query =
        wingsPersistence.createQuery(Artifact.class).field(ID_KEY).equal(artifactId).field("appId").equal(appId);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class);
    setUnset(ops, "status", status);
    setUnset(ops, "errorMessage", errorMessage);
    wingsPersistence.update(query, ops);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#addArtifactFile(java.lang.String, java.lang.String,
   * java.util.List)
   */
  @Override
  public void addArtifactFile(String artifactId, String appId, List<ArtifactFile> artifactFile) {
    Query<Artifact> query =
        wingsPersistence.createQuery(Artifact.class).field(ID_KEY).equal(artifactId).field("appId").equal(appId);
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
                                   .collect(Collectors.toList());
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

    PruneFileJob.addDefaultJob(jobScheduler, Artifact.class, artifactId, FileBucket.ARTIFACTS);
    return wingsPersistence.delete(artifact);
  }

  @Override
  public void pruneByApplication(String appId) {
    wingsPersistence.createQuery(Artifact.class)
        .field(Artifact.APP_ID_KEY)
        .equal(appId)
        .asList()
        .forEach(artifact -> delete(appId, artifact.getUuid()));
  }

  @Override
  public Artifact fetchLatestArtifactForArtifactStream(
      String appId, String artifactStreamId, String artifactSourceName) {
    return wingsPersistence.createQuery(Artifact.class)
        .field("appId")
        .equal(appId)
        .field("artifactStreamId")
        .equal(artifactStreamId)
        .field("artifactSourceName")
        .equal(artifactSourceName)
        .order("-createdAt")
        .field("status")
        .hasAnyOf(asList(QUEUED, RUNNING, REJECTED, WAITING, READY, APPROVED, FAILED))
        .get();
  }

  @Override
  public Artifact fetchLastCollectedArtifactForArtifactStream(
      String appId, String artifactStreamId, String artifactSourceName) {
    return wingsPersistence.createQuery(Artifact.class)
        .field("appId")
        .equal(appId)
        .field("artifactStreamId")
        .equal(artifactStreamId)
        .field("artifactSourceName")
        .equal(artifactSourceName)
        .order("-createdAt")
        .field("status")
        .hasAnyOf(asList(READY, APPROVED))
        .get();
  }

  @Override
  public void deleteByArtifactStream(String appId, String artifactStreamId) {
    wingsPersistence.createQuery(Artifact.class)
        .field(Artifact.APP_ID_KEY)
        .equal(appId)
        .field("artifactStreamId")
        .equal(artifactStreamId)
        .asList()
        .forEach(artifact -> delete(appId, artifact.getUuid()));
  }

  @Override
  public void deleteArtifacts(int retentionSize) {
    List<Key<Application>> appKeys = wingsPersistence.createQuery(Application.class).asKeyList();
    for (Key<Application> app : appKeys) {
      String appId = app.getId().toString();
      List<Service> services = wingsPersistence.createQuery(Service.class).field("appId").equal(appId).asList();
      for (Service service : services) {
        if (ArtifactType.DOCKER.equals(service.getArtifactType())
            || ArtifactType.AMI.name().equals(service.getArtifactType())) {
          logger.info("Service [{}] artifact type   for the app [{}] is Docker or AMI. Skipping deleting artifacts",
              service.getName(), appId);
          continue;
        }
        List<ArtifactStream> artifactStreams = wingsPersistence.createQuery(ArtifactStream.class)
                                                   .field("appId")
                                                   .equal(appId)
                                                   .field("serviceId")
                                                   .equal(service.getUuid())
                                                   .asList();
        for (ArtifactStream artifactStream : artifactStreams) {
          if (artifactStream.isMetadataOnly()) {
            logger.info("Service [{}] artifact type   for the app [{}] is Metadata only. Skipping deleting artifacts",
                service.getName(), appId);
            continue;
          }
          List<Artifact> toBeDeletedArtifacts = wingsPersistence.createQuery(Artifact.class)
                                                    .field("appId")
                                                    .equal(appId)
                                                    .field("artifactStreamId")
                                                    .equal(artifactStream.getUuid())
                                                    .field("status")
                                                    .in(asList(READY, APPROVED))
                                                    .offset(retentionSize)
                                                    .asList();
          if (isNotEmpty(toBeDeletedArtifacts)) {
            toBeDeletedArtifacts = toBeDeletedArtifacts.stream()
                                       .filter(artifact -> !artifact.getArtifactFiles().isEmpty())
                                       .collect(Collectors.toList());
            logger.info("Deleting artifacts for artifactStreamId [{}]  of size: [{}] for appId [{}]",
                artifactStream.getUuid(), toBeDeletedArtifacts.size(), appId);
            deleteArtifacts(app.getId().toString(), artifactStream.getUuid(), toBeDeletedArtifacts);
          } else {
            logger.info(
                "ArtifactStreamId [{}] for the app [{}] does not have more than [{}] successful artifacts. Not deleting",
                artifactStream.getUuid(), appId, retentionSize);
          }
        }
      }
    }
  }

  private void deleteArtifacts(String appId, String artifactStreamId, List<Artifact> toBeDeletedArtifacts) {
    try {
      List<String> artifactUuids = toBeDeletedArtifacts.stream().map(Artifact::getUuid).collect(Collectors.toList());
      List<ObjectId> artifactFileUuids = new ArrayList<>();
      for (Artifact artifact : toBeDeletedArtifacts) {
        for (ArtifactFile artifactFile : artifact.getArtifactFiles()) {
          if (artifactFile.getFileUuid() != null) {
            artifactFileUuids.add(new ObjectId(artifactFile.getFileUuid()));
          }
        }
      }
      if (!artifactFileUuids.isEmpty()) {
        wingsPersistence.getCollection("artifacts")
            .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactUuids.toArray())));
        wingsPersistence.getCollection("artifacts.files")
            .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactFileUuids.toArray())));
        wingsPersistence.getCollection("artifacts.chunks")
            .remove(new BasicDBObject("files_id", new BasicDBObject("$in", artifactFileUuids.toArray())));
      }
    } catch (Exception ex) {
      logger.warn(String.format("Failed to purge(delete) artifacts for artifactStreamId %s of size: %s for appId %s",
                      artifactStreamId, toBeDeletedArtifacts.size(), appId),
          ex);
    }
    logger.info("Deleting artifacts for artifactStreamId {}  of size: {} for appId {} success", artifactStreamId,
        toBeDeletedArtifacts.size(), appId);
  }

  @Override
  public void deleteArtifactFiles() {
    try {
      List<String> fileUuids = wingsPersistence.getCollection("artifacts").distinct("artifactFiles.fileUuid");
      logger.info("Artifact with files size {} and uuids {} ", fileUuids.size(), fileUuids);
      List<ObjectId> artifactFileUuids = wingsPersistence.getCollection("artifacts.files").distinct("_id");
      logger.info("Artifact files size {} and  uuids {}", artifactFileUuids.size(), artifactFileUuids);
      List<ObjectId> fileObjectUuids =
          fileUuids.stream().map((String s) -> new ObjectId(s)).collect(Collectors.toList());
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
  public Artifact getArtifactByBuildNumber(String appId, String artifactStreamId, String buildNumber) {
    return wingsPersistence.createQuery(Artifact.class)
        .field("appId")
        .equal(appId)
        .field("artifactStreamId")
        .equal(artifactStreamId)
        .field("metadata.buildNo")
        .equal(buildNumber)
        .get();
  }
}
