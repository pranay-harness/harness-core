package software.wings.service.impl.expression;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by sgurubelli on 8/9/17.
 */
@Singleton
public class EnvironmentExpressionBuilder extends ExpressionBuilder {
  @Inject private ServiceExpressionBuilder serviceExpressionBuilder;

  @Override
  public Set<String> getExpressions(String appId, String entityId, String serviceId) {
    Set<String> expressions = new TreeSet<>(getStaticExpressions());
    if (isNotBlank(serviceId)) {
      expressions.addAll(serviceExpressionBuilder.getDynamicExpressions(appId, serviceId));
      expressions.addAll(getServiceTemplateVariableExpressions(appId, entityId, serviceId));
    } else {
      expressions.addAll(getServiceTemplateVariableExpressions(appId, entityId));
    }

    return expressions;
  }

  @Override
  public Set<String> getExpressions(String appId, String entityId) {
    return getStaticExpressions();
  }
  @Override
  public Set<String> getDynamicExpressions(String appId, String entityId) {
    return new TreeSet<>();
  }

  public Set<String> getServiceTemplateVariableExpressions(String appId, String envId, String serviceId) {
    List<String> serviceIds = serviceExpressionBuilder.getServiceIds(appId, serviceId);

    if (EmptyPredicate.isEmpty(serviceIds)) {
      return Collections.emptySet();
    }

    return getServiceVariablesOfTemplates(appId,
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter("appId", EQ, appId)
            .addFilter("envId", EQ, envId)
            .addFilter("serviceId", IN, serviceIds.toArray())
            .addFieldsIncluded("uuid")
            .build(),
        SERVICE);
  }

  public Set<String> getServiceTemplateVariableExpressions(String appId, String envId) {
    return getServiceVariablesOfTemplates(appId,
        aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, appId).addFilter("envId", EQ, envId).build(),
        ENVIRONMENT);
  }
}
