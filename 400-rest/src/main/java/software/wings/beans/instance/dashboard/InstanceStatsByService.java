package software.wings.beans.instance.dashboard;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 08/13/17
 */
@Data
@Builder
public class InstanceStatsByService {
  private long totalCount;
  private ServiceSummary serviceSummary;
  private List<InstanceStatsByEnvironment> instanceStatsByEnvList;

  public InstanceStatsByService clone(long newCount) {
    return InstanceStatsByService.builder()
        .totalCount(newCount)
        .serviceSummary(serviceSummary)
        .instanceStatsByEnvList(instanceStatsByEnvList)
        .build();
  }
}
