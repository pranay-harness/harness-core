package software.wings.beans.container;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.DelegateTaskNotifyResponseData;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EcsSteadyStateCheckResponse extends DelegateTaskNotifyResponseData {
  private String errorMessage;
  private ExecutionStatus executionStatus;
  private List<ContainerInfo> containerInfoList;
}