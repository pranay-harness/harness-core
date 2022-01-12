package io.harness.cvng.core.beans.dynatrace;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DynatraceServiceDTO {
  @NonNull String displayName;
  @NonNull String entityId;
  List<String> serviceMethodIds;
  List<String> serviceGroupIds;

}
