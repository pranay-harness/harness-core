package io.harness.cdng.sam;

import io.harness.pms.sdk.core.data.Outcome;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SamDeployOutcome implements Outcome {
    Map<String, String> outputs;
}
