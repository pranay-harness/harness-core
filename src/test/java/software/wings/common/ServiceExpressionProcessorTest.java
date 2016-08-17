/**
 *
 */

package software.wings.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.Lists;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
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
  private String appId = UUIDGenerator.getUuid();

  @Mock private ExecutionContextImpl context;

  @Mock private ServiceResourceService serviceResourceService;

  /**
   * Should return matching services.
   */
  @Test
  public void shouldReturnMatchingServices() {
    List<Service> services = Lists.newArrayList(aService().withName("A1234").build(),
        aService().withName("B1234").build(), aService().withName("C1234").build());

    when(context.getApp()).thenReturn(Application.Builder.anApplication().withUuid(appId).build());

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context);
    processor.setServiceResourceService(serviceResourceService);

    List<Service> matchingServices = processor.matchingServices(services, "A1234", "B1234");
    assertThat(matchingServices).isNotNull();
    assertThat(matchingServices.size()).isEqualTo(2);
    assertThat(matchingServices.get(0)).isNotNull();
    assertThat(matchingServices.get(0).getName()).isNotNull();
    assertThat(matchingServices.get(1)).isNotNull();
    assertThat(matchingServices.get(1).getName()).isNotNull();
    assertThat(matchingServices.get(0).getName()).isIn("A1234", "B1234");
    assertThat(matchingServices.get(1).getName()).isIn("A1234", "B1234");

    matchingServices = processor.matchingServices(services, "B*4", "C1234");
    assertThat(matchingServices).isNotNull();
    assertThat(matchingServices.size()).isEqualTo(2);
    assertThat(matchingServices.get(0)).isNotNull();
    assertThat(matchingServices.get(0).getName()).isNotNull();
    assertThat(matchingServices.get(1)).isNotNull();
    assertThat(matchingServices.get(1).getName()).isNotNull();
    assertThat(matchingServices.get(0).getName()).isIn("B1234", "C1234");
    assertThat(matchingServices.get(1).getName()).isIn("B1234", "C1234");
  }

  /**
   * Should return list all.
   */
  @Test
  public void shouldReturnListAll() {
    List<Service> services = Lists.newArrayList(aService().withName("A1234").build(),
        aService().withName("B1234").build(), aService().withName("C1234").build());

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getApp()).thenReturn(Application.Builder.anApplication().withUuid(appId).build());
    ServiceResourceService serviceResourceService = mock(ServiceResourceService.class);

    PageResponse<Service> res = new PageResponse<Service>();
    res.setResponse(services);

    when(serviceResourceService.list(any(PageRequest.class))).thenReturn(res);

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context);
    processor.setServiceResourceService(serviceResourceService);

    List<ServiceElement> matchingServices = processor.list();
    assertThat(matchingServices).isNotNull();
    assertThat(matchingServices.size()).isEqualTo(3);
    assertThat(matchingServices.get(0).getName()).isEqualTo("A1234");
    assertThat(matchingServices.get(1).getName()).isEqualTo("B1234");
    assertThat(matchingServices.get(2).getName()).isEqualTo("C1234");
  }

  /**
   * Should return list all from context.
   */
  @Test
  public void shouldReturnListAllFromContext() {
    Service serviceC = aService().withName("C1234").withUuid(SERVICE_ID).build();

    ServiceElement serviceCElement =
        aServiceElement().withName(serviceC.getName()).withUuid(serviceC.getUuid()).build();

    when(context.getApp()).thenReturn(Application.Builder.anApplication().withUuid(appId).build());
    when(context.getContextElement(ContextElementType.SERVICE)).thenReturn(serviceCElement);

    when(serviceResourceService.get(anyString(), anyString())).thenReturn(serviceC);

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context);
    processor.setServiceResourceService(serviceResourceService);

    List<ServiceElement> matchingServices = processor.list();
    assertThat(matchingServices).isNotNull();
    assertThat(matchingServices.size()).isEqualTo(1);
    assertThat(matchingServices.get(0).getName()).isEqualTo("C1234");
  }

  /**
   * Should return list some by name.
   */
  @Test
  public void shouldReturnListSomeByName() {
    List<Service> services = Lists.newArrayList(aService().withName("A1234").build(),
        aService().withName("B1234").build(), aService().withName("C1234").build());

    when(context.getApp()).thenReturn(Application.Builder.anApplication().withUuid(appId).build());

    PageResponse<Service> res = new PageResponse<Service>();
    res.setResponse(services);

    when(serviceResourceService.list(any(PageRequest.class))).thenReturn(res);

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context);
    processor.setServiceResourceService(serviceResourceService);

    List<ServiceElement> matchingServices = processor.withNames("B1234", "C12*").list();
    assertThat(matchingServices).isNotNull();
    assertThat(matchingServices.size()).isEqualTo(2);
    assertThat(matchingServices.get(0).getName()).isEqualTo("B1234");
    assertThat(matchingServices.get(1).getName()).isEqualTo("C1234");
  }

  /**
   * Should return not from context.
   */
  @Test
  public void shouldReturnNotFromContext() {
    Service serviceC = aService().withName("C1234").build();

    List<Service> services =
        Lists.newArrayList(aService().withName("A1234").build(), aService().withName("B1234").build(), serviceC);

    ServiceElement serviceCElement = new ServiceElement();
    serviceCElement.setName(serviceC.getName());

    when(context.getApp()).thenReturn(Application.Builder.anApplication().withUuid(appId).build());
    when(context.getContextElement(ContextElementType.SERVICE)).thenReturn(serviceCElement);

    PageResponse<Service> res = new PageResponse<Service>();
    res.setResponse(services);
    when(serviceResourceService.list(any(PageRequest.class))).thenReturn(res);

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context);
    processor.setServiceResourceService(serviceResourceService);

    List<ServiceElement> matchingServices = processor.withNames("A*").list();
    assertThat(matchingServices).isNotNull();
    assertThat(matchingServices.size()).isEqualTo(1);
    assertThat(matchingServices.get(0).getName()).isEqualTo("A1234");
  }
}
