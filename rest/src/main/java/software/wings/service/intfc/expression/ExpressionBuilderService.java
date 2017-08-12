package software.wings.service.intfc.expression;

import software.wings.beans.EntityType;
import software.wings.sm.StateType;

import java.util.List;

/**
 * Created by sgurubelli on 8/7/17.
 */
public interface ExpressionBuilderService {
  List<String> listExpressions(String appId, String entityId, EntityType entityType);
  List<String> listExpressions(String appId, String entityId, EntityType entityType, String serviceId);
  List<String> listExpressions(
      String appId, String entityId, EntityType entityType, String serviceId, StateType stateType);
}
