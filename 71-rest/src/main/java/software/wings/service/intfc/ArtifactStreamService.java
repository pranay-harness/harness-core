package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamSummary;
import software.wings.service.intfc.ownership.OwnedByService;
import software.wings.utils.ArtifactType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;

/**
 * ArtifactStreamService.
 */
public interface ArtifactStreamService extends OwnedByService {
  PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req);

  PageResponse<ArtifactStream> list(
      PageRequest<ArtifactStream> req, String accountId, boolean withArtifactCount, String artifactSearchString);

  PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req, String accountId, boolean withArtifactCount,
      String artifactSearchString, ArtifactType artifactType, int maxArtifacts);

  ArtifactStream get(String artifactStreamId);

  ArtifactStream getArtifactStreamByName(String appId, String serviceId, String artifactStreamName);

  ArtifactStream getArtifactStreamByName(String settingId, String artifactStreamName);

  @ValidationGroups(Create.class) ArtifactStream create(@Valid ArtifactStream artifactStream);

  @ValidationGroups(Create.class) ArtifactStream create(@Valid ArtifactStream artifactStream, boolean validate);

  @ValidationGroups(Update.class) ArtifactStream update(@Valid ArtifactStream artifactStream);

  @ValidationGroups(Update.class) ArtifactStream update(@Valid ArtifactStream artifactStream, boolean validate);

  @ValidationGroups(Update.class)
  ArtifactStream update(@Valid ArtifactStream artifactStream, boolean validate, boolean fromTemplate);

  boolean delete(@NotEmpty String appId, @NotEmpty String artifactStreamId);

  boolean delete(@NotEmpty String artifactStreamId, boolean syncFromGit);

  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String artifactStreamId);

  Map<String, String> getSupportedBuildSourceTypes(String appId, String serviceId);

  boolean artifactStreamsExistForService(String appId, String serviceId);

  List<ArtifactStream> getArtifactStreamsForService(String appId, String serviceId);

  Map<String, String> fetchArtifactSourceProperties(String accountId, String artifactStreamId);

  List<ArtifactStream> fetchArtifactStreamsForService(String appId, String serviceId);

  List<String> fetchArtifactStreamIdsForService(String appId, String serviceId);

  boolean updateFailedCronAttempts(String accountId, String artifactStreamId, int counter);

  boolean updateCollectionStatus(String accountId, String artifactStreamId, String collectionStatus);

  List<ArtifactStream> listBySettingId(String settingId);

  List<ArtifactStream> listByIds(Collection<String> artifactStreamIds);

  List<ArtifactStreamSummary> listArtifactStreamSummary(String appId);

  ArtifactStream createWithBinding(String appId, ArtifactStream artifactStream, boolean validate);

  boolean deleteWithBinding(String appId, String artifactStreamId, boolean forceDelete, boolean syncFromGit);

  List<ArtifactStream> listBySettingId(String appId, String settingId);

  List<ArtifactStream> listByAppId(String appId);

  boolean pruneArtifactStream(String appId, String artifactStreamId);
}
