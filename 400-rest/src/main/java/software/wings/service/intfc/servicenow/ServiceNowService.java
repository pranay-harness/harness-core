/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.api.ServiceNowExecutionData;
import software.wings.beans.SettingAttribute;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.beans.approval.ServiceNowApprovalParams;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowFieldType;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowMetaDTO;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
public interface ServiceNowService {
  void validateCredential(SettingAttribute settingAttribute);
  List<ServiceNowMetaDTO> getStates(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId);
  List<ServiceNowMetaDTO> getApprovalValues(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId);
  Map<String, List<ServiceNowMetaDTO>> getCreateMeta(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId);
  List<ServiceNowMetaDTO> getAdditionalFields(ServiceNowTicketType ticketType, String accountId, String connectorId,
      String appId, ServiceNowFieldType typeFilter);
  ServiceNowExecutionData getIssueUrl(String appId, String accountId, ServiceNowApprovalParams approvalParams);

  ServiceNowExecutionData getApprovalStatus(ApprovalPollingJobEntity entity);

  void handleServiceNowPolling(ApprovalPollingJobEntity entity);
}
