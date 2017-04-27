/**
 *
 */

package software.wings.common;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.ArrayUtils;
import software.wings.api.ServiceElement;
import software.wings.beans.ErrorCode;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExpressionProcessor;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.MapperUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

/**
 * The Class ServiceExpressionProcessor.
 *
 * @author Rishi
 */
public class ServiceExpressionProcessor implements ExpressionProcessor {
  /**
   * The Expression start pattern.
   */
  public static final String DEFAULT_EXPRESSION = "${services}";
  private static final String EXPRESSION_START_PATTERN = "services()";
  private static final String EXPRESSION_EQUAL_PATTERN = "services";

  private static final String SERVICE_EXPR_PROCESSOR = "serviceExpressionProcessor";

  @Inject private ServiceResourceService serviceResourceService;

  private String[] serviceNames;
  private ExecutionContextImpl context;
  private String appId;

  private List<ServiceElement> selectedServices;

  /**
   * Instantiates a new service expression processor.
   *
   * @param context the context
   */
  public ServiceExpressionProcessor(ExecutionContext context) {
    ExecutionContextImpl contextImpl = (ExecutionContextImpl) context;
    this.context = contextImpl;
  }

  /**
   * Convert to service element service element.
   *
   * @param service the service
   * @return the service element
   */
  static ServiceElement convertToServiceElement(Service service) {
    ServiceElement element = new ServiceElement();
    MapperUtils.mapObject(service, element);
    return element;
  }

  @Override
  public String getPrefixObjectName() {
    return SERVICE_EXPR_PROCESSOR;
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
    return ContextElementType.SERVICE;
  }

  /**
   * Gets services.
   *
   * @return the services
   */
  public ServiceExpressionProcessor getServices() {
    return this;
  }

  /**
   * Services.
   *
   * @param serviceNames the service names
   * @return the service expression processor
   */
  public ServiceExpressionProcessor services(String... serviceNames) {
    this.serviceNames = serviceNames;
    return this;
  }

  /**
   * With names.
   *
   * @param serviceNames the service names
   * @return the service expression processor
   */
  public ServiceExpressionProcessor withNames(String... serviceNames) {
    this.serviceNames = serviceNames;
    return this;
  }

  /**
   * List.
   *
   * @return the list
   */
  public List<ServiceElement> list() {
    if (ArrayUtils.isEmpty(serviceNames)) {
      ServiceElement element = context.getContextElement(ContextElementType.SERVICE);
      if (element != null)
        return Lists.newArrayList(element);
      else {
        return getSelectedServices();
      }
    } else {
      return matchingServices(getSelectedServices(), serviceNames);
    }
  }

  /**
   * Matching services.
   *
   * @param services the services
   * @param names    the names
   * @return the list
   */
  List<ServiceElement> matchingServices(List<ServiceElement> services, String... names) {
    if (services == null) {
      return null;
    }

    List<Pattern> patterns = new ArrayList<>();
    for (String name : names) {
      patterns.add(Pattern.compile(name.replaceAll("\\" + Constants.WILD_CHAR, "." + Constants.WILD_CHAR)));
    }

    List<ServiceElement> matchingServices = new ArrayList<>();
    for (Pattern pattern : patterns) {
      for (ServiceElement service : services) {
        Matcher matcher = pattern.matcher(service.getName());
        if (matcher.matches()) {
          matchingServices.add(service);
          break;
        }
      }
    }
    return matchingServices;
  }

  private List<ServiceElement> convertToServiceElements(List<Service> services) {
    if (services == null) {
      return null;
    }

    List<ServiceElement> elements = new ArrayList<>();
    for (Service service : services) {
      elements.add(convertToServiceElement(service));
    }

    return elements;
  }

  private String getAppId() {
    if (appId == null) {
      appId = context.getApp().getUuid();
    }
    return appId;
  }

  private List<ServiceElement> getSelectedServices() {
    if (selectedServices == null) {
      WorkflowStandardParams stdParams = context.getContextElement(ContextElementType.STANDARD);
      if (stdParams == null) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", "Standard params is null");
      }
      selectedServices = stdParams.getServices();
    }

    if (selectedServices == null) {
      Builder pageRequest =
          PageRequest.Builder.aPageRequest()
              .withLimit(PageRequest.UNLIMITED)
              .addFilter(SearchFilter.Builder.aSearchFilter().withField("appId", Operator.EQ, getAppId()).build());
      List<Service> services = serviceResourceService.list(pageRequest.build(), false, true);
      selectedServices = convertToServiceElements(services);
    }
    return selectedServices;
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
