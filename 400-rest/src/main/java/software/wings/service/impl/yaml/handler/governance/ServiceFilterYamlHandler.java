package software.wings.service.impl.yaml.handler.governance;

import static io.harness.governance.ServiceFilter.ServiceFilterType;
import static io.harness.governance.ServiceFilter.Yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.governance.ServiceFilter;

import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
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
    return Yaml.builder()
        .filterType(bean.getFilterType().name())
        .services(serviceResourceService.getNames(accountId, bean.getServiceIds()))
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
        bean.setServiceIds(getServiceIds(yaml.getServices(), appId));
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
    return serviceNames.stream()
        .map(name -> serviceResourceService.getServiceByName(appId, name, false).getUuid())
        .collect(Collectors.toList());
  }
}
