package software.wings.beans;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

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
}
