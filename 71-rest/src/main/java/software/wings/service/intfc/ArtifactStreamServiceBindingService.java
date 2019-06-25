package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamBinding;
import software.wings.service.intfc.ownership.OwnedByArtifactStream;

import java.util.List;

public interface ArtifactStreamServiceBindingService extends OwnedByArtifactStream {
  ArtifactStreamBinding create(
      @NotEmpty String appId, @NotEmpty String serviceId, ArtifactStreamBinding artifactStreamBinding);

  ArtifactStreamBinding update(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String name,
      ArtifactStreamBinding artifactStreamBinding);

  void delete(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String name);

  List<ArtifactStreamBinding> list(@NotEmpty String appId, @NotEmpty String serviceId);

  ArtifactStreamBinding get(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String name);

  List<ServiceVariable> getServiceVariables(String appId, String serviceId);

  List<ServiceVariable> getServiceVariablesByName(String appId, String serviceId, String name);

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
   * @param appId            the app id
   * @param serviceId        the service id
   * @param artifactStreamId the artifact stream id
   * @return true, if successful
   */
  boolean deleteOld(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String artifactStreamId);

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
