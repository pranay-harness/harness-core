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
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.service.impl.expression.ExpressionBuilder.INFRA_PCF_CLOUDPROVIDER_NAME;
import static software.wings.service.impl.expression.ExpressionBuilder.INFRA_PCF_ORG;
import static software.wings.service.impl.expression.ExpressionBuilder.INFRA_PCF_SPACE;
import static software.wings.service.impl.expression.ExpressionBuilder.PCF_PLUGIN_SERVICE_MANIFEST;
import static software.wings.service.impl.expression.ExpressionBuilder.PCF_PLUGIN_SERVICE_MANIFEST_REPO_ROOT;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
}
