package io.harness.ci.plan.creator.filter;

import static io.harness.git.GitClientHelper.getGitRepo;
import static io.harness.pms.yaml.YAMLFieldNameConstants.CI;
import static io.harness.pms.yaml.YAMLFieldNameConstants.CI_CODE_BASE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.PROPERTIES;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ci.integrationstage.IntegrationStageUtils;
import io.harness.ci.plan.creator.filter.CIFilter.CIFilterBuilder;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.filters.FilterCreatorHelper;
import io.harness.ng.core.BaseNGAccess;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.filter.creation.FilterCreationResponse;
import io.harness.pms.filter.creation.FilterCreationResponse.FilterCreationResponseBuilder;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIStageFilterJsonCreator implements FilterJsonCreator<StageElementConfig> {
  @Inject ConnectorUtils connectorUtils;

  @Override
  public Class<StageElementConfig> getFieldClass() {
    return StageElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap("stage", Collections.singleton("CI"));
  }

  @Override
  public FilterCreationResponse handleNode(FilterCreationContext filterCreationContext, StageElementConfig yamlField) {
    FilterCreationResponseBuilder creationResponse = FilterCreationResponse.builder();

    String accountId = filterCreationContext.getSetupMetadata().getAccountId();
    String orgIdentifier = filterCreationContext.getSetupMetadata().getOrgId();
    String projectIdentifier = filterCreationContext.getSetupMetadata().getProjectId();

    BaseNGAccess baseNGAccess = BaseNGAccess.builder()
                                    .accountIdentifier(accountId)
                                    .orgIdentifier(orgIdentifier)
                                    .projectIdentifier(projectIdentifier)
                                    .build();
    YamlField variablesField =
        filterCreationContext.getCurrentField().getNode().getField(YAMLFieldNameConstants.VARIABLES);
    if (variablesField != null) {
      FilterCreatorHelper.checkIfVariableNamesAreValid(variablesField);
    }

    CIFilterBuilder ciFilterBuilder = CIFilter.builder();
    CodeBase ciCodeBase = null;
    try {
      YamlNode properties =
          YamlUtils.getGivenYamlNodeFromParentPath(filterCreationContext.getCurrentField().getNode(), PROPERTIES);
      YamlNode ciCodeBaseNode = properties.getField(CI).getNode().getField(CI_CODE_BASE).getNode();
      ciCodeBase = IntegrationStageUtils.getCiCodeBase(ciCodeBaseNode);
    } catch (Exception ex) {
      // Ignore exception because code base is not mandatory in case git clone is false
      log.warn("Failed to retrieve ciCodeBase from pipeline");
    }

    if (ciCodeBase != null) {
      if (ciCodeBase.getRepoName() != null) {
        ciFilterBuilder.repoName(ciCodeBase.getRepoName());
      } else if (ciCodeBase.getConnectorRef() != null) {
        try {
          ConnectorDetails connectorDetails =
              connectorUtils.getConnectorDetails(baseNGAccess, ciCodeBase.getConnectorRef());
          String repoName = getGitRepo(connectorUtils.retrieveURL(connectorDetails));
          ciFilterBuilder.repoName(repoName);
        } catch (Exception exception) {
          log.warn("Failed to retrieve repo");
        }
      }
    }

    return creationResponse.pipelineFilter(ciFilterBuilder.build()).build();
  }
}
