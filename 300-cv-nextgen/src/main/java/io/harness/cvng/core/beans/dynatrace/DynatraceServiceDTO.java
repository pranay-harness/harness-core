package io.harness.cvng.core.beans.dynatrace;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DynatraceServiceDTO {

     String displayName;
     String entityId;
}
