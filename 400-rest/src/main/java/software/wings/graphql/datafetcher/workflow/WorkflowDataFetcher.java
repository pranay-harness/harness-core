/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;

import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLWorkflowQueryParameters;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AuthService;

import com.google.inject.Inject;
import java.util.Collections;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class WorkflowDataFetcher extends AbstractObjectDataFetcher<QLWorkflow, QLWorkflowQueryParameters> {
  public static final String WORKFLOW_DOES_NOT_EXIST_MSG = "Workflow does not exist";
  public static final String EMPTY_WORKFLOW_NAME = "Empty Workflow name";
  public static final String EMPTY_APPLICATION_ID = "Empty Application Id";

  @Inject HPersistence persistence;
  @Inject AuthService authService;

  @Override
  @AuthRule(permissionType = PermissionType.WORKFLOW, action = Action.READ)
  public QLWorkflow fetch(QLWorkflowQueryParameters qlQuery, String accountId) {
    Workflow workflow = null;

    if (qlQuery.getWorkflowId() != null) {
      workflow = persistence.get(Workflow.class, qlQuery.getWorkflowId());
    } else if (qlQuery.getWorkflowName() != null) {
      if (EmptyPredicate.isEmpty(qlQuery.getApplicationId())) {
        throw new InvalidRequestException(EMPTY_APPLICATION_ID, WingsException.USER);
      }

      if (EmptyPredicate.isEmpty(qlQuery.getWorkflowName())) {
        throw new InvalidRequestException(EMPTY_WORKFLOW_NAME, WingsException.USER);
      }
      workflow = persistence.createQuery(Workflow.class)
                     .filter(WorkflowKeys.name, qlQuery.getWorkflowName())
                     .filter(WorkflowKeys.appId, qlQuery.getApplicationId())
                     .get();

    } else if (qlQuery.getExecutionId() != null) {
      // TODO: add this to in memory cache
      final String workflowId = persistence.createQuery(WorkflowExecution.class)
                                    .filter(WorkflowExecutionKeys.uuid, qlQuery.getExecutionId())
                                    .project(WorkflowExecutionKeys.workflowId, true)
                                    .get()
                                    .getWorkflowId();

      workflow = persistence.get(Workflow.class, workflowId);
    }

    if (workflow == null) {
      return null;
    }

    if (!workflow.getAccountId().equals(accountId)) {
      throw new InvalidRequestException(WORKFLOW_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
    final User user = UserThreadLocal.get();
    if (user != null) {
      authService.authorize(accountId, workflow.getAppId(), workflow.getUuid(), user,
          Collections.singletonList(
              new PermissionAttribute(ResourceType.WORKFLOW, PermissionType.WORKFLOW, Action.READ)));
    }

    final QLWorkflowBuilder builder = QLWorkflow.builder();
    WorkflowController.populateWorkflow(workflow, builder);
    return builder.build();
  }
}
