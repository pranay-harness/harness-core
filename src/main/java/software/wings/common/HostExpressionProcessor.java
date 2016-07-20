/**
 *
 */

package software.wings.common;

import software.wings.api.HostElement;
import software.wings.beans.Host;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.service.intfc.HostService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExpressionProcessor;
import software.wings.utils.MapperUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

/**
 * The Class HostExpressionProcessor.
 *
 * @author Rishi
 */
public class HostExpressionProcessor implements ExpressionProcessor {
  /**
   * The Expression start pattern.
   */
  public static final String DEFAULT_EXPRESSION = "${hosts}";

  private static final String EXPRESSION_START_PATTERN = "hosts()";
  private static final String EXPRESSION_EQUAL_PATTERN = "hosts";
  private static final String HOST_EXPR_PROCESSOR = "hostExpressionProcessor";
  @Inject private HostService hostService;
  private String[] hostNames;
  private String appId;

  /**
   * Instantiates a new host expression processor.
   *
   * @param context the context
   */
  public HostExpressionProcessor(ExecutionContext context) {
    // Derive appId, serviceId, serviceTemplate and tags associated from the context
  }

  /**
   * Convert to host element host element.
   *
   * @param host the host
   * @return the host element
   */
  static HostElement convertToHostElement(Host host) {
    HostElement element = new HostElement();
    MapperUtils.mapObject(host, element);
    return element;
  }

  @Override
  public String getPrefixObjectName() {
    return HOST_EXPR_PROCESSOR;
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
    return ContextElementType.HOST;
  }

  /**
   * Gets hosts.
   *
   * @return the hosts
   */
  public HostExpressionProcessor getHosts() {
    return this;
  }

  /**
   * Hosts.
   *
   * @param hostNames the host names
   * @return the host expression processor
   */
  public HostExpressionProcessor hosts(String... hostNames) {
    this.hostNames = hostNames;
    return this;
  }

  /**
   * With names.
   *
   * @param hostNames the host names
   * @return the host expression processor
   */
  public HostExpressionProcessor withNames(String... hostNames) {
    this.hostNames = hostNames;
    return this;
  }

  /**
   * List.
   *
   * @return the list
   */
  public List<HostElement> list() {
    List<Host> hosts = null;
    PageRequest<Host> pageRequest =
        PageRequest.Builder.aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(SearchFilter.Builder.aSearchFilter().withField("appId", Operator.EQ, appId).build())
            .build();
    if (hostNames == null || hostNames.length == 0) {
      hosts = hostService.list(pageRequest);
    } else {
      // TODO :
    }

    return convertToHostElements(hosts);
  }

  private List<HostElement> convertToHostElements(List<Host> hosts) {
    if (hosts == null) {
      return null;
    }
    List<HostElement> hostElements = new ArrayList<>();
    for (Host host : hosts) {
      hostElements.add(convertToHostElement(host));
    }
    return hostElements;
  }
}
