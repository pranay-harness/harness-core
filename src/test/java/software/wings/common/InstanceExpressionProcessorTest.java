/**
 *
 */
package software.wings.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import org.junit.Test;
import software.wings.api.InstanceElement;
import software.wings.beans.ServiceInstance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;

import java.util.List;

/**
 * @author Rishi
 */
public class InstanceExpressionProcessorTest {
  private String appId = UUIDGenerator.getUuid();

  @Test
  public void shouldReturnInstances() {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().withAppId(appId).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);

    PageResponse<ServiceInstance> res = new PageResponse<>();
    ServiceInstance instance1 =
        ServiceInstance.ServiceInstanceBuilder.aServiceInstance().withUuid(UUIDGenerator.getUuid()).build();
    ServiceInstance instance2 =
        ServiceInstance.ServiceInstanceBuilder.aServiceInstance().withUuid(UUIDGenerator.getUuid()).build();
    ServiceInstance instance3 =
        ServiceInstance.ServiceInstanceBuilder.aServiceInstance().withUuid(UUIDGenerator.getUuid()).build();
    List<ServiceInstance> instances = Lists.newArrayList(instance1, instance2, instance3);
    res.setResponse(instances);

    ServiceInstanceService serviceInstanceService = mock(ServiceInstanceService.class);
    when(serviceInstanceService.list(any(PageRequest.class))).thenReturn(res);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);
    processor.setServiceInstanceService(serviceInstanceService);

    List<InstanceElement> elements = processor.lists();

    assertThat(elements).isNotNull();
    assertThat(elements.size()).isEqualTo(3);
    assertThat(elements.get(0)).isNotNull();
    assertThat(elements.get(0).getUuid()).isEqualTo(instance1.getUuid());
    assertThat(elements.get(1)).isNotNull();
    assertThat(elements.get(1).getUuid()).isEqualTo(instance2.getUuid());
    assertThat(elements.get(2)).isNotNull();
    assertThat(elements.get(2).getUuid()).isEqualTo(instance3.getUuid());
  }
}
