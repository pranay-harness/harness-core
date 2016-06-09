package software.wings.service.impl;

import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.dl.MongoHelper.setUnset;

import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ArtifactSource;
import software.wings.beans.JenkinsArtifactSource;
import software.wings.beans.Release;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

// TODO: Auto-generated Javadoc

/**
 * The Class ReleaseServiceImpl.
 */
@Singleton
@ValidateOnExecution
public class ReleaseServiceImpl implements ReleaseService {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private SettingsService settingsService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Release> list(PageRequest<Release> req) {
    req.addFilter("active", true, Operator.EQ);
    PageResponse<Release> releases = wingsPersistence.query(Release.class, req);
    releases.forEach(release -> populateJenkinsSettingName(release.getArtifactSources()));
    return releases;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#get(java.lang.String, java.lang.String)
   */
  @Override
  public Release get(String id, String appId) {
    Release release = wingsPersistence.get(Release.class, appId, id);
    if (release != null) {
      populateJenkinsSettingName(release.getArtifactSources());
    }
    return release;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#create(software.wings.beans.Release)
   */
  @Override
  @ValidationGroups(Create.class)
  public Release create(Release release) {
    String id = wingsPersistence.save(release);
    return get(id, release.getAppId());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#update(software.wings.beans.Release)
   */
  @Override
  @ValidationGroups(Update.class)
  public Release update(Release release) {
    Query<Release> query = wingsPersistence.createQuery(Release.class)
                               .field(ID_KEY)
                               .equal(release.getUuid())
                               .field("appId")
                               .equal(release.getAppId());

    UpdateOperations<Release> updateOperations = wingsPersistence.createUpdateOperations(Release.class);

    setUnset(updateOperations, "releaseName", release.getReleaseName());
    setUnset(updateOperations, "description", release.getDescription());
    setUnset(updateOperations, "targetDate", release.getTargetDate());

    wingsPersistence.update(query, updateOperations);
    return get(release.getUuid(), release.getAppId());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#addArtifactSource(java.lang.String, java.lang.String,
   * software.wings.beans.ArtifactSource)
   */
  @Override
  public Release addArtifactSource(String id, String appId, ArtifactSource artifactSource) {
    Release release = wingsPersistence.get(Release.class, appId, id);
    if (release == null) {
      throw new NotFoundException("Release with id " + id + " not found");
    }
    if (isEmpty(release.getArtifactSources())) {
      wingsPersistence.getDatastore().findAndModify(wingsPersistence.createQuery(Release.class)
                                                        .field(ID_KEY)
                                                        .equal(id)
                                                        .field("appId")
                                                        .equal(appId)
                                                        .field("artifactSources")
                                                        .doesNotExist(),
          wingsPersistence.createUpdateOperations(Release.class).add("artifactSources", artifactSource));
    } else {
      if (release.getArtifactSources().get(0).getClass() != artifactSource.getClass()) {
        throw new BadRequestException("Release with id " + id + " doesn't allow buildSource of this type ");
      }
      release.getArtifactSources().add(artifactSource);

      wingsPersistence.update(
          wingsPersistence.createQuery(Release.class).field(ID_KEY).equal(id).field("appId").equal(appId),
          wingsPersistence.createUpdateOperations(Release.class).add("artifactSources", artifactSource));
    }
    return get(id, appId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#deleteArtifactSource(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public <T extends ArtifactSource> Release deleteArtifactSource(String id, String appId, String artifactSourceName) {
    Release release = wingsPersistence.get(Release.class, appId, id);
    if (release == null) {
      throw new NotFoundException("Release with id " + id + " not found");
    }

    wingsPersistence.update(
        wingsPersistence.createQuery(Release.class).field(ID_KEY).equal(id).field("appId").equal(appId),
        wingsPersistence.createUpdateOperations(Release.class)
            .removeAll("artifactSources", new BasicDBObject("sourceName", artifactSourceName)));

    return get(id, appId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#delete(java.lang.String, java.lang.String)
   */
  @Override
  public boolean delete(String id, String appId) {
    return wingsPersistence.getDatastore()
               .delete(wingsPersistence.createQuery(Release.class)
                           .field("active")
                           .equal(false)
                           .field(ID_KEY)
                           .equal(id)
                           .field("appId")
                           .equal(appId))
               .getN()
        > 0;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#softDelete(java.lang.String, java.lang.String)
   */
  @Override
  public Release softDelete(String id, String appId) {
    Query<Release> query =
        wingsPersistence.createQuery(Release.class).field(ID_KEY).equal(id).field("appId").equal(appId);

    UpdateOperations<Release> updateOperations =
        wingsPersistence.createUpdateOperations(Release.class).set("active", false);

    wingsPersistence.update(query, updateOperations);
    return get(id, appId);
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
