package software.wings.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.model.ARMResourceType;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
public class InfrastructureProvisionerDetails {
  private String uuid;
  private String name;
  private String description;
  private String infrastructureProvisionerType;
  private String repository;
  private Map<String, String> services;
  private String cloudFormationSourceType;
  private ARMResourceType azureARMResourceType;
  private transient List<HarnessTagLink> tagLinks;
}
