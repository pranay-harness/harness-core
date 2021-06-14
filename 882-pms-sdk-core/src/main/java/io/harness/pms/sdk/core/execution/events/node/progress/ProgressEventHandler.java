package io.harness.pms.sdk.core.execution.events.node.progress;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.progress.ProgressEvent;
import io.harness.pms.sdk.core.execution.ExecutableProcessor;
import io.harness.pms.sdk.core.execution.ExecutableProcessorFactory;
import io.harness.pms.sdk.core.execution.ProgressPackage;
import io.harness.pms.sdk.core.execution.events.node.NodeBaseEventHandler;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ProgressData;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class ProgressEventHandler extends NodeBaseEventHandler<ProgressEvent> {
  @Inject private ExecutableProcessorFactory executableProcessorFactory;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  @NonNull
  protected Map<String, String> extraLogProperties(ProgressEvent event) {
    return ImmutableMap.<String, String>builder().put("eventType", NodeExecutionEventType.PROGRESS.name()).build();
  }

  @Override
  protected Ambiance extractAmbiance(ProgressEvent event) {
    return event.getAmbiance();
  }

  @Override
  protected boolean handleEventWithContext(ProgressEvent event) {
    try {
      log.info("Starting to handle PROGRESS event");
      StepParameters stepParameters =
          RecastOrchestrationUtils.fromDocumentJson(event.getStepParameters().toStringUtf8(), StepParameters.class);
      ProgressData progressData =
          (ProgressData) kryoSerializer.asInflatedObject(event.getProgressBytes().toByteArray());

      ExecutableProcessor processor = executableProcessorFactory.obtainProcessor(event.getExecutionMode());
      ProgressPackage progressPackage = ProgressPackage.builder()
                                            .ambiance(event.getAmbiance())
                                            .stepParameters(stepParameters)
                                            .progressData(progressData)
                                            .build();
      processor.handleProgress(progressPackage);
      log.info("PROGRESS Event Handled Successfully");
      return true;
    } catch (Exception ex) {
      log.error("Error while Handling progress", ex);
      return false;
    }
  }
}
