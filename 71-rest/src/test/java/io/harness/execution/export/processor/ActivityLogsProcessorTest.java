package io.harness.execution.export.processor;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Log.Builder.aLog;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.ExportExecutionsException;
import io.harness.execution.export.metadata.ActivityCommandUnitMetadata;
import io.harness.execution.export.metadata.ApprovalMetadata;
import io.harness.execution.export.metadata.ExecutionHistoryMetadata;
import io.harness.execution.export.metadata.GraphNodeMetadata;
import io.harness.execution.export.metadata.PipelineExecutionMetadata;
import io.harness.execution.export.metadata.PipelineStageExecutionMetadata;
import io.harness.execution.export.metadata.WorkflowExecutionMetadata;
import io.harness.execution.export.processor.ActivityLogsProcessor.ActivityIdsVisitor;
import io.harness.rule.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.service.intfc.LogService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ActivityLogsProcessorTest extends CategoryTest {
  @Mock private LogService logService;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testVisitExecutionMetadata() {
    ActivityLogsProcessor activityLogsProcessor = new ActivityLogsProcessor(null, null, new HashMap<>());

    activityLogsProcessor.visitExecutionMetadata(WorkflowExecutionMetadata.builder().id("id").build());
    assertThat(activityLogsProcessor.getActivityIdToExecutionDetailsMap()).isEmpty();
    assertThat(activityLogsProcessor.getActivityIdToExecutionIdMap()).isEmpty();

    WorkflowExecutionMetadata workflowExecutionMetadata =
        WorkflowExecutionMetadata.builder()
            .id("id")
            .executionGraph(asList(
                GraphNodeMetadata.builder()
                    .activityId("id1")
                    .subCommands(Collections.singletonList(ActivityCommandUnitMetadata.builder().build()))
                    .executionHistory(Collections.singletonList(
                        ExecutionHistoryMetadata.builder()
                            .activityId("id2")
                            .subCommands(Collections.singletonList(ActivityCommandUnitMetadata.builder().build()))
                            .build()))
                    .build(),
                GraphNodeMetadata.builder()
                    .activityId("id3")
                    .subCommands(Collections.singletonList(ActivityCommandUnitMetadata.builder().build()))
                    .build()))
            .build();
    PipelineExecutionMetadata pipelineExecutionMetadata =
        PipelineExecutionMetadata.builder()
            .id("pid")
            .stages(asList(
                PipelineStageExecutionMetadata.builder().approvalData(ApprovalMetadata.builder().build()).build(),
                PipelineStageExecutionMetadata.builder()
                    .approvalData(ApprovalMetadata.builder().activityId("aid").build())
                    .build(),
                PipelineStageExecutionMetadata.builder().workflowExecution(workflowExecutionMetadata).build()))
            .build();
    activityLogsProcessor.visitExecutionMetadata(pipelineExecutionMetadata);
    assertThat(activityLogsProcessor.getActivityIdToExecutionDetailsMap().keySet())
        .containsExactlyInAnyOrder("aid", "id1", "id2", "id3");
    assertThat(activityLogsProcessor.getActivityIdToExecutionDetailsMap().get("aid"))
        .isEqualTo(pipelineExecutionMetadata.getStages().get(1).getApprovalData());
    assertThat(activityLogsProcessor.getActivityIdToExecutionDetailsMap().get("id1"))
        .isEqualTo(workflowExecutionMetadata.getExecutionGraph().get(0));
    assertThat(activityLogsProcessor.getActivityIdToExecutionDetailsMap().get("id2"))
        .isEqualTo(workflowExecutionMetadata.getExecutionGraph().get(0).getExecutionHistory().get(0));
    assertThat(activityLogsProcessor.getActivityIdToExecutionDetailsMap().get("id3"))
        .isEqualTo(workflowExecutionMetadata.getExecutionGraph().get(1));

    assertThat(activityLogsProcessor.getActivityIdToExecutionIdMap().keySet())
        .containsExactlyInAnyOrder("aid", "id1", "id2", "id3");
    assertThat(activityLogsProcessor.getActivityIdToExecutionIdMap().get("aid")).isEqualTo("pid");
    assertThat(activityLogsProcessor.getActivityIdToExecutionIdMap().get("id1")).isEqualTo("pid");
    assertThat(activityLogsProcessor.getActivityIdToExecutionIdMap().get("id2")).isEqualTo("pid");
    assertThat(activityLogsProcessor.getActivityIdToExecutionIdMap().get("id3")).isEqualTo("pid");
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testProcess() throws IOException {
    ZipOutputStream zipOutputStream = mock(ZipOutputStream.class);
    ActivityLogsProcessor activityLogsProcessor =
        new ActivityLogsProcessor(zipOutputStream, ImmutableMap.of("id1", "fn1", "id2", "fn2"), new HashMap<>());
    activityLogsProcessor.setLogService(logService);

    activityLogsProcessor.process();
    verify(logService, never()).list(anyString(), any());

    ActivityCommandUnitMetadata commandUnit11 = ActivityCommandUnitMetadata.builder().name("cu1").build();
    ActivityCommandUnitMetadata commandUnit12 = ActivityCommandUnitMetadata.builder().name("cu2").build();
    ActivityCommandUnitMetadata commandUnit21 = ActivityCommandUnitMetadata.builder().name("cu1").build();
    ActivityCommandUnitMetadata commandUnit22 = ActivityCommandUnitMetadata.builder().name("cu2").build();
    ActivityCommandUnitMetadata commandUnit31 = ActivityCommandUnitMetadata.builder().name("cu1").build();
    ActivityCommandUnitMetadata[] currCommandUnit = new ActivityCommandUnitMetadata[] {null};

    doAnswer(invocation -> {
      ZipEntry entry = invocation.getArgumentAt(0, ZipEntry.class);
      String name = entry.getName().substring(4);
      if (name.equals(commandUnit11.getExecutionLogFile())) {
        currCommandUnit[0] = commandUnit11;
      } else if (name.equals(commandUnit12.getExecutionLogFile())) {
        currCommandUnit[0] = commandUnit12;
      } else if (name.equals(commandUnit21.getExecutionLogFile())) {
        currCommandUnit[0] = commandUnit21;
      } else if (name.equals(commandUnit22.getExecutionLogFile())) {
        currCommandUnit[0] = commandUnit22;
      } else if (name.equals(commandUnit31.getExecutionLogFile())) {
        currCommandUnit[0] = commandUnit31;
      }

      return null;
    })
        .when(zipOutputStream)
        .putNextEntry(any(ZipEntry.class));

    doAnswer(invocation -> {
      if (currCommandUnit[0] == null) {
        return null;
      }

      if (currCommandUnit[0].getExecutionLogFileContent() == null) {
        currCommandUnit[0].setExecutionLogFile("0");
      } else {
        currCommandUnit[0].setExecutionLogFile(
            String.valueOf(currCommandUnit[0].getExecutionLogFileContent().split("\n").length));
      }

      return null;
    })
        .when(zipOutputStream)
        .write(any());

    doAnswer(invocation -> {
      currCommandUnit[0] = null;
      return null;
    })
        .when(zipOutputStream)
        .closeEntry();

    WorkflowExecutionMetadata workflowExecutionMetadata =
        WorkflowExecutionMetadata.builder()
            .id("id1")
            .executionGraph(asList(
                GraphNodeMetadata.builder()
                    .activityId("aid1")
                    .subCommands(asList(commandUnit11, commandUnit12))
                    .executionHistory(Collections.singletonList(ExecutionHistoryMetadata.builder()
                                                                    .activityId("aid2")
                                                                    .subCommands(asList(commandUnit21, commandUnit22))
                                                                    .build()))
                    .build(),
                GraphNodeMetadata.builder()
                    .activityId("aid3")
                    .subCommands(Collections.singletonList(commandUnit31))
                    .build()))
            .build();

    activityLogsProcessor.visitExecutionMetadata(workflowExecutionMetadata);
    when(logService.list(anyString(), any())).thenReturn(aPageResponse().withResponse(Collections.emptyList()).build());
    activityLogsProcessor.process();
    verify(logService, times(1)).list(anyString(), any());

    when(logService.list(anyString(), any()))
        .thenReturn(aPageResponse()
                        .withResponse(asList(aLog().withActivityId("aid1").withCommandUnitName("cu1").build(),
                            aLog().withActivityId("aid2").withCommandUnitName("cu1").build(),
                            aLog().withActivityId("aid1").withCommandUnitName("cu1").build(),
                            aLog().withActivityId("aid1").withCommandUnitName("cu2").build(),
                            aLog().withActivityId("aid3").withCommandUnitName("cu1").build()))
                        .build())
        .thenReturn(aPageResponse().withResponse(Collections.emptyList()).build());

    activityLogsProcessor.process();
    verify(logService, times(3)).list(anyString(), any());
    assertThat(commandUnit11.getExecutionLogFile()).isEqualTo("2");
    assertThat(commandUnit12.getExecutionLogFile()).isEqualTo("1");
    assertThat(commandUnit21.getExecutionLogFile()).isEqualTo("1");
    assertThat(commandUnit22.getExecutionLogFile()).isNull();
    assertThat(commandUnit31.getExecutionLogFile()).isEqualTo("1");

    when(logService.list(anyString(), any()))
        .thenReturn(aPageResponse()
                        .withResponse(asList(aLog().withActivityId("aid1").withCommandUnitName("cu1").build(),
                            aLog().withActivityId("aid2").withCommandUnitName("cu1").build()))
                        .build())
        .thenReturn(aPageResponse().withResponse(Collections.emptyList()).build());

    doThrow(IOException.class).when(zipOutputStream).closeEntry();
    assertThatThrownBy(activityLogsProcessor::process).isInstanceOf(ExportExecutionsException.class);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testActivityIdsVisitor() {
    ActivityIdsVisitor activityIdsVisitor = new ActivityIdsVisitor();
    activityIdsVisitor.visitGraphNode(null);
    assertThat(activityIdsVisitor.getActivityIdToNodeMetadataMap()).isEmpty();

    activityIdsVisitor = new ActivityIdsVisitor();
    activityIdsVisitor.visitGraphNode(
        GraphNodeMetadata.builder()
            .activityId("id1")
            .subCommands(Collections.singletonList(ActivityCommandUnitMetadata.builder().build()))
            .build());
    assertThat(activityIdsVisitor.getActivityIdToNodeMetadataMap().keySet()).containsExactly("id1");

    activityIdsVisitor = new ActivityIdsVisitor();
    activityIdsVisitor.visitGraphNode(
        GraphNodeMetadata.builder()
            .activityId("id1")
            .subCommands(Collections.singletonList(ActivityCommandUnitMetadata.builder().build()))
            .executionHistory(asList(ExecutionHistoryMetadata.builder().build(), null,
                ExecutionHistoryMetadata.builder().activityId("id2").build(),
                ExecutionHistoryMetadata.builder()
                    .activityId("id3")
                    .subCommands(Collections.singletonList(ActivityCommandUnitMetadata.builder().build()))
                    .build()))
            .build());
    assertThat(activityIdsVisitor.getActivityIdToNodeMetadataMap().keySet()).containsExactly("id1", "id3");
  }
}
