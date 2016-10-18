package software.wings.service.impl;

import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.INVALID_REQUEST;

import com.google.inject.Singleton;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactSource;
import software.wings.beans.artifact.JenkinsArtifactSource;
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
  public PageResponse<ArtifactSource> list(PageRequest<ArtifactSource> req) {
    return wingsPersistence.query(ArtifactSource.class, req);
  }

  @Override
  public ArtifactSource get(String id, String appId) {
    return wingsPersistence.get(ArtifactSource.class, appId, id);
  }

  @Override
  @ValidationGroups(Create.class)
  public ArtifactSource create(ArtifactSource artifactSource) {
    String id = wingsPersistence.save(artifactSource);
    return get(id, artifactSource.getAppId());
  }

  @Override
  @ValidationGroups(Update.class)
  public ArtifactSource update(ArtifactSource artifactSource) {
    ArtifactSource savedArtifactSource =
        wingsPersistence.get(ArtifactSource.class, artifactSource.getAppId(), artifactSource.getUuid());
    if (savedArtifactSource == null) {
      throw new NotFoundException("Artifact stream with id " + artifactSource.getUuid() + " not found");
    }
    throw new WingsException(INVALID_REQUEST, "message", "feature not implemented");
  }

  @Override
  public boolean delete(String id, String appId) {
    return wingsPersistence.delete(
        wingsPersistence.createQuery(ArtifactSource.class).field(ID_KEY).equal(id).field("appId").equal(appId));
  }

  @Override
  public void deleteByApplication(String appId) {
    wingsPersistence.createQuery(ArtifactSource.class)
        .field("appId")
        .equal(appId)
        .asList()
        .forEach(artifactSource -> delete(artifactSource.getUuid(), appId));
  }

  private void populateJenkinsSettingName(List<ArtifactSource> artifactSources) {
    if (!isEmpty(artifactSources)) {
      for (ArtifactSource artifactSource : artifactSources) {
        if (artifactSource instanceof JenkinsArtifactSource) {
          try {
            SettingAttribute attribute =
                settingsService.get(((JenkinsArtifactSource) artifactSource).getJenkinsSettingId());
            if (attribute != null) {
              ((JenkinsArtifactSource) artifactSource).setName(attribute.getName());
            }
          } catch (Exception e) {
            // Ignore
          }
        }
      }
    }
  }
}
