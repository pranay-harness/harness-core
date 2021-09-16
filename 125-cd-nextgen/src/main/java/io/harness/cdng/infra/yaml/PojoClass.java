package io.harness.cdng.infra.yaml;

public class PojoClass {
  private String name;
  private InfrastructureType infrastructureType;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public InfrastructureType getInfrastructureType() {
    return infrastructureType;
  }

  public void setInfrastructureType(InfrastructureType infrastructureType) {
    this.infrastructureType = infrastructureType;
  }
}