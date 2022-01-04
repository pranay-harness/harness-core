package io.harness.terraform.expression;

public interface TerraformPlanExpressionInterface {
  String FUNCTOR_NAME = "terraformPlan";
  String DELEGATE_FUNCTOR_NAME = "delegateTerraformPlan";

  String DELEGATE_EXPRESSION = "${" + DELEGATE_FUNCTOR_NAME + ".obtainPlan(\"%s\", %d).%s()}";

  String EXAMPLE_USAGE = "${" + FUNCTOR_NAME + ".jsonFilePath()}";
  String DESTROY_EXAMPLE_USAGE = "${" + FUNCTOR_NAME + ".destroy.jsonFilePath()}";

  String jsonFilePath();
}
