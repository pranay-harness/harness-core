package software.wings.service.impl;

import java.io.File;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.app.WingsBootstrap;
import software.wings.beans.Application;
import software.wings.beans.Artifact;
import software.wings.beans.Artifact.Status;
import software.wings.beans.ArtifactFile;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Release;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.FileService;
import software.wings.utils.FileUtils;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;
import software.wings.utils.Validator;

import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

@Singleton
@ValidateOnExecution
public class ArtifactServiceImpl implements ArtifactService {
  private static final String DEFAULT_ARTIFACT_FILE_NAME = "ArtifactFile";

  @Inject private ExecutorService executorService;

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Artifact> list(PageRequest<Artifact> pageRequest) {
    return wingsPersistence.query(Artifact.class, pageRequest);
  }

  @Override
  @ValidationGroups(Create.class)
  public Artifact create(@Valid Artifact artifact) {
    Application application = wingsPersistence.get(Application.class, artifact.getApplication().getUuid());
    Validator.notNullCheck("application", application);
    Release release = wingsPersistence.get(Release.class, artifact.getRelease().getUuid());
    Validator.notNullCheck("release", release);

    Validator.equalCheck(application.getUuid(), release.getApplication().getUuid());

    artifact.setApplication(application);
    artifact.setRelease(release);
    artifact.setStatus(Status.RUNNING);
    String key = wingsPersistence.save(artifact);

    executorService.submit(
        new ArtifactCollector(wingsPersistence, release, artifact.getArtifactSourceName(), artifact));

    return wingsPersistence.get(Artifact.class, key);
  }

  @Override
  @ValidationGroups(value = {Update.class})
  public Artifact update(@Valid Artifact artifact) {
    Artifact dbArtifact = wingsPersistence.get(Artifact.class, artifact.getUuid());
    dbArtifact.setDisplayName(artifact.getDisplayName());
    String key = wingsPersistence.save(dbArtifact);

    return wingsPersistence.get(Artifact.class, key);
  }

  @Override
  public File download(String applicationId, String artifactId) {
    Artifact artifact = wingsPersistence.get(Artifact.class, artifactId);
    if (artifact == null || artifact.getStatus() != Status.READY || artifact.getArtifactFile() == null
        || artifact.getArtifactFile().getFileUUID() == null) {
      return null;
    }

    File tempDir = FileUtils.createTempDirPath();
    String fileName = artifact.getArtifactFile().getFileName();
    if (fileName == null) {
      fileName = DEFAULT_ARTIFACT_FILE_NAME;
    }

    File artifactFile = new File(tempDir, fileName);

    FileService fileService = WingsBootstrap.lookup(FileService.class);
    fileService.download(artifact.getArtifactFile().getFileUUID(), artifactFile, ARTIFACTS);
    return artifactFile;
  }

  @Override
  public Artifact get(String applicationId, String artifactId) {
    return wingsPersistence.get(Artifact.class, artifactId);
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
        ArtifactFile artifactFile = release.getArtifactSources().get(artifactSourceName).collect(null);
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

    private static final Logger logger = LoggerFactory.getLogger(ArtifactCollector.class);
  }
}
