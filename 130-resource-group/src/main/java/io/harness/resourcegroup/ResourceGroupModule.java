package io.harness.resourcegroup;

import io.harness.organizationmanagerclient.OrganizationManagementClientModule;
import io.harness.organizationmanagerclient.remote.OrganizationManagerClient;
import io.harness.projectmanagerclient.ProjectManagementClientModule;
import io.harness.projectmanagerclient.remote.ProjectManagerClient;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.framework.service.ResourceGroupValidatorService;
import io.harness.resourcegroup.framework.service.ResourceTypeService;
import io.harness.resourcegroup.framework.service.impl.DynamicResourceGroupValidatorServiceImpl;
import io.harness.resourcegroup.framework.service.impl.ResourceGroupServiceImpl;
import io.harness.resourcegroup.framework.service.impl.ResourceTypeServiceImpl;
import io.harness.resourcegroup.framework.service.impl.StaticResourceGroupValidatorServiceImpl;
import io.harness.resourcegroup.resourceclient.api.ResourceValidator;
import io.harness.secretmanagerclient.remote.SecretManagerClient;

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
  ResourceGroupConfig resourceGroupConfig;

  public ResourceGroupModule(ResourceGroupConfig resourceGroupConfig) {
    this.resourceGroupConfig = resourceGroupConfig;
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
    installResourceValidators();
    addResourceValidatorConstraints();
  }

  private void addResourceValidatorConstraints() {
    requireBinding(SecretManagerClient.class);
    requireBinding(ProjectManagerClient.class);
    requireBinding(OrganizationManagerClient.class);
  }

  private void installResourceValidators() {
    install(new ProjectManagementClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceGroupConfig.getNgManager().getBaseUrl()).build(),
        resourceGroupConfig.getNgManager().getSecret(), RESOURCE_GROUP_CLIENT));
    install(new OrganizationManagementClientModule(
        ServiceHttpClientConfig.builder().baseUrl(resourceGroupConfig.getNgManager().getBaseUrl()).build(),
        resourceGroupConfig.getNgManager().getSecret(), RESOURCE_GROUP_CLIENT));
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
}
