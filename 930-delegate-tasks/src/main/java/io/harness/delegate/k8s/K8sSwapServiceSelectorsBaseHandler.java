/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.k8s.KubernetesHelperService.toDisplayYaml;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.Service;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class K8sSwapServiceSelectorsBaseHandler {
  @Inject private KubernetesContainerService kubernetesContainerService;

  public boolean swapServiceSelectors(
      KubernetesConfig kubernetesConfig, String serviceOne, String serviceTwo, LogCallback logCallback) {
    Service service1 = null;
    Service service2 = null;
    service1 = kubernetesContainerService.getServiceFabric8(kubernetesConfig, serviceOne);
    service2 = kubernetesContainerService.getServiceFabric8(kubernetesConfig, serviceTwo);

    if (service1 == null) {
      logCallback.saveExecutionLog(
          String.format("Service %s not found.", serviceOne), LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }

    if (service2 == null) {
      logCallback.saveExecutionLog(
          String.format("Service %s not found.", serviceTwo), LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }

    Map<String, String> serviceOneSelectors = service1.getSpec().getSelector();
    Map<String, String> serviceTwoSelectors = service2.getSpec().getSelector();

    logCallback.saveExecutionLog(String.format("%nSelectors for Service One : [name:%s]%n%s",
                                     service1.getMetadata().getName(), toDisplayYaml(service1.getSpec().getSelector())),
        LogLevel.INFO);

    logCallback.saveExecutionLog(String.format("%nSelectors for Service Two : [name:%s]%n%s",
                                     service2.getMetadata().getName(), toDisplayYaml(service2.getSpec().getSelector())),
        LogLevel.INFO);

    logCallback.saveExecutionLog(String.format("%nSwapping Service Selectors..%n"), LogLevel.INFO);

    service1.getSpec().setSelector(serviceTwoSelectors);
    service2.getSpec().setSelector(serviceOneSelectors);

    Service serviceOneUpdated = kubernetesContainerService.createOrReplaceService(kubernetesConfig, service1);

    Service serviceTwoUpdated = kubernetesContainerService.createOrReplaceService(kubernetesConfig, service2);

    logCallback.saveExecutionLog(
        String.format("%nUpdated Selectors for Service One : [name:%s]%n%s", serviceOneUpdated.getMetadata().getName(),
            toDisplayYaml(serviceOneUpdated.getSpec().getSelector())),
        LogLevel.INFO);

    logCallback.saveExecutionLog(
        String.format("%nUpdated Selectors for Service Two : [name:%s]%n%s", serviceTwoUpdated.getMetadata().getName(),
            toDisplayYaml(serviceTwoUpdated.getSpec().getSelector())),
        LogLevel.INFO);

    logCallback.saveExecutionLog("Done", LogLevel.INFO, CommandExecutionStatus.SUCCESS);

    return true;
  }
}
