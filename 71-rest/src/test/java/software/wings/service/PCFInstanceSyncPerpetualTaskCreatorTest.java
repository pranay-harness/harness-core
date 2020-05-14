package software.wings.service;

import static io.harness.rule.OwnerRule.AMAN;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskClient;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.api.PcfDeploymentInfo;

import java.util.Collections;
import java.util.List;

public class PCFInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "accountId";
  @Mock PcfInstanceSyncPerpetualTaskClient pcfPerpTaskClient;
  @InjectMocks PCFInstanceSyncPerpetualTaskCreator pcfInstanceSyncPerpetualTaskCreator;

  @Before
  public void setUp() throws Exception {
    when(pcfPerpTaskClient.create(any(), any())).thenReturn("success");
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void createPerpetualTaskForNewDeployment() {
    DeploymentSummary deploymentSummary =
        DeploymentSummary.builder()
            .appId("appId")
            .accountId("accountId")
            .infraMappingId("infraId")
            .deploymentInfo(
                PcfDeploymentInfo.builder().applicationGuild("guid").applicationName("applicationName").build())
            .build();
    List<String> tasks = pcfInstanceSyncPerpetualTaskCreator.createPerpetualTasksForNewDeployment(
        Collections.singletonList(deploymentSummary), Collections.emptyList());
    assertEquals(1, tasks.size());
  }
}