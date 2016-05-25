package software.wings.service.impl;

import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ArtifactSource;
import software.wings.beans.Release;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ReleaseService;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

@Singleton
@ValidateOnExecution
public class ReleaseServiceImpl implements ReleaseService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Release> list(PageRequest<Release> req) {
    return wingsPersistence.query(Release.class, req);
  }

  @Override
  public Release get(String id, String appId) {
    return wingsPersistence.get(Release.class, appId, id);
  }

  @Override
  @ValidationGroups(Create.class)
  public Release create(@Valid Release release) {
    return wingsPersistence.saveAndGet(Release.class, release);
  }

  @Override
  @ValidationGroups(Update.class)
  public Release update(@Valid Release release) {
    Query<Release> query = wingsPersistence.createQuery(Release.class)
                               .field(ID_KEY)
                               .equal(release.getUuid())
                               .field("appId")
                               .equal(release.getAppId());

    UpdateOperations<Release> updateOperations = wingsPersistence.createUpdateOperations(Release.class)
                                                     .set("releaseName", release.getReleaseName())
                                                     .set("description", release.getDescription())
                                                     .set("targetDate", release.getTargetDate());

    wingsPersistence.update(query, updateOperations);
    return wingsPersistence.get(Release.class, release.getAppId(), release.getUuid());
  }

  @Override
  public Release addArtifactSource(
      @NotEmpty String uuid, @NotEmpty String appId, @Valid ArtifactSource artifactSource) {
    Release release = wingsPersistence.get(Release.class, appId, uuid);
    if (release == null) {
      throw new NotFoundException("Release with id " + uuid + " not found");
    }
    if (isEmpty(release.getArtifactSources())) {
      wingsPersistence.getDatastore().findAndModify(wingsPersistence.createQuery(Release.class)
                                                        .field(ID_KEY)
                                                        .equal(uuid)
                                                        .field("appId")
                                                        .equal(appId)
                                                        .field("artifactSources")
                                                        .doesNotExist(),
          wingsPersistence.createUpdateOperations(Release.class).add("artifactSources", artifactSource));
    } else {
      if (release.getArtifactSources().get(0).getClass() != artifactSource.getClass()) {
        throw new BadRequestException("Release with id " + uuid + " doesn't allow buildSource of this type ");
      }
      release.getArtifactSources().add(artifactSource);

      wingsPersistence.update(
          wingsPersistence.createQuery(Release.class).field(ID_KEY).equal(uuid).field("appId").equal(appId),
          wingsPersistence.createUpdateOperations(Release.class).add("artifactSources", artifactSource));
    }
    return wingsPersistence.get(Release.class, uuid);
  }

  @Override
  public <T extends ArtifactSource> Release deleteArtifactSource(
      @NotEmpty String uuid, @NotEmpty String appId, @NotEmpty String artifactSourceName) {
    Release release = wingsPersistence.get(Release.class, appId, uuid);
    if (release == null) {
      throw new NotFoundException("Release with id " + uuid + " not found");
    }

    wingsPersistence.update(
        wingsPersistence.createQuery(Release.class).field(ID_KEY).equal(uuid).field("appId").equal(appId),
        wingsPersistence.createUpdateOperations(Release.class)
            .removeAll("artifactSources", new BasicDBObject("sourceName", artifactSourceName)));

    return wingsPersistence.get(Release.class, uuid);
  }

  @Override
  public void delete(String releaseId) {
    wingsPersistence.getDatastore().delete(
        wingsPersistence.createQuery(Release.class).field("active").equal(false).field(ID_KEY).equal(releaseId));
  }
}
