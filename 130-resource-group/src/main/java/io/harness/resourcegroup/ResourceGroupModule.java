package io.harness.resourcegroup;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResourceClientModule;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.ng.core.account.remote.AccountClient;
import io.harness.ng.core.account.remote.AccountClientModule;
import io.harness.organizationmanagerclient.OrganizationManagementClientModule;
import io.harness.organizationmanagerclient.remote.OrganizationManagerClient;
import io.harness.pipeline.PipelineRemoteClientModule;
import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.projectmanagerclient.ProjectManagementClientModule;
import io.harness.projectmanagerclient.remote.ProjectManagerClient;
import io.harness.redis.RedisConfig;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.resourcegroup.framework.beans.ResourceGroupConstants;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.framework.service.ResourceTypeService;
import io.harness.resourcegroup.framework.service.ResourceValidator;
import io.harness.resourcegroup.framework.service.impl.DynamicResourceGroupValidatorServiceImpl;
import io.harness.resourcegroup.framework.service.impl.ResourceGroupServiceImpl;
import io.harness.resourcegroup.framework.service.impl.ResourceTypeServiceImpl;
import io.harness.resourcegroup.framework.service.impl.StaticResourceGroupValidatorServiceImpl;
import io.harness.secrets.SecretNGManagerClientModule;
import io.harness.secrets.remote.SecretNGManagerClient;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.reflections.Reflections;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResourceGroupModule extends AbstractModule {
  private static final String RESOURCE_GROUP_CLIENT = "ng-manager";
  private static final String RESOURCE_GROUP_CONSUMER_GROUP = "resource-group";

  ResourceGroupConfig resourceGroupConfig;
  RedisConfig redisConfig;

  public ResourceGroupModule(ResourceGroupConfig resourceGroupConfig, RedisConfig redisConfig) {
    this.resourceGroupConfig = resourceGroupConfig;
    this.redisConfig = redisConfig;
  }

  @Override
  protected void configure() {
    bind(ResourceGroupService.class).to(ResourceGroupServiceImpl.class);
    bind(ResourceGroupValidatorService.class)
        .annotatedWith(Names.named("StaticResourceValidator"))
        .to(StaticResourceGroupValidatorServiceImpl.class);
    bind(ResourceTypeService.class).to(ResourceTypeServiceImpl.class);
    bind(ResourceGroupValidatorService.class)
        .annotatedWith(Names.named("DynamicResourceValidator"))
        .to(DynamicResourceGroupValidatorServiceImpl.class);
    bind(String.class).annotatedWith(Names.named("serviceId")).toInstance(NG_MANAGER.getServiceId());
    requireBinding(AccessControlAdminClient.class);
    installResourceValidators();
    addResourceValidatorConstraints();
  }

  @Provides
  @Named(ResourceGroupConstants.ENTITY_CRUD)
  Producer getProducer() {
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      return NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME);
    }
    return RedisProducer.of(
        EventsFrameworkConstants.ENTITY_CRUD, redisConfig, EventsFrameworkConstants.ENTITY_CRUD_MAX_TOPIC_SIZE);
  }

  @Provides
  @Named(ResourceGroupConstants.ENTITY_CRUD)
  Consumer getConsumer() {
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      return NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME);
    }
    return RedisConsumer.of(EventsFrameworkConstants.ENTITY_CRUD, RESOURCE_GROUP_CONSUMER_GROUP, redisConfig,
        EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME, EventsFrameworkConstants.ENTITY_CRUD_READ_BATCH_SIZE);
  }

  @Named("resourceValidatorMap")
  @Provides
  public Map<String, ResourceValidator> getResourceValidatorMap(Injector injector) {
    Reflections reflections = new Reflections("io.harness.resourcegroup.resourceclient");
    Set<Class<? extends ResourceValidator>> resourceValidators = reflections.getSubTypesOf(ResourceValidator.class);
    Map<String, ResourceValidator> resourceValidatorMap = new HashMap<>();
    for (Class<? extends ResourceValidator> clz : resourceValidators) {
      ResourceValidator resourceValidator = injector.getInstance(clz);
      resourceValidatorMap.put(resourceValidator.getResourceType(), resourceValidator);
    }
    return resourceValidatorMap;
  }

  private void addResourceValidatorConstraints() {
    requireBinding(SecretNGManagerClient.class);
    requireBinding(ProjectManagerClient.class);
    requireBinding(OrganizationManagerClient.class);
    requireBinding(ConnectorResourceClient.class);
    requireBinding(PipelineServiceClient.class);
    requireBinding(AccountClient.class);
  }

  private void installResourceValidators() {
    install(new ProjectManagementClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceGroupConfig.getNgManager().getBaseUrl()).build(),
        resourceGroupConfig.getNgManager().getSecret(), RESOURCE_GROUP_CLIENT));
    install(new OrganizationManagementClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceGroupConfig.getNgManager().getBaseUrl()).build(),
        resourceGroupConfig.getNgManager().getSecret(), RESOURCE_GROUP_CLIENT));
    install(new SecretNGManagerClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceGroupConfig.getNgManager().getBaseUrl()).build(),
        resourceGroupConfig.getNgManager().getSecret(), RESOURCE_GROUP_CLIENT));
    install(new ConnectorResourceClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceGroupConfig.getNgManager().getBaseUrl()).build(),
        resourceGroupConfig.getNgManager().getSecret(), RESOURCE_GROUP_CLIENT));
    install(new AccountClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceGroupConfig.getManager().getBaseUrl()).build(),
        resourceGroupConfig.getManager().getSecret(), RESOURCE_GROUP_CLIENT));
    install(new PipelineRemoteClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceGroupConfig.getPipelineService().getBaseUrl()).build(),
        resourceGroupConfig.getPipelineService().getSecret(), RESOURCE_GROUP_CLIENT));
  }
}
