package io.harness.cdng.provision.terraform;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.yaml.BitbucketStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TerraformPlanStepInfoTest extends CategoryTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateParams() {
    TerraformPlanStepInfo terraformPlanStepInfo = new TerraformPlanStepInfo();
    Assertions.assertThatThrownBy(terraformPlanStepInfo::validateSpecParams)
        .hasMessageContaining("Terraform Plan configuration is NULL");

    TerraformPlanExecutionData terraformPlanExecutionData = new TerraformPlanExecutionData();
    terraformPlanStepInfo.setTerraformPlanExecutionData(terraformPlanExecutionData);
    Assertions.assertThatThrownBy(terraformPlanStepInfo::validateSpecParams)
        .hasMessageContaining("Config files are null");

    TerraformConfigFilesWrapper terraformConfigFilesWrapper = new TerraformConfigFilesWrapper();
    terraformPlanExecutionData.setTerraformConfigFilesWrapper(terraformConfigFilesWrapper);
    Assertions.assertThatThrownBy(terraformPlanStepInfo::validateSpecParams)
        .hasMessageContaining("Store cannot be null in Config Files");

    StoreConfigWrapper storeConfigWrapper =
        StoreConfigWrapper.builder().type(StoreConfigType.BITBUCKET).spec(BitbucketStore.builder().build()).build();
    terraformConfigFilesWrapper.setStore(storeConfigWrapper);
    Assertions.assertThatThrownBy(terraformPlanStepInfo::validateSpecParams)
        .hasMessageContaining("Terraform Plan command is null");

    terraformPlanExecutionData.setCommand(TerraformPlanCommand.APPLY);
    Assertions.assertThatThrownBy(terraformPlanStepInfo::validateSpecParams)
        .hasMessageContaining("Secret Manager Ref for Tf plan is null");

    // should validate successfully
    terraformPlanExecutionData.setSecretManagerRef(ParameterField.createValueField("KMS"));
    terraformPlanStepInfo.validateSpecParams();
  }
}
