/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.beans.EnvironmentType.NON_PROD;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.TriggeredBy.triggeredBy;
import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.rule.OwnerRule.VGLIJIN;

import static software.wings.beans.Activity.Type.Verification;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;

import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ActivityTest extends CategoryTest {
  private static final ExecutionContextImpl executionContext = mock(ExecutionContextImpl.class);
  private static final Application application = mock(Application.class);
  private static final Environment environment = mock(Environment.class);
  private static final WorkflowStandardParams workflowStandardParams = mock(WorkflowStandardParams.class);
  private static final InstanceElement instanceElement = mock(InstanceElement.class);
  private static final EmbeddedUser embeddedUser = mock(EmbeddedUser.class);
  private static final ServiceTemplateElement serviceTemplateElement = mock(ServiceTemplateElement.class);

  static {
    when(executionContext.getWorkflowType()).thenReturn(ORCHESTRATION);
    when(executionContext.getWorkflowExecutionName()).thenReturn("workflowExecutionName");
    when(executionContext.getStateExecutionInstanceId()).thenReturn("stateExecutionInstanceId");
    when(executionContext.getStateExecutionInstanceName()).thenReturn("stateExecutionInstanceName");
    when(executionContext.getWorkflowId()).thenReturn("workflowId");
    when(executionContext.getWorkflowExecutionId()).thenReturn("workflowExecutionId");

    when(executionContext.fetchRequiredApp()).thenReturn(application);
    when(executionContext.getContextElement(ContextElementType.INSTANCE)).thenReturn(instanceElement);
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(executionContext.getEnv()).thenReturn(environment);

    when(application.getAppId()).thenReturn("appId");
    when(application.getName()).thenReturn("appName");

    when(workflowStandardParams.getCurrentUser()).thenReturn(embeddedUser);

    when(embeddedUser.getName()).thenReturn("currentUser.name");
    when(embeddedUser.getEmail()).thenReturn("currentUser.email");

    when(environment.getName()).thenReturn("environment.name");
    when(environment.getUuid()).thenReturn("environment.uuid");
    when(environment.getEnvironmentType()).thenReturn(NON_PROD);

    when(instanceElement.getUuid()).thenReturn("instanceElement.uuid");
    when(instanceElement.getHost()).thenReturn(mock(HostElement.class));
    when(instanceElement.getHost().getHostName()).thenReturn("instanceElement.host.hostName");
    when(instanceElement.getServiceTemplateElement()).thenReturn(serviceTemplateElement);

    when(serviceTemplateElement.getUuid()).thenReturn("serviceTemplateElement.uuid");
    when(serviceTemplateElement.getName()).thenReturn("serviceTemplateElement.name");
    when(serviceTemplateElement.getServiceElement()).thenReturn(mock(ServiceElement.class));
    when(serviceTemplateElement.getServiceElement().getUuid()).thenReturn("serviceElement.uuid");
    when(serviceTemplateElement.getServiceElement().getName()).thenReturn("serviceElement.name");
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void withExecutionContextTest() {
    final Activity activity = new Activity();
    activity.setStatus(RUNNING);
    activity.setAppId(application.getUuid());
    activity.setApplicationName(application.getName());
    activity.setType(Verification);
    activity.setWorkflowType(executionContext.getWorkflowType());
    activity.setWorkflowExecutionName(executionContext.getWorkflowExecutionName());

    activity.setStateExecutionInstanceId(executionContext.getStateExecutionInstanceId());
    activity.setStateExecutionInstanceName(executionContext.getStateExecutionInstanceName());
    activity.setWorkflowId(executionContext.getWorkflowId());
    activity.setWorkflowExecutionId(executionContext.getWorkflowExecutionId());
    activity.setCommandUnits(emptyList());
    activity.setTriggeredBy(triggeredBy(embeddedUser.getName(), embeddedUser.getEmail()));
    activity.setEnvironmentId(environment.getUuid());
    activity.setEnvironmentName(environment.getName());
    activity.setEnvironmentType(environment.getEnvironmentType());

    activity.setServiceTemplateId(serviceTemplateElement.getUuid());
    activity.setServiceTemplateName(serviceTemplateElement.getName());
    activity.setServiceId(serviceTemplateElement.getServiceElement().getUuid());
    activity.setServiceName(serviceTemplateElement.getServiceElement().getName());
    activity.setServiceInstanceId(instanceElement.getUuid());
    activity.setHostName(instanceElement.getHost().getHostName());

    ActivityBuilder builder = Activity.builder();
    ExecutionContextImpl.populateActivity(builder, executionContext);
    assertThat(builder.build()).isEqualToIgnoringGivenFields(activity, "validUntil");
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void withStateTest() {
    State state = mock(State.class);
    when(state.getName()).thenReturn("state.name");
    when(state.getStateType()).thenReturn("state.type");
    final Activity activity = new Activity();
    activity.setCommandName(state.getName());
    activity.setCommandType(state.getStateType());

    ActivityBuilder builder = Activity.builder();
    State.populateActivity(builder, state);
    assertThat(builder.build()).isEqualToIgnoringGivenFields(activity, "validUntil");
  }
}
