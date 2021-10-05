package io.harness.pms.instrumentaion;

import static io.harness.telemetry.Destination.AMPLITUDE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.OrchestrationEndObserver;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.pipeline.observer.OrchestrationObserverUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.plan.execution.service.PipelineExecutionSummaryService;
import io.harness.pms.security.PmsSecurityContextEventGuard;
import io.harness.telemetry.Category;
import io.harness.telemetry.TelemetryReporter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class InstrumentationPipelineEndEventHandler implements OrchestrationEndObserver {
  @Inject TelemetryReporter telemetryReporter;
  @Inject PipelineExecutionSummaryService pipelineExecutionSummaryService;

  private static String STAGE_TYPE = "stage_type";
  private static String STATUS = "status";
  private static String PIPELINE_EXECUTION = "pipeline_execution";

  @Override
  public void onEnd(Ambiance ambiance) {
    String planExecutionId = ambiance.getPlanExecutionId();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String projectId = AmbianceUtils.getProjectIdentifier(ambiance);
    String orgId = AmbianceUtils.getOrgIdentifier(ambiance);
    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity =
        pipelineExecutionSummaryService.getPipelineExecutionSummaryEntity(
            accountId, orgId, projectId, planExecutionId, false);

    Set<String> executedModules =
        OrchestrationObserverUtils.getExecutedModulesInPipeline(pipelineExecutionSummaryEntity);

    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      for (String module : executedModules) {
        HashMap<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put(STAGE_TYPE, module);
        propertiesMap.put(STATUS, pipelineExecutionSummaryEntity.getStatus());
        telemetryReporter.sendTrackEvent(
            PIPELINE_EXECUTION, propertiesMap, Collections.singletonMap(AMPLITUDE, true), Category.GLOBAL);
      }

    } catch (Exception exception) {
      log.error("Could not add principal in security context", exception);
    }
  }
}
