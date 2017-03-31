/**
 *
 */

package software.wings.common;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.intersection;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PartitionElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SortOrder;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExpressionProcessor;
import software.wings.utils.MapperUtils;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * The Class InstanceExpressionProcessor.
 *
 * @author Rishi
 */
public class InstanceExpressionProcessor implements ExpressionProcessor {
  /**
   * The Expression start pattern.
   */
  public static final String DEFAULT_EXPRESSION = "${instances}";

  private static final String EXPRESSION_START_PATTERN = "instances()";
  private static final String EXPRESSION_EQUAL_PATTERN = "instances";

  private static final String INSTANCE_EXPR_PROCESSOR = "instanceExpressionProcessor";

  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private HostService hostService;

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

  /**
   * Convert to instance element instance element.
   *
   * @param instance        the instance
   * @param host            the host
   * @param serviceTemplate the service template
   * @return the instance element
   */
  static InstanceElement convertToInstanceElement(
      ServiceInstance instance, Host host, ServiceTemplate serviceTemplate) {
    InstanceElement element = new InstanceElement();
    MapperUtils.mapObject(instance, element);
    element.setHostElement(HostExpressionProcessor.convertToHostElement(host));
    element.setServiceTemplateElement(
        ServiceTemplateExpressionProcessor.convertToServiceTemplateElement(serviceTemplate));
    element.setDisplayName(host.getHostName() + ":" + serviceTemplate.getName());
    return element;
  }

  @Override
  public String getPrefixObjectName() {
    return INSTANCE_EXPR_PROCESSOR;
  }

  @Override
  public List<String> getExpressionStartPatterns() {
    return Collections.singletonList(EXPRESSION_START_PATTERN);
  }

  @Override
  public List<String> getExpressionEqualPatterns() {
    return Collections.singletonList(EXPRESSION_EQUAL_PATTERN);
  }

  @Override
  public ContextElementType getContextElementType() {
    return ContextElementType.INSTANCE;
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
    PartitionElement instancePartition = getInstancesPartition();
    if (instancePartition != null) {
      // TODO -- apply additional filters based on host name and instanceIds
      return instancePartition.getPartitionElements()
          .stream()
          .map(contextElement -> (InstanceElement) contextElement)
          .collect(toList());
    }

    InstanceElementListParam instanceListParam = getInstanceListParam();
    if (instanceListParam != null) {
      return instanceListParam.getInstanceElements();
    }

    PageRequest<ServiceInstance> pageRequest = buildPageRequest();
    PageResponse<ServiceInstance> instances = serviceInstanceService.list(pageRequest);
    return convertToInstanceElements(instances.getResponse());
  }

  private PartitionElement getInstancesPartition() {
    List<ContextElement> partitions = context.getContextElementList(ContextElementType.PARTITION);
    if (partitions == null || partitions.isEmpty()) {
      return null;
    }

    for (ContextElement element : partitions) {
      PartitionElement partition = (PartitionElement) element;
      if (partition.getPartitionElementType() == ContextElementType.INSTANCE) {
        return partition;
      }
    }
    return null;
  }

  /**
   * Build page request page request.
   *
   * @return the page request
   */
  PageRequest<ServiceInstance> buildPageRequest() {
    Application app = ((ExecutionContextImpl) context).getApp();
    Environment env = ((ExecutionContextImpl) context).getEnv();
    Builder pageRequest = PageRequest.Builder.aPageRequest();

    pageRequest.addFilter(aSearchFilter().withField("appId", Operator.EQ, app.getUuid()).build());
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
      ServiceTemplate serviceTemplate =
          serviceTemplateService.get(instance.getAppId(), instance.getEnvId(), instance.getServiceTemplateId(), false);
      Host host = hostService.getHostByEnv(instance.getAppId(), instance.getEnvId(), instance.getHostId());
      elements.add(convertToInstanceElement(instance, host, serviceTemplate));
    }

    if (ArrayUtils.isNotEmpty(instanceIds)) {
      Map<String, InstanceElement> map =
          elements.stream().collect(Collectors.toMap(InstanceElement::getUuid, Function.identity()));
      elements = new ArrayList<>();
      for (String instanceId : instanceIds) {
        if (map.containsKey(instanceId)) {
          elements.add(map.get(instanceId));
        }
      }
    }

    return elements;
  }

  private void applyServiceInstanceIdsFilter(String appId, Builder pageRequest) {
    ServiceInstanceIdsParam serviceInstanceIdsParam = getServiceInstanceIdsParam();
    if (serviceInstanceIdsParam != null) {
      if (ArrayUtils.isNotEmpty(instanceIds)) {
        Collection<String> commonInstanceIds =
            intersection(Arrays.asList(instanceIds), serviceInstanceIdsParam.getInstanceIds());
        instanceIds = commonInstanceIds.toArray(new String[commonInstanceIds.size()]);
      } else {
        instanceIds = serviceInstanceIdsParam.getInstanceIds().toArray(
            new String[serviceInstanceIdsParam.getInstanceIds().size()]);
      }
    }

    if (ArrayUtils.isNotEmpty(instanceIds)) {
      pageRequest.addFilter(aSearchFilter().withField(ID_KEY, Operator.IN, instanceIds).build());
    } else {
      InstanceElement element = context.getContextElement(ContextElementType.INSTANCE);
      if (element != null) {
        pageRequest.addFilter(aSearchFilter().withField(ID_KEY, Operator.IN, new Object[] {element.getUuid()}).build());
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
      pageRequest.addFilter(aSearchFilter()
                                .withField("serviceTemplate", Operator.IN,
                                    serviceTemplates.stream().map(ServiceTemplate::getUuid).toArray())
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
            .addFilter(aSearchFilter().withField("envId", Operator.EQ, envId).build())
            .addOrder(SortOrder.Builder.aSortOrder().withField("createdAt", OrderType.ASC).build());
    if (services != null && !services.isEmpty()) {
      pageRequestBuilder.addFilter(
          aSearchFilter()
              .withField("serviceId", Operator.IN, services.stream().map(Service::getUuid).collect(toList()).toArray())
              .build());
    }
    if (!ArrayUtils.isEmpty(names)) {
      pageRequestBuilder.addFilter(aSearchFilter().withField("name", Operator.IN, serviceTemplateNames).build());
    }
    return serviceTemplateService.list(pageRequestBuilder.build(), false).getResponse();
  }

  private InstanceElementListParam getInstanceListParam() {
    List<ContextElement> params = context.getContextElementList(ContextElementType.PARAM);
    if (params == null) {
      return null;
    }
    for (ContextElement param : params) {
      if (Constants.INSTANCE_LIST_PARAMS.equals(param.getName())) {
        return (InstanceElementListParam) param;
      }
    }
    return null;
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
              .addFilter(aSearchFilter().withField("appId", Operator.EQ, appId).build())
              .addFilter(aSearchFilter().withField("name", Operator.EQ, serviceName).build())
              .addFieldsIncluded(ID_KEY)
              .build();

      PageResponse<Service> services = serviceResourceService.list(svcPageRequest, false);
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

  /**
   * Sets service template service.
   *
   * @param serviceTemplateService the service template service
   */
  public void setServiceTemplateService(ServiceTemplateService serviceTemplateService) {
    this.serviceTemplateService = serviceTemplateService;
  }
}
