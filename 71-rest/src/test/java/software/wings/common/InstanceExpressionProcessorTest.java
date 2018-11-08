package software.wings.common;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.mockChecker;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.limits.LimitCheckerFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.api.InstanceElement;
import software.wings.api.PartitionElement;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.BackgroundJobScheduler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;

/**
 * The Class InstanceExpressionProcessorTest.
 *
 * @author Rishi
 */
public class InstanceExpressionProcessorTest extends WingsBaseTest {
  private ServiceTemplate SERVICE_TEMPLATE =
      aServiceTemplate().withUuid(TEMPLATE_ID).withName("template").withServiceId(SERVICE_ID).build();
  /**
   * The Injector.
   */
  @Inject Injector injector;
  /**
   * The App service.
   */
  @Inject @InjectMocks AppService appService;
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
  @Inject ServiceTemplateService serviceTemplateService;
  /**
   * The Service template service mock.
   */
  @Mock ServiceTemplateService serviceTemplateServiceMock;
  /**
   * The Service resource service mock.
   */
  @Mock ServiceResourceService serviceResourceServiceMock;

  /**
   * The Settings service.
   */
  @Mock SettingsService settingsService;

  @Mock private BackgroundJobScheduler jobScheduler;

  /**
   * The Host service.
   */
  @Mock HostService hostService;
  @Inject private WingsPersistence wingsPersistence;

  @Mock private LimitCheckerFactory limitCheckerFactory;

  /**
   * Sets .
   */
  @Before
  public void setup() {
    when(settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.APP_DYNAMICS.name()))
        .thenReturn(Lists.newArrayList(aSettingAttribute().withUuid("id").build()));
    on(appService).set("settingsService", settingsService);
  }

  /**
   * Should return instances.
   */
  @Test
  @Ignore // Ignoring as instance without any filter is disabled
  public void shouldReturnInstances() {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    String appId = app.getUuid();
    Environment env = wingsPersistence.saveAndGet(Environment.class, anEnvironment().withAppId(app.getUuid()).build());

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getApp()).thenReturn(app);
    when(context.getEnv()).thenReturn(env);

    PageResponse<ServiceInstance> res = new PageResponse<>();
    ServiceInstance instance1 = aServiceInstance()
                                    .withUuid(generateUuid())
                                    .withHost(aHost().withHostName("host1").build())
                                    .withServiceTemplate(SERVICE_TEMPLATE)
                                    .build();
    ServiceInstance instance2 = aServiceInstance()
                                    .withUuid(generateUuid())
                                    .withHost(aHost().withHostName("host2").build())
                                    .withServiceTemplate(SERVICE_TEMPLATE)
                                    .build();
    ServiceInstance instance3 = aServiceInstance()
                                    .withUuid(generateUuid())
                                    .withHost(aHost().withHostName("host3").build())
                                    .withServiceTemplate(SERVICE_TEMPLATE)
                                    .build();

    List<ServiceInstance> instances = Lists.newArrayList(instance1, instance2, instance3);
    res.setResponse(instances);

    when(serviceInstanceServiceMock.list(any(PageRequest.class))).thenReturn(res);
    when(serviceTemplateServiceMock.list(any(PageRequest.class), eq(false), eq(OBTAIN_VALUE)))
        .thenReturn(new PageResponse<>());

    instances.forEach(instance
        -> when(hostService.getHostByEnv(anyString(), anyString(), eq(instance.getHostId())))
               .thenReturn(aHost().withHostName(instance.getHostName()).build()));
    SERVICE_TEMPLATE.setServiceId(SERVICE_ID);
    when(serviceTemplateServiceMock.get(anyString(), anyString(), anyString(), anyBoolean(), any()))
        .thenReturn(SERVICE_TEMPLATE);
    when(serviceResourceServiceMock.get(anyString(), anyString()))
        .thenReturn(Service.builder().uuid("uuid1").name("svc1").build());

    instances.forEach(instance
        -> when(hostService.getHostByEnv(anyString(), anyString(), eq(instance.getHostId())))
               .thenReturn(aHost().withHostName(instance.getHostName()).build()));

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);
    processor.setServiceInstanceService(serviceInstanceServiceMock);
    processor.setServiceTemplateService(serviceTemplateServiceMock);
    processor.setServiceResourceService(serviceResourceServiceMock);
    on(processor).set("hostService", hostService);

    List<InstanceElement> elements = processor.list();
    assertThat(elements).isNotNull().hasSize(3).doesNotContainNull().extracting("uuid").contains(
        instance1.getUuid(), instance2.getUuid(), instance3.getUuid());
    assertThat(elements).extracting("host").doesNotContainNull().extracting("uuid").contains(
        instance1.getHostId(), instance2.getHostId(), instance3.getHostId());
    assertThat(elements)
        .extracting("serviceTemplateElement")
        .doesNotContainNull()
        .extracting("uuid")
        .contains(instance1.getServiceTemplateId(), instance2.getServiceTemplateId(), instance3.getServiceTemplateId());
  }

  /**
   * Should return instances from param.
   */
  @Test
  public void shouldReturnInstancesFromParam() {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    String appId = app.getUuid();
    Environment env = wingsPersistence.saveAndGet(Environment.class, anEnvironment().withAppId(app.getUuid()).build());

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getApp()).thenReturn(app);
    when(context.getEnv()).thenReturn(env);

    String instance1 = generateUuid();
    String instance2 = generateUuid();

    ServiceInstanceIdsParam element = new ServiceInstanceIdsParam();
    element.setInstanceIds(Lists.newArrayList(instance1, instance2));

    List<ContextElement> paramList = Lists.newArrayList(element);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(paramList);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);
    when(serviceTemplateServiceMock.list(any(PageRequest.class), eq(false), eq(OBTAIN_VALUE)))
        .thenReturn(new PageResponse<>());
    processor.setServiceTemplateService(serviceTemplateServiceMock);
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

  /**
   * Should return common instances from param.
   */
  @Test
  public void shouldReturnCommonInstancesFromParam() {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    String appId = app.getUuid();
    Environment env = wingsPersistence.saveAndGet(Environment.class, anEnvironment().withAppId(app.getUuid()).build());

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getApp()).thenReturn(app);
    when(context.getEnv()).thenReturn(env);

    String instance1 = generateUuid();
    String instance2 = generateUuid();
    String instance3 = generateUuid();

    ServiceInstanceIdsParam element = new ServiceInstanceIdsParam();
    element.setInstanceIds(Lists.newArrayList(instance1, instance2));

    List<ContextElement> paramList = Lists.newArrayList(element);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(paramList);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);
    when(serviceTemplateServiceMock.list(any(PageRequest.class), eq(false), eq(OBTAIN_VALUE)))
        .thenReturn(new PageResponse<>());
    processor.setServiceTemplateService(serviceTemplateServiceMock);

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

  /**
   * Should return common instances from param 2.
   */
  @Test
  public void shouldReturnCommonInstancesFromParam2() {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    String appId = app.getUuid();
    Environment env = wingsPersistence.saveAndGet(Environment.class, anEnvironment().withAppId(app.getUuid()).build());

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getApp()).thenReturn(app);
    when(context.getEnv()).thenReturn(env);

    String instance1 = generateUuid();
    String instance2 = generateUuid();

    ServiceInstanceIdsParam element = new ServiceInstanceIdsParam();
    element.setInstanceIds(Lists.newArrayList(instance1, instance2));

    List<ContextElement> paramList = Lists.newArrayList(element);
    when(context.getContextElementList(ContextElementType.PARAM)).thenReturn(paramList);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);
    when(serviceTemplateServiceMock.list(any(PageRequest.class), eq(false), eq(OBTAIN_VALUE)))
        .thenReturn(new PageResponse<>());
    processor.setServiceTemplateService(serviceTemplateServiceMock);

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

  /**
   * Should fetch context element.
   */
  @Test
  public void shouldRenderExpressionFromInstanceElement() {
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setDisplayName("abc");
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    injector.injectMembers(context);

    when(limitCheckerFactory.getInstance(Mockito.any())).thenReturn(mockChecker());

    Application app = anApplication().withName("AppA").withAccountId(ACCOUNT_ID).build();
    app = appService.save(app);
    Environment env = wingsPersistence.saveAndGet(
        Environment.class, anEnvironment().withAppId(app.getUuid()).withName("DEV").build());
    Service service = wingsPersistence.saveAndGet(Service.class, Service.builder().name("svc1").build());

    ServiceTemplate serviceTemplate = serviceTemplateService.save(aServiceTemplate()
                                                                      .withAppId(app.getUuid())
                                                                      .withEnvId(env.getUuid())
                                                                      .withName("template")
                                                                      .withServiceId(service.getUuid())
                                                                      .build());

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());

    String timeStampId = std.getTimestampId();

    injector.injectMembers(std);
    context.pushContextElement(std);

    PageResponse<ServiceInstance> res = new PageResponse<>();
    ServiceInstance instance1 = aServiceInstance()
                                    .withUuid(generateUuid())
                                    .withAppId(app.getUuid())
                                    .withEnvId(env.getUuid())
                                    .withHost(aHost().withHostName("host1").build())
                                    .withServiceTemplate(serviceTemplate)
                                    .build();
    List<ServiceInstance> instances = Lists.newArrayList(instance1);
    res.setResponse(instances);

    ServiceInstanceIdsParam element = new ServiceInstanceIdsParam();
    element.setInstanceIds(Lists.newArrayList(instance1.getUuid()));
    context.pushContextElement(element);

    when(serviceInstanceServiceMock.list(any(PageRequest.class))).thenReturn(res);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);
    when(serviceTemplateServiceMock.list(any(PageRequest.class), eq(false), eq(OBTAIN_VALUE)))
        .thenReturn(new PageResponse<>());
    processor.setServiceTemplateService(serviceTemplateServiceMock);
    processor.setServiceInstanceService(serviceInstanceServiceMock);
    processor.setServiceResourceService(serviceResourceServiceMock);
    on(processor).set("hostService", hostService);

    when(serviceTemplateServiceMock.get(app.getAppId(), env.getUuid(), serviceTemplate.getUuid(), false, OBTAIN_VALUE))
        .thenReturn(serviceTemplate);
    when(serviceResourceServiceMock.get(anyString(), anyString()))
        .thenReturn(Service.builder().uuid("uuid1").name("svc1").build());

    instances.forEach(instance
        -> when(hostService.getHostByEnv(anyString(), anyString(), eq(instance.getHostId())))
               .thenReturn(aHost().withHostName(instance.getHostName()).build()));

    List<InstanceElement> elements = processor.list();
    assertThat(elements).isNotNull();
    assertThat(elements.size()).isEqualTo(1);

    context.pushContextElement(elements.get(0));

    String expr =
        "$HOME/${env.name}/${app.name}/${service.name}/${serviceTemplate.name}/${host.name}/${timestampId}/runtime";
    String path = context.renderExpression(expr);
    assertThat(path).isEqualTo("$HOME/DEV/AppA/svc1/template/host1/" + timeStampId + "/runtime");
  }

  /**
   * Should fetch context element.
   */
  @Test
  @Ignore
  public void shouldFetchInstanceElements() {
    Application app = anApplication().withName("AppA").withAccountId(ACCOUNT_ID).build();
    app = appService.save(app);
    Environment env = wingsPersistence.saveAndGet(
        Environment.class, Environment.Builder.anEnvironment().withAppId(app.getUuid()).build());
    Host applicationHost = wingsPersistence.saveAndGet(
        Host.class, aHost().withAppId(app.getAppId()).withEnvId(env.getUuid()).withHostName("host1").build());

    Service service = wingsPersistence.saveAndGet(
        Service.class, Service.builder().appId(app.getAppId()).uuid(generateUuid()).name("svc1").build());
    ServiceTemplate serviceTemplate = serviceTemplateService.save(aServiceTemplate()
                                                                      .withAppId(app.getUuid())
                                                                      .withEnvId(env.getUuid())
                                                                      .withServiceId(service.getUuid())
                                                                      .withName("TEMPLATE_NAME")
                                                                      .withDescription("TEMPLATE_DESCRIPTION")
                                                                      .build());

    ServiceInstance.Builder builder =
        aServiceInstance().withServiceTemplate(serviceTemplate).withAppId(app.getUuid()).withEnvId(env.getUuid());

    ServiceInstance instance1 = serviceInstanceService.save(builder.withHost(applicationHost).build());
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setDisplayName("abc");
    ExecutionContextImpl context = new ExecutionContextImpl(stateExecutionInstance);
    injector.injectMembers(context);

    WorkflowStandardParams std = new WorkflowStandardParams();
    std.setAppId(app.getUuid());
    std.setEnvId(env.getUuid());

    injector.injectMembers(std);
    context.pushContextElement(std);

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);
    processor.setServiceTemplateService(serviceTemplateService);
    processor.setServiceInstanceService(serviceInstanceServiceMock);

    on(processor).set("hostService", hostService);

    when(hostService.getHostByEnv(anyString(), anyString(), eq(instance1.getHostId())))
        .thenReturn(aHost().withHostName(instance1.getHostName()).build());

    String expr = "${instances}";
    List<InstanceElement> elements = (List<InstanceElement>) context.evaluateExpression(expr);
    assertThat(elements).isNotNull();
    assertThat(elements.size()).isEqualTo(1);
    assertThat(elements.get(0)).isNotNull();
    assertThat(elements.get(0).getUuid()).isEqualTo(instance1.getUuid());
    assertThat(elements.get(0).getDisplayName()).isEqualTo(instance1.getPublicDns());
  }

  /**
   * Should return from partition.
   */
  @Test
  public void shouldReturnInstancesFromPartition() {
    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
    String appId = app.getUuid();
    Environment env = wingsPersistence.saveAndGet(Environment.class, anEnvironment().withAppId(app.getUuid()).build());

    ExecutionContextImpl context = mock(ExecutionContextImpl.class);
    when(context.getApp()).thenReturn(app);
    when(context.getEnv()).thenReturn(env);
    InstanceElement i1 = anInstanceElement().withUuid(generateUuid()).build();
    InstanceElement i2 = anInstanceElement().withUuid(generateUuid()).build();
    InstanceElement i3 = anInstanceElement().withUuid(generateUuid()).build();
    PartitionElement pe = new PartitionElement();
    pe.setPartitionElements(Lists.newArrayList(i1, i2, i3));
    when(context.getContextElementList(ContextElementType.PARTITION)).thenReturn(Lists.newArrayList(pe));

    InstanceExpressionProcessor processor = new InstanceExpressionProcessor(context);
    List<InstanceElement> elements = processor.list();

    assertThat(elements).isNotNull().hasSize(3).doesNotContainNull().containsExactly(i1, i2, i3);
  }
}
