package io.harness.cvng.cdng.services.impl;

import static io.harness.walktree.visitor.utilities.VisitorParentPathUtils.PATH_CONNECTOR;

import io.harness.common.NGExpressionUtils;
import io.harness.cvng.cdng.beans.CVNGStepInfo;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.filters.FilterCreatorHelper;
import io.harness.filters.GenericStepPMSFilterJsonCreator;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CVNGStepFilterJsonCreator extends GenericStepPMSFilterJsonCreator {
  private static final String INFRASTRUCTURE_KEY = "infrastructure";
  private static final String SERVICE_CONFIG_KEY = "serviceConfig";
  private static final String SERVICE_REF_KEY = "serviceRef";
  private static final String ENVIRONMENT_REF_KEY = "environmentRef";
  @Inject private MonitoredServiceService monitoredServiceService;
  @Override
  public Set<String> getSupportedStepTypes() {
    return CVNGPlanCreator.CVNG_SUPPORTED_TYPES;
  }

  @Override
  public FilterCreationResponse handleNode(FilterCreationContext filterCreationContext, StepElementConfig yamlField) {
    Preconditions.checkState(yamlField.getStepSpecType() instanceof CVNGStepInfo);
    String accountIdentifier = filterCreationContext.getSetupMetadata().getAccountId();
    String orgIdentifier = filterCreationContext.getSetupMetadata().getOrgId();
    String projectIdentifier = filterCreationContext.getSetupMetadata().getProjectId();
    CVNGStepInfo cvngStepInfo = (CVNGStepInfo) yamlField.getStepSpecType();
    cvngStepInfo.validate();
    List<EntityDetailProtoDTO> result = new ArrayList<>();
    // This is handling the case when the monitoring service is defined. Runtime case needs to be handled separately
    // https://harness.atlassian.net/browse/CDNG-10512
    YamlNode stageLevelYamlNode = getStageSpecYamlNode(filterCreationContext.getCurrentField().getNode(), 4);
    String serviceIdentifier = getServiceRefNode(stageLevelYamlNode).asText();
    String envIdentifier = getEnvRefNode(stageLevelYamlNode).asText();

    if (!(NGExpressionUtils.isRuntimeOrExpressionField(serviceIdentifier)
            || NGExpressionUtils.isRuntimeOrExpressionField(envIdentifier))) {
      MonitoredServiceDTO monitoredServiceDTO = monitoredServiceService.getMonitoredServiceDTO(
          accountIdentifier, orgIdentifier, projectIdentifier, serviceIdentifier, envIdentifier);
      Preconditions.checkNotNull(monitoredServiceDTO, "MonitoredService does not exist for service %s and env %s",
          serviceIdentifier, envIdentifier);
      Preconditions.checkState(!monitoredServiceDTO.getSources().getHealthSources().isEmpty(),
          "No health sources exists for monitoredService for service %s and env %s", serviceIdentifier, envIdentifier);
      monitoredServiceDTO.getSources().getHealthSources().forEach(healthSource -> {
        String connectorIdentifier = healthSource.getSpec().getConnectorRef();
        String fullQualifiedDomainName =
            YamlUtils.getFullyQualifiedName(filterCreationContext.getCurrentField().getNode()) + PATH_CONNECTOR
            + YAMLFieldNameConstants.SPEC + PATH_CONNECTOR + "monitoredService.healthSources.connectorRef";
        result.add(FilterCreatorHelper.convertToEntityDetailProtoDTO(accountIdentifier, orgIdentifier,
            projectIdentifier, fullQualifiedDomainName, ParameterField.createValueField(connectorIdentifier),
            EntityTypeProtoEnum.CONNECTORS));
      });
    }
    return FilterCreationResponse.builder().referredEntities(result).build();
  }

  private YamlNode getServiceRefNode(YamlNode stageYaml) {
    return stageYaml.getField(SERVICE_CONFIG_KEY).getNode().getField(SERVICE_REF_KEY).getNode();
  }
  private YamlNode getEnvRefNode(YamlNode stageYaml) {
    return stageYaml.getField(INFRASTRUCTURE_KEY).getNode().getField(ENVIRONMENT_REF_KEY).getNode();
  }

  private YamlNode getStageSpecYamlNode(YamlNode yamlNode, int parentNo) {
    Preconditions.checkNotNull(yamlNode, "Invalid yaml. Can't find stage spec.");
    if (parentNo == 0) {
      return yamlNode;
    } else {
      return getStageSpecYamlNode(yamlNode.getParentNode(), parentNo - 1);
    }
  }
}
