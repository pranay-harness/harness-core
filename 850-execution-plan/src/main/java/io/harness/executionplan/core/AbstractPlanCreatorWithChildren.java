package io.harness.executionplan.core;

import io.harness.beans.WithIdentifier;
import io.harness.executionplan.plancreator.beans.PlanLevelNode;
import io.harness.executionplan.utils.ParentPathInfoUtils;

import java.util.List;
import java.util.Map;

public abstract class AbstractPlanCreatorWithChildren<T>
    extends AbstractPlanCreator<T> implements ExecutionPlanCreator<T> {
  /** Gets the planNodeType which is required for parent path. */
  protected abstract String getPlanNodeType(T input);

  /** Add parent path to the context. */
  private void addParentPathToContext(T input, ExecutionPlanCreationContext context) {
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
  private void removeParentPathFromContext(T input, ExecutionPlanCreationContext context) {
    if (input instanceof WithIdentifier) {
      ParentPathInfoUtils.removeFromParentPath(context);
    }
  }

  /** Create plan for children. */
  protected abstract Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      T input, ExecutionPlanCreationContext context);

  /** Create PlanNode for self. */
  protected abstract ExecutionPlanCreatorResponse createPlanForSelf(T input,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context);

  @Override
  public ExecutionPlanCreatorResponse createPlanForSelf(T input, ExecutionPlanCreationContext context) {
    addParentPathToContext(input, context);
    Map<String, List<ExecutionPlanCreatorResponse>> planForChildren = createPlanForChildren(input, context);
    removeParentPathFromContext(input, context);
    return createPlanForSelf(input, planForChildren, context);
  }
}
