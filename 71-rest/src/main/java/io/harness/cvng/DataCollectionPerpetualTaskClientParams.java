package io.harness.cvng;

import io.harness.perpetualtask.PerpetualTaskClientParams;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DataCollectionPerpetualTaskClientParams implements PerpetualTaskClientParams {
  String cvConfigId;
}
