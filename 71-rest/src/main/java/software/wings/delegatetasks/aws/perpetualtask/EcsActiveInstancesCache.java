package software.wings.delegatetasks.aws.perpetualtask;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
public class EcsActiveInstancesCache {
  private Set<String> activeTaskArns;
  private Set<String> activeEc2InstanceIds;
  private Set<String> activeContainerInstanceArns;
  private Instant lastProcessedTimestamp;
}
