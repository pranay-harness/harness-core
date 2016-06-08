/**
 *
 */

package software.wings.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.common.collect.Lists;

import org.junit.Test;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.beans.SearchFilter;
import software.wings.beans.ServiceInstance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;

import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * The Class InstanceExpressionProcessorTest.
 *
 * @author Rishi
 */
public class InstanceExpressionProcessorTest {
  private String appId = UUIDGenerator.getUuid();

  /**
   * Should return instances.
   */
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

    List<InstanceElement> elements = processor.list();

    assertThat(elements).isNotNull();
    assertThat(elements.size()).isEqualTo(3);
    assertThat(elements.get(0)).isNotNull();
    assertThat(elements.get(0).getUuid()).isEqualTo(instance1.getUuid());
    assertThat(elements.get(1)).isNotNull();
    assertThat(elements.get(1).getUuid()).isEqualTo(instance2.getUuid());
    assertThat(elements.get(2)).isNotNull();
    assertThat(elements.get(2).getUuid()).isEqualTo(instance3.getUuid());
  }

  @Test
  public void shouldReturnInstancesFromParam() {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().withAppId(appId).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);

    String instance1 = UUIDGenerator.getUuid();
    String instance2 = UUIDGenerator.getUuid();

    ServiceInstanceIdsParam element = new ServiceInstanceIdsParam();
    element.setInstanceIds(Lists.newArrayList(instance1, instance2));

    List<ContextElement> paramList = Lists.newArrayList(element);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(paramList);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);

    PageRequest<ServiceInstance> pageRequest = processor.buildPageRequest();

    assertThat(pageRequest).isNotNull();
    assertThat(pageRequest.getFilters()).isNotNull();

    boolean appIdFound = false;
    boolean instanceIdsMatched = false;
    for (SearchFilter filter : pageRequest.getFilters()) {
      assertThat(filter.getFieldValues()).isNotEmpty();
      if (filter.getFieldName().equals("appId")) {
        assertThat(filter.getFieldValues()[0]).isEqualTo(appId);
        appIdFound = true;
      } else if (filter.getFieldName().equals(ID_KEY)) {
        assertThat(filter.getFieldValues().length).isEqualTo(2);
        assertThat(filter.getFieldValues()[0]).isEqualTo(instance1);
        assertThat(filter.getFieldValues()[1]).isEqualTo(instance2);
        instanceIdsMatched = true;
      }
    }
    assertThat(appIdFound).isTrue();
    assertThat(instanceIdsMatched).isTrue();
  }

  @Test
  public void shouldReturnCommonInstancesFromParam() {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().withAppId(appId).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);

    String instance1 = UUIDGenerator.getUuid();
    String instance2 = UUIDGenerator.getUuid();
    String instance3 = UUIDGenerator.getUuid();

    ServiceInstanceIdsParam element = new ServiceInstanceIdsParam();
    element.setInstanceIds(Lists.newArrayList(instance1, instance2));

    List<ContextElement> paramList = Lists.newArrayList(element);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(paramList);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);

    processor.withInstanceIds(instance1, instance2, instance3);
    PageRequest<ServiceInstance> pageRequest = processor.buildPageRequest();

    assertThat(pageRequest).isNotNull();
    assertThat(pageRequest.getFilters()).isNotNull();

    boolean appIdFound = false;
    boolean instanceIdsMatched = false;
    for (SearchFilter filter : pageRequest.getFilters()) {
      assertThat(filter.getFieldValues()).isNotEmpty();
      if (filter.getFieldName().equals("appId")) {
        assertThat(filter.getFieldValues()[0]).isEqualTo(appId);
        appIdFound = true;
      } else if (filter.getFieldName().equals(ID_KEY)) {
        assertThat(filter.getFieldValues().length).isEqualTo(2);
        assertThat(filter.getFieldValues()[0]).isIn(instance1, instance2);
        assertThat(filter.getFieldValues()[1]).isIn(instance1, instance2);
        instanceIdsMatched = true;
      }
    }
    assertThat(appIdFound).isTrue();
    assertThat(instanceIdsMatched).isTrue();
  }

  @Test
  public void shouldReturnCommonInstancesFromParam2() {
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().withAppId(appId).build();
    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);

    String instance1 = UUIDGenerator.getUuid();
    String instance2 = UUIDGenerator.getUuid();

    ServiceInstanceIdsParam element = new ServiceInstanceIdsParam();
    element.setInstanceIds(Lists.newArrayList(instance1, instance2));

    List<ContextElement> paramList = Lists.newArrayList(element);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(paramList);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);

    processor.withInstanceIds(instance1);
    PageRequest<ServiceInstance> pageRequest = processor.buildPageRequest();

    assertThat(pageRequest).isNotNull();
    assertThat(pageRequest.getFilters()).isNotNull();

    boolean appIdFound = false;
    boolean instanceIdsMatched = false;
    for (SearchFilter filter : pageRequest.getFilters()) {
      assertThat(filter.getFieldValues()).isNotEmpty();
      if (filter.getFieldName().equals("appId")) {
        assertThat(filter.getFieldValues()[0]).isEqualTo(appId);
        appIdFound = true;
      } else if (filter.getFieldName().equals(ID_KEY)) {
        assertThat(filter.getFieldValues().length).isEqualTo(1);
        assertThat(filter.getFieldValues()[0]).isIn(instance1);
        instanceIdsMatched = true;
      }
    }
    assertThat(appIdFound).isTrue();
    assertThat(instanceIdsMatched).isTrue();
  }
}
