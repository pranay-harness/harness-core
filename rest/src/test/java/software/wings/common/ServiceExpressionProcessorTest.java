/**
 *
 */

package software.wings.common;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.Lists;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;

import java.util.List;

/**
 * The Class ServiceExpressionProcessorTest.
 *
 * @author Rishi
 */
public class ServiceExpressionProcessorTest {
  /**
   * The Mockito rule.
   */
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  private String appId = generateUuid();

  @Mock private ExecutionContextImpl context;

  @Mock private ServiceResourceService serviceResourceService;

  /**
   * Should return matching services.
   */
  @Test
  public void shouldReturnMatchingServices() {
    List<ServiceElement> services = Lists.newArrayList(aServiceElement().withName("A1234").build(),
        aServiceElement().withName("B1234").build(), aServiceElement().withName("C1234").build());

    when(context.getApp()).thenReturn(Application.Builder.anApplication().withUuid(appId).build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(aWorkflowStandardParams().build());

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context);
    processor.setServiceResourceService(serviceResourceService);

    List<ServiceElement> matchingServices = processor.matchingServices(services, "A1234", "B1234");
    assertThat(matchingServices)
        .isNotNull()
        .hasSize(2)
        .doesNotContainNull()
        .extracting("name")
        .containsExactly("A1234", "B1234");

    matchingServices = processor.matchingServices(services, "B*4", "C1234");
    assertThat(matchingServices)
        .isNotNull()
        .hasSize(2)
        .doesNotContainNull()
        .extracting("name")
        .containsExactly("B1234", "C1234");
  }

  /**
   * Should return list all.
   */
  @Test
  public void shouldReturnListAll() {
    List<Service> services = Lists.newArrayList(Service.builder().name("A1234").build(),
        Service.builder().name("B1234").build(), Service.builder().name("C1234").build());

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getApp()).thenReturn(Application.Builder.anApplication().withUuid(appId).build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(aWorkflowStandardParams().build());

    ServiceResourceService serviceResourceService = mock(ServiceResourceService.class);

    PageResponse<Service> res = new PageResponse<>();
    res.setResponse(services);

    when(serviceResourceService.list(any(PageRequest.class), eq(false), eq(true))).thenReturn(res);

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context);
    processor.setServiceResourceService(serviceResourceService);

    List<ServiceElement> matchingServices = processor.list();
    assertThat(matchingServices)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting("name")
        .contains("A1234", "B1234", "C1234");
  }

  /**
   * Should return list all from context.
   */
  @Test
  public void shouldReturnListAllFromContext() {
    Service serviceC = Service.builder().name("C1234").uuid(SERVICE_ID).build();

    ServiceElement serviceCElement =
        aServiceElement().withName(serviceC.getName()).withUuid(serviceC.getUuid()).build();

    when(context.getApp()).thenReturn(Application.Builder.anApplication().withUuid(appId).build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(aWorkflowStandardParams().build());
    when(context.getContextElement(ContextElementType.SERVICE)).thenReturn(serviceCElement);

    when(serviceResourceService.get(anyString(), anyString())).thenReturn(serviceC);

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context);
    processor.setServiceResourceService(serviceResourceService);

    List<ServiceElement> matchingServices = processor.list();
    assertThat(matchingServices).isNotNull().hasSize(1).doesNotContainNull().extracting("name").contains("C1234");
  }

  /**
   * Should return list some by name.
   */
  @Test
  public void shouldReturnListSomeByName() {
    List<Service> services = Lists.newArrayList(Service.builder().name("A1234").build(),
        Service.builder().name("B1234").build(), Service.builder().name("C1234").build());

    when(context.getApp()).thenReturn(Application.Builder.anApplication().withUuid(appId).build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(aWorkflowStandardParams().build());

    PageResponse<Service> res = new PageResponse<>();
    res.setResponse(services);

    when(serviceResourceService.list(any(PageRequest.class), eq(false), eq(true))).thenReturn(res);

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context);
    processor.setServiceResourceService(serviceResourceService);

    List<ServiceElement> matchingServices = processor.withNames("B1234", "C12*").list();
    assertThat(matchingServices)
        .isNotNull()
        .hasSize(2)
        .doesNotContainNull()
        .extracting("name")
        .containsExactly("B1234", "C1234");
  }

  /**
   * Should return list some by name.
   */
  @Test
  public void shouldReturnSelectedListSomeByName() {
    when(context.getApp()).thenReturn(Application.Builder.anApplication().withUuid(appId).build());
    when(context.getContextElement(ContextElementType.STANDARD))
        .thenReturn(aWorkflowStandardParams()
                        .withServices(Lists.newArrayList(aServiceElement().withName("A1234").build(),
                            aServiceElement().withName("B1234").build(), aServiceElement().withName("C1234").build()))
                        .build());

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context);
    List<ServiceElement> matchingServices = processor.withNames("B1234", "C12*").list();
    assertThat(matchingServices)
        .isNotNull()
        .hasSize(2)
        .doesNotContainNull()
        .extracting("name")
        .contains("B1234", "C1234");
  }

  /**
   * Should return not from context.
   */
  @Test
  public void shouldReturnNotFromContext() {
    Service serviceC = Service.builder().name("C1234").build();

    List<Service> services =
        Lists.newArrayList(Service.builder().name("A1234").build(), Service.builder().name("B1234").build(), serviceC);

    ServiceElement serviceCElement = new ServiceElement();
    serviceCElement.setName(serviceC.getName());

    when(context.getApp()).thenReturn(Application.Builder.anApplication().withUuid(appId).build());
    when(context.getContextElement(ContextElementType.SERVICE)).thenReturn(serviceCElement);
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(aWorkflowStandardParams().build());

    PageResponse<Service> res = new PageResponse<>();
    res.setResponse(services);
    when(serviceResourceService.list(any(PageRequest.class), eq(false), eq(true))).thenReturn(res);

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context);
    processor.setServiceResourceService(serviceResourceService);

    List<ServiceElement> matchingServices = processor.withNames("A*").list();
    assertThat(matchingServices).isNotNull().hasSize(1).doesNotContainNull().extracting("name").contains("A1234");
  }
}
