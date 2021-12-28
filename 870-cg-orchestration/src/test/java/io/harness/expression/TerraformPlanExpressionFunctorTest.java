package io.harness.expression;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.provision.TerraformConstants.TF_APPLY_VAR_NAME;
import static io.harness.provision.TerraformConstants.TF_DESTROY_VAR_NAME;
import static io.harness.rule.OwnerRule.ABOSII;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.terraform.TerraformPlanParam;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.terraform.expression.TerraformPlanExpressionInterface;

import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class TerraformPlanExpressionFunctorTest extends CategoryTest {
  private static final int FUNCTOR_TOKEN = 1234567890;
  @Mock private Function<String, TerraformPlanParam> obtainTfPlanFunction;

  private TerraformPlanExpressionFunctor expressionFunctor;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.expressionFunctor = TerraformPlanExpressionFunctor.builder()
                                 .obtainTfPlanFunction(this.obtainTfPlanFunction)
                                 .expressionFunctorToken(FUNCTOR_TOKEN)
                                 .build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testJsonFilePath() {
    TerraformPlanParam tfPlanApply = TerraformPlanParam.builder().tfPlanJsonFileId("applyFileId").build();
    TerraformPlanParam tfPlanDestroy = TerraformPlanParam.builder().tfPlanJsonFileId("destroyFileId").build();

    doReturn(tfPlanApply).when(obtainTfPlanFunction).apply(TF_APPLY_VAR_NAME);
    doReturn(tfPlanDestroy).when(obtainTfPlanFunction).apply(TF_DESTROY_VAR_NAME);

    assertThat(expressionFunctor.jsonFilePath())
        .isEqualTo(
            format(TerraformPlanExpressionInterface.DELEGATE_EXPRESSION, "applyFileId", FUNCTOR_TOKEN, "jsonFilePath"));
    assertThat(expressionFunctor.destroy.jsonFilePath())
        .isEqualTo(format(
            TerraformPlanExpressionInterface.DELEGATE_EXPRESSION, "destroyFileId", FUNCTOR_TOKEN, "jsonFilePath"));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testReuseCachedValue() {
    doReturn(TerraformPlanParam.builder().tfPlanJsonFileId("applyFileIdOriginal").build())
        .when(obtainTfPlanFunction)
        .apply(TF_APPLY_VAR_NAME);
    doReturn(TerraformPlanParam.builder().tfPlanJsonFileId("destroyFileIdOriginal").build())
        .when(obtainTfPlanFunction)
        .apply(TF_DESTROY_VAR_NAME);
    expressionFunctor.jsonFilePath();
    expressionFunctor.destroy.jsonFilePath();

    doReturn(TerraformPlanParam.builder().tfPlanJsonFileId("applyFileIdUpdated").build())
        .when(obtainTfPlanFunction)
        .apply(TF_APPLY_VAR_NAME);

    doReturn(TerraformPlanParam.builder().tfPlanJsonFileId("destroyFileIdUpdated").build())
        .when(obtainTfPlanFunction)
        .apply(TF_DESTROY_VAR_NAME);

    assertThat(expressionFunctor.jsonFilePath())
        .isEqualTo(format(TerraformPlanExpressionInterface.DELEGATE_EXPRESSION, "applyFileIdOriginal", FUNCTOR_TOKEN,
            "jsonFilePath"));
    assertThat(expressionFunctor.destroy.jsonFilePath())
        .isEqualTo(format(TerraformPlanExpressionInterface.DELEGATE_EXPRESSION, "destroyFileIdOriginal", FUNCTOR_TOKEN,
            "jsonFilePath"));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testJsonFilePathNoPlan() {
    assertThatThrownBy(() -> expressionFunctor.jsonFilePath()).hasMessageContaining("EXPRESSION_EVALUATION_FAILED");
    assertThatThrownBy(() -> expressionFunctor.destroy.jsonFilePath())
        .hasMessageContaining("EXPRESSION_EVALUATION_FAILED");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testJsonFilePathNoFileId() {
    doReturn(TerraformPlanParam.builder().build()).when(obtainTfPlanFunction).apply(TF_APPLY_VAR_NAME);
    doReturn(TerraformPlanParam.builder().build()).when(obtainTfPlanFunction).apply(TF_DESTROY_VAR_NAME);

    assertThatThrownBy(() -> expressionFunctor.jsonFilePath()).hasMessageContaining("EXPRESSION_EVALUATION_FAILED");
    assertThatThrownBy(() -> expressionFunctor.destroy.jsonFilePath())
        .hasMessageContaining("EXPRESSION_EVALUATION_FAILED");
  }
}