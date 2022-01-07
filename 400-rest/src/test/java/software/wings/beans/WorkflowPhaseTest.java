/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.rule.OwnerRule.SRINIVAS;

import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.getInfraTemplateExpression;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.getServiceTemplateExpression;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.prepareInfraDefTemplateExpression;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.ArrayList;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WorkflowPhaseTest extends CategoryTest {
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFetchTemplateExpressionNames() {
    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .templateExpressions(asList(getServiceTemplateExpression(),
                                          getInfraTemplateExpression(), prepareInfraDefTemplateExpression()))
                                      .build();

    assertThat(workflowPhase.fetchServiceTemplatizedName()).isNotNull().isEqualTo("Service");
    assertThat(workflowPhase.fetchInfraMappingTemplatizedName()).isNotNull().isEqualTo("ServiceInfra_SSH");
    assertThat(workflowPhase.fetchInfraDefinitionTemplatizedName()).isNotNull().isEqualTo("InfraDef_SSH");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFetchEmptyTemplateExpressionNames() {
    WorkflowPhase workflowPhase = aWorkflowPhase().templateExpressions(new ArrayList<>()).build();

    assertThat(workflowPhase.fetchServiceTemplatizedName()).isNullOrEmpty();
    assertThat(workflowPhase.fetchInfraMappingTemplatizedName()).isNullOrEmpty();
    assertThat(workflowPhase.fetchInfraDefinitionTemplatizedName()).isNullOrEmpty();
  }
}
