package io.harness.cvng.core.beans.dynatrace;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DynatraceServiceDTO {

     String displayName;
     String entityId;
     List<String> serviceMethodIds;
}
