/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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

import static software.wings.service.impl.expression.ExpressionBuilder.ARTIFACT;
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
import static software.wings.service.impl.expression.ExpressionBuilder.PCF_PLUGIN_SERVICE_CLI;
import static software.wings.service.impl.expression.ExpressionBuilder.PCF_PLUGIN_SERVICE_MANIFEST;
import static software.wings.service.impl.expression.ExpressionBuilder.PCF_PLUGIN_SERVICE_MANIFEST_REPO_ROOT;
import static software.wings.service.impl.expression.ExpressionBuilder.PIPELINE_DESCRIPTION;
import static software.wings.service.impl.expression.ExpressionBuilder.PIPELINE_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.PIPELINE_START_TS;
import static software.wings.service.impl.expression.ExpressionBuilder.ROLLBACK_ARTIFACT_PREFIX;
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
import java.util.stream.Collectors;
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
        .containsAll(
            Arrays.asList(PCF_PLUGIN_SERVICE_MANIFEST, PCF_PLUGIN_SERVICE_MANIFEST_REPO_ROOT, PCF_PLUGIN_SERVICE_CLI));
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
        .containsAll(ExpressionBuilder.getArtifactExpressionSuffixes()
                         .stream()
                         .map(ARTIFACT::concat)
                         .collect(Collectors.toSet()));
    assertThat(staticExpressionSuggestions)
        .containsAll(ExpressionBuilder.getArtifactExpressionSuffixes()
                         .stream()
                         .map(ROLLBACK_ARTIFACT_PREFIX::concat)
                         .collect(Collectors.toSet()));
    assertThat(staticExpressionSuggestions)
        .containsAll(asList(WORKFLOW_NAME, WORKFLOW_DESCRIPTION, WORKFLOW_DISPLAY_NAME, WORKFLOW_RELEASE_NO,
            WORKFLOW_LAST_GOOD_DEPLOYMENT_DISPLAY_NAME, WORKFLOW_LAST_GOOD_RELEASE_NO,
            WORKFLOW_PIPELINE_DEPLOYMENT_UUID, WORKFLOW_START_TS, PIPELINE_NAME, PIPELINE_DESCRIPTION,
            PIPELINE_START_TS, SERVICE_NAME, SERVICE_DESCRIPTION));
  }
}
