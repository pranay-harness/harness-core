/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.workflow;

import static io.harness.rule.OwnerRule.PRABU;
import static io.harness.rule.OwnerRule.RUSHABH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import io.harness.category.element.UnitTests;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLWorkflowQueryParameters;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WorkflowDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject WorkflowDataFetcher workflowDataFetcher;
  private User user;
  @Before
  public void setup() throws SQLException {
    user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testWorkflowDataFetcher() {
    Workflow workflow = createWorkflow(ACCOUNT1_ID, APP1_ID_ACCOUNT1, WORKFLOW1);

    user.setUserRequestContext(
        UserRequestContext.builder().userPermissionInfo(UserPermissionInfo.builder().build()).build());
    user.getUserRequestContext().getUserPermissionInfo().setAppPermissionMapInternal(ImmutableMap.of(APP1_ID_ACCOUNT1,
        AppPermissionSummary.builder()
            .workflowPermissions(ImmutableMap.of(PermissionAttribute.Action.READ, ImmutableSet.of(workflow.getUuid())))
            .build()));

    QLWorkflow qlWorkflow = workflowDataFetcher.fetch(
        QLWorkflowQueryParameters.builder().workflowId(workflow.getUuid()).build(), ACCOUNT1_ID);

    assertThat(qlWorkflow.getId()).isEqualTo(workflow.getUuid());
    assertThat(qlWorkflow.getName()).isEqualTo(WORKFLOW1);

    String workflowExecutionId = createWorkflowExecution(ACCOUNT1_ID, APP1_ID_ACCOUNT1, qlWorkflow.getId());
    qlWorkflow = workflowDataFetcher.fetch(
        QLWorkflowQueryParameters.builder().executionId(workflowExecutionId).build(), ACCOUNT1_ID);

    assertThat(qlWorkflow.getId()).isEqualTo(workflow.getUuid());
    assertThat(qlWorkflow.getName()).isEqualTo(WORKFLOW1);

    qlWorkflow = workflowDataFetcher.fetch(
        QLWorkflowQueryParameters.builder().workflowName(workflow.getName()).applicationId(workflow.getAppId()).build(),
        ACCOUNT1_ID);

    assertThat(qlWorkflow.getApplicationId()).isEqualTo(APP1_ID_ACCOUNT1);
    assertThat(qlWorkflow.getName()).isEqualTo(WORKFLOW1);

    qlWorkflow =
        workflowDataFetcher.fetch(QLWorkflowQueryParameters.builder().workflowId("fakeId").build(), ACCOUNT1_ID);

    assertThat(qlWorkflow).isNull();

    try {
      qlWorkflow = workflowDataFetcher.fetch(
          QLWorkflowQueryParameters.builder().workflowId(workflow.getUuid()).build(), ACCOUNT2_ID);
      fail("InvalidRequestException expected here");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
    }
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForInaccessibleWorkflow() {
    createWorkflow(ACCOUNT1_ID, APP1_ID_ACCOUNT1, WORKFLOW1);
    user.setUserRequestContext(
        UserRequestContext.builder().userPermissionInfo(UserPermissionInfo.builder().build()).build());
    user.getUserRequestContext().getUserPermissionInfo().setAppPermissionMap(ImmutableMap.of(APP1_ID_ACCOUNT1,
        AppPermissionSummaryForUI.builder()
            .servicePermissions(ImmutableMap.of(SERVICE1_ID_APP1_ACCOUNT1,
                ImmutableSet.of(PermissionAttribute.Action.READ, PermissionAttribute.Action.UPDATE,
                    PermissionAttribute.Action.CREATE)))
            .build()));
    user.getUserRequestContext().getUserPermissionInfo().setAppPermissionMapInternal(ImmutableMap.of(APP1_ID_ACCOUNT1,
        AppPermissionSummary.builder()
            .servicePermissions(
                ImmutableMap.of(PermissionAttribute.Action.READ, ImmutableSet.of(SERVICE1_ID_APP1_ACCOUNT1)))
            .build()));
    assertThatThrownBy(
        ()
            -> workflowDataFetcher.fetch(
                QLWorkflowQueryParameters.builder().applicationId(APP1_ID_ACCOUNT1).workflowName(WORKFLOW1).build(),
                ACCOUNT1_ID))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("Not authorized");
  }
}
