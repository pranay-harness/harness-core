package software.wings.service.impl.instance.sync.response;

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
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContainerSyncResponse {
  private List<ContainerInfo> containerInfoList;
}
