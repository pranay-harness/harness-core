package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HarnessSuccessFulChangeDetail implements SuccessfulChangeDetail {
  String yamlChangeSetId;
}
