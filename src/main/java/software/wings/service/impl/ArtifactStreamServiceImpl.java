package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Singleton;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Validator;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.stream.Collectors;
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
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req) {
    PageResponse<ArtifactStream> pageResponse = wingsPersistence.query(ArtifactStream.class, req);
    pageResponse.getResponse().forEach(this ::populateStreamSpecificData);
    return pageResponse;
  }

  private void populateStreamSpecificData(ArtifactStream artifactStream) {
    if (artifactStream instanceof JenkinsArtifactStream) {
      ((JenkinsArtifactStream) artifactStream)
          .getArtifactPathServices()
          .forEach(artifactPathServiceEntry
              -> artifactPathServiceEntry.setServices(
                  artifactPathServiceEntry.getServiceIds()
                      .stream()
                      .map(sid -> serviceResourceService.get(artifactStream.getAppId(), sid))
                      .collect(Collectors.toList())));
    }
  }

  @Override
  public ArtifactStream get(String id, String appId) {
    ArtifactStream artifactStream = wingsPersistence.get(ArtifactStream.class, appId, id);
    populateStreamSpecificData(artifactStream);
    return artifactStream;
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
    artifactStream.setUuid(savedArtifactStream.getUuid());
    return create(artifactStream);
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

  @Override
  public ArtifactStream addStreamAction(String appId, String streamId, ArtifactStreamAction artifactStreamAction) {
    Query<ArtifactStream> query = wingsPersistence.createQuery(ArtifactStream.class)
                                      .field("appId")
                                      .equal(appId)
                                      .field(ID_KEY)
                                      .equal(streamId)
                                      .field("streamActions.workflowId")
                                      .notEqual(artifactStreamAction.getWorkflowId());
    UpdateOperations<ArtifactStream> operations =
        wingsPersistence.createUpdateOperations(ArtifactStream.class).add("streamActions", artifactStreamAction);
    UpdateResults update = wingsPersistence.update(query, operations);
    return get(streamId, appId);
  }

  @Override
  public ArtifactStream deleteStreamAction(String appId, String streamId, String workflowId) {
    Query<ArtifactStream> query = wingsPersistence.createQuery(ArtifactStream.class)
                                      .field("appId")
                                      .equal(appId)
                                      .field(ID_KEY)
                                      .equal(streamId)
                                      .field("streamActions.workflowId")
                                      .equal(workflowId);

    ArtifactStream artifactStream = query.get();
    Validator.notNullCheck("ArtifactStream", artifactStream);

    ArtifactStreamAction streamAction =
        artifactStream.getStreamActions()
            .stream()
            .filter(artifactStreamAction -> artifactStreamAction.getWorkflowId().equals(workflowId))
            .findFirst()
            .orElseGet(null);
    Validator.notNullCheck("StreamAction", streamAction);

    UpdateOperations<ArtifactStream> operations =
        wingsPersistence.createUpdateOperations(ArtifactStream.class).removeAll("streamActions", streamAction);

    wingsPersistence.update(query, operations);
    return get(streamId, appId);
  }

  @Override
  public ArtifactStream updateStreamAction(String appId, String streamId, ArtifactStreamAction artifactStreamAction) {
    deleteStreamAction(appId, streamId, artifactStreamAction.getWorkflowId());
    return addStreamAction(appId, streamId, artifactStreamAction);
  }
}
