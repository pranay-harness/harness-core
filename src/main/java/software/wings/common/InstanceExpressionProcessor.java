/**
 *
 */
package software.wings.common;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceElement;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExpressionProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Rishi
 *
 */
public class InstanceExpressionProcessor implements ExpressionProcessor {
  static final String EXPRESSION_START_PATTERN = "instances()";
  private static final String INSTANCE_EXPR_PROCESSOR = "instanceExpressionProcessor";
  private ServiceInstanceService serviceInstanceService;
  private ServiceResourceService serviceResourceService;

  private ExecutionContextImpl context;

  private String serviceName;
  private String[] serviceTemplateNames;
  private String[] hostNames;
  private String[] serviceInstanceIds;

  public InstanceExpressionProcessor(ExecutionContext context, ServiceInstanceService serviceInstanceService) {
    this.serviceInstanceService = serviceInstanceService;
    ExecutionContextImpl contextImpl = (ExecutionContextImpl) context;
    this.context = contextImpl;
  }

  @Override
  public String getPrefixObjectName() {
    return INSTANCE_EXPR_PROCESSOR;
  }

  @Override
  public String normalizeExpression(String expression) {
    if (expression == null || !expression.startsWith(EXPRESSION_START_PATTERN)) {
      return null;
    }
    expression = INSTANCE_EXPR_PROCESSOR + "." + expression;
    if (!expression.endsWith(Constants.EXPRESSION_LIST_SUFFIX)) {
      expression = expression + Constants.EXPRESSION_LIST_SUFFIX;
    }
    return expression;
  }

  public InstanceExpressionProcessor instances(String... serviceInstanceIds) {
    this.serviceInstanceIds = serviceInstanceIds;
    return this;
  }

  public InstanceExpressionProcessor withService(String serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  public InstanceExpressionProcessor withServiceTemplates(String... serviceTemplateNames) {
    this.serviceTemplateNames = serviceTemplateNames;
    return this;
  }

  public InstanceExpressionProcessor withHosts(String... hosts) {
    this.hostNames = hosts;
    return this;
  }

  public List<InstanceElement> lists() {
    String appId = context.getStateExecutionInstance().getAppId();
    Builder pageRequest = PageRequest.Builder.aPageRequest();

    applyServiceFilter(appId, pageRequest);
    applyServiceTemplatesFilter(appId, pageRequest);
    applyHostNamesFilter(appId, pageRequest);
    applyServiceInstanceIdsFilter(appId, pageRequest);

    PageResponse<ServiceInstance> instances = serviceInstanceService.list(pageRequest.build());

    return convertToInstanceElements(instances.getResponse());
  }

  private List<InstanceElement> convertToInstanceElements(List<ServiceInstance> instances) {
    ModelMapper mm = new ModelMapper();

    List<InstanceElement> elements = new ArrayList<>();
    for (ServiceInstance instance : instances) {
      InstanceElement element = new InstanceElement();
      mm.map(instance, element);
      elements.add(element);
    }

    return elements;
  }

  private void applyServiceInstanceIdsFilter(String appId, Builder pageRequest) {
    if (!ArrayUtils.isEmpty(serviceInstanceIds)) {
      pageRequest.addFilter(
          SearchFilter.Builder.aSearchFilter().withField("uuid", Operator.IN, serviceInstanceIds).build());
    } else {
      InstanceElement element = context.getContextElement(ContextElementType.INSTANCE);
      if (element != null) {
        pageRequest.addFilter(SearchFilter.Builder.aSearchFilter()
                                  .withField("uuid", Operator.IN, new Object[] {element.getUuid()})
                                  .build());
      }
    }
  }

  private void applyHostNamesFilter(String appId, Builder pageRequest) {
    // TODO
  }

  private void applyServiceTemplatesFilter(String appId, Builder pageRequest) {
    // TODO
  }

  private void applyServiceFilter(String appId, Builder pageRequest) {
    List<String> serviceIds = null;

    if (!StringUtils.isBlank(serviceName)) {
      PageRequest<Service> svcPageRequest =
          PageRequest.Builder.aPageRequest()
              .addFilter(SearchFilter.Builder.aSearchFilter().withField("appId", Operator.EQ, appId).build())
              .addFilter(SearchFilter.Builder.aSearchFilter().withField("name", Operator.EQ, serviceName).build())
              .withFieldsIncluded(Lists.newArrayList("uuid"))
              .build();

      PageResponse<Service> services = serviceResourceService.list(svcPageRequest);
      if (services != null && services.size() > 0) {
        serviceIds = services.stream().map(Service::getUuid).collect(Collectors.toList());
      }
    }
    if (serviceIds != null) {
      ServiceElement serviceElement = context.getContextElement(ContextElementType.SERVICE);
      if (serviceElement != null) {
        serviceIds = Lists.newArrayList(serviceElement.getUuid());
      }
    }

    if (serviceIds != null) {
      pageRequest.addFilter(
          SearchFilter.Builder.aSearchFilter().withField("service", Operator.EQ, serviceIds.toArray()).build());
    }
  }
}
