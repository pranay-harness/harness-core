/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.governance.services;

import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.governance.beans.SLOPolicyDTO;
import io.harness.cvng.governance.beans.SLOPolicyExpandedValue;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardApiFilter;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelObjectiveResponse;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelObjectiveService;
import io.harness.governance.ConnectorRefExpandedValue;
import io.harness.ng.beans.PageResponse;
import io.harness.pms.contracts.governance.ExpansionPlacementStrategy;
import io.harness.pms.contracts.governance.ExpansionRequestMetadata;
import io.harness.pms.sdk.core.governance.ExpandedValue;
import io.harness.pms.sdk.core.governance.ExpansionResponse;
import io.harness.pms.sdk.core.governance.JsonExpansionHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import java.util.List;

public class SLOPolicyExpansionHandler implements JsonExpansionHandler {
  @Inject ServiceLevelObjectiveService serviceLevelObjectiveService;
  @Override
  public ExpansionResponse expand(JsonNode fieldValue, ExpansionRequestMetadata metadata) {
    String accountId = metadata.getAccountId();
    String orgId = metadata.getOrgId();
    String projectId = metadata.getProjectId();
    String serviceRef = fieldValue.get("serviceConfig").asText("serviceRef");
    String environmentRef = fieldValue.get("infrastructure").asText("environmentRef");
    String monitoredServiceRef = serviceRef + "_" + environmentRef;
    ProjectParams projectParams =
        ProjectParams.builder().accountIdentifier(accountId).projectIdentifier(projectId).orgIdentifier(orgId).build();
    SLODashboardApiFilter sloDashboardApiFilter =
        SLODashboardApiFilter.builder().monitoredServiceIdentifier(monitoredServiceRef).build();
    PageParams pageParams = PageParams.builder().page(0).size(Integer.MAX_VALUE).build();
    PageResponse<ServiceLevelObjectiveResponse> serviceLevelObjectiveResponsePageResponse =
        serviceLevelObjectiveService.getSLOForDashboard(projectParams, sloDashboardApiFilter, pageParams);
    List<ServiceLevelObjectiveResponse> serviceLevelObjectiveResponseList =
        serviceLevelObjectiveResponsePageResponse.getContent();
    SLOPolicyDTO sloPolicyDTO = SLOPolicyDTO.builder().build();
    ExpandedValue value = SLOPolicyExpandedValue.builder().sloPolicyDTO(sloPolicyDTO).build();

    return ExpansionResponse.builder()
        .success(true)
        .key(value.getKey())
        .value(value)
        .placement(ExpansionPlacementStrategy.APPEND)
        .build();
  }
}
