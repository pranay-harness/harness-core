package software.wings.service.impl.yaml.gitsync;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;

@JsonTypeName("RUNNING")
@Builder
public class RunningChangesetInformation implements ChangesetInformation {
  Long queuedAt;
  Long startedRunningAt;
}
