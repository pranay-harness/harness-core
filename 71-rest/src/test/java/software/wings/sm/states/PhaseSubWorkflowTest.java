package software.wings.sm.states;

import static io.harness.rule.OwnerRule.PRABU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.PHASE_STEP;
import static software.wings.utils.WingsTestConstants.SERVICE;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Service;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.utils.WingsTestConstants;

import java.util.Arrays;
import java.util.Collections;

public class PhaseSubWorkflowTest extends WingsBaseTest {
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  private Service service = Service.builder().name(SERVICE).build();
  private PhaseSubWorkflow phaseSubWorkflow = new PhaseSubWorkflow(PHASE_STEP);

  @Before
  public void setUp() throws Exception {
    Reflect.on(phaseSubWorkflow).set("artifactStreamServiceBindingService", artifactStreamServiceBindingService);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldFetchCorrectArtifactFromService() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    Artifact artifact = anArtifact()
                            .withDisplayName(WingsTestConstants.ARTIFACT_NAME)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withUuid(ARTIFACT_ID)
                            .build();
    executionArgs.setArtifacts(Collections.singletonList(artifact));
    WorkflowExecution workflowExecution = WorkflowExecution.builder().executionArgs(executionArgs).build();
    doReturn(Arrays.asList(ARTIFACT_STREAM_ID, ARTIFACT_STREAM_ID + "2"))
        .when(artifactStreamServiceBindingService)
        .listArtifactStreamIds(service);
    assertThat(phaseSubWorkflow.findRollbackArtifactId(service, workflowExecution)).isEqualTo(ARTIFACT_ID);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldNotFetchArtifactFromServiceIfUnavailable() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    Artifact artifact = anArtifact()
                            .withDisplayName(WingsTestConstants.ARTIFACT_NAME)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID + "3")
                            .withUuid(ARTIFACT_ID)
                            .build();
    executionArgs.setArtifacts(Collections.singletonList(artifact));
    WorkflowExecution workflowExecution = WorkflowExecution.builder().executionArgs(executionArgs).build();
    doReturn(Arrays.asList(ARTIFACT_STREAM_ID, ARTIFACT_STREAM_ID + "2"))
        .when(artifactStreamServiceBindingService)
        .listArtifactStreamIds(service);
    assertThat(phaseSubWorkflow.findRollbackArtifactId(service, workflowExecution)).isNull();
  }
}
