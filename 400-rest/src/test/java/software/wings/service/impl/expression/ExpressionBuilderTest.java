package software.wings.service.impl.expression;

import static io.harness.pcf.model.PcfConstants.CONTEXT_APP_FINAL_ROUTES_EXPR;
import static io.harness.pcf.model.PcfConstants.CONTEXT_APP_TEMP_ROUTES_EXPR;
import static io.harness.pcf.model.PcfConstants.CONTEXT_NEW_APP_GUID_EXPR;
import static io.harness.pcf.model.PcfConstants.CONTEXT_NEW_APP_NAME_EXPR;
import static io.harness.pcf.model.PcfConstants.CONTEXT_NEW_APP_ROUTES_EXPR;
import static io.harness.pcf.model.PcfConstants.CONTEXT_OLD_APP_GUID_EXPR;
import static io.harness.pcf.model.PcfConstants.CONTEXT_OLD_APP_NAME_EXPR;
import static io.harness.pcf.model.PcfConstants.CONTEXT_OLD_APP_ROUTES_EXPR;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_ARTIFACT_FILE_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_BUCKET_KEY;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_BUCKET_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_BUILDNO;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_BUILD_FULL_DISPLAYNAME;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_DESCRIPTION;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_DISPLAY_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_FILE_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_LABEL;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_METADATA_IMAGE;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_METADATA_TAG;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_PATH;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_REVISION;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_SOURCE_REGISTRY_URL;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_SOURCE_REPOSITORY_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_SOURCE_USER_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT_URL;
import static software.wings.service.impl.expression.ExpressionBuilder.HELM_CHART_BASE_PATH;
import static software.wings.service.impl.expression.ExpressionBuilder.HELM_CHART_BUCKET_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.HELM_CHART_DESCRIPTION;
import static software.wings.service.impl.expression.ExpressionBuilder.HELM_CHART_DISPLAY_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.HELM_CHART_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.HELM_CHART_REPO_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.HELM_CHART_URL;
import static software.wings.service.impl.expression.ExpressionBuilder.HELM_CHART_VERSION;
import static software.wings.service.impl.expression.ExpressionBuilder.INFRA_PCF_CLOUDPROVIDER_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.INFRA_PCF_ORG;
import static software.wings.service.impl.expression.ExpressionBuilder.INFRA_PCF_SPACE;
import static software.wings.service.impl.expression.ExpressionBuilder.PCF_PLUGIN_SERVICE_MANIFEST;
import static software.wings.service.impl.expression.ExpressionBuilder.PCF_PLUGIN_SERVICE_MANIFEST_REPO_ROOT;
import static software.wings.service.impl.expression.ExpressionBuilder.PIPELINE_DESCRIPTION;
import static software.wings.service.impl.expression.ExpressionBuilder.PIPELINE_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.PIPELINE_START_TS;
import static software.wings.service.impl.expression.ExpressionBuilder.SERVICE_DESCRIPTION;
import static software.wings.service.impl.expression.ExpressionBuilder.SERVICE_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.WORKFLOW_DESCRIPTION;
import static software.wings.service.impl.expression.ExpressionBuilder.WORKFLOW_DISPLAY_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.WORKFLOW_LAST_GOOD_DEPLOYMENT_DISPLAY_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.WORKFLOW_LAST_GOOD_RELEASE_NO;
import static software.wings.service.impl.expression.ExpressionBuilder.WORKFLOW_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.WORKFLOW_PIPELINE_DEPLOYMENT_UUID;
import static software.wings.service.impl.expression.ExpressionBuilder.WORKFLOW_RELEASE_NO;
import static software.wings.service.impl.expression.ExpressionBuilder.WORKFLOW_START_TS;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExpressionBuilderTest extends WingsBaseTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetStateTypeExpressions() {
    List<String> setupExpressions =
        new ArrayList<>(Arrays.asList(INFRA_PCF_ORG, INFRA_PCF_SPACE, INFRA_PCF_CLOUDPROVIDER_NAME));

    List<String> afterSetupExpressions = Arrays.asList(INFRA_PCF_ORG, INFRA_PCF_SPACE, INFRA_PCF_CLOUDPROVIDER_NAME,
        CONTEXT_NEW_APP_GUID_EXPR, CONTEXT_NEW_APP_NAME_EXPR, CONTEXT_NEW_APP_ROUTES_EXPR, CONTEXT_OLD_APP_GUID_EXPR,
        CONTEXT_OLD_APP_NAME_EXPR, CONTEXT_OLD_APP_ROUTES_EXPR, CONTEXT_APP_FINAL_ROUTES_EXPR,
        CONTEXT_APP_TEMP_ROUTES_EXPR);

    Set<String> expressionList = new HashSet<>();

    // SETUP
    expressionList.addAll(ExpressionBuilder.getStateTypeExpressions(StateType.PCF_SETUP));
    assertThat(expressionList).containsAll(setupExpressions);

    expressionList.clear();
    expressionList.addAll(ExpressionBuilder.getStateTypeExpressions(StateType.PCF_RESIZE));
    assertThat(expressionList).containsAll(setupExpressions);
    assertThat(expressionList).containsAll(afterSetupExpressions);

    expressionList.clear();
    expressionList.addAll(ExpressionBuilder.getStateTypeExpressions(StateType.PCF_PLUGIN));
    assertThat(expressionList).containsAll(setupExpressions);
    assertThat(expressionList).containsAll(afterSetupExpressions);
    assertThat(expressionList)
        .containsAll(Arrays.asList(PCF_PLUGIN_SERVICE_MANIFEST, PCF_PLUGIN_SERVICE_MANIFEST_REPO_ROOT));
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testGetStaticExpressions() {
    Set<String> staticExpressionSuggestions = ExpressionBuilder.getStaticExpressions(false);
    assertThat(staticExpressionSuggestions)
        .containsAll(asList(HELM_CHART_BASE_PATH, HELM_CHART_DESCRIPTION, HELM_CHART_NAME, HELM_CHART_REPO_NAME,
            HELM_CHART_URL, HELM_CHART_VERSION, HELM_CHART_BUCKET_NAME, HELM_CHART_DISPLAY_NAME));
    assertThat(staticExpressionSuggestions)
        .containsAll(asList(ARTIFACT_DISPLAY_NAME, ARTIFACT_BUILDNO, ARTIFACT_REVISION, ARTIFACT_DESCRIPTION,
            ARTIFACT_FILE_NAME, ARTIFACT_ARTIFACT_FILE_NAME, ARTIFACT_BUILD_FULL_DISPLAYNAME, ARTIFACT_BUCKET_NAME,
            ARTIFACT_BUCKET_KEY, ARTIFACT_PATH, ARTIFACT_URL, ARTIFACT_SOURCE_USER_NAME, ARTIFACT_SOURCE_REGISTRY_URL,
            ARTIFACT_SOURCE_REPOSITORY_NAME, ARTIFACT_METADATA_IMAGE, ARTIFACT_METADATA_TAG, ARTIFACT_LABEL));
    assertThat(staticExpressionSuggestions)
        .containsAll(asList(WORKFLOW_NAME, WORKFLOW_DESCRIPTION, WORKFLOW_DISPLAY_NAME, WORKFLOW_RELEASE_NO,
            WORKFLOW_LAST_GOOD_DEPLOYMENT_DISPLAY_NAME, WORKFLOW_LAST_GOOD_RELEASE_NO,
            WORKFLOW_PIPELINE_DEPLOYMENT_UUID, WORKFLOW_START_TS, PIPELINE_NAME, PIPELINE_DESCRIPTION,
            PIPELINE_START_TS, SERVICE_NAME, SERVICE_DESCRIPTION));
  }
}
