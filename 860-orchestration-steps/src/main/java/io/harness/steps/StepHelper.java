package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.ng.core.environment.beans.EnvironmentType.Production;
import static io.harness.telemetry.Destination.AMPLITUDE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.steps.environment.EnvironmentOutcome;
import io.harness.telemetry.TelemetryReporter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
@Singleton
public class StepHelper {
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private TelemetryReporter telemetryReporter;

  public static String TELEMETRY_ROLLBACK_PROP_NAME = "name";
  public static String TELEMETRY_ROLLBACK_PROP_LEVEL = "level";
  public static String TELEMETRY_ROLLBACK_PROP_STATUS = "status";
  public static String ROLLBACK_EXECUTION = "rollbackExecution";

  public EnvironmentType getEnvironmentType(Ambiance ambiance) {
    OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputResolver.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(OutputExpressionConstants.ENVIRONMENT));
    if (!optionalSweepingOutput.isFound()) {
      return EnvironmentType.ALL;
    }

    EnvironmentOutcome envOutcome = (EnvironmentOutcome) optionalSweepingOutput.getOutput();

    if (envOutcome == null || envOutcome.getType() == null) {
      return EnvironmentType.ALL;
    }

    return Production == envOutcome.getType() ? EnvironmentType.PROD : EnvironmentType.NON_PROD;
  }

  public Map<String, Object> sendRollbackTelemetryEvent(Ambiance ambiance, Status status) {
    HashMap<String, Object> properties = null;

    if (ambiance != null && status != null) {
      Level level = AmbianceUtils.obtainCurrentLevel(ambiance);

      if (level != null) {
        if (telemetryReporter != null) {
          properties = new HashMap<>();

          properties.put(TELEMETRY_ROLLBACK_PROP_NAME, level.getIdentifier());
          properties.put(TELEMETRY_ROLLBACK_PROP_LEVEL, level.getGroup());
          properties.put(TELEMETRY_ROLLBACK_PROP_STATUS, String.valueOf(status));

          log.info(String.format("Sending Rollback Telemetry event: [name=%s], [level=%s], [status=%s]",
              properties.get(TELEMETRY_ROLLBACK_PROP_NAME), properties.get(TELEMETRY_ROLLBACK_PROP_LEVEL),
              properties.get(TELEMETRY_ROLLBACK_PROP_STATUS)));

          telemetryReporter.sendTrackEvent(ROLLBACK_EXECUTION, properties, Collections.singletonMap(AMPLITUDE, true),
              io.harness.telemetry.Category.GLOBAL);

          return properties;
        } else {
          log.error("TelemetryReporter was not injected.");
        }
      } else {
        log.error("Can not obtain current level.");
      }
    } else {
      log.error("One or more arguments for method io.harness.steps.StepHelper.sendRollbackTelemetryEvent are invalid.");
    }

    log.error("Unable to send rollback telemetry event!");
    return properties;
  }
}
