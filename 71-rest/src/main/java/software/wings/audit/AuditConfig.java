package software.wings.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;

@OwnedBy(PL)
@Data
@Singleton
public class AuditConfig {
  @JsonProperty(defaultValue = "false") private boolean storeRequestPayload;
  @JsonProperty(defaultValue = "false") private boolean storeResponsePayload;
}
