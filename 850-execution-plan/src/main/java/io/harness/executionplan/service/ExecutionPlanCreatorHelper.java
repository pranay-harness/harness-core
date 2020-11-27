package io.harness.executionplan.service;

import io.harness.exception.NoResultFoundException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorRegistry;
import io.harness.executionplan.core.impl.PlanCreatorSearchContextImpl;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutionPlanCreatorHelper {
  @Inject private ExecutionPlanCreatorRegistry executionPlanCreatorRegistry;

  public <T> ExecutionPlanCreator<T> getExecutionPlanCreator(
      String type, T objectToPlan, ExecutionPlanCreationContext context, String errorMsg) {
    return executionPlanCreatorRegistry
        .<T>obtainCreator(PlanCreatorSearchContextImpl.<T>builder()
                              .type(type)
                              .createExecutionPlanContext(context)
                              .objectToPlan(objectToPlan)
                              .build())
        .orElseThrow(() -> NoResultFoundException.newBuilder().message(errorMsg).build());
  }
}
