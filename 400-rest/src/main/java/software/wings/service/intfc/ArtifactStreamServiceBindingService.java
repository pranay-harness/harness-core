/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamBinding;

import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@TargetModule(_870_CG_ORCHESTRATION)
public interface ArtifactStreamServiceBindingService {
  ArtifactStreamBinding create(
      @NotEmpty String appId, @NotEmpty String serviceId, ArtifactStreamBinding artifactStreamBinding);

  ArtifactStreamBinding update(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String name,
      ArtifactStreamBinding artifactStreamBinding);

  void delete(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String name);

  List<ArtifactStreamBinding> list(@NotEmpty String appId, @NotEmpty String serviceId);

  ArtifactStreamBinding get(@NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String name);

  List<ServiceVariable> fetchArtifactServiceVariables(String appId, String serviceId);

  List<ServiceVariable> fetchArtifactServiceVariableByName(String appId, String serviceId, String name);

  List<ServiceVariable> fetchArtifactServiceVariableByArtifactStreamId(String accountId, String artifactStreamId);

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

  List<String> listArtifactStreamIds(Service service);

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

  List<ArtifactStream> listArtifactStreams(Service service);

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

  List<Workflow> listWorkflows(@NotEmpty String artifactStreamId);

  Service getService(@NotEmpty String appId, @NotEmpty String artifactStreamId, boolean throwException);

  String getServiceId(@NotEmpty String appId, @NotEmpty String artifactStreamId, boolean throwException);

  void processServiceVariables(List<ServiceVariable> serviceVariables);

  void processVariables(List<Variable> variables);

  void deleteByArtifactStream(String artifactStreamId, boolean syncFromGit);
}
