package software.wings.sm.states;

import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.IntegrationTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerMetadata;
import software.wings.service.impl.instance.ContainerInstanceHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Created by rsingh on 4/9/18.
 */
@Slf4j
public class EcsContainerInfoIntegrationTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private ScmSecret scmSecret;
  @Inject private AwsHelperService awsHelperService;
  @Inject private ContainerService containerService;
  @Mock private ContainerInstanceHandler containerInstanceHandler;
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private AppService appService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  private final String workflowId = UUID.randomUUID().toString();
  private final String appId = UUID.randomUUID().toString();
  private final String accountId = UUID.randomUUID().toString();
  private final String previousWorkflowExecutionId = UUID.randomUUID().toString();

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    when(containerInstanceHandler.isContainerDeployment(anyObject())).thenReturn(true);
    when(infraMappingService.get(anyString(), anyString())).thenReturn(null);
    when(appService.get(appId))
        .thenReturn(Application.Builder.anApplication().uuid(appId).accountId(accountId).build());
    when(delegateProxyFactory.get(eq(ContainerService.class), any(SyncTaskContext.class))).thenReturn(containerService);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testGetLastExecutionNodesECS() throws IllegalAccessException {
    AwsConfig awsConfig = AwsConfig.builder()
                              .accessKey(scmSecret.decryptToString(new SecretName("aws_config_access_key_1")))
                              .secretKey(scmSecret.decryptToCharArray(new SecretName("aws_config_secret_key_1")))
                              .accountId(accountId)
                              .build();
    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(awsConfig).build();
    String awsConfigid = wingsPersistence.save(settingAttribute);
    WorkflowExecution workflowExecution = WorkflowExecution.builder()
                                              .appId(appId)
                                              .uuid(previousWorkflowExecutionId)
                                              .workflowId(workflowId)
                                              .status(ExecutionStatus.SUCCESS)
                                              .build();

    Workflow workflow = WorkflowBuilder.aWorkflow().appId(appId).uuid(workflowId).build();

    wingsPersistence.save(workflow);
    wingsPersistence.save(workflowExecution);

    StateExecutionInstance stateExecutionInstance = mock(StateExecutionInstance.class);
    when(stateExecutionInstance.getStateExecutionMap()).thenReturn(Collections.emptyMap());

    ExecutionContext context = spy(new ExecutionContextImpl(stateExecutionInstance));
    doReturn(PhaseElement.builder().serviceElement(ServiceElement.builder().uuid("serviceA").build()).build())
        .when(context)
        .getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    doReturn(appId).when(context).getAppId();
    doReturn(UUID.randomUUID().toString()).when(context).getWorkflowExecutionId();
    when(infraMappingService.get(anyString(), anyString()))
        .thenReturn(anEcsInfrastructureMapping()
                        .withClusterName("Learning-Engine-Experimental")
                        .withRegion(Regions.US_EAST_1.getName())
                        .withDeploymentType(DeploymentType.ECS.name())
                        .withComputeProviderSettingId(awsConfigid)
                        .build());
    when(containerInstanceHandler.getContainerServiceNames(anyObject(), anyString(), anyString(), anyObject()))
        .thenReturn(Sets.newHashSet(ContainerMetadata.builder()
                                        .containerServiceName("Harness__Verification__Learning__Engine__ECS__2")
                                        .namespace("default")
                                        .build(),
            ContainerMetadata.builder()
                .containerServiceName("Harness__Verification__Learning__Engine__ECS__3")
                .namespace("default")
                .build()));

    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));
    doReturn(workflowId).when(splunkV2State).getWorkflowId(context);
    FieldUtils.writeField(splunkV2State, "containerInstanceHandler", containerInstanceHandler, true);
    FieldUtils.writeField(splunkV2State, "infraMappingService", infraMappingService, true);
    FieldUtils.writeField(splunkV2State, "settingsService", settingsService, true);
    FieldUtils.writeField(splunkV2State, "secretManager", secretManager, true);
    FieldUtils.writeField(splunkV2State, "awsHelperService", awsHelperService, true);
    FieldUtils.writeField(splunkV2State, "appService", appService, true);
    FieldUtils.writeField(splunkV2State, "delegateProxyFactory", delegateProxyFactory, true);
    Reflect.on(splunkV2State).set("workflowExecutionService", workflowExecutionService);
    Map<String, String> nodes = splunkV2State.getLastExecutionNodes(context);
    assertThat(nodes.size() >= 1).isTrue();
    logger.info("do_not_delete__ecs__container__integration__test__Prod: " + nodes);
  }
}
