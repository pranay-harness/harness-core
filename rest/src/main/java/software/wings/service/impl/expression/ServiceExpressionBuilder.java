package software.wings.service.impl.expression;

import static java.util.Arrays.asList;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.dl.PageRequest;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.utils.Misc;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Created by sgurubelli on 8/7/17.
 */
@Singleton
public class ServiceExpressionBuilder extends ExpressionBuilder {
  @Inject private ServiceVariableService serviceVariablesService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public Set<String> getExpressions(String appId, String entityId) {
    SortedSet<String> expressions = new TreeSet<>();
    expressions.addAll(getStaticExpressions());
    expressions.addAll(getDynamicExpressions(appId, entityId));
    expressions.addAll(getServiceTemplateVariableExpressions(appId, entityId));
    return expressions;
  }

  @Override
  public Set<String> getDynamicExpressions(String appId, String entityId) {
    List<String> serviceIds = getServiceIds(appId, entityId);
    if (serviceIds == null) {
      return new TreeSet<>();
    }
    PageRequest<ServiceVariable> pageRequest = aPageRequest()
                                                   .withLimit(UNLIMITED)
                                                   .addFilter("appId", EQ, appId)
                                                   .addFilter("entityId", IN, serviceIds.toArray())
                                                   .addFilter("entityType", EQ, SERVICE)
                                                   .build();

    List<ServiceVariable> serviceVariables = serviceVariablesService.list(pageRequest, true);
    if (CollectionUtils.isNotEmpty(serviceVariables)) {
      return serviceVariables.stream()
          .map(serviceVariable -> "serviceVariable." + serviceVariable.getName())
          .collect(Collectors.toSet());
    }
    return new TreeSet<>();
  }

  public List<String> getServiceIds(String appId, String entityId) {
    if (entityId.equalsIgnoreCase("All")) {
      List<Service> services = serviceResourceService.list(
          aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, appId).addFieldsIncluded("uuid").build(), false,
          false);
      if (services != null) {
        return services.stream().map(Service::getUuid).collect(Collectors.toList());
      }
    } else {
      return asList(Misc.commaCharPattern.split(entityId));
    }
    return asList();
  }

  public Set<String> getServiceTemplateVariableExpressions(String appId, String serviceId) {
    List<String> serviceIds = getServiceIds(appId, serviceId);
    if (serviceIds == null) {
      return new TreeSet<>();
    }
    PageRequest<ServiceTemplate> serviceTemplatePageRequest = aPageRequest()
                                                                  .withLimit(UNLIMITED)
                                                                  .addFilter("appId", EQ, appId)
                                                                  .addFilter("serviceId", IN, serviceIds.toArray())
                                                                  .build();
    List<ServiceTemplate> serviceTemplates = serviceTemplateService.list(serviceTemplatePageRequest, false, false);
    if (CollectionUtils.isNotEmpty(serviceTemplates)) {
      List<String> serviceTemplateIds =
          serviceTemplates.stream().map(ServiceTemplate::getUuid).collect(Collectors.toList());
      PageRequest<ServiceVariable> serviceVariablePageRequest =
          aPageRequest()
              .withLimit(PageRequest.UNLIMITED)
              .addFilter("appId", EQ, appId)
              .addFilter("entityId", IN, serviceTemplateIds.toArray())
              .build();
      List<ServiceVariable> serviceVariables = serviceVariablesService.list(serviceVariablePageRequest, true);
      if (CollectionUtils.isNotEmpty(serviceVariables)) {
        return serviceVariables.stream()
            .map(serviceVariable -> "serviceVariable." + serviceVariable.getName())
            .collect(Collectors.toSet());
      }
    }
    return new TreeSet<>();
  }
}
