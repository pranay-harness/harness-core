/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.execution;

import static io.harness.rule.OwnerRule.MILOS;
import static io.harness.rule.OwnerRule.PRABU;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.beans.CreatedByType;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLExecutionQueryParameters;
import software.wings.graphql.schema.type.QLExecution;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;

public class ExecutionDataFetcherTest extends AbstractDataFetcherTestBase {
  public static final String EXECUTION_ID_1 = "EXECUTION_ID1";
  @Mock WingsPersistence wingsPersistence;
  @Mock Query<WorkflowExecution> query;
  @Mock FieldEnd fieldEnd;
  @Inject @InjectMocks ExecutionQueryHelper executionQueryHelper;
  @Inject @InjectMocks ExecutionDataFetcher executionDataFetcher;

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testExecutionDataFetcherAlongPipeline() {
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("execution/build_workflow_execution.json", WorkflowExecution.class);
    assertThat(workflowExecution).isNotNull();
    when(wingsPersistence.createAuthorizedQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter("_id", EXECUTION_ID_1)).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    QLExecution execution = executionDataFetcher.fetch(new QLExecutionQueryParameters(EXECUTION_ID_1), ACCOUNT1_ID);
    JsonNode actual = JsonUtils.toJsonNode(execution);
    assertThat(execution).isNotNull();
    JsonNode expected = JsonUtils.readResourceFile("execution/build_workflow_qlExecution.json", JsonNode.class);
    assertEquals("QL execution should be equal", expected, actual);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testExecutionDataFetcherByTrigger() {
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("execution/build_workflow_execution.json", WorkflowExecution.class);
    assertThat(workflowExecution).isNotNull();
    workflowExecution.setDeploymentTriggerId("TRIGGER_ID");
    workflowExecution.setPipelineExecutionId(null);
    when(wingsPersistence.createAuthorizedQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter("_id", EXECUTION_ID_1)).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    QLExecution execution = executionDataFetcher.fetch(new QLExecutionQueryParameters(EXECUTION_ID_1), ACCOUNT1_ID);
    JsonNode actual = JsonUtils.toJsonNode(execution);
    assertThat(execution).isNotNull();
    JsonNode expected =
        JsonUtils.readResourceFile("execution/build_workflow_qlExecution_by_trigger.json", JsonNode.class);
    assertEquals("QL execution should be equal", expected, actual);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testExecutionDataFetcherByApiKey() {
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("execution/build_workflow_execution.json", WorkflowExecution.class);
    assertThat(workflowExecution).isNotNull();
    workflowExecution.setPipelineExecutionId(null);
    workflowExecution.setCreatedByType(CreatedByType.API_KEY);
    workflowExecution.setCreatedBy(EmbeddedUser.builder().name("API_KEY").uuid("KEY_ID").build());
    when(wingsPersistence.createAuthorizedQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter("_id", EXECUTION_ID_1)).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    QLExecution execution = executionDataFetcher.fetch(new QLExecutionQueryParameters(EXECUTION_ID_1), ACCOUNT1_ID);
    JsonNode actual = JsonUtils.toJsonNode(execution);
    assertThat(execution).isNotNull();
    JsonNode expected =
        JsonUtils.readResourceFile("execution/build_workflow_qlExecution_by_apikey.json", JsonNode.class);
    assertEquals("QL execution should be equal", expected, actual);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testExecutionDataFetcherByUser() {
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("execution/build_workflow_execution.json", WorkflowExecution.class);
    assertThat(workflowExecution).isNotNull();
    workflowExecution.setPipelineExecutionId(null);
    workflowExecution.setCreatedByType(CreatedByType.USER);
    workflowExecution.setTriggeredBy(
        EmbeddedUser.builder().name("admin").uuid("USER_ID").email("admin@harness.io").build());
    when(wingsPersistence.createAuthorizedQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter("_id", EXECUTION_ID_1)).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    QLExecution execution = executionDataFetcher.fetch(new QLExecutionQueryParameters(EXECUTION_ID_1), ACCOUNT1_ID);
    JsonNode actual = JsonUtils.toJsonNode(execution);
    assertThat(execution).isNotNull();
    JsonNode expected = JsonUtils.readResourceFile("execution/build_workflow_qlExecution_by_user.json", JsonNode.class);
    assertEquals("QL execution should be equal", expected, actual);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testExecutionDataFetcherWithFilters() {
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("execution/build_workflow_execution.json", WorkflowExecution.class);
    String pipelineExecutionId = "F-SlLs66TU-Bu8qfRJnFhQ";
    String[] idArray = new String[1];
    idArray[0] = pipelineExecutionId;

    List<QLExecutionFilter> filters = Arrays.asList(
        QLExecutionFilter.builder()
            .status(QLIdFilter.builder().operator(QLIdOperator.NOT_NULL).build())
            .pipelineExecutionId(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(idArray).build())
            .build());

    doReturn(fieldEnd).when(query).field(WorkflowExecutionKeys.status);
    doReturn(fieldEnd).when(query).field(WorkflowExecutionKeys.pipelineExecutionId);
    executionQueryHelper.setQuery(filters, query, ACCOUNT1_ID);

    assertThat(workflowExecution).isNotNull();
    workflowExecution.setPipelineExecutionId(null);
    workflowExecution.setStatus(ExecutionStatus.SUCCESS);
    workflowExecution.setCreatedByType(CreatedByType.USER);
    workflowExecution.setTriggeredBy(
        EmbeddedUser.builder().name("admin").uuid("USER_ID").email("admin@harness.io").build());

    when(wingsPersistence.createAuthorizedQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter("_id", EXECUTION_ID_1)).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);

    QLExecution execution = executionDataFetcher.fetch(new QLExecutionQueryParameters(EXECUTION_ID_1), ACCOUNT1_ID);
    JsonNode actual = JsonUtils.toJsonNode(execution);
    assertThat(execution).isNotNull();
    JsonNode expected = JsonUtils.readResourceFile("execution/build_workflow_qlExecution_by_user.json", JsonNode.class);
    assertEquals("QL execution should be equal", expected, actual);
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testExecutionDataFetcherWithInvalidFilters() {
    List<QLExecutionFilter> filters = Arrays.asList(
        QLExecutionFilter.builder().status(QLIdFilter.builder().operator(QLIdOperator.EQUALS).build()).build());

    doReturn(fieldEnd).when(query).field(WorkflowExecutionKeys.status);

    assertThatThrownBy(() -> { executionQueryHelper.setQuery(filters, query, ACCOUNT1_ID); })
        .isInstanceOf(WingsException.class)
        .hasMessageContaining("Values cannot be empty");
  }
}
