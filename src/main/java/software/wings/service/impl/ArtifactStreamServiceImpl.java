package software.wings.service.impl;

import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.INVALID_REQUEST;

import com.google.inject.Singleton;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.NotFoundException;

/**
 * The Class ArtifactStreamServiceImpl.
 */
@Singleton
@ValidateOnExecution
public class ArtifactStreamServiceImpl implements ArtifactStreamService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;

  @Override
  public PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req) {
    return wingsPersistence.query(ArtifactStream.class, req);
  }

  @Override
  public ArtifactStream get(String id, String appId) {
    return wingsPersistence.get(ArtifactStream.class, appId, id);
  }

  @Override
  @ValidationGroups(Create.class)
  public ArtifactStream create(ArtifactStream artifactStream) {
    String id = wingsPersistence.save(artifactStream);
    return get(id, artifactStream.getAppId());
  }

  @Override
  @ValidationGroups(Update.class)
  public ArtifactStream update(ArtifactStream artifactStream) {
    ArtifactStream savedArtifactStream =
        wingsPersistence.get(ArtifactStream.class, artifactStream.getAppId(), artifactStream.getUuid());
    if (savedArtifactStream == null) {
      throw new NotFoundException("Artifact stream with id " + artifactStream.getUuid() + " not found");
    }
    throw new WingsException(INVALID_REQUEST, "message", "feature not implemented");
  }

  @Override
  public boolean delete(String id, String appId) {
    return wingsPersistence.delete(
        wingsPersistence.createQuery(ArtifactStream.class).field(ID_KEY).equal(id).field("appId").equal(appId));
  }

  @Override
  public void deleteByApplication(String appId) {
    wingsPersistence.createQuery(ArtifactStream.class)
        .field("appId")
        .equal(appId)
        .asList()
        .forEach(artifactSource -> delete(artifactSource.getUuid(), appId));
  }

  private void populateJenkinsSettingName(List<ArtifactStream> artifactStreams) {
    if (!isEmpty(artifactStreams)) {
      for (ArtifactStream artifactStream : artifactStreams) {
        if (artifactStream instanceof JenkinsArtifactStream) {
          try {
            SettingAttribute attribute =
                settingsService.get(((JenkinsArtifactStream) artifactStream).getJenkinsSettingId());
            if (attribute != null) {
              ((JenkinsArtifactStream) artifactStream).setSourceName(attribute.getName());
            }
          } catch (Exception e) {
            // Ignore
          }
        }
      }
    }
  }
}
