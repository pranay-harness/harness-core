/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.common;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SRINIVAS;

import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.service.intfc.ServiceTemplateService.EncryptedFieldComputeMode.OBTAIN_VALUE;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.beans.ServiceVariable;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.sm.WorkflowStandardParams;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Created by peeyushaggarwal on 9/27/16.
 */
public class VariableProcessorTest extends CategoryTest {
  /**
   * The Mockito rule.
   */
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ServiceTemplateService serviceTemplateService;

  @InjectMocks private VariableProcessor variableProcessor = new VariableProcessor();

  /**
   * Should get no variables.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldGetNoVariables() throws Exception {
    assertThat(variableProcessor.getVariables(new ArrayDeque<>(), null)).isEmpty();
  }

  /**
   * Should get variables for instance element.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGetVariablesForInstanceElement() {
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, OBTAIN_VALUE))
        .thenReturn(asList(ServiceVariable.builder().name("PORT").value("8080".toCharArray()).build()));

    WorkflowStandardParams workflowStandardParams =
        aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
    InstanceElement instanceElement =
        anInstanceElement()
            .serviceTemplateElement(aServiceTemplateElement().withUuid(TEMPLATE_ID).build())
            .host(HostElement.builder().uuid(HOST_ID).build())
            .build();

    assertThat(variableProcessor.getVariables(new ArrayDeque<>(asList(workflowStandardParams, instanceElement)), null))
        .hasSize(1)
        .containsAllEntriesOf(ImmutableMap.of("PORT", "8080"));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetVariablesForInstanceElementWithoutServiceTemplate() {
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, OBTAIN_VALUE))
        .thenReturn(asList(ServiceVariable.builder().name("PORT").value("8080".toCharArray()).build()));

    WorkflowStandardParams workflowStandardParams =
        aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
    InstanceElement instanceElement = anInstanceElement()

                                          .host(HostElement.builder().uuid(HOST_ID).build())
                                          .build();

    assertThat(variableProcessor.getVariables(new ArrayDeque<>(asList(workflowStandardParams, instanceElement)), null))
        .hasSize(0);
  }
}
