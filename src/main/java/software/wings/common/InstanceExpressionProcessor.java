/**
 *
 */

package software.wings.common;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.common.collect.Lists;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExpressionProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class InstanceExpressionProcessor.
 *
 * @author Rishi
 */
public class InstanceExpressionProcessor implements ExpressionProcessor {
  /**
   * The Expression start pattern.
   */
  static final String EXPRESSION_START_PATTERN = "instances()";
  private static final String INSTANCE_EXPR_PROCESSOR = "instanceExpressionProcessor";
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ServiceResourceService serviceResourceService;

  private ExecutionContextImpl context;

  private String serviceName;
  private String[] serviceTemplateNames;
  private String[] hostNames;
  private String[] instanceIds;

  /**
   * Instantiates a new instance expression processor.
   *
   * @param context the context
   */
  public InstanceExpressionProcessor(ExecutionContext context) {
    ExecutionContextImpl contextImpl = (ExecutionContextImpl) context;
    this.context = contextImpl;
  }

  @Override
  public String getPrefixObjectName() {
    return INSTANCE_EXPR_PROCESSOR;
  }

  /* (non-Javadoc)
   * @see software.wings.sm.ExpressionProcessor#normalizeExpression(java.lang.String)
   */
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

  /**
   * Instances.
   *
   * @param serviceInstanceIds the service instance ids
   * @return the instance expression processor
   */
  public InstanceExpressionProcessor instances(String... serviceInstanceIds) {
    this.instanceIds = serviceInstanceIds;
    return this;
  }

  /**
   * With service.
   *
   * @param serviceName the service name
   * @return the instance expression processor
   */
  public InstanceExpressionProcessor withService(String serviceName) {
    this.serviceName = serviceName;
    return this;
  }

  /**
   * With service templates.
   *
   * @param serviceTemplateNames the service template names
   * @return the instance expression processor
   */
  public InstanceExpressionProcessor withServiceTemplates(String... serviceTemplateNames) {
    this.serviceTemplateNames = serviceTemplateNames;
    return this;
  }

  /**
   * With hosts.
   *
   * @param hosts the hosts
   * @return the instance expression processor
   */
  public InstanceExpressionProcessor withHosts(String... hosts) {
    this.hostNames = hosts;
    return this;
  }

  /**
   * With instance ids.
   *
   * @param instanceIds the instance ids
   * @return the instance expression processor
   */
  public InstanceExpressionProcessor withInstanceIds(String... instanceIds) {
    this.instanceIds = instanceIds;
    return this;
  }

  /**
   * Lists.
   *
   * @return the list
   */
  public List<InstanceElement> list() {
    PageRequest<ServiceInstance> pageRequest = buildPageRequest();

    PageResponse<ServiceInstance> instances = serviceInstanceService.list(pageRequest);
    return convertToInstanceElements(instances.getResponse());
  }

  /**
   * Build page request page request.
   *
   * @return the page request
   */
  PageRequest<ServiceInstance> buildPageRequest() {
    String appId = context.getStateExecutionInstance().getAppId();
    Builder pageRequest = PageRequest.Builder.aPageRequest();

    pageRequest.addFilter(SearchFilter.Builder.aSearchFilter().withField("appId", Operator.EQ, appId).build());
    applyServiceFilter(appId, pageRequest);
    applyServiceTemplatesFilter(appId, pageRequest);
    applyHostNamesFilter(appId, pageRequest);
    applyServiceInstanceIdsFilter(appId, pageRequest);
    return pageRequest.build();
  }

  private List<InstanceElement> convertToInstanceElements(List<ServiceInstance> instances) {
    if (instances == null) {
      return null;
    }

    List<InstanceElement> elements = new ArrayList<>();
    for (ServiceInstance instance : instances) {
      elements.add(convertToInstanceElement(instance));
    }

    return elements;
  }

  static InstanceElement convertToInstanceElement(ServiceInstance instance) {
    ModelMapper mm = new ModelMapper();
    InstanceElement element = new InstanceElement();
    mm.map(instance, element);
    element.setHostElement(HostExpressionProcessor.convertToHostElement(instance.getHost()));
    element.setServiceTemplateElement(
        ServiceTemplateExpressionProcessor.convertToServiceTemplateElement(instance.getServiceTemplate()));

    return element;
  }

  private void applyServiceInstanceIdsFilter(String appId, Builder pageRequest) {
    ServiceInstanceIdsParam serviceInstanceIdsParam = getServiceInstanceIdsParam();
    if (serviceInstanceIdsParam != null) {
      if (ArrayUtils.isNotEmpty(instanceIds)) {
        Collection<String> commonInstanceIds =
            CollectionUtils.intersection(Arrays.asList(instanceIds), serviceInstanceIdsParam.getInstanceIds());
        instanceIds = commonInstanceIds.toArray(new String[commonInstanceIds.size()]);
      } else {
        instanceIds = serviceInstanceIdsParam.getInstanceIds().toArray(
            new String[serviceInstanceIdsParam.getInstanceIds().size()]);
      }
    }

    if (ArrayUtils.isNotEmpty(instanceIds)) {
      pageRequest.addFilter(SearchFilter.Builder.aSearchFilter().withField(ID_KEY, Operator.IN, instanceIds).build());
    } else {
      InstanceElement element = context.getContextElement(ContextElementType.INSTANCE);
      if (element != null) {
        pageRequest.addFilter(SearchFilter.Builder.aSearchFilter()
                                  .withField(ID_KEY, Operator.IN, new Object[] {element.getUuid()})
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

  private ServiceInstanceIdsParam getServiceInstanceIdsParam() {
    List<ContextElement> params = context.getContextElementList(ContextElementType.PARAM);
    if (params == null) {
      return null;
    }
    for (ContextElement param : params) {
      if (Constants.SERVICE_INSTANCE_IDS_PARAMS.equals(param.getName())) {
        return (ServiceInstanceIdsParam) param;
      }
    }
    return null;
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

  /**
   * Sets service instance service.
   *
   * @param serviceInstanceService the service instance service
   */
  void setServiceInstanceService(ServiceInstanceService serviceInstanceService) {
    this.serviceInstanceService = serviceInstanceService;
  }

  /**
   * Sets service resource service.
   *
   * @param serviceResourceService the service resource service
   */
  void setServiceResourceService(ServiceResourceService serviceResourceService) {
    this.serviceResourceService = serviceResourceService;
  }
}
