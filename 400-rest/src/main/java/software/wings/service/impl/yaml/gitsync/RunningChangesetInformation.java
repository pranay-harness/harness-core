package software.wings.service.impl.yaml.gitsync;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@JsonTypeName("RUNNING")
@Builder
public class RunningChangesetInformation implements ChangesetInformation {
  Long queuedAt;
  Long startedRunningAt;
}
