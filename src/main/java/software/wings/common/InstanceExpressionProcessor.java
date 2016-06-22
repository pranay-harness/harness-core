/**
 *
 */

package software.wings.common;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.common.collect.Lists;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SortOrder;
import software.wings.beans.SortOrder.OrderType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExpressionProcessor;
import software.wings.utils.MapperUtils;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private static final String EXPRESSION_START_PATTERN = "instances()";
  private static final Object EXPRESSION_EQUAL_PATTERN = "instances";

  private static final String INSTANCE_EXPR_PROCESSOR = "instanceExpressionProcessor";

  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;

  private ExecutionContext context;

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
    this.context = context;
  }

  static InstanceElement convertToInstanceElement(ServiceInstance instance) {
    InstanceElement element = new InstanceElement();
    MapperUtils.mapObject(instance, element);
    element.setHostElement(HostExpressionProcessor.convertToHostElement(instance.getHost()));
    element.setServiceTemplateElement(
        ServiceTemplateExpressionProcessor.convertToServiceTemplateElement(instance.getServiceTemplate()));

    return element;
  }

  @Override
  public String getPrefixObjectName() {
    return INSTANCE_EXPR_PROCESSOR;
  }

  @Override
  public boolean matches(String expression) {
    if (expression != null
        && (expression.startsWith(EXPRESSION_START_PATTERN) || expression.equals(EXPRESSION_EQUAL_PATTERN))) {
      return true;
    }
    return false;
  }

  @Override
  public String normalizeExpression(String expression) {
    if (!matches(expression)) {
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
   * Instances.
   *
   * @return the instance expression processor
   */
  public InstanceExpressionProcessor getInstances() {
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
    Application app = context.getApp();
    Environment env = context.getEnv();
    Builder pageRequest = PageRequest.Builder.aPageRequest();

    pageRequest.addFilter(SearchFilter.Builder.aSearchFilter().withField("appId", Operator.EQ, app.getUuid()).build());
    applyServiceTemplatesFilter(app.getUuid(), env.getUuid(), pageRequest);
    applyHostNamesFilter(app.getUuid(), pageRequest);
    applyServiceInstanceIdsFilter(app.getUuid(), pageRequest);
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

  private void applyServiceTemplatesFilter(String appId, String envId, Builder pageRequest) {
    List<Service> services = null;
    List<ServiceTemplate> serviceTemplates = null;
    if (ArrayUtils.isEmpty(serviceTemplateNames)) {
      services = getServices(appId);
      serviceTemplates = getServiceTemplates(envId, services, serviceTemplateNames);
    } else {
      if (Misc.isWildCharPresent(serviceTemplateNames)) {
        serviceTemplates = getServiceTemplates(envId, services);
        serviceTemplates = matchingServiceTemplates(serviceTemplates, serviceTemplateNames);
      } else {
        serviceTemplates = getServiceTemplates(envId, services, serviceTemplateNames);
      }
    }

    if (serviceTemplates != null && !serviceTemplates.isEmpty()) {
      pageRequest.addFilter(
          SearchFilter.Builder.aSearchFilter()
              .withField("serviceTemplate", Operator.IN, serviceTemplates.toArray(new ServiceTemplate[0]))
              .build());
    }
  }

  /**
   * Matching ServiceTemplates.
   *
   * @param serviceTemplates the serviceTemplates
   * @param names            the names
   * @return the list
   */
  List<ServiceTemplate> matchingServiceTemplates(List<ServiceTemplate> serviceTemplates, String... names) {
    if (serviceTemplates == null) {
      return null;
    }

    List<Pattern> patterns = new ArrayList<>();
    for (String name : names) {
      patterns.add(Pattern.compile(name.replaceAll("\\" + Constants.WILD_CHAR, "." + Constants.WILD_CHAR)));
    }

    List<ServiceTemplate> matchingServiceTemplates = new ArrayList<>();
    for (ServiceTemplate serviceTemplate : serviceTemplates) {
      for (Pattern pattern : patterns) {
        Matcher matcher = pattern.matcher(serviceTemplate.getName());
        if (matcher.matches()) {
          matchingServiceTemplates.add(serviceTemplate);
          break;
        }
      }
    }
    return matchingServiceTemplates;
  }

  private List<ServiceTemplate> getServiceTemplates(String envId, List<Service> services, String... names) {
    Builder pageRequestBuilder =
        PageRequest.Builder.aPageRequest()
            .addFilter(SearchFilter.Builder.aSearchFilter().withField("envId", Operator.EQ, envId).build())
            .addOrder(SortOrder.Builder.aSortOrder().withField("createdAt", OrderType.ASC).build());
    if (services != null && !services.isEmpty()) {
      pageRequestBuilder.addFilter(SearchFilter.Builder.aSearchFilter()
                                       .withField("service", Operator.IN, services.toArray(new Service[0]))
                                       .build());
    }
    if (!ArrayUtils.isEmpty(names)) {
      pageRequestBuilder.addFilter(
          SearchFilter.Builder.aSearchFilter().withField("name", Operator.IN, serviceTemplateNames).build());
    }
    return serviceTemplateService.list(pageRequestBuilder.build()).getResponse();
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

  private List<Service> getServices(String appId) {
    if (!StringUtils.isBlank(serviceName)) {
      PageRequest<Service> svcPageRequest =
          PageRequest.Builder.aPageRequest()
              .addFilter(SearchFilter.Builder.aSearchFilter().withField("appId", Operator.EQ, appId).build())
              .addFilter(SearchFilter.Builder.aSearchFilter().withField("name", Operator.EQ, serviceName).build())
              .withFieldsIncluded(Lists.newArrayList("uuid"))
              .build();

      PageResponse<Service> services = serviceResourceService.list(svcPageRequest);
      return services.getResponse();
    } else {
      ServiceElement serviceElement = context.getContextElement(ContextElementType.SERVICE);
      if (serviceElement != null) {
        Service service = new Service();
        MapperUtils.mapObject(serviceElement, service);
        return Lists.newArrayList(service);
      }
    }
    return null;
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

  public void setServiceTemplateService(ServiceTemplateService serviceTemplateService) {
    this.serviceTemplateService = serviceTemplateService;
  }
}
