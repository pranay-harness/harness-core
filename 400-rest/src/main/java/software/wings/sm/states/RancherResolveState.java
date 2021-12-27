package software.wings.sm.states;

import com.google.inject.Inject;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.context.ContextElementType;
import io.harness.data.structure.UUIDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.PhaseElement;
import software.wings.api.RancherClusterElement;
import software.wings.common.RancherK8sClusterProcessor;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static software.wings.sm.StateType.RANCHER_RESOLVE;

@Slf4j
public class RancherResolveState extends State {

    @Inject @Transient private SweepingOutputService sweepingOutputService;

    public RancherResolveState(String name) {
        super(name, RANCHER_RESOLVE.name());
    }

    @Override
    public ExecutionResponse execute(ExecutionContext context) {
        StateExecutionData executionData = new StateExecutionData();

        List<String> clusterNames = Arrays.asList("cluster1", "cluster2", "cluster3");
        List<RancherClusterElement> clusterElements = clusterNames.stream()
                .map(name -> new RancherClusterElement(UUIDGenerator.generateUuid(), name))
                .collect(Collectors.toList());

        sweepingOutputService.save(context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
                        .name(RancherK8sClusterProcessor.RancherClusterElementList.SWEEPING_OUTPUT_NAME +
                                ((PhaseElement)context.getContextElement(ContextElementType.PARAM,
                                        PhaseElement.PHASE_PARAM))
                                        .getPhaseName().trim())
                        .value(new RancherK8sClusterProcessor.RancherClusterElementList(clusterElements))
                .build());

        return ExecutionResponse.builder()
                .stateExecutionData(executionData)
                .executionStatus(ExecutionStatus.SUCCESS)
                .build();
    }

    @Override
    public void handleAbortEvent(ExecutionContext context) {
        // NoOp
    }
}