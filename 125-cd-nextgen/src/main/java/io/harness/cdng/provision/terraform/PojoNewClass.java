package io.harness.cdng.provision.terraform;

public class PojoNewClass {
  private String name;
  private TerraformPlanCommand terraformPlanCommand;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TerraformPlanCommand getTerraformPlanCommand() {
    return terraformPlanCommand;
  }

  public void setTerraformPlanCommand(TerraformPlanCommand terraformPlanCommand) {
    this.terraformPlanCommand = terraformPlanCommand;
  }
}
