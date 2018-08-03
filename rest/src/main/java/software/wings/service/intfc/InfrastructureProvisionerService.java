package software.wings.service.intfc;

import io.harness.validation.Create;
import io.harness.validation.Update;
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
import software.wings.sm.states.ManagerExecutionLogCallback;

import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;

public interface InfrastructureProvisionerService extends OwnedByApplication {
  PageResponse<InfrastructureProvisioner> list(PageRequest<InfrastructureProvisioner> pageRequest);

  PageResponse<InfrastructureProvisioner> listByBlueprintDetails(@NotEmpty String appId,
      String infrastructureProvisionerType, String serviceId, DeploymentType deploymentType,
      CloudProviderType cloudProviderType);

  PageResponse<InfrastructureProvisionerDetails> listDetails(PageRequest<InfrastructureProvisioner> pageRequest);

  @ValidationGroups(Create.class)
  InfrastructureProvisioner save(@Valid InfrastructureProvisioner infrastructureProvisioner);

  @ValidationGroups(Update.class)
  InfrastructureProvisioner update(@Valid InfrastructureProvisioner infrastructureProvisioner);

  InfrastructureProvisioner get(String appId, String infrastructureProvisionerId);
  InfrastructureProvisioner getByName(String appId, String provisionerName);

  void delete(String appId, String infrastructureProvisionerId);

  void pruneDescendingEntities(String appId, String infrastructureProvisionerId);

  void regenerateInfrastructureMappings(String provisionerId, ExecutionContext context, Map<String, Object> outputs);

  void regenerateInfrastructureMappings(String provisionerId, ExecutionContext context, Map<String, Object> outputs,
      Optional<ManagerExecutionLogCallback> executionLogCallback, Optional<String> region);
}
