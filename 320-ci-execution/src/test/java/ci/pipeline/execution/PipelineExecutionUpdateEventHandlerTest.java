package ci.pipeline.execution;

import static io.harness.rule.OwnerRule.HARSH;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.rule.Owner;

import io.fabric8.utils.Lists;
import org.apache.groovy.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PipelineExecutionUpdateEventHandlerTest extends CategoryTest {
  @Mock private NodeExecutionServiceImpl nodeExecutionService;
  @Mock private GitBuildStatusUtility gitBuildStatusUtility;
  @InjectMocks private PipelineExecutionUpdateEventHandler pipelineExecutionUpdateEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testHandleEvent() {
    OrchestrationEvent orchestrationEvent =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.newBuilder()
                          .putAllSetupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                              "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                          .addAllLevels(Lists.newArrayList(Level.newBuilder().setRuntimeId("node1").build()))
                          .build())
            .build();
    when(gitBuildStatusUtility.shouldSendStatus(any())).thenReturn(true);
    pipelineExecutionUpdateEventHandler.handleEvent(orchestrationEvent);

    verify(gitBuildStatusUtility).sendStatusToGit(any(), any(), any());
  }
}
