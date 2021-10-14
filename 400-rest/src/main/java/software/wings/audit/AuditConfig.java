package software.wings.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;
import lombok.Data;

@OwnedBy(PL)
@Data
@Singleton
@TargetModule(HarnessModule._940_CG_AUDIT_SERVICE)
public class AuditConfig {
  @JsonProperty(defaultValue = "false") private boolean storeRequestPayload;
  @JsonProperty(defaultValue = "false") private boolean storeResponsePayload;
}
