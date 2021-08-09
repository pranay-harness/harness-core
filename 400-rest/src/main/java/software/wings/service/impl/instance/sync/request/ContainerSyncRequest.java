package software.wings.service.impl.instance.sync.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author rktummala on 09/05/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerSyncRequest {
  private ContainerFilter filter;
}
