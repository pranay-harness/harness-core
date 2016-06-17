/**
 *
 */

package software.wings.common;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.ArrayUtils;
import software.wings.api.ServiceElement;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExpressionProcessor;
import software.wings.utils.MapperUtils;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class ServiceExpressionProcessor.
 *
 * @author Rishi
 */
public class ServiceExpressionProcessor implements ExpressionProcessor {
  /**
   * The Expression start pattern.
   */
  private static final String EXPRESSION_START_PATTERN = "services()";
  private static final String EXPRESSION_EQUAL_PATTERN = "services";

  private static final String SERVICE_EXPR_PROCESSOR = "serviceExpressionProcessor";

  @Inject private ServiceResourceService serviceResourceService;

  private String[] serviceNames;
  private ExecutionContextImpl context;

  /**
   * Instantiates a new service expression processor.
   *
   * @param context the context
   */
  public ServiceExpressionProcessor(ExecutionContext context) {
    ExecutionContextImpl contextImpl = (ExecutionContextImpl) context;
    this.context = contextImpl;
  }

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
    expression = SERVICE_EXPR_PROCESSOR + "." + expression;
    if (!expression.endsWith(Constants.EXPRESSION_LIST_SUFFIX)) {
      expression = expression + Constants.EXPRESSION_LIST_SUFFIX;
    }
    return expression;
  }

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
    String appId = context.getStateExecutionInstance().getAppId();

    List<Service> services = null;

    Builder pageRequest =
        PageRequest.Builder.aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(SearchFilter.Builder.aSearchFilter().withField("appId", Operator.EQ, appId).build());

    if (ArrayUtils.isEmpty(serviceNames)) {
      ServiceElement element = context.getContextElement(ContextElementType.SERVICE);
      if (element != null) {
        services = Lists.newArrayList(serviceResourceService.get(appId, element.getUuid()));
      } else {
        services = serviceResourceService.list(pageRequest.build());
      }
    } else if (Misc.isWildCharPresent(serviceNames)) {
      services = serviceResourceService.list(pageRequest.build());
      services = matchingServices(services, serviceNames);
    } else {
      pageRequest.addFilter(SearchFilter.Builder.aSearchFilter().withField("name", Operator.IN, serviceNames).build());
      services = serviceResourceService.list(pageRequest.build());
    }

    return convertToServiceElements(services);
  }

  /**
   * Matching services.
   *
   * @param services the services
   * @param names    the names
   * @return the list
   */
  List<Service> matchingServices(List<Service> services, String... names) {
    if (services == null) {
      return null;
    }

    List<Pattern> patterns = new ArrayList<>();
    for (String name : names) {
      patterns.add(Pattern.compile(name.replaceAll("\\" + Constants.WILD_CHAR, "." + Constants.WILD_CHAR)));
    }

    List<Service> matchingServices = new ArrayList<>();
    for (Service service : services) {
      for (Pattern pattern : patterns) {
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

  /**
   * Sets service resource service.
   *
   * @param serviceResourceService the service resource service
   */
  void setServiceResourceService(ServiceResourceService serviceResourceService) {
    this.serviceResourceService = serviceResourceService;
  }
}
