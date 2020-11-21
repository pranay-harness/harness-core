package software.wings.beans;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InfrastructureProvisionerDetails {
  private String uuid;
  private String name;
  private String description;
  private String infrastructureProvisionerType;
  private String repository;
  private Map<String, String> services;
  private String cloudFormationSourceType;
  private transient List<HarnessTagLink> tagLinks;
}
