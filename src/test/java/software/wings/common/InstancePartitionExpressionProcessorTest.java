/**
 *
 */

package software.wings.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;

import com.google.common.collect.Lists;
import com.google.inject.Injector;

import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PartitionElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Host.Builder;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContextImpl;

import java.util.List;
import javax.inject.Inject;

/**
 * The Class InstanceExpressionProcessorTest.
 *
 * @author Rishi
 */
public class InstancePartitionExpressionProcessorTest extends WingsBaseTest {
  /**
   * The Injector.
   */
  @Inject Injector injector;
  /**
   * The App service.
   */
  @Inject AppService appService;
  /**
   * The Environment service.
   */
  @Inject EnvironmentService environmentService;

  /**
   * The Service instance service.
   */
  @Inject ServiceInstanceService serviceInstanceService;

  /**
   * The Service instance service mock.
   */
  @Mock ServiceInstanceService serviceInstanceServiceMock;
  /**
   * The Service template service mock.
   */
  @Mock ServiceTemplateService serviceTemplateServiceMock;
  @Inject private WingsPersistence wingsPersistence;

  /**
   * Should partition.
   */
  @Test
  public void shouldPartitionInstances() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    String appId = app.getUuid();
    Environment env = wingsPersistence.saveAndGet(Environment.class, anEnvironment().withAppId(app.getUuid()).build());

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getApp()).thenReturn(app);
    when(context.getEnv()).thenReturn(env);

    PageResponse<ServiceInstance> res = new PageResponse<>();
    ServiceInstance instance1 =
        aServiceInstance()
            .withUuid(UUIDGenerator.getUuid())
            .withHost(Builder.aHost().withHostName("host1").build())
            .withServiceTemplate(aServiceTemplate()
                                     .withName("template")
                                     .withService(Service.Builder.aService().withUuid("uuid1").withName("svc1").build())
                                     .build())
            .build();
    ServiceInstance instance2 =
        aServiceInstance()
            .withUuid(UUIDGenerator.getUuid())
            .withHost(Builder.aHost().withHostName("host2").build())
            .withServiceTemplate(aServiceTemplate()
                                     .withName("template")
                                     .withService(Service.Builder.aService().withUuid("uuid1").withName("svc1").build())
                                     .build())
            .build();
    ServiceInstance instance3 =
        aServiceInstance()
            .withUuid(UUIDGenerator.getUuid())
            .withHost(Builder.aHost().withHostName("host3").build())
            .withServiceTemplate(aServiceTemplate()
                                     .withName("template")
                                     .withService(Service.Builder.aService().withUuid("uuid1").withName("svc1").build())
                                     .build())
            .build();
    ServiceInstance instance4 =
        aServiceInstance()
            .withUuid(UUIDGenerator.getUuid())
            .withHost(Builder.aHost().withHostName("host3").build())
            .withServiceTemplate(aServiceTemplate()
                                     .withName("template")
                                     .withService(Service.Builder.aService().withUuid("uuid1").withName("svc1").build())
                                     .build())
            .build();
    ServiceInstance instance5 =
        aServiceInstance()
            .withUuid(UUIDGenerator.getUuid())
            .withHost(Builder.aHost().withHostName("host3").build())
            .withServiceTemplate(aServiceTemplate()
                                     .withName("template")
                                     .withService(Service.Builder.aService().withUuid("uuid1").withName("svc1").build())
                                     .build())
            .build();
    ServiceInstance instance6 =
        aServiceInstance()
            .withUuid(UUIDGenerator.getUuid())
            .withHost(Builder.aHost().withHostName("host3").build())
            .withServiceTemplate(aServiceTemplate()
                                     .withName("template")
                                     .withService(Service.Builder.aService().withUuid("uuid1").withName("svc1").build())
                                     .build())
            .build();
    ServiceInstance instance7 =
        aServiceInstance()
            .withUuid(UUIDGenerator.getUuid())
            .withHost(Builder.aHost().withHostName("host3").build())
            .withServiceTemplate(aServiceTemplate()
                                     .withName("template")
                                     .withService(Service.Builder.aService().withUuid("uuid1").withName("svc1").build())
                                     .build())
            .build();
    List<ServiceInstance> instances =
        Lists.newArrayList(instance1, instance2, instance3, instance4, instance5, instance6, instance7);
    res.setResponse(instances);

    when(serviceInstanceServiceMock.list(any(PageRequest.class))).thenReturn(res);
    when(serviceTemplateServiceMock.list(any(PageRequest.class))).thenReturn(new PageResponse<>());

    InstancePartitionExpressionProcessor processor = new InstancePartitionExpressionProcessor(context);
    processor.setServiceInstanceService(serviceInstanceServiceMock);
    processor.setServiceTemplateService(serviceTemplateServiceMock);

    List<PartitionElement> partitions = processor.partitions("2", "30 %", "50 %");
    assertThat(partitions).isNotNull().hasSize(3).doesNotContainNull();
    assertThat(partitions.get(0).getPartitionElements()).hasSize(2).doesNotContainNull();
    assertThat(partitions.get(1).getPartitionElements()).hasSize(2).doesNotContainNull();
    assertThat(partitions.get(2).getPartitionElements()).hasSize(3).doesNotContainNull();

    String[] instanceIds = new String[] {instance1.getUuid(), instance2.getUuid(), instance3.getUuid(),
        instance4.getUuid(), instance5.getUuid(), instance6.getUuid(), instance7.getUuid()};
    assertThat(partitions.get(0).getPartitionElements()).extracting(ContextElement::getUuid).isSubsetOf(instanceIds);
    assertThat(partitions.get(1).getPartitionElements()).extracting(ContextElement::getUuid).isSubsetOf(instanceIds);
    assertThat(partitions.get(2).getPartitionElements()).extracting(ContextElement::getUuid).isSubsetOf(instanceIds);
  }
}
