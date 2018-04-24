package software.wings.sm.states;

import lombok.Builder;
import lombok.Data;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;

@Data
@Builder
public class KubernetesSteadyStateCheckResponse implements NotifyResponseData {
  private ExecutionStatus executionStatus;
  private List<ContainerInfo> containerInfoList;
}