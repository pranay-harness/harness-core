package io.harness.connector.entities.embedded.gcpconnector;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("io.harness.connector.entities.embedded.gcpconnector.GcpDelegateDetails")
public class GcpDelegateDetails implements GcpCredential {
  Set<String> delegateSelectors;
}
