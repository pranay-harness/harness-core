package software.wings.service.intfc.expression;

import java.util.List;

/**
 * Created by sgurubelli on 8/7/17.
 */
public interface ExpressionBuilder {
  public abstract List<String> getExpressions();

  public abstract List<String> getDynamicExpressions(String appId, String entityId);
}
