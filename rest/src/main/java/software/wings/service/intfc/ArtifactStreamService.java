package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByService;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

/**
 * ArtifactStreamService.
 *
 * @author Rishi
 */
public interface ArtifactStreamService extends OwnedByService {
  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req);

  /**
   * Get artifact stream.
   *
   * @param appId            the app id
   * @param artifactStreamId the id
   * @return the artifact stream
   */
  ArtifactStream get(String appId, String artifactStreamId);

  ArtifactStream getArtifactStreamByName(String appId, String serviceId, String artifactStreamName);

  /**
   * Create artifact stream.
   *
   * @param artifactStream the artifact stream
   * @return the artifact stream
   */
  @ValidationGroups(Create.class) ArtifactStream create(@Valid ArtifactStream artifactStream);

  /**
   * Creates artifact without checking the validity artifact stream.
   *
   * @param artifactStream the artifact stream
   * @return the artifact stream
   */
  @ValidationGroups(Create.class) ArtifactStream forceCreate(@Valid ArtifactStream artifactStream);

  /**
   * Update artifact stream.
   *
   * @param artifactStream the artifact stream
   * @return the artifact stream
   */
  @ValidationGroups(Update.class) ArtifactStream update(@Valid ArtifactStream artifactStream);

  /**
   * Delete.
   *
   * @param appId            the app id
   * @param artifactStreamId the id
   * @return true, if successful
   */
  boolean delete(@NotEmpty String appId, @NotEmpty String artifactStreamId);

  /**
   * Prune owned from the app entities.
   *
   * @param appId the app id
   * @param artifactStreamId the id
   */
  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String artifactStreamId);

  /**
   * Gets build source.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the build source
   */
  Map<String, String> getSupportedBuildSourceTypes(String appId, String serviceId);

  List<ArtifactStream> getArtifactStreamsForService(String appId, String serviceId);

  Map<String, String> fetchArtifactSourceProperties(String accountId, String appId, String artifactStreamId);

  List<ArtifactStream> fetchArtifactStreamsForService(String appId, String serviceId);
}
