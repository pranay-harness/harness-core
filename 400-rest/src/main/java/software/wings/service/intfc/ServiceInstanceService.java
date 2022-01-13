/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;

import software.wings.beans.Activity;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.infrastructure.Host;
import software.wings.service.intfc.ownership.OwnedByHost;
import software.wings.service.intfc.ownership.OwnedByInfrastructureMapping;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

/**
 * Created by anubhaw on 5/26/16.
 */
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface ServiceInstanceService extends OwnedByHost, OwnedByInfrastructureMapping {
  /**
   * List page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<ServiceInstance> list(PageRequest<ServiceInstance> pageRequest);

  /**
   * Save.
   *
   * @param serviceInstance the service instance
   * @return the service instance
   */
  @ValidationGroups(Create.class) ServiceInstance save(@Valid ServiceInstance serviceInstance);

  /**
   * Delete.
   *
   * @param appId      the app id
   * @param envId      the env id
   * @param instanceId the instance id
   */
  void delete(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String instanceId);

  /**
   * Gets the.
   *
   * @param appId      the app id
   * @param envId      the env id
   * @param instanceId the instance id
   * @return the service instance
   */
  ServiceInstance get(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String instanceId);

  /**
   * Update host mappings.
   *
   * @param template              the template
   * @param infraMapping          the infra mapping
   * @param addedHosts            the added hosts
   */
  List<ServiceInstance> updateInstanceMappings(
      @NotNull ServiceTemplate template, InfrastructureMapping infraMapping, List<Host> addedHosts);

  /**
   * Delete by env.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void deleteByEnv(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Delete by service template.
   *
   * @param appId      the app id
   * @param envId      the env id
   * @param templateId the template id
   */
  void deleteByServiceTemplate(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String templateId);

  /**
   * Update activity.
   *
   * @param activity the activity
   */
  void updateActivity(@NotNull Activity activity);

  /**
   * Delete by host.
   *
   * @param appId  the app id
   * @param hostId the host id
   */
  @Override void pruneByHost(String appId, String hostId);

  List<ServiceInstance> fetchServiceInstances(String appId, Set<String> uuids);
}
