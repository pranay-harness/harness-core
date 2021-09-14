/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.jira;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.common.NGTimeConversionHelper;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.JiraTaskNGResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.jira.JiraIssueNG;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.remote.client.NGRestUtils;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;

import java.sql.Timestamp;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@OwnedBy(PIPELINE)
@RunWith(PowerMockRunner.class)
@PrepareForTest({NGRestUtils.class, NGTimeConversionHelper.class, StepUtils.class})
public class JiraStepHelperServiceImplTest extends CategoryTest {
  @Mock private ConnectorResourceClient connectorResourceClient;
  @Mock private SecretManagerClientService secretManagerClientService;
  @Mock private KryoSerializer kryoSerializer;
  @InjectMocks JiraStepHelperServiceImpl jiraStepHelperService;

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testPrepareTestRequest() {
    PowerMockito.mockStatic(NGRestUtils.class);
    PowerMockito.mockStatic(NGTimeConversionHelper.class);
    PowerMockito.mockStatic(StepUtils.class);
    Ambiance ambiance = Ambiance.newBuilder()
                            .putSetupAbstractions("accountId", "accountId")
                            .putSetupAbstractions("orgIdentifier", "orgIdentifier")
                            .putSetupAbstractions("projectIdentifier", "projectIdentifier")
                            .build();
    when(NGRestUtils.getResponse(any())).thenReturn(Optional.empty());
    assertThatCode(()
                       -> jiraStepHelperService.prepareTaskRequest(
                           JiraTaskNGParameters.builder(), ambiance, "connectorref", "time", "task"))
        .isInstanceOf(InvalidRequestException.class);
    when(NGRestUtils.getResponse(any()))
        .thenReturn(Optional.of(
            ConnectorDTO.builder()
                .connectorInfo(ConnectorInfoDTO.builder().connectorConfig(DockerConnectorDTO.builder().build()).build())
                .build()));
    assertThatCode(()
                       -> jiraStepHelperService.prepareTaskRequest(
                           JiraTaskNGParameters.builder(), ambiance, "connectorref", "time", "task"))
        .isInstanceOf(InvalidRequestException.class);
    when(NGRestUtils.getResponse(any()))
        .thenReturn(Optional.of(
            ConnectorDTO.builder()
                .connectorInfo(ConnectorInfoDTO.builder().connectorConfig(JiraConnectorDTO.builder().build()).build())
                .build()));
    jiraStepHelperService.prepareTaskRequest(JiraTaskNGParameters.builder(), ambiance, "connectorref",
        new Timestamp(System.currentTimeMillis()).toString(), "task");
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testPrepareStepResponse() throws Exception {
    StepResponse stepResponse =
        jiraStepHelperService.prepareStepResponse(() -> JiraTaskNGResponse.builder().issue(new JiraIssueNG()).build());
    assertEquals(stepResponse.getStatus(), Status.SUCCEEDED);
  }
}
