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
import io.harness.filters.GenericStageFilterJsonCreator;
import io.harness.ng.core.BaseNGAccess;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.pipeline.filter.PipelineFilter;
import io.harness.pms.sdk.core.filter.creation.beans.FilterCreationContext;
import io.harness.pms.yaml.PmsYamlUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIStageFilterJsonCreator extends GenericStageFilterJsonCreator {
  @Inject ConnectorUtils connectorUtils;
  @Inject private SimpleVisitorFactory simpleVisitorFactory;

  @Override
  public Set<String> getSupportedStageTypes() {
    return Collections.singleton("CI");
  }

  @Override
  public PipelineFilter getFilter(FilterCreationContext filterCreationContext, StageElementConfig stageElementConfig) {
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
          PmsYamlUtils.getGivenYamlNodeFromParentPath(filterCreationContext.getCurrentField().getNode(), PROPERTIES);
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

    return ciFilterBuilder.build();
  }
}
