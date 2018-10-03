package software.wings.common;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.service.intfc.ServiceTemplateService.EncryptedFieldComputeMode.OBTAIN_VALUE;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.api.InstanceElement;
import software.wings.beans.ServiceVariable;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayDeque;

/**
 * Created by peeyushaggarwal on 9/27/16.
 */
public class VariableProcessorTest {
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
  public void shouldGetNoVariables() throws Exception {
    assertThat(variableProcessor.getVariables(new ArrayDeque<>(), null)).isEmpty();
  }

  /**
   * Should get variables for instance element.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldGetVariablesForInstanceElement() throws Exception {
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, OBTAIN_VALUE))
        .thenReturn(asList(ServiceVariable.builder().name("PORT").value("8080".toCharArray()).build()));

    WorkflowStandardParams workflowStandardParams =
        aWorkflowStandardParams().withAppId(APP_ID).withEnvId(ENV_ID).build();
    InstanceElement instanceElement =
        anInstanceElement()
            .withServiceTemplateElement(aServiceTemplateElement().withUuid(TEMPLATE_ID).build())
            .withHost(aHostElement().withUuid(HOST_ID).build())
            .build();

    assertThat(variableProcessor.getVariables(new ArrayDeque<>(asList(workflowStandardParams, instanceElement)), null))
        .hasSize(1)
        .containsAllEntriesOf(ImmutableMap.of("PORT", "8080"));
  }
}
