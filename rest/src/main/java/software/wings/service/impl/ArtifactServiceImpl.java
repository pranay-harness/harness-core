package software.wings.service.impl;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.EXISTS;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.artifact.Artifact.Status.ABORTED;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.ERROR;
import static software.wings.beans.artifact.Artifact.Status.FAILED;
import static software.wings.beans.artifact.Artifact.Status.NEW;
import static software.wings.beans.artifact.Artifact.Status.QUEUED;
import static software.wings.beans.artifact.Artifact.Status.READY;
import static software.wings.beans.artifact.Artifact.Status.REJECTED;
import static software.wings.beans.artifact.Artifact.Status.RUNNING;
import static software.wings.beans.artifact.Artifact.Status.WAITING;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.collect.CollectEvent.Builder.aCollectEvent;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

import com.google.common.io.Files;

import com.mongodb.BasicDBObject;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.collect.CollectEvent;
import software.wings.common.Constants;
import software.wings.core.queue.Queue;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Validator;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class ArtifactServiceImpl.
 */
@Singleton
@ValidateOnExecution
public class ArtifactServiceImpl implements ArtifactService {
  private static final String DEFAULT_ARTIFACT_FILE_NAME = "ArtifactFile";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;
  @Inject private Queue<CollectEvent> collectQueue;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private AmazonS3Service amazonS3Service;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ExecutorService executorService;

  private final DateFormat dateFormat = new SimpleDateFormat("HHMMSS");
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Artifact> list(PageRequest<Artifact> pageRequest, boolean withServices) {
    PageResponse<Artifact> pageResponse = wingsPersistence.query(Artifact.class, pageRequest);
    if (withServices) {
      pageResponse.getResponse().forEach(artifact
          -> artifact.setServices(artifact.getServiceIds()
                                      .stream()
                                      .map(serviceId -> serviceResourceService.get(artifact.getAppId(), serviceId))
                                      .collect(Collectors.toList())));
    }
    return pageResponse;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactService#create(software.wings.beans.artifact.Artifact)
   */
  @Override
  @ValidationGroups(Create.class)
  public Artifact create(@Valid Artifact artifact) {
    if (!appService.exist(artifact.getAppId())) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT);
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
    Validator.notNullCheck("Artifact Stream", artifactStream);

    artifact.setArtifactSourceName(artifactStream.getSourceName());
    artifact.setServiceIds(Arrays.asList(artifactStream.getServiceId()));
    Status status = getArtifactStatus(artifactStream);
    artifact.setStatus(status);
    if (ArtifactStreamType.AMAZON_S3.getName().equals(artifactStream.getArtifactStreamType())) {
      SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, artifactStream.getSettingId());
      AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
      BuildDetails artifactMetadata = amazonS3Service.getArtifactMetadata(
          awsConfig, artifactStream.getArtifactStreamAttributes(), artifactStream.getAppId());
      Map<String, String> metadataMap = new HashMap<>(artifact.getMetadata());
      metadataMap.put(Constants.BUILD_NO, artifactMetadata.getNumber());
      metadataMap.put(Constants.URL, artifactMetadata.getBuildParameters().get(Constants.URL));
      artifact.setMetadata(metadataMap);
    }

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
      return READY;
    }
    return (DOCKER.name().equals(artifactStream.getArtifactStreamType())
               || ECR.name().equals(artifactStream.getArtifactStreamType())
               || GCR.name().equals(artifactStream.getArtifactStreamType())
               || ARTIFACTORY.name().equals(artifactStream.getArtifactStreamType()))
        ? (artifactStream.isAutoApproveForProduction() ? APPROVED : READY)
        : QUEUED;
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
  public void updateStatus(String artifactId, String appId, Artifact.Status status) {
    Query<Artifact> query =
        wingsPersistence.createQuery(Artifact.class).field(ID_KEY).equal(artifactId).field("appId").equal(appId);
    UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class).set("status", status);
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
    Validator.notNullCheck("Artifact", artifact);
    boolean deleted = wingsPersistence.delete(artifact);
    if (deleted) {
      artifact.getArtifactFiles().forEach(
          artifactFile -> fileService.deleteFile(artifactFile.getFileUuid(), ARTIFACTS));
    }
    return deleted;
  }

  @Override
  public void deleteByApplication(String appId) {
    wingsPersistence.createQuery(Artifact.class)
        .field("appId")
        .equal(appId)
        .asList()
        .forEach(artifact -> delete(appId, artifact.getUuid()));
  }

  public Artifact fetchLatestArtifactForArtifactStream(String appId, String artifactStreamId) {
    return wingsPersistence.createQuery(Artifact.class)
        .field("appId")
        .equal(appId)
        .field("artifactStreamId")
        .equal(artifactStreamId)
        .order("-createdAt")
        .field("status")
        .hasAnyOf(Arrays.asList(Status.RUNNING, Status.REJECTED, Status.WAITING, READY, APPROVED))
        .get();
  }

  @Override
  public void deleteByArtifactStream(String appId, String artifactStreamId) {
    wingsPersistence.createQuery(Artifact.class)
        .field("appId")
        .equal(appId)
        .field("artifactStreamId")
        .equal(artifactStreamId)
        .asList()
        .forEach(artifact -> delete(appId, artifact.getUuid()));
  }

  @Override
  public void deleteArtifacts(long retentionSize) {
    List<Application> apps =
        appService.list(aPageRequest().withLimit(UNLIMITED).addFieldsIncluded("uuid").build()).getResponse();
    for (Application app : apps) {
      String appId = app.getUuid();
      List<Artifact> artifacts = list(aPageRequest()
                                          .addFilter("appId", EQ, appId)
                                          .addFieldsIncluded("appId")
                                          .addFieldsIncluded("uuid")
                                          .addFieldsIncluded("artifactStreamId")
                                          .withLimit(UNLIMITED)
                                          .build(),
          false)
                                     .getResponse();
      Set<String> artifactStreamIds = artifacts.stream().map(Artifact::getArtifactStreamId).collect(Collectors.toSet());
      for (String artifactStreamId : artifactStreamIds) {
        List<Artifact> toBeDeletedArtifacts =
            wingsPersistence
                .query(Artifact.class,
                    aPageRequest()
                        .withLimit(UNLIMITED)
                        .withOffset(String.valueOf(retentionSize))
                        .addFilter("appId", EQ, appId)
                        .addFilter("artifactStreamId", EQ, artifactStreamId)
                        .addFilter("status", IN, NEW, RUNNING, QUEUED, WAITING, READY, APPROVED)
                        .addFilter("artifactFiles", EXISTS)
                        .build())
                .getResponse();
        if (!CollectionUtils.isEmpty(toBeDeletedArtifacts)) {
          logger.info("Deleting artifacts for artifactStreamId {}  of size: {} for appId {}", artifactStreamId,
              toBeDeletedArtifacts.size(), appId);
          deleteArtifacts(appId, artifactStreamId, toBeDeletedArtifacts);
        } else {
          logger.info(
              "ArtifactStreamId {} for the app {} does not have more than {} successful artifacts. Not deleting",
              artifactStreamId, appId, retentionSize);
        }
        toBeDeletedArtifacts = wingsPersistence
                                   .query(Artifact.class,
                                       aPageRequest()
                                           .withLimit(UNLIMITED)
                                           .addFilter("appId", EQ, appId)
                                           .addFilter("artifactStreamId", EQ, artifactStreamId)
                                           .addFilter("status", IN, FAILED, REJECTED, ERROR, ABORTED)
                                           .build())
                                   .getResponse();
        if (!CollectionUtils.isEmpty(toBeDeletedArtifacts)) {
          logger.info("Deleting failed artifacts for artifactStreamId {}  of size: {} for appId {}", artifactStreamId,
              toBeDeletedArtifacts.size(), appId);
          deleteArtifacts(appId, artifactStreamId, toBeDeletedArtifacts);
        } else {
          logger.info(
              "ArtifactStreamId {} for the app {} does not have failed artifacts to delete", artifactStreamId, appId);
        }
      }
    }
  }
  private void deleteArtifacts(String appId, String artifactStreamId, List<Artifact> toBeDeletedArtifacts) {
    try {
      List<String> artifactUuids = toBeDeletedArtifacts.stream().map(Artifact::getUuid).collect(Collectors.toList());
      List<ObjectId> artifactFileUuids =
          toBeDeletedArtifacts.stream()
              .flatMap(artifact -> artifact.getArtifactFiles().stream())
              .map((ArtifactFile artifactFile) -> new ObjectId(artifactFile.getFileUuid()))
              .collect(Collectors.toList());
      wingsPersistence.getCollection("artifacts")
          .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactUuids.toArray())));
      wingsPersistence.getCollection("artifacts.files")
          .remove(new BasicDBObject("_id", new BasicDBObject("$in", artifactFileUuids.toArray())));
      wingsPersistence.getCollection("artifacts.chunks")
          .remove(new BasicDBObject("files_id", new BasicDBObject("$in", artifactFileUuids.toArray())));
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
}
