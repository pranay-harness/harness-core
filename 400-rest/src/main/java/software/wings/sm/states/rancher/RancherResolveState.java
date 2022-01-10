package software.wings.sm.states.rancher;

import com.google.inject.Inject;
import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.context.ContextElementType;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.PhaseElement;
import software.wings.api.RancherClusterElement;
import software.wings.beans.RancherConfig;
import software.wings.beans.RancherKubernetesInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.RancherK8sClusterProcessor;
import software.wings.helpers.ext.k8s.request.RancherResolveClustersTaskParameters;
import software.wings.helpers.ext.k8s.response.RancherResolveClustersResponse;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.RancherKubernetesInfrastructure;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.ExceptionUtils.getMessage;
import static software.wings.sm.StateType.RANCHER_RESOLVE;

@Slf4j
public class RancherResolveState extends State {

    @Inject public RancherStateHelper rancherStateHelper;
    @Inject @Transient private SweepingOutputService sweepingOutputService;
    @Inject private transient SettingsService settingsService;
    @Inject private transient DelegateService delegateService;
    @Inject private transient InfrastructureDefinitionService infrastructureDefinitionService;
    @Inject private SecretManager secretManager;

    public RancherResolveState(String name) {
        super(name, RANCHER_RESOLVE.name());
    }

    @Override
    public ExecutionResponse execute(ExecutionContext context) {
        try {
            return executeInternal(context);
        } catch (WingsException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidRequestException(getMessage(e), e);
        }
    }
    
    private ExecutionResponse executeInternal(ExecutionContext context) {
        RancherKubernetesInfrastructureMapping rancherKubernetesInfrastructureMapping = rancherStateHelper.fetchRancherKubernetesInfrastructureMapping(context);

        InfrastructureDefinition infrastructureDefinition = infrastructureDefinitionService.getInfraDefById(context.getAccountId(), rancherKubernetesInfrastructureMapping.getInfrastructureDefinitionId());
        SettingAttribute settingAttribute = settingsService.get(rancherKubernetesInfrastructureMapping.getComputeProviderSettingId());
        RancherConfig rancherConfig = (RancherConfig) settingAttribute.getValue();

        List<EncryptedDataDetail> encryptedDataDetails =
                secretManager.getEncryptionDetails(rancherConfig, context.getAppId(), context.getWorkflowExecutionId());

        RancherResolveClustersTaskParameters rancherResolveClustersTaskParameters =
                RancherResolveClustersTaskParameters.builder()
                        .rancherConfig(rancherConfig)
                        .encryptedDataDetails(encryptedDataDetails)
                        .clusterSelectionCriteria(((RancherKubernetesInfrastructure) infrastructureDefinition.getInfrastructure()).getClusterSelectionCriteria())
                        .build();

        String waitId = generateUuid();
        DelegateTask delegateTask =
                DelegateTask.builder()
                        .accountId(context.getApp().getAccountId())
                        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, context.getApp().getUuid())
                        .waitId(waitId)
                        .data(TaskData.builder()
                                .async(true)
                                .taskType(TaskType.RANCHER_RESOLVE_CLUSTERS.name())
                                .parameters(new Object[] {rancherResolveClustersTaskParameters})
                                // TODO: Update task timeout from state
                                .timeout(60000)
                                .build())
                        .selectionLogsTrackingEnabled(true)
                        .build();

        delegateService.queueTask(delegateTask);
        StateExecutionData executionData = new StateExecutionData();

        return ExecutionResponse.builder()
                .async(true)
                .correlationIds(Collections.singletonList(waitId))
                .stateExecutionData(executionData)
                .build();
    }


    @Override
    public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
        RancherResolveClustersResponse rancherResolveClustersResponse = (RancherResolveClustersResponse) response.values().iterator().next();
        List<String> clusterNames = rancherResolveClustersResponse.getClusters();
        List<RancherClusterElement> clusterElements = clusterNames.stream()
                .map(name -> new RancherClusterElement(UUIDGenerator.generateUuid(), name))
                .collect(Collectors.toList());

        sweepingOutputService.save(context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
                .name(RancherK8sClusterProcessor.RancherClusterElementList.getSweepingOutputID(context))
                .value(new RancherK8sClusterProcessor.RancherClusterElementList(clusterElements))
                .build());

        return ExecutionResponse.builder().executionStatus(ExecutionStatus.SUCCESS).build();
    }

    @NotNull
    private String getPhaseParamName(ExecutionContext context) {
        return ((PhaseElement) context.getContextElement(ContextElementType.PARAM,
                PhaseElement.PHASE_PARAM))
                .getPhaseName().trim();
    }

    @Override
    public void handleAbortEvent(ExecutionContext context) {
        // NoOp
    }
}