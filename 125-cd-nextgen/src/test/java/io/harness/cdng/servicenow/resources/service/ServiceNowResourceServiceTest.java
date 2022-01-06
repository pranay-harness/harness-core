/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.servicenow.resources.service;

import static io.harness.rule.OwnerRule.PRABU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.common.NGTaskType;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.servicenow.ServiceNowConnectorDTO;
import io.harness.delegate.task.servicenow.ServiceNowTaskNGParameters;
import io.harness.delegate.task.servicenow.ServiceNowTaskNGResponse;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.servicenow.ServiceNowActionNG;
import io.harness.servicenow.ServiceNowFieldNG;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ServiceNowResourceServiceTest extends CategoryTest {
  private static final String ACCOUNT_ID = "accountId";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private static final String IDENTIFIER = "identifier";

  private static final IdentifierRef identifierRef = IdentifierRef.builder()
                                                         .accountIdentifier(ACCOUNT_ID)
                                                         .identifier(IDENTIFIER)
                                                         .projectIdentifier(PROJECT_IDENTIFIER)
                                                         .orgIdentifier(ORG_IDENTIFIER)
                                                         .build();

  @Mock ConnectorService connectorService;
  @Mock SecretManagerClientService secretManagerClientService;
  @Mock DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @InjectMocks @Inject ServiceNowResourceServiceImpl serviceNowResourceService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(connectorService.get(any(), any(), any(), any())).thenReturn(Optional.of(getConnector()));
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(EncryptedDataDetail.builder().build()));
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testGetIssueCreateMeta() {
    List<ServiceNowFieldNG> serviceNowFieldNGList =
        Arrays.asList(ServiceNowFieldNG.builder().name("name1").key("key1").build(),
            ServiceNowFieldNG.builder().name("name2").key("key2").build());
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(ServiceNowTaskNGResponse.builder().serviceNowFieldNGList(serviceNowFieldNGList).build());
    assertThat(serviceNowResourceService.getIssueCreateMetadata(
                   identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "CHANGE_REQUEST"))
        .isEqualTo(serviceNowFieldNGList);
    ArgumentCaptor<DelegateTaskRequest> requestArgumentCaptor = ArgumentCaptor.forClass(DelegateTaskRequest.class);
    verify(delegateGrpcClientWrapper).executeSyncTask(requestArgumentCaptor.capture());
    assertThat(requestArgumentCaptor.getValue().getTaskType()).isEqualTo(NGTaskType.SERVICENOW_TASK_NG.name());
    assertThat(requestArgumentCaptor.getValue().getAccountId()).isEqualTo(ACCOUNT_ID);
    ServiceNowTaskNGParameters parameters =
        (ServiceNowTaskNGParameters) requestArgumentCaptor.getValue().getTaskParameters();
    assertThat(parameters.getAction()).isEqualTo(ServiceNowActionNG.GET_TICKET_CREATE_METADATA);
    assertThat(parameters.getTicketType()).isEqualTo("CHANGE_REQUEST");
  }

  private ConnectorResponseDTO getConnector() {
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                            .connectorType(ConnectorType.SERVICENOW)
                                            .connectorConfig(ServiceNowConnectorDTO.builder()
                                                                 .serviceNowUrl("url")
                                                                 .username("username")
                                                                 .passwordRef(SecretRefData.builder().build())
                                                                 .build())
                                            .build();
    return ConnectorResponseDTO.builder().connector(connectorInfoDTO).build();
  }
}
