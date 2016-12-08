package software.wings.service.impl;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.collect.CollectEvent.Builder.aCollectEvent;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ErrorCodes;
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
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Validator;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private BuildSourceService buildSourceService;

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
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT);
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());
    Validator.notNullCheck("artifactStream", artifactStream);

    artifact.setServiceIds(artifactStream.getServiceIds().stream().collect(Collectors.toList()));
    artifact.setStatus(Status.QUEUED);
    String key = wingsPersistence.save(artifact);

    Artifact savedArtifact = wingsPersistence.get(Artifact.class, artifact.getAppId(), key);
    collectQueue.send(aCollectEvent().withArtifact(savedArtifact).build());

    return savedArtifact;
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
    if (artifact == null || artifact.getStatus() != Status.READY || isEmpty(artifact.getArtifactFiles())) {
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
    return wingsPersistence.delete(
        wingsPersistence.createQuery(Artifact.class).field("appId").equal(appId).field(ID_KEY).equal(artifactId));
  }

  @Override
  public void deleteByApplication(String appId) {
    wingsPersistence.createQuery(Artifact.class)
        .field("appId")
        .equal(appId)
        .asList()
        .forEach(artifact -> delete(appId, artifact.getUuid()));
  }

  @Override
  public Artifact collectNewArtifactsFromArtifactStream(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    Validator.notNullCheck("artifactStream", artifactStream);
    BuildDetails lastSuccessfulBuild =
        buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());

    if (lastSuccessfulBuild != null) {
      Artifact lastCollectedArtifact = fetchLatestArtifactForArtifactStream(appId, artifactStreamId);
      int buildNo = (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get("buildNo") != null)
          ? Integer.parseInt(lastCollectedArtifact.getMetadata().get("buildNo"))
          : 0;
      if (lastSuccessfulBuild.getNumber() > buildNo) {
        logger.info(
            "Existing build no {} is older than new build number {}. Collect new Artifact for ArtifactStream {}",
            buildNo, lastSuccessfulBuild.getNumber(), artifactStreamId);
        Artifact artifact =
            anArtifact()
                .withAppId(appId)
                .withArtifactStreamId(artifactStreamId)
                .withDisplayName(artifactStream.getArtifactDisplayName(lastSuccessfulBuild.getNumber()))
                .withMetadata(ImmutableMap.of("buildNo", Integer.toString(lastSuccessfulBuild.getNumber())))
                .withRevision(lastSuccessfulBuild.getRevision())
                .build();
        return create(artifact);
      }
    }
    return null;
  }

  @Override
  public Artifact fetchLatestArtifactForArtifactStream(String appId, String artifactStreamId) {
    return wingsPersistence.createQuery(Artifact.class)
        .field("appId")
        .equal(appId)
        .field("artifactStreamId")
        .equal(artifactStreamId)
        .order("-createdAt")
        .field("status")
        .hasAnyOf(Arrays.asList(Status.READY, Status.APPROVED))
        .get();
  }
}
