package software.wings.graphql.datafetcher.execution;

import static io.harness.rule.OwnerRule.POOJA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.mutation.execution.input.QLStartExecutionInput;
import software.wings.graphql.schema.mutation.execution.payload.QLStartExecutionPayload;
import software.wings.graphql.schema.type.QLPipelineExecution;
import software.wings.graphql.schema.type.QLWorkflowExecution;
import software.wings.service.intfc.AppService;

public class StartExecutionDataFetcherTest extends CategoryTest {
  @InjectMocks @Inject StartExecutionDataFetcher startExecutionDataFetcher;
  @Mock WorkflowExecutionController workflowExecutionController;
  @Mock PipelineExecutionController pipelineExecutionController;
  @Mock AppService appService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(appService.getAccountIdByAppId("appId")).thenReturn("accountId");
    //        configureAppService();
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void startPipelineExecution() {
    final MutationContext mutationContext = getMutationContext();
    QLStartExecutionInput input = QLStartExecutionInput.builder()
                                      .applicationId("appId")
                                      .clientMutationId("clientMutationId")
                                      .entityId("pipelineId")
                                      .executionType(QLExecutionType.PIPELINE)
                                      .build();
    when(pipelineExecutionController.startPipelineExecution(any(), any(), any()))
        .thenReturn(QLPipelineExecution.builder().build());
    QLStartExecutionPayload paylaod = startExecutionDataFetcher.mutateAndFetch(input, mutationContext);
    assertThat(paylaod.getClientMutationId()).isEqualTo("clientMutationId");
    assertThat(paylaod.getExecution()).isNotNull();
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void startWorkflowExecution() {
    final MutationContext mutationContext = getMutationContext();
    QLStartExecutionInput input = QLStartExecutionInput.builder()
                                      .applicationId("appId")
                                      .clientMutationId("clientMutationId")
                                      .entityId("workflowId")
                                      .executionType(QLExecutionType.WORKFLOW)
                                      .build();
    when(workflowExecutionController.startWorkflowExecution(any(), any(), any()))
        .thenReturn(QLWorkflowExecution.builder().build());
    QLStartExecutionPayload paylaod = startExecutionDataFetcher.mutateAndFetch(input, mutationContext);
    assertThat(paylaod.getClientMutationId()).isEqualTo("clientMutationId");
    assertThat(paylaod.getExecution()).isNotNull();
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void executionNullExecution() {
    final MutationContext mutationContext = getMutationContext();
    QLStartExecutionInput input = QLStartExecutionInput.builder()
                                      .applicationId("appId")
                                      .clientMutationId("clientMutationId")
                                      .entityId("workflowId")
                                      .executionType(QLExecutionType.WORKFLOW)
                                      .build();
    when(workflowExecutionController.startWorkflowExecution(any(), any(), any())).thenReturn(null);
    assertThatThrownBy(() -> startExecutionDataFetcher.mutateAndFetch(input, mutationContext))
        .isInstanceOf(InvalidRequestException.class);
  }

  private MutationContext getMutationContext() {
    return MutationContext.builder()
        .accountId("accountId")
        .dataFetchingEnvironment(Mockito.mock(DataFetchingEnvironment.class))
        .build();
  }
}