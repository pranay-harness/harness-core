package software.wings.beans.instance.dashboard;

import lombok.Builder;
import lombok.Data;

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
}