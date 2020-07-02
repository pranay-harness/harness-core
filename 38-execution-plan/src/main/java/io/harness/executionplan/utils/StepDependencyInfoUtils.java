package io.harness.executionplan.utils;

import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.stepsdependency.StepDependencyInstructor;
import lombok.experimental.UtilityClass;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@UtilityClass
public class StepDependencyInfoUtils {
  public final String STEP_DEPENDENCY_PROVIDER_LIST = "STEP_DEPENDENCY_PROVIDER_LIST";

  private <T> void setConfig(String key, T config, CreateExecutionPlanContext context) {
    if (config == null) {
      context.removeAttribute(key);
    } else {
      context.addAttribute(key, config);
    }
  }

  private <T> Optional<T> getConfig(String key, CreateExecutionPlanContext context) {
    return context.getAttribute(key);
  }

  public void addInstructor(StepDependencyInstructor instructor, CreateExecutionPlanContext context) {
    Optional<LinkedList<StepDependencyInstructor>> stepDependencyProviders =
        getConfig(STEP_DEPENDENCY_PROVIDER_LIST, context);
    LinkedList<StepDependencyInstructor> instructors = stepDependencyProviders.orElse(new LinkedList<>());
    instructors.addLast(instructor);
    setConfig(STEP_DEPENDENCY_PROVIDER_LIST, instructors, context);
  }

  public List<StepDependencyInstructor> getInstructorsList(CreateExecutionPlanContext context) {
    Optional<LinkedList<StepDependencyInstructor>> instructors = getConfig(STEP_DEPENDENCY_PROVIDER_LIST, context);
    return instructors.orElse(new LinkedList<>());
  }
}
