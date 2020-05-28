package software.wings.service.impl.instance.sync.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;

import java.util.List;

/**
 * @author rktummala on 09/02/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerSyncResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private List<ContainerInfo> containerInfoList;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
