package software.wings.service.impl.instance.sync.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.infrastructure.instance.info.ContainerInfo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

  /*
  This field should help if the containerInfoList is empty to tell which controller has has being scaled down to 0
   */
  private String controllerName;
}
