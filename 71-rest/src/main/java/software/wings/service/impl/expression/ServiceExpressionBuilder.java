package software.wings.service.impl.expression;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import software.wings.api.DeploymentType;
import software.wings.api.PcfInstanceElement.PcfInstanceElementKeys;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by sgurubelli on 8/7/17.
 */
@Singleton
public class ServiceExpressionBuilder extends ExpressionBuilder {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private AppService appService;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public Set<String> getExpressions(String appId, String entityId) {
    SortedSet<String> expressions = new TreeSet<>();
    String accountId = appService.getAccountIdByAppId(appId);
    boolean isMultiArtifact = featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId);
    expressions.addAll(getStaticExpressions(isMultiArtifact));
    expressions.addAll(getDynamicExpressions(appId, entityId));
    expressions.addAll(getServiceTemplateVariableExpressions(appId, entityId, ENVIRONMENT));
    return expressions;
  }

  @Override
  public Set<String> getExpressions(String appId, String entityId, StateType stateType) {
    if (stateType == null) {
      return getExpressions(appId, entityId);
    }
    SortedSet<String> expressions = new TreeSet<>(getExpressions(appId, entityId));
    expressions.addAll(getStateTypeExpressions(stateType));
    return expressions;
  }

  @Override
  public Set<String> getDynamicExpressions(String appId, String entityId) {
    return getServiceVariables(appId, getServiceIds(appId, entityId), SERVICE);
  }

  public List<String> getServiceIds(String appId, String entityId) {
    if (entityId.equalsIgnoreCase("All")) {
      List<Service> services = serviceResourceService.list(
          aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, appId).addFieldsIncluded("uuid").build(), false,
          false, false, null);
      return services.stream().map(Service::getUuid).collect(toList());
    } else {
      return asList(Misc.commaCharPattern.split(entityId));
    }
  }

  public Set<String> getServiceTemplateVariableExpressions(String appId, String serviceId, EntityType entityType) {
    List<String> serviceIds = getServiceIds(appId, serviceId);
    if (!serviceIds.isEmpty()) {
      PageRequest<ServiceTemplate> serviceTemplatePageRequest = aPageRequest()
                                                                    .withLimit(UNLIMITED)
                                                                    .addFilter("appId", EQ, appId)
                                                                    .addFilter("serviceId", IN, serviceIds.toArray())
                                                                    .build();
      return getServiceVariablesOfTemplates(appId, serviceTemplatePageRequest, entityType);
    }
    return new TreeSet<>();
  }

  public List<String> getContinuousVerificationVariables(String appId, String serviceId) {
    final Service service = serviceResourceService.get(appId, serviceId);
    if (service == null) {
      return new ArrayList<>();
    }
    List<String> rv = new ArrayList<>();
    rv.add("host.hostName");
    if (service.getDeploymentType() == DeploymentType.PCF) {
      rv.add("host.pcfElement." + PcfInstanceElementKeys.applicationId);
      rv.add("host.pcfElement." + PcfInstanceElementKeys.displayName);
      rv.add("host.pcfElement." + PcfInstanceElementKeys.instanceIndex);
    }
    return rv;
  }
}
