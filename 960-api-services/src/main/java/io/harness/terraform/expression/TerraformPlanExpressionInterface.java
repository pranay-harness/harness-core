/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.terraform.expression;

public interface TerraformPlanExpressionInterface {
  String FUNCTOR_NAME = "terraformPlan";
  String DELEGATE_FUNCTOR_NAME = "delegateTerraformPlan";

  String DELEGATE_EXPRESSION = "${" + DELEGATE_FUNCTOR_NAME + ".obtainPlan(\"%s\", %d).%s()}";

  String EXAMPLE_USAGE = "${" + FUNCTOR_NAME + ".jsonFilePath()}";
  String DESTROY_EXAMPLE_USAGE = "${" + FUNCTOR_NAME + ".destroy.jsonFilePath()}";

  String jsonFilePath();
}
