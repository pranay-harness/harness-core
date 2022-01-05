package io.harness.cdng.pipeline.executions;

import io.harness.account.services.AccountService;
import io.harness.cdng.pipeline.helpers.CDPipelineInstrumentationHelper;
import io.harness.dtos.InstanceDTO;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.repositories.executions.CDAccountExecutionMetadataRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class CDPipelineEndEventHandler implements OrchestrationEventHandler {
  @Inject CDAccountExecutionMetadataRepository cdAccountExecutionMetadataRepository;
  @Inject CDPipelineInstrumentationHelper cdPipelineInstrumentationHelper;
  @Inject AccountService accountService;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String planExecutionId = ambiance.getPlanExecutionId();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    io.harness.ng.core.dto.AccountDTO accountDTO = accountService.getAccount(accountId);
    String accountName = accountDTO.getName();
    String projectId = AmbianceUtils.getProjectIdentifier(ambiance);
    String orgId = AmbianceUtils.getOrgIdentifier(ambiance);
    String pipelineId = ambiance.getMetadata().getPipelineIdentifier();
    String identity = ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getExtraInfoMap().get("email");

    cdAccountExecutionMetadataRepository.updateAccountExecutionMetadata(accountId, event.getEndTs());

    cdPipelineInstrumentationHelper.sendServiceUsedEventsForPipelineExecution(
        pipelineId, identity, accountId, accountName, orgId, projectId, planExecutionId, event);

    long currentTS = System.currentTimeMillis();
    long searchingPeriod = 30L * 24 * 60 * 60 * 1000;

    List<InstanceDTO> serviceInstances = cdPipelineInstrumentationHelper.getServiceInstancesInInterval(
        accountId, orgId, projectId, currentTS - searchingPeriod, currentTS);

    cdPipelineInstrumentationHelper.sendCountOfServiceInstancesEvent(
        pipelineId, identity, accountId, accountName, orgId, projectId, serviceInstances);

    cdPipelineInstrumentationHelper.sendCountOfDistinctActiveServicesEvent(
        pipelineId, identity, accountId, accountName, orgId, projectId, serviceInstances);
  }
}
