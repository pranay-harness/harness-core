package software.wings.beans.instance.dashboard;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.infrastructure.instance.InvocationCount;

/**
 * @author rktummala on 01/03/18
 */
@Data
@Builder
public class InstanceSummaryStatsByService {
  private long totalCount;
  private long prodCount;
  private long nonprodCount;
  private ServiceSummary serviceSummary;
  private InvocationCount invocationCount;
}
