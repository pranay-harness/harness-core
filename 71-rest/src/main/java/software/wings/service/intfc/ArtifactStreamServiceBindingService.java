package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamBinding;
import software.wings.beans.artifact.ArtifactStreamBindingDetails;
import software.wings.service.intfc.ownership.OwnedByArtifactStream;

import java.util.List;

public interface ArtifactStreamServiceBindingService extends OwnedByArtifactStream {
  /**
   * Create artifact stream service binding.
   *
   * @param appId                 the app id
   * @param serviceId             the service id
   * @param artifactStreamBinding the artifact stream binding
   * @return artifact stream binding
   */
  ArtifactStreamBinding create(
      @NotEmpty String appId, @NotEmpty String serviceId, ArtifactStreamBinding artifactStreamBinding);

  /**
   * Create artifact stream service binding.
   *
   * @param appId            the app id
   * @param serviceId        the service id
   * @param artifactStreamId the artifact stream id
   * @return artifact stream
   */
  ArtifactStream createOld(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String artifactStreamId);

  /**
   * Delete artifact stream service binding.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param name      the artifact stream binding name
   * @return true, if successful
   */
  boolean delete(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String name);

  /**
   * Delete artifact stream service binding.
   *
   * @param appId            the app id
   * @param serviceId        the service id
   * @param artifactStreamId the artifact stream id
   * @return true, if successful
   */
  boolean deleteOld(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String artifactStreamId);

  /**
   * Update artifact stream service binding.
   *
   * @param appId                 the app id
   * @param serviceId             the service id
   * @param name                  existing artifact variable name
   * @param artifactStreamBinding the artifact stream binding
   * @return artifact stream binding
   */
  ArtifactStreamBinding update(
      @NotEmpty String appId, @NotEmpty String serviceId, String name, ArtifactStreamBinding artifactStreamBinding);

  List<ArtifactStreamBindingDetails> list(@NotEmpty String appId, @NotEmpty String serviceId);

  ArtifactStreamBindingDetails get(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String name);

  /**
   * List artifact stream ids with bindings to service.
   *
   * @param appId      the app id
   * @param serviceId  the service id
   * @return list of artifact stream ids
   */
  List<String> listArtifactStreamIds(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * List artifact stream ids with bindings to service.
   *
   * @param serviceId  the service id
   * @return list of artifact stream ids
   */
  List<String> listArtifactStreamIds(@NotEmpty String serviceId);

  /**
   * List artifact streams with bindings to service.
   *
   * @param appId      the app id
   * @param serviceId  the service id
   * @return list of artifact streams
   */
  List<ArtifactStream> listArtifactStreams(@NotEmpty String appId, @NotEmpty String serviceId);

  /**
   * List artifact streams with bindings to service.
   *
   * @param serviceId  the service id
   * @return list of artifact streams
   */
  List<ArtifactStream> listArtifactStreams(@NotEmpty String serviceId);

  /**
   * List service ids with bindings to artifact stream.
   *
   * @param appId             the app id
   * @param artifactStreamId  the artifact stream id
   * @return list of service ids
   */
  List<String> listServiceIds(@NotEmpty String appId, @NotEmpty String artifactStreamId);

  /**
   * List service ids with bindings to artifact stream.
   *
   * @param artifactStreamId  the artifact stream id
   * @return list of service ids
   */
  List<String> listServiceIds(@NotEmpty String artifactStreamId);

  /**
   * List services with bindings to artifact stream.
   *
   * @param appId             the app id
   * @param artifactStreamId  the artifact stream id
   * @return list of services
   */
  List<Service> listServices(@NotEmpty String appId, @NotEmpty String artifactStreamId);

  /**
   * List services with bindings to artifact stream.
   *
   * @param artifactStreamId  the artifact stream id
   * @return list of services
   */
  List<Service> listServices(@NotEmpty String artifactStreamId);

  Service getService(@NotEmpty String appId, @NotEmpty String artifactStreamId, boolean throwException);

  Service getService(@NotEmpty String artifactStreamId, boolean throwException);
}
