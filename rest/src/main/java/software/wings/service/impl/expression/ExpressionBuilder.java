package software.wings.service.impl.expression;

import java.util.List;

/**
 * Created by sgurubelli on 8/7/17.
 */
public abstract class ExpressionBuilder {
  protected String appId;
  protected String entityId;

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public abstract List<String> getExpressions();

  public abstract List<String> getDynamicExpressions();
}
