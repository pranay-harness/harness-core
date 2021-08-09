package io.harness.cvng.beans.activity.cd10;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CD10RegisterActivityDTO {
  String activityId;
  String serviceIdentifier;
  String envIdentifier;
}
