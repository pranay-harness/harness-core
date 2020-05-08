package software.wings.service.impl.yaml.gitsync;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;

@Builder
@JsonTypeName("QUEUED")
public class QueuedChangesetInformation implements ChangesetInformation {
  Long queuedAt;
}
