package software.wings.service.impl.expression;

import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

/**
 * Created by sgurubelli on 8/9/17.
 */
@Singleton
public class EnvironmentExpressionBuilder extends ExpressionBuilder {
  @Inject private ServiceExpressionBuilder serviceExpressionBuilder;

  @Override
  public List<String> getExpressions(String appId, String entityId, String serviceId) {
    if (serviceId == null) {
      return getExpressions(appId, entityId);
    }
    List<String> expressions = new ArrayList<>();
    expressions.addAll(getStaticExpressions());
    expressions.addAll(serviceExpressionBuilder.getDynamicExpressions(appId, serviceId));
    return expressions;
  }

  @Override
  public List<String> getExpressions(String appId, String entityId) {
    List<String> expressions = new ArrayList<>();
    expressions.addAll(getStaticExpressions());
    return expressions;
  }
  @Override
  public List<String> getDynamicExpressions(String appId, String entityId) {
    return null;
  }
}
