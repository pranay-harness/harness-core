package software.wings.service.impl;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Application;
import software.wings.beans.Artifact;
import software.wings.beans.Artifact.Status;
import software.wings.beans.ArtifactFile;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Release;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.FileService;
import software.wings.utils.Validator;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class ArtifactServiceImpl implements ArtifactService {
  private static final String DEFAULT_ARTIFACT_FILE_NAME = "ArtifactFile";
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollector.class);

  @Inject private ExecutorService executorService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;

  @Override
  public PageResponse<Artifact> list(PageRequest<Artifact> pageRequest) {
    return wingsPersistence.query(Artifact.class, pageRequest);
  }

  @Override
  @ValidationGroups(Create.class)
  public Artifact create(@Valid Artifact artifact) {
    Application application = wingsPersistence.get(Application.class, artifact.getAppId());
    Validator.notNullCheck("application", application);
    Release release = wingsPersistence.get(Release.class, artifact.getRelease().getUuid());
    Validator.notNullCheck("release", release);
    Validator.notNullCheck("artifactSourceName", release.get(artifact.getArtifactSourceName()));

    Validator.equalCheck(application.getUuid(), release.getAppId());

    artifact.setRelease(release);
    artifact.setStatus(Status.RUNNING);
    String key = wingsPersistence.save(artifact);

    executorService.submit(
        new ArtifactCollector(wingsPersistence, release, artifact.getArtifactSourceName(), artifact));

    return wingsPersistence.get(Artifact.class, key);
  }

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

  @Override
  public File download(String appId, String artifactId, String serviceId) {
    Artifact artifact = wingsPersistence.get(Artifact.class, appId, artifactId);
    if (artifact == null || artifact.getStatus() != Status.READY || isEmpty(artifact.getArtifactFiles())
        || !artifact.getArtifactFiles()
                .stream()
                .filter(artifactFile
                    -> artifactFile.getServices()
                           .stream()
                           .map(Service::getUuid)
                           .filter(id -> id.equals(serviceId))
                           .findFirst()
                           .isPresent())
                .findFirst()
                .isPresent()) {
      return null;
    }

    ArtifactFile artifactFile = artifact.getArtifactFiles()
                                    .stream()
                                    .filter(artifactFile1
                                        -> artifactFile1.getServices()
                                               .stream()
                                               .map(Service::getUuid)
                                               .filter(id -> id.equals(serviceId))
                                               .findFirst()
                                               .isPresent())
                                    .findFirst()
                                    .get();

    File tempDir = Files.createTempDir();
    String fileName = Optional.ofNullable(artifactFile.getName()).orElse(DEFAULT_ARTIFACT_FILE_NAME);

    File file = new File(tempDir, fileName);

    fileService.download(artifactFile.getFileUuid(), file, ARTIFACTS);
    return file;
  }

  @Override
  public Artifact get(String appId, String artifactId) {
    return wingsPersistence.get(Artifact.class, appId, artifactId);
  }

  static class ArtifactCollector implements Runnable {
    private WingsPersistence wingsPersistence;
    private Release release;
    private Artifact artifact;
    private String artifactSourceName;

    public ArtifactCollector(
        WingsPersistence wingsPersistence, Release release, String artifactSourceName, Artifact artifact) {
      this.wingsPersistence = wingsPersistence;
      this.release = release;
      this.artifactSourceName = artifactSourceName;
      this.artifact = artifact;
    }

    @Override
    public void run() {
      try {
        ArtifactFile artifactFile = release.get(artifactSourceName).collect(null);
        UpdateOperations<Artifact> ops = wingsPersistence.createUpdateOperations(Artifact.class)
                                             .set("artifactFile", artifactFile)
                                             .set("status", Status.READY);
        wingsPersistence.update(artifact, ops);
        logger.info("Artifact collection completed - artifactId : " + artifact.getUuid());
      } catch (Exception ex) {
        logger.error(ex.getMessage(), ex);
        UpdateOperations<Artifact> ops =
            wingsPersistence.createUpdateOperations(Artifact.class).set("status", Status.FAILED);
        wingsPersistence.update(artifact, ops);
      }
    }
  }
}
