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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
  public List<String> getExpressions(String appId, String entityId, String serviceId) {
    List<String> expressions = new ArrayList<>();
    expressions.addAll(getStaticExpressions());
    if (serviceId != null) {
      expressions.addAll(serviceExpressionBuilder.getDynamicExpressions(appId, serviceId));
    }
    expressions.addAll(getServiceTemplateVariableExpressions(appId, entityId, serviceId));
    return expressions;
  }

  @Override
  public List<String> getExpressions(String appId, String entityId) {
    List<String> expressions = new ArrayList<>();
    expressions.addAll(getStaticExpressions());
    return expressions;
  }
  @Override
  public List<String> getDynamicExpressions(String appId, String entityId) {
    return null;
  }

  private List<String> getServiceTemplateVariableExpressions(String appId, String envId, String serviceId) {
    PageRequest<ServiceTemplate> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    pageRequest.addFilter("appId", appId, EQ);
    if (serviceId != null) {
      pageRequest.addFilter("serviceId", serviceId, EQ);
    }
    pageRequest.addFilter("envId", envId, EQ);

    List<ServiceTemplate> serviceTemplates = serviceTemplateService.list(pageRequest, false, false);
    if (CollectionUtils.isNotEmpty(serviceTemplates)) {
      List<String> serviceTemplateIds =
          serviceTemplates.stream().map(ServiceTemplate::getUuid).collect(Collectors.toList());
      PageRequest<ServiceVariable> variablePageRequest = aPageRequest().withLimit(UNLIMITED).build();
      pageRequest.addFilter("appId", appId, EQ);
      pageRequest.addFilter("entityId", serviceTemplateIds, IN);
      List<ServiceVariable> serviceVariables = serviceVariablesService.list(variablePageRequest, true);
      if (CollectionUtils.isNotEmpty(serviceVariables)) {
        return serviceVariables.stream()
            .map(serviceVariable -> "serviceVariable." + serviceVariable.getName())
            .collect(Collectors.toList());
      }
    }
    return Arrays.asList();
  }
}
