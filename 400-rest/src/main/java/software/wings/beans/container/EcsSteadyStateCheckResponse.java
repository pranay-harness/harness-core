package software.wings.beans.container;

import io.harness.beans.ExecutionStatus;
import io.harness.container.ContainerInfo;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class EcsSteadyStateCheckResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private String errorMessage;
  private ExecutionStatus executionStatus;
  private List<ContainerInfo> containerInfoList;
}
