/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.governance;

import static io.harness.governance.ServiceFilter.ServiceFilterType;
import static io.harness.governance.ServiceFilter.Yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.governance.ServiceFilter;
import io.harness.validation.Validator;

import software.wings.beans.Service;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDC)
public class ServiceFilterYamlHandler extends BaseYamlHandler<Yaml, ServiceFilter> {
  @Inject ServiceResourceService serviceResourceService;
  @Inject AppService appService;
  @Override
  public void delete(ChangeContext<Yaml> changeContext) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Yaml toYaml(ServiceFilter bean, String accountId) {
    if (ServiceFilterType.ALL == bean.getFilterType()) {
      return Yaml.builder().filterType(bean.getFilterType().name()).build();
    }

    // Preserve the order of services
    return Yaml.builder()
        .filterType(bean.getFilterType().name())
        .services(serviceResourceService.fetchServicesByUuidsByAccountId(accountId, bean.getServices())
                      .stream()
                      .map(Service::getName)
                      .collect(Collectors.toList()))
        .build();
  }

  @Override
  public ServiceFilter upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String appId = changeContext.getEntityIdMap().get("appId");
    ServiceFilter bean = ServiceFilter.builder().build();
    ServiceFilter.Yaml yaml = changeContext.getYaml();
    try {
      ServiceFilterType filterType = ServiceFilterType.valueOf(yaml.getFilterType());
      bean.setFilterType(filterType);
      if (ServiceFilterType.ALL == filterType) {
        return bean;
      } else {
        bean.setServices(getServiceIds(yaml.getServices(), appId));
      }
    } catch (IllegalArgumentException illegalArgumentException) {
      throw new InvalidRequestException(
          String.format("Invalid service filter type. Please, provide valid value: %s", ServiceFilterType.values()));
    }
    return bean;
  }

  @Override
  public Class getYamlClass() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServiceFilter get(String accountId, String yamlFilePath) {
    throw new UnsupportedOperationException();
  }

  private List<String> getServiceIds(List<String> serviceNames, String appId) {
    Validator.notNullCheck("Services are required for CUSTOM service filter.", serviceNames);
    List<String> serviceIds = new ArrayList<>();
    for (String serviceName : serviceNames) {
      Service service = serviceResourceService.getServiceByName(appId, serviceName);
      if (service == null) {
        throw new InvalidRequestException("Invalid Service: " + serviceName);
      }
      serviceIds.add(service.getUuid());
    }
    return serviceIds;
  }
}
