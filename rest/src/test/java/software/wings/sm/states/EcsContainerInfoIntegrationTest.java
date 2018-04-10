package software.wings.sm.states;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement.PhaseElementBuilder;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.instance.ContainerInstanceHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;

/**
 * Created by rsingh on 4/9/18.
 */
public class EcsContainerInfoIntegrationTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private AwsHelperService awsHelperService;
  @Inject private ContainerService containerService;
  @Mock private ContainerInstanceHelper containerInstanceHelper;
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private AppService appService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  private final String workflowId = UUID.randomUUID().toString();
  private final String appId = UUID.randomUUID().toString();
  private final String accountId = UUID.randomUUID().toString();
  private final String previousWorkflowExecutionId = UUID.randomUUID().toString();

  @Mock ExecutionContext context;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    when(containerInstanceHelper.isContainerDeployment(anyObject())).thenReturn(true);
    when(infraMappingService.get(anyString(), anyString())).thenReturn(null);
    when(appService.get(appId))
        .thenReturn(Application.Builder.anApplication().withUuid(appId).withAccountId(accountId).build());
    when(delegateProxyFactory.get(eq(ContainerService.class), any(SyncTaskContext.class))).thenReturn(containerService);
  }

  @Test
  public void testGetLastExecutionNodesECS() throws NoSuchAlgorithmException, KeyManagementException {
    AwsConfig awsConfig = AwsConfig.builder()
                              .accessKey("AKIAIVRKRUMJ3LAVBMSQ")
                              .secretKey("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".toCharArray())
                              .accountId(accountId)
                              .build();
    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(awsConfig).build();
    String awsConfigid = wingsPersistence.save(settingAttribute);
    WorkflowExecution workflowExecution = WorkflowExecutionBuilder.aWorkflowExecution()
                                              .withAppId(appId)
                                              .withUuid(previousWorkflowExecutionId)
                                              .withWorkflowId(workflowId)
                                              .withStatus(ExecutionStatus.SUCCESS)
                                              .build();

    Workflow workflow = WorkflowBuilder.aWorkflow().withAppId(appId).withUuid(workflowId).build();

    wingsPersistence.save(workflow);
    wingsPersistence.save(workflowExecution);

    when(context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM))
        .thenReturn(PhaseElementBuilder.aPhaseElement()
                        .withServiceElement(ServiceElement.Builder.aServiceElement().withUuid("serviceA").build())
                        .build());
    when(context.getAppId()).thenReturn(appId);
    when(context.getWorkflowExecutionId()).thenReturn(UUID.randomUUID().toString());
    when(infraMappingService.get(anyString(), anyString()))
        .thenReturn(EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping()
                        .withClusterName("harness-qa")
                        .withRegion(Regions.US_EAST_1.getName())
                        .withComputeProviderSettingId(awsConfigid)
                        .build());
    when(containerInstanceHelper.getContainerServiceNames(anyObject(), anyString(), anyString()))
        .thenReturn(Sets.newHashSet("do_not_delete__ecs__container__integration__test__Prod__1",
            "do_not_delete__ecs__container__integration__test__Prod__2"));

    SplunkV2State splunkV2State = spy(new SplunkV2State("SplunkState"));
    doReturn(workflowId).when(splunkV2State).getWorkflowId(context);
    setInternalState(splunkV2State, "containerInstanceHelper", containerInstanceHelper);
    setInternalState(splunkV2State, "infraMappingService", infraMappingService);
    setInternalState(splunkV2State, "settingsService", settingsService);
    setInternalState(splunkV2State, "secretManager", secretManager);
    setInternalState(splunkV2State, "awsHelperService", awsHelperService);
    setInternalState(splunkV2State, "appService", appService);
    setInternalState(splunkV2State, "delegateProxyFactory", delegateProxyFactory);
    Reflect.on(splunkV2State).set("workflowExecutionService", workflowExecutionService);
    Set<String> nodes = splunkV2State.getLastExecutionNodes(context);
    assertTrue(nodes.size() >= 1);
    System.out.println("do_not_delete__ecs__container__integration__test__Prod: " + nodes);
  }
}
