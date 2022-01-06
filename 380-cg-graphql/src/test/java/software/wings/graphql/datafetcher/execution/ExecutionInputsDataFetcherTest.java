/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.execution;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.PRABU;
import static io.harness.rule.OwnerRule.VIKAS_S;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.graphql.datafetcher.execution.ExecutionInputsDataFetcher.APPLICATION_DOES_NOT_EXIST_MSG;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.query.QLServiceInputsForExecutionParams;
import software.wings.graphql.schema.type.QLService;
import software.wings.graphql.schema.type.execution.QLExecutionInputs;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CDC)
public class ExecutionInputsDataFetcherTest extends AbstractDataFetcherTestBase {
  @Mock ServiceResourceService serviceResourceService;
  @Mock AppService appService;
  @Mock WorkflowService workflowService;
  @Mock PipelineService pipelineService;

  @Inject @InjectMocks PipelineExecutionController pipelineExecutionController;
  @Inject @InjectMocks WorkflowExecutionController workflowExecutionController;
  @Inject @InjectMocks ExecutionInputsDataFetcher executionInputsDataFetcher;

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void populateArtifactNeededServiceIds() {
    QLServiceInputsForExecutionParams params = QLServiceInputsForExecutionParams.builder()
                                                   .executionType(QLExecutionType.WORKFLOW)
                                                   .applicationId(APP_ID)
                                                   .entityId(WORKFLOW_ID)
                                                   .build();
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    PageResponse<Service> response = aPageResponse()
                                         .withResponse(asList(Service.builder().uuid(SERVICE_ID).build(),
                                             Service.builder().uuid(SERVICE_ID + 2).build()))
                                         .build();
    Workflow workflow =
        aWorkflow().uuid(WORKFLOW_ID).orchestrationWorkflow(aCanaryOrchestrationWorkflow().build()).build();
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(workflowService.fetchDeploymentMetadata(eq(APP_ID), eq(workflow), any(), eq(null), eq(null), eq(false),
             eq(null), eq(DeploymentMetadata.Include.ARTIFACT_SERVICE)))
        .thenReturn(
            DeploymentMetadata.builder().artifactRequiredServiceIds(asList(SERVICE_ID, SERVICE_ID + 2)).build());
    when(serviceResourceService.list(any(), eq(true), eq(false), eq(false), eq(null))).thenReturn(response);
    QLExecutionInputs executionInputs = executionInputsDataFetcher.fetch(params, ACCOUNT_ID);
    assertThat(executionInputs).isNotNull();
    assertThat(executionInputs.getServiceInputs().stream().map(QLService::getId).collect(Collectors.toList()))
        .containsExactlyInAnyOrder(SERVICE_ID, SERVICE_ID + 2);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnEmptyServiceIds() {
    QLServiceInputsForExecutionParams params = QLServiceInputsForExecutionParams.builder()
                                                   .executionType(QLExecutionType.WORKFLOW)
                                                   .applicationId(APP_ID)
                                                   .entityId(WORKFLOW_ID)
                                                   .build();
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    Workflow workflow =
        aWorkflow().uuid(WORKFLOW_ID).orchestrationWorkflow(aCanaryOrchestrationWorkflow().build()).build();
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(workflowService.fetchDeploymentMetadata(eq(APP_ID), eq(workflow), any(), eq(null), eq(null), eq(false),
             eq(null), eq(DeploymentMetadata.Include.ARTIFACT_SERVICE)))
        .thenReturn(DeploymentMetadata.builder().build());

    QLExecutionInputs executionInputs = executionInputsDataFetcher.fetch(params, ACCOUNT_ID);
    assertThat(executionInputs).isNotNull();
    assertThat(executionInputs.getServiceInputs()).isEmpty();
  }

  @Test
  @Owner(developers = VIKAS_S)
  @Category(UnitTests.class)
  public void shouldProvideMeaningfulErrorMessagesForInvalidApplicationId() {
    // Case when application doesn't belong to provided account.
    String applicationId = "app1";
    String providedAccountId = "account1";
    String actualAccountIdForProvidedApp = "account2";
    QLServiceInputsForExecutionParams params = QLServiceInputsForExecutionParams.builder()
                                                   .executionType(QLExecutionType.WORKFLOW)
                                                   .applicationId(applicationId)
                                                   .entityId(WORKFLOW_ID)
                                                   .build();
    when(appService.getAccountIdByAppId(applicationId)).thenReturn(actualAccountIdForProvidedApp);
    assertThatThrownBy(() -> executionInputsDataFetcher.fetch(params, providedAccountId))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(APPLICATION_DOES_NOT_EXIST_MSG);

    // Case when application doesn't exist at all.
    String nonExistentApplicationId = "nonExistentApp";
    QLServiceInputsForExecutionParams paramsForNonExistentApp = QLServiceInputsForExecutionParams.builder()
                                                                    .executionType(QLExecutionType.WORKFLOW)
                                                                    .applicationId(nonExistentApplicationId)
                                                                    .entityId(WORKFLOW_ID)
                                                                    .build();
    when(appService.getAccountIdByAppId(nonExistentApplicationId)).thenReturn(null);
    when(appService.getAccountIdByAppId(applicationId)).thenReturn(actualAccountIdForProvidedApp);
    assertThatThrownBy(() -> executionInputsDataFetcher.fetch(paramsForNonExistentApp, providedAccountId))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(APPLICATION_DOES_NOT_EXIST_MSG);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void populateArtifactNeededServiceIdsForPipeline() {
    QLServiceInputsForExecutionParams params = QLServiceInputsForExecutionParams.builder()
                                                   .executionType(QLExecutionType.PIPELINE)
                                                   .applicationId(APP_ID)
                                                   .entityId(PIPELINE_ID)
                                                   .build();
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    PageResponse<Service> response = aPageResponse()
                                         .withResponse(asList(Service.builder().uuid(SERVICE_ID).build(),
                                             Service.builder().uuid(SERVICE_ID + 2).build()))
                                         .build();
    Pipeline pipeline = Pipeline.builder().uuid(PIPELINE_ID).build();
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(pipelineService.fetchDeploymentMetadata(eq(APP_ID), eq(pipeline), any()))
        .thenReturn(
            DeploymentMetadata.builder().artifactRequiredServiceIds(asList(SERVICE_ID, SERVICE_ID + 2)).build());
    when(serviceResourceService.list(any(), eq(true), eq(false), eq(false), eq(null))).thenReturn(response);
    QLExecutionInputs executionInputs = executionInputsDataFetcher.fetch(params, ACCOUNT_ID);
    assertThat(executionInputs).isNotNull();
    assertThat(executionInputs.getServiceInputs().stream().map(QLService::getId).collect(Collectors.toList()))
        .containsExactlyInAnyOrder(SERVICE_ID, SERVICE_ID + 2);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void populateManifestNeededServiceIds() {
    QLServiceInputsForExecutionParams params = QLServiceInputsForExecutionParams.builder()
                                                   .executionType(QLExecutionType.WORKFLOW)
                                                   .applicationId(APP_ID)
                                                   .entityId(WORKFLOW_ID)
                                                   .build();

    Workflow workflow =
        aWorkflow().uuid(WORKFLOW_ID).orchestrationWorkflow(aCanaryOrchestrationWorkflow().build()).build();
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(workflowService.fetchDeploymentMetadata(eq(APP_ID), eq(workflow), any(), eq(null), eq(null), eq(false),
             eq(null), eq(DeploymentMetadata.Include.ARTIFACT_SERVICE)))
        .thenReturn(
            DeploymentMetadata.builder().manifestRequiredServiceIds(asList(SERVICE_ID, SERVICE_ID + 2)).build());

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    PageResponse<Service> response = aPageResponse()
                                         .withResponse(asList(Service.builder().uuid(SERVICE_ID).build(),
                                             Service.builder().uuid(SERVICE_ID + 2).build()))
                                         .build();
    when(serviceResourceService.list(any(), eq(true), eq(false), eq(false), eq(null))).thenReturn(response);

    QLExecutionInputs executionInputs = executionInputsDataFetcher.fetch(params, ACCOUNT_ID);
    assertThat(executionInputs).isNotNull();
    assertThat(executionInputs.getServiceInputs()).isEmpty();
    assertThat(executionInputs.getServiceManifestInputs().stream().map(QLService::getId).collect(Collectors.toList()))
        .containsExactlyInAnyOrder(SERVICE_ID, SERVICE_ID + 2);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void populateManifestAndArtifactServiceIds() {
    QLServiceInputsForExecutionParams params = QLServiceInputsForExecutionParams.builder()
                                                   .executionType(QLExecutionType.PIPELINE)
                                                   .applicationId(APP_ID)
                                                   .entityId(PIPELINE_ID)
                                                   .build();

    Pipeline pipeline = Pipeline.builder().uuid(PIPELINE_ID).build();
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(pipelineService.fetchDeploymentMetadata(eq(APP_ID), eq(pipeline), any()))
        .thenReturn(DeploymentMetadata.builder()
                        .artifactRequiredServiceIds(asList(SERVICE_ID, SERVICE_ID + 2))
                        .manifestRequiredServiceIds(asList(SERVICE_ID, SERVICE_ID + 2))
                        .build());
    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    PageResponse<Service> response = aPageResponse()
                                         .withResponse(asList(Service.builder().uuid(SERVICE_ID).build(),
                                             Service.builder().uuid(SERVICE_ID + 2).build()))
                                         .build();
    when(serviceResourceService.list(any(), eq(true), eq(false), eq(false), eq(null))).thenReturn(response);

    QLExecutionInputs executionInputs = executionInputsDataFetcher.fetch(params, ACCOUNT_ID);
    assertThat(executionInputs).isNotNull();
    assertThat(executionInputs.getServiceInputs().stream().map(QLService::getId).collect(Collectors.toList()))
        .containsExactlyInAnyOrder(SERVICE_ID, SERVICE_ID + 2);
    assertThat(executionInputs.getServiceManifestInputs().stream().map(QLService::getId).collect(Collectors.toList()))
        .containsExactlyInAnyOrder(SERVICE_ID, SERVICE_ID + 2);
  }
}
