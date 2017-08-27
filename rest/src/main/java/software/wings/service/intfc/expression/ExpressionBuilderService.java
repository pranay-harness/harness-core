package software.wings.service.intfc.expression;

import software.wings.beans.EntityType;
import software.wings.sm.StateType;

import java.util.Set;

/**
 * Created by sgurubelli on 8/7/17.
 */
public interface ExpressionBuilderService {
  Set<String> listExpressions(String appId, String entityId, EntityType entityType);
  Set<String> listExpressions(String appId, String entityId, EntityType entityType, String serviceId);
  Set<String> listExpressions(
      String appId, String entityId, EntityType entityType, String serviceId, StateType stateType);
}
