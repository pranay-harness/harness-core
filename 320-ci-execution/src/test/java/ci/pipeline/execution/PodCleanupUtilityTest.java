package ci.pipeline.execution;

import static io.harness.rule.OwnerRule.HARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.beans.sweepingoutputs.PodCleanupDetails;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.k8s.CIK8CleanupTaskParams;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.rule.Owner;
import io.harness.stateutils.buildstate.ConnectorUtils;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.groovy.util.Maps;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PodCleanupUtilityTest extends CIExecutionTestBase {
  @Mock private ConnectorUtils connectorUtils;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;

  @InjectMocks private PodCleanupUtility podCleanupUtility;
  private Ambiance ambiance = Ambiance.newBuilder()
                                  .putAllSetupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                                      "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                                  .build();
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void testHandleEventForRunning() throws IOException {
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ciExecutionPlanTestHelper.getGitConnector());
    when(executionSweepingOutputResolver.resolve(any(), any()))
        .thenReturn(PodCleanupDetails.builder()
                        .podName("podName")
                        .cleanUpContainerNames(new ArrayList<>())
                        .infrastructure(ciExecutionPlanTestHelper.getInfrastructure())
                        .build());

    CIK8CleanupTaskParams cik8CleanupTaskParams = podCleanupUtility.buildAndfetchCleanUpParameters(ambiance);

    assertThat(cik8CleanupTaskParams).isNotNull();
  }
}