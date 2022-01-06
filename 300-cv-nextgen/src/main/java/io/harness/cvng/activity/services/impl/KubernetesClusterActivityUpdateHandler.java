/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.activity.services.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.activity.entities.KubernetesClusterActivity;
import io.harness.cvng.activity.entities.KubernetesClusterActivity.ServiceEnvironment;
import io.harness.cvng.activity.services.api.ActivityUpdateHandler;
import io.harness.cvng.core.beans.dependency.KubernetesDependencyMetadata;
import io.harness.cvng.core.beans.dependency.ServiceDependencyMetadata.DependencyMetadataType;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO.ServiceDependencyDTO;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceResponse;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.core.services.api.monitoredService.ServiceDependencyService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class KubernetesClusterActivityUpdateHandler extends ActivityUpdateHandler<KubernetesClusterActivity> {
  @Inject private ServiceDependencyService serviceDependencyService;
  @Inject private MonitoredServiceService monitoredServiceService;
  @Override
  public void handleCreate(KubernetesClusterActivity activity) {
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(activity.getAccountId())
                                                            .orgIdentifier(activity.getOrgIdentifier())
                                                            .projectIdentifier(activity.getProjectIdentifier())
                                                            .serviceIdentifier(activity.getServiceIdentifier())
                                                            .environmentIdentifier(activity.getEnvironmentIdentifier())
                                                            .build();
    MonitoredServiceResponse monitoredServiceResponse = monitoredServiceService.get(serviceEnvironmentParams);
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(activity.getAccountId())
                                      .orgIdentifier(activity.getOrgIdentifier())
                                      .projectIdentifier(activity.getProjectIdentifier())
                                      .build();
    Set<ServiceDependencyDTO> serviceDependencies = serviceDependencyService.getDependentServicesToMonitoredService(
        projectParams, monitoredServiceResponse.getMonitoredServiceDTO().getIdentifier());
    Set<String> dependentMonitoredServices = new HashSet<>();
    serviceDependencies.forEach(
        serviceDependency -> { dependentMonitoredServices.add(serviceDependency.getMonitoredServiceIdentifier()); });
    if (isNotEmpty(dependentMonitoredServices)) {
      List<MonitoredServiceResponse> monitoredServiceResponseList =
          monitoredServiceService.get(projectParams, dependentMonitoredServices);
      monitoredServiceResponseList.forEach(response -> {
        Optional<ServiceDependencyDTO> dependencyDTOOptional =
            response.getMonitoredServiceDTO()
                .getDependencies()
                .stream()
                .filter(dependencyDTO
                    -> dependencyDTO.getMonitoredServiceIdentifier().equals(
                        monitoredServiceResponse.getMonitoredServiceDTO().getIdentifier()))
                .findFirst();
        if (dependencyDTOOptional.isPresent()) {
          ServiceDependencyDTO serviceDependency = dependencyDTOOptional.get();
          if (serviceDependency.getDependencyMetadata() != null
              && serviceDependency.getDependencyMetadata().getType().equals(DependencyMetadataType.KUBERNETES)) {
            KubernetesDependencyMetadata kubernetesDependencyMetadata =
                (KubernetesDependencyMetadata) serviceDependency.getDependencyMetadata();
            if (kubernetesDependencyMetadata.getNamespace().equals(activity.getNamespace())
                && kubernetesDependencyMetadata.getWorkload().equals(activity.getWorkload())) {
              if (activity.getRelatedAppServices() == null) {
                activity.setRelatedAppServices(new ArrayList<>());
              }
              activity.getRelatedAppServices().add(
                  ServiceEnvironment.builder()
                      .environmentIdentifier(response.getMonitoredServiceDTO().getEnvironmentRef())
                      .serviceIdentifier(response.getMonitoredServiceDTO().getServiceRef())
                      .build());
            }
          }
        }
      });
    }
  }

  @Override
  public void handleDelete(KubernetesClusterActivity activity) {}

  @Override
  public void handleUpdate(KubernetesClusterActivity existingActivity, KubernetesClusterActivity newActivity) {}
}
