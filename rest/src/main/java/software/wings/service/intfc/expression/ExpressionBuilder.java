package software.wings.service.intfc.expression;

import java.util.List;

/**
 * Created by sgurubelli on 8/7/17.
 */
public interface ExpressionBuilder {
  List<String> getExpressions(String appId, String entityId);

  List<String> getDynamicExpressions(String appId, String entityId);

  List<String> getStaticExpressions(String appId, String entityId);
}
