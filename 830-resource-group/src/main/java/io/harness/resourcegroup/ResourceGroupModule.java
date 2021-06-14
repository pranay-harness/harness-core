package io.harness.resourcegroup;

import static io.harness.AuthorizationServiceHeader.RESOUCE_GROUP_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.AccessControlAdminClientModule;
import io.harness.account.AccountClient;
import io.harness.account.AccountClientModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResourceClientModule;
import io.harness.delegate.DelegateServiceResourceClient;
import io.harness.delegate.DelegateServiceResourceClientModule;
import io.harness.environment.EnvironmentResourceClientModule;
import io.harness.organization.OrganizationClientModule;
import io.harness.organization.remote.OrganizationClient;
import io.harness.outbox.api.OutboxEventHandler;
import io.harness.outbox.api.OutboxService;
import io.harness.pipeline.PipelineRemoteClientModule;
import io.harness.pipeline.remote.PipelineServiceClient;
import io.harness.project.ProjectClientModule;
import io.harness.project.remote.ProjectClient;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.framework.service.ResourceTypeService;
import io.harness.resourcegroup.framework.service.impl.ResourceGroupEventHandler;
import io.harness.resourcegroup.framework.service.impl.ResourceGroupServiceImpl;
import io.harness.resourcegroup.framework.service.impl.ResourceGroupValidatorServiceImpl;
import io.harness.resourcegroup.framework.service.impl.ResourceTypeServiceImpl;
import io.harness.secrets.SecretNGManagerClientModule;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.service.ServiceResourceClientModule;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.reflections.Reflections;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@OwnedBy(PL)
public class ResourceGroupModule extends AbstractModule {
  public static final String RESOURCECLIENT_PACKAGE = "io.harness.resourcegroup.resourceclient";
  ResourceGroupServiceConfig resourceGroupServiceConfig;

  public ResourceGroupModule(ResourceGroupServiceConfig resourceGroupServiceConfig) {
    this.resourceGroupServiceConfig = resourceGroupServiceConfig;
  }

  @Override
  protected void configure() {
    install(new AccessControlAdminClientModule(
        resourceGroupServiceConfig.getAccessControlAdminClientConfiguration(), RESOUCE_GROUP_SERVICE.toString()));
    bind(ResourceGroupService.class).to(ResourceGroupServiceImpl.class);
    bind(ResourceTypeService.class).to(ResourceTypeServiceImpl.class);
    bind(String.class).annotatedWith(Names.named("serviceId")).toInstance(RESOUCE_GROUP_SERVICE.toString());
    bind(OutboxEventHandler.class).to(ResourceGroupEventHandler.class);
    requireBinding(OutboxService.class);
    installResourceValidators();
    addResourceValidatorConstraints();
  }

  @Provides
  public Map<String, Resource> getResourceMap(Injector injector) {
    Reflections reflections = new Reflections(RESOURCECLIENT_PACKAGE);
    Set<Class<? extends Resource>> resources = reflections.getSubTypesOf(Resource.class);
    Map<String, Resource> resourceMap = new HashMap<>();
    for (Class<? extends Resource> clz : resources) {
      Resource resource = injector.getInstance(clz);
      resourceMap.put(resource.getType(), resource);
    }
    return resourceMap;
  }

  private void addResourceValidatorConstraints() {
    requireBinding(ProjectClient.class);
    requireBinding(OrganizationClient.class);
    requireBinding(SecretNGManagerClient.class);
    requireBinding(ConnectorResourceClient.class);
    requireBinding(PipelineServiceClient.class);
    requireBinding(AccountClient.class);
    requireBinding(DelegateServiceResourceClient.class);
  }

  private void installResourceValidators() {
    io.harness.resourcegroup.ResourceClientConfigs resourceClients =
        resourceGroupServiceConfig.getResourceClientConfigs();
    install(new ProjectClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceClients.getNgManager().getBaseUrl()).build(),
        resourceClients.getNgManager().getSecret(), RESOUCE_GROUP_SERVICE.toString()));
    install(new OrganizationClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceClients.getNgManager().getBaseUrl()).build(),
        resourceClients.getNgManager().getSecret(), RESOUCE_GROUP_SERVICE.toString()));
    install(new SecretNGManagerClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceClients.getNgManager().getBaseUrl()).build(),
        resourceClients.getNgManager().getSecret(), RESOUCE_GROUP_SERVICE.toString()));
    install(new ConnectorResourceClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceClients.getNgManager().getBaseUrl()).build(),
        resourceClients.getNgManager().getSecret(), RESOUCE_GROUP_SERVICE.toString(), ClientMode.PRIVILEGED));
    install(new AccountClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceClients.getManager().getBaseUrl()).build(),
        resourceClients.getManager().getSecret(), RESOUCE_GROUP_SERVICE.toString()));
    install(new DelegateServiceResourceClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceClients.getManager().getBaseUrl()).build(),
        resourceClients.getManager().getSecret(), RESOUCE_GROUP_SERVICE.toString()));
    install(new PipelineRemoteClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceClients.getPipelineService().getBaseUrl()).build(),
        resourceClients.getPipelineService().getSecret(), RESOUCE_GROUP_SERVICE.toString()));
    install(new ServiceResourceClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceClients.getNgManager().getBaseUrl()).build(),
        resourceClients.getNgManager().getSecret(), RESOUCE_GROUP_SERVICE.toString()));
    install(new EnvironmentResourceClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceClients.getNgManager().getBaseUrl()).build(),
        resourceClients.getNgManager().getSecret(), RESOUCE_GROUP_SERVICE.toString()));
  }
}
