package io.harness.ngtriggers.buildtriggers.helper.generator;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.buildtriggers.helpers.dtos.BuildTriggerOpsData;
import io.harness.polling.contracts.Category;
import io.harness.polling.contracts.PollingItem;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class PollingItemGeneratorTestHelper {
  public void baseAssert(PollingItem pollingItem, Category category) {
    assertThat(pollingItem).isNotNull();
    assertThat(pollingItem.getCategory() == category).isTrue();
    assertThat(pollingItem.getSignature()).isEqualTo("sig1");
    assertThat(pollingItem.getQualifier().getAccountId()).isEqualTo("acc");
    assertThat(pollingItem.getQualifier().getOrganizationId()).isEqualTo("org");
    assertThat(pollingItem.getQualifier().getProjectId()).isEqualTo("proj");
  }

  public void validateBuildType(BuildTriggerOpsData buildTriggerOpsData, BuildTriggerHelper buildTriggerHelper) {
    try {
      buildTriggerHelper.validateBuildType(buildTriggerOpsData);
    } catch (Exception e) {
      fail("Exception not expected");
    }
  }
}
