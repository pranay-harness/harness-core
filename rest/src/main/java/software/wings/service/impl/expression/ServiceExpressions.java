package software.wings.service.impl.expression;

import java.util.Arrays;
import java.util.List;

/**
 * Created by sgurubelli on 8/7/17.
 */
public class ServiceExpressions extends ExpressionBuilder {
  public static final String appName = "app.name";
  public static final String appDescription = "app.description";
  public static final String serviceName = "service.name";
  public static final String serviceDescription = "service.description";

  @Override
  public List<String> getExpressions() {
    return Arrays.asList(appName, appDescription, serviceName, serviceDescription);
  }

  @Override
  public List<String> getDynamicExpressions() {
    return Arrays.asList();
  }

  public static class Builder {
    protected String appId;
    protected String entityId;

    private Builder() {}
    public static Builder aServiceExpressions() {
      return new Builder();
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public ServiceExpressions build() {
      ServiceExpressions serviceExpressions = new ServiceExpressions();
      serviceExpressions.setAppId(this.appId);
      serviceExpressions.setEntityId(this.entityId);
      return serviceExpressions;
    }
  }
}
