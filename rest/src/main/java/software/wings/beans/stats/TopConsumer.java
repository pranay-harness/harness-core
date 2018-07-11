package software.wings.beans.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopConsumer {
  @Id private String appId;
  private String appName;
  private String serviceId;
  private String serviceName;
  private int successfulActivityCount;
  private int failedActivityCount;
  private int totalCount;
}
