package software.wings.common;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public interface ProvisionerConstants {
  String ROLLBACK_TERRAFORM_NAME = "Terraform Rollback";
  String PROVISION_SHELL_SCRIPT = "Shell Script Provision";
  String DE_PROVISION_CLOUD_FORMATION = "CloudFormation Delete Stack";
  String PROVISION_CLOUD_FORMATION = "CloudFormation Create Stack";
  String ROLLBACK_CLOUD_FORMATION = "CloudFormation Rollback Stack";
  String ARM_ROLLBACK = "ARM Rollback";
  String ROLLBACK_TERRAGRUNT_NAME = "Terragrunt Rollback";
  String DESTROY_TERRAGRUNT_NAME = "Terragrunt Destroy";
  String PROVISION_TERRAGRUNT_NAME = "Terragrunt Provision";
  String GENERIC_ROLLBACK_NAME_FORMAT = "Rollback %s";
}
