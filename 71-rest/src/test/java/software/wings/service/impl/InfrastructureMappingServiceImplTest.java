package software.wings.service.impl;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.PROVISIONER_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping;

import java.util.List;

public class InfrastructureMappingServiceImplTest extends WingsBaseTest {
  @Inject InfrastructureMappingServiceImpl infrastructureMappingService;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void hostsListShouldReturnEmptyWhenDynamicInfra() {
    InfrastructureMapping infraMapping = PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
                                             .withProvisionerId(PROVISIONER_ID)
                                             .build();

    List<String> hostDisplayNames = infrastructureMappingService.getInfrastructureMappingHostDisplayNames(
        infraMapping, APP_ID, WORKFLOW_EXECUTION_ID);

    assertThat(hostDisplayNames).isEmpty();
  }
}