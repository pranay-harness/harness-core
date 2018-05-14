package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.beans.InfrastructureMappingBlueprint.CloudProviderType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisionerDetails;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.sm.ExecutionContext;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.Map;
import javax.validation.Valid;

public interface InfrastructureProvisionerService extends OwnedByApplication {
  PageResponse<InfrastructureProvisioner> list(PageRequest<InfrastructureProvisioner> pageRequest);

  PageResponse<InfrastructureProvisioner> listForTask(@NotEmpty String appId, String infrastructureProvisionerType,
      String serviceId, DeploymentType deploymentType, CloudProviderType cloudProviderType);

  PageResponse<InfrastructureProvisionerDetails> listDetails(PageRequest<InfrastructureProvisioner> pageRequest);

  @ValidationGroups(Create.class)
  InfrastructureProvisioner save(@Valid InfrastructureProvisioner infrastructureProvisioner);

  @ValidationGroups(Update.class)
  InfrastructureProvisioner update(@Valid InfrastructureProvisioner infrastructureProvisioner);

  InfrastructureProvisioner get(String appId, String infrastructureProvisionerId);

  void delete(String appId, String infrastructureProvisionerId);

  void pruneDescendingEntities(String appId, String infrastructureProvisionerId);

  void regenerateInfrastructureMappings(String provisionerId, ExecutionContext context, Map<String, Object> outputs);
}
