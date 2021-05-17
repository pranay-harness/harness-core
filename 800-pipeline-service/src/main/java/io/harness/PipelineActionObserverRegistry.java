package io.harness;

import io.harness.pms.ngpipeline.inputset.observers.InputSetsDeleteObserver;
import io.harness.pms.pipeline.PipelineEntityCrudObserver;
import io.harness.pms.pipeline.PipelineSetupUsageHelper;
import io.harness.pms.pipeline.observer.PipelineActionObserver;
import io.harness.pms.plan.execution.observers.PipelineExecutionSummaryDeleteObserver;

import com.google.common.collect.Sets;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class PipelineActionObserverRegistry {
  public Set<Class<? extends PipelineActionObserver>> getObservers() {
    return Sets.newHashSet(InputSetsDeleteObserver.class, PipelineEntityCrudObserver.class,
        PipelineExecutionSummaryDeleteObserver.class, PipelineSetupUsageHelper.class);
  }
}
