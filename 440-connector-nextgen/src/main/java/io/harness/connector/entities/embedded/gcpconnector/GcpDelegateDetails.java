package io.harness.connector.entities.embedded.gcpconnector;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("io.harness.connector.entities.embedded.gcpconnector.GcpDelegateDetails")
@OwnedBy(DX)
public class GcpDelegateDetails implements GcpCredential {
  Set<String> delegateSelectors;
}
