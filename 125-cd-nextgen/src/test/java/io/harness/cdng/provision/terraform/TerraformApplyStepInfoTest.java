package io.harness.cdng.provision.terraform;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.yaml.BitbucketStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.rule.Owner;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TerraformApplyStepInfoTest extends CategoryTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateParams() {
    TerraformApplyStepInfo terraformApplyStepInfo = new TerraformApplyStepInfo();
    Assertions.assertThatThrownBy(terraformApplyStepInfo::validateSpecParams)
        .hasMessageContaining("Terraform Step configuration is null");

    TerraformStepConfiguration terraformStepConfiguration = new TerraformStepConfiguration();
    terraformApplyStepInfo.setTerraformStepConfiguration(terraformStepConfiguration);
    Assertions.assertThatThrownBy(terraformApplyStepInfo::validateSpecParams)
        .hasMessageContaining("Step Configuration Type is null");

    terraformStepConfiguration.setTerraformStepConfigurationType(TerraformStepConfigurationType.INHERIT_FROM_APPLY);
    terraformApplyStepInfo.validateSpecParams();

    terraformStepConfiguration.setTerraformStepConfigurationType(TerraformStepConfigurationType.INLINE);
    Assertions.assertThatThrownBy(terraformApplyStepInfo::validateSpecParams)
        .hasMessageContaining("Spec inside Configuration cannot be null");

    TerraformExecutionData terraformExecutionData = new TerraformExecutionData();
    terraformStepConfiguration.setTerraformExecutionData(terraformExecutionData);
    Assertions.assertThatThrownBy(terraformApplyStepInfo::validateSpecParams)
        .hasMessageContaining("Config files are null");

    TerraformConfigFilesWrapper terraformConfigFilesWrapper = new TerraformConfigFilesWrapper();
    terraformExecutionData.setTerraformConfigFilesWrapper(terraformConfigFilesWrapper);
    Assertions.assertThatThrownBy(terraformApplyStepInfo::validateSpecParams)
        .hasMessageContaining("Store cannot be null in Config Files");

    StoreConfigWrapper storeConfigWrapper =
        StoreConfigWrapper.builder().type(StoreConfigType.BITBUCKET).spec(BitbucketStore.builder().build()).build();
    terraformConfigFilesWrapper.setStore(storeConfigWrapper);
    terraformApplyStepInfo.validateSpecParams();
  }
}
