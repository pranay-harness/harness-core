package io.harness.executionplan.core;

import io.harness.executionplan.plancreator.beans.PlanLevelNode;
import io.harness.executionplan.utils.ParentPathInfoUtils;
import io.harness.yaml.core.intfc.WithIdentifier;

import java.util.List;
import java.util.Map;

public abstract class AbstractPlanCreatorWithChildren<T>
    extends AbstractPlanCreator<T> implements ExecutionPlanCreator<T> {
  /** Gets the planNodeType which is required for parent path. */
  protected abstract String getPlanNodeType(T input);

  /** Add parent path to the context. */
  private void addParentPathToContext(T input, CreateExecutionPlanContext context) {
    if (input instanceof WithIdentifier) {
      WithIdentifier withIdentifier = (WithIdentifier) input;
      ParentPathInfoUtils.addToParentPath(context,
          PlanLevelNode.builder()
              .planNodeType(getPlanNodeType(input))
              .identifier(withIdentifier.getIdentifier())
              .build());
    }
  }

  /** Remove parent path from the context. */
  private void removeParentPathFromContext(T input, CreateExecutionPlanContext context) {
    if (input instanceof WithIdentifier) {
      ParentPathInfoUtils.removeFromParentPath(context);
    }
  }

  /** Create plan for children. */
  protected abstract Map<String, List<CreateExecutionPlanResponse>> createPlanForChildren(
      T input, CreateExecutionPlanContext context);

  /** Create PlanNode for self. */
  protected abstract CreateExecutionPlanResponse createPlanForSelf(
      T input, Map<String, List<CreateExecutionPlanResponse>> planForChildrenMap, CreateExecutionPlanContext context);

  @Override
  public CreateExecutionPlanResponse createPlanForSelf(T input, CreateExecutionPlanContext context) {
    addParentPathToContext(input, context);
    Map<String, List<CreateExecutionPlanResponse>> planForChildren = createPlanForChildren(input, context);
    removeParentPathFromContext(input, context);
    return createPlanForSelf(input, planForChildren, context);
  }
}
