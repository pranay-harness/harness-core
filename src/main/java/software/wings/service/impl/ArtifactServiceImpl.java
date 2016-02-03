package software.wings.service.impl;

import java.io.File;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.app.WingsBootstrap;
import software.wings.beans.Application;
import software.wings.beans.Artifact;
import software.wings.beans.Artifact.Status;
import software.wings.beans.ArtifactFile;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Release;
import software.wings.common.thread.ThreadPool;
import software.wings.dl.MongoHelper;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.FileService;
import software.wings.utils.FileUtils;
import software.wings.utils.Validator;

public class ArtifactServiceImpl implements ArtifactService {
  private static final String DEFAULT_ARTIFACT_FILE_NAME = "ArtifatcFile";
  private Datastore datastore;

  public ArtifactServiceImpl(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public PageResponse<Artifact> list(PageRequest<Artifact> pageRequest) {
    return MongoHelper.queryPageRequest(datastore, Artifact.class, pageRequest);
  }

  @Override
  public Artifact create(String applicationId, String releaseId, String artifactSourceName) {
    Validator.notNullCheck("applicationId", applicationId);
    Validator.notNullCheck("releaseId", releaseId);
    Application application = datastore.get(Application.class, applicationId);
    Release release = datastore.get(Release.class, releaseId);
    Validator.equalCheck(applicationId, release.getApplication().getUuid());

    Artifact artifact = new Artifact();
    artifact.setApplication(application);
    artifact.setRelease(release);
    artifact.setArtifactSourceName(artifactSourceName);
    artifact.setStatus(Status.RUNNING);
    Key<Artifact> key = datastore.save(artifact);

    ThreadPool.execute(new ArtifactCollector(datastore, release, artifactSourceName, artifact));

    return datastore.get(Artifact.class, key.getId());
  }

  @Override
  public File download(String applicationId, String artifactId) {
    Artifact artifact = datastore.get(Artifact.class, artifactId);
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
    fileService.download(artifact.getArtifactFile().getFileUUID(), artifactFile);
    return artifactFile;
  }

  @Override
  public Artifact get(String applicationId, String artifactId) {
    return datastore.get(Artifact.class, artifactId);
  }
}

class ArtifactCollector implements Runnable {
  private Datastore datastore;
  private Release release;
  private Artifact artifact;
  private String artifactSourceName;

  public ArtifactCollector(Datastore datastore, Release release, String artifactSourceName, Artifact artifact) {
    this.datastore = datastore;
    this.release = release;
    this.artifactSourceName = artifactSourceName;
    this.artifact = artifact;
  }

  @Override
  public void run() {
    try {
      ArtifactFile artifactFile = release.getArtifactSources().get(artifactSourceName).collect(null);
      UpdateOperations<Artifact> ops = datastore.createUpdateOperations(Artifact.class)
                                           .set("artifactFile", artifactFile)
                                           .set("status", Artifact.Status.READY);
      datastore.update(artifact, ops);
      logger.info("Artifact collection completed - artifactId : " + artifact.getUuid());
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      UpdateOperations<Artifact> ops =
          datastore.createUpdateOperations(Artifact.class).set("status", Artifact.Status.FAILED);
      datastore.update(artifact, ops);
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollector.class);
}
