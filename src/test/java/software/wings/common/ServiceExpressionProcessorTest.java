/**
 *
 */

package software.wings.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jersey.repackaged.com.google.common.collect.Lists;
import org.junit.Test;
import software.wings.api.ServiceElement;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;

import java.util.List;

/**
 * @author Rishi
 */
public class ServiceExpressionProcessorTest {
  private String appId = UUIDGenerator.getUuid();

  @Test
  public void shouldReturnMatchingServices() {
    List<Service> services = Lists.newArrayList(Service.Builder.aService().withName("A1234").build(),
        Service.Builder.aService().withName("B1234").build(), Service.Builder.aService().withName("C1234").build());

    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().withAppId(appId).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);
    ServiceResourceService serviceResourceService = mock(ServiceResourceService.class);

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context, serviceResourceService);

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

  @Test
  public void shouldReturnListAll() {
    List<Service> services = Lists.newArrayList(Service.Builder.aService().withName("A1234").build(),
        Service.Builder.aService().withName("B1234").build(), Service.Builder.aService().withName("C1234").build());

    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().withAppId(appId).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);
    ServiceResourceService serviceResourceService = mock(ServiceResourceService.class);

    PageResponse<Service> res = new PageResponse<Service>();
    res.setResponse(services);

    when(serviceResourceService.list(any(PageRequest.class))).thenReturn(res);

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context, serviceResourceService);
    List<ServiceElement> matchingServices = processor.list();
    assertThat(matchingServices).isNotNull();
    assertThat(matchingServices.size()).isEqualTo(3);
    assertThat(matchingServices.get(0).getName()).isEqualTo("A1234");
    assertThat(matchingServices.get(1).getName()).isEqualTo("B1234");
    assertThat(matchingServices.get(2).getName()).isEqualTo("C1234");
  }

  @Test
  public void shouldReturnListSomeByName() {
    List<Service> services = Lists.newArrayList(Service.Builder.aService().withName("A1234").build(),
        Service.Builder.aService().withName("B1234").build(), Service.Builder.aService().withName("C1234").build());

    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().withAppId(appId).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);
    ServiceResourceService serviceResourceService = mock(ServiceResourceService.class);

    PageResponse<Service> res = new PageResponse<Service>();
    res.setResponse(services);

    when(serviceResourceService.list(any(PageRequest.class))).thenReturn(res);

    ServiceExpressionProcessor processor = new ServiceExpressionProcessor(context, serviceResourceService);
    List<ServiceElement> matchingServices = processor.withNames("B1234", "C12*").list();
    assertThat(matchingServices).isNotNull();
    assertThat(matchingServices.size()).isEqualTo(2);
    assertThat(matchingServices.get(0).getName()).isEqualTo("B1234");
    assertThat(matchingServices.get(1).getName()).isEqualTo("C1234");
  }
}
