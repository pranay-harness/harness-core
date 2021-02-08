package io.harness.resourcegroup;

import io.harness.organizationmanagerclient.OrganizationManagementClientModule;
import io.harness.projectmanagerclient.ProjectManagementClientModule;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.resourcegroup.model.ResourceType;
import io.harness.resourcegroup.resource.client.organization.OrganizationResourceValidatorImpl;
import io.harness.resourcegroup.resource.client.project.ProjectResourceValidatorImpl;
import io.harness.resourcegroup.resource.client.secretmanager.SecretManagerResourceValidatorImpl;
import io.harness.resourcegroup.resource.validator.ResourceGroupValidatorService;
import io.harness.resourcegroup.resource.validator.ResourceValidator;
import io.harness.resourcegroup.resource.validator.impl.DynamicResourceGroupValidatorServiceImpl;
import io.harness.resourcegroup.resource.validator.impl.StaticResourceGroupValidatorServiceImpl;
import io.harness.resourcegroup.service.ResourceGroupService;
import io.harness.resourcegroup.service.ResourceTypeService;
import io.harness.resourcegroup.service.impl.ResourceGroupServiceImpl;
import io.harness.resourcegroup.service.impl.ResourceTypeServiceImpl;
import io.harness.secretmanagerclient.remote.SecretManagerClient;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.EnumMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResourceGroupModule extends AbstractModule {
  private static final String RESOURCE_GROUP_CLIENT = "ng-manager";
  ResourceGroupConfig resourceGroupConfig;

  public ResourceGroupModule(ResourceGroupConfig resourceGroupConfig) {
    this.resourceGroupConfig = resourceGroupConfig;
  }

  @Override
  protected void configure() {
    install(new ProjectManagementClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceGroupConfig.getNgManager().getBaseUrl()).build(),
        resourceGroupConfig.getNgManager().getSecret(), RESOURCE_GROUP_CLIENT));
    install(new OrganizationManagementClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceGroupConfig.getNgManager().getBaseUrl()).build(),
        resourceGroupConfig.getNgManager().getSecret(), RESOURCE_GROUP_CLIENT));
    bind(ResourceGroupService.class).to(ResourceGroupServiceImpl.class);
    bind(ResourceGroupValidatorService.class)
        .annotatedWith(Names.named("StaticResourceValidator"))
        .to(StaticResourceGroupValidatorServiceImpl.class);
    bind(ResourceTypeService.class).to(ResourceTypeServiceImpl.class);
    bind(ResourceGroupValidatorService.class)
        .annotatedWith(Names.named("DynamicResourceValidator"))
        .to(DynamicResourceGroupValidatorServiceImpl.class);
    requireBinding(SecretManagerClient.class);
  }

  @Named("resourceValidatorMap")
  @Provides
  public Map<ResourceType, ResourceValidator> getResourceValidatorMap(Injector injector) {
    Map<ResourceType, ResourceValidator> resourceValidators = new EnumMap<>(ResourceType.class);
    resourceValidators.put(ResourceType.PROJECT, injector.getInstance(ProjectResourceValidatorImpl.class));
    resourceValidators.put(ResourceType.ORGANIZATION, injector.getInstance(OrganizationResourceValidatorImpl.class));
    resourceValidators.put(ResourceType.SECRET_MANAGER, injector.getInstance(SecretManagerResourceValidatorImpl.class));
    return resourceValidators;
  }
}
