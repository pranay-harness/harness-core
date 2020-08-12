package software.wings.audit;

import com.google.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@Singleton
public class AuditConfig {
  @JsonProperty(defaultValue = "false") private boolean storeRequestPayload;
  @JsonProperty(defaultValue = "false") private boolean storeResponsePayload;
}
