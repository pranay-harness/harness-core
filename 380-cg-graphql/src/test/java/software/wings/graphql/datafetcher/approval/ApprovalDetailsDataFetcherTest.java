/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ABHINAV_MITTAL;

import static org.mockito.Mockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLApprovalDetailsInput;
import software.wings.service.intfc.WorkflowExecutionService;

import graphql.schema.DataFetchingEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mongodb.morphia.query.Query;

@OwnedBy(CDC)
public class ApprovalDetailsDataFetcherTest extends AbstractDataFetcherTestBase {
  @Mock protected WingsPersistence persistence;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private Query<WorkflowExecution> workflowExecutionQuery;
  @InjectMocks @Spy ApprovalDetailsDataFetcher approvalDetailsDataFetcher;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV_MITTAL)
  @Category(UnitTests.class)
  public void testFetchForInvalidExecutionId() throws Exception {
    final DataFetchingEnvironment dataFetchingEnvironment = Mockito.mock(DataFetchingEnvironment.class);

    QLApprovalDetailsInput qlApprovalDetailsInput = new QLApprovalDetailsInput("appId", "executionId");

    when(persistence.createAuthorizedQuery(WorkflowExecution.class)).thenReturn(workflowExecutionQuery);

    when(workflowExecutionQuery.filter("_id", qlApprovalDetailsInput.getExecutionId()))
        .thenReturn(workflowExecutionQuery);

    when(workflowExecutionQuery.get()).thenReturn(null);

    doThrow(new InvalidRequestException("Execution does not exist or access is denied", WingsException.USER))
        .when(approvalDetailsDataFetcher)
        .fetch(qlApprovalDetailsInput, "accountId");
  }
}
