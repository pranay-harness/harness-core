package software.wings.service.impl.expression;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.dl.PageRequest;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Singleton;

/**
 * Created by sgurubelli on 8/9/17.
 */
@Singleton
public class EnvironmentExpressionBuilder extends ExpressionBuilder {
  @Inject private ServiceExpressionBuilder serviceExpressionBuilder;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceVariableService serviceVariablesService;

  @Override
  public Set<String> getExpressions(String appId, String entityId, String serviceId) {
    SortedSet<String> expressions = new TreeSet<>();
    expressions.addAll(getStaticExpressions());
    if (serviceId != null && !serviceId.isEmpty()) {
      expressions.addAll(serviceExpressionBuilder.getDynamicExpressions(appId, serviceId));
    }
    expressions.addAll(getServiceTemplateVariableExpressions(appId, entityId, serviceId));
    return expressions;
  }

  @Override
  public Set<String> getExpressions(String appId, String entityId) {
    SortedSet<String> expressions = new TreeSet<>();
    expressions.addAll(getStaticExpressions());
    return expressions;
  }
  @Override
  public Set<String> getDynamicExpressions(String appId, String entityId) {
    return null;
  }

  public Set<String> getServiceTemplateVariableExpressions(String appId, String envId, String serviceId) {
    List<String> serviceIds = serviceExpressionBuilder.getServiceIds(appId, serviceId);
    if (serviceIds == null) {
      return new TreeSet<>();
    }
    PageRequest<ServiceTemplate> pageRequest = aPageRequest()
                                                   .withLimit(UNLIMITED)
                                                   .addFilter("appId", EQ, appId)
                                                   .addFilter("envId", EQ, envId)
                                                   .addFilter("serviceId", IN, serviceIds.toArray())
                                                   .addFieldsIncluded("uuid")
                                                   .build();
    List<ServiceTemplate> serviceTemplates = serviceTemplateService.list(pageRequest, false, false);
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

  public Set<String> getServiceTemplateVariableExpressions(String appId, String envId) {
    PageRequest<ServiceTemplate> pageRequest =
        aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, appId).addFilter("envId", EQ, envId).build();
    List<ServiceTemplate> serviceTemplates = serviceTemplateService.list(pageRequest, false, false);
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
