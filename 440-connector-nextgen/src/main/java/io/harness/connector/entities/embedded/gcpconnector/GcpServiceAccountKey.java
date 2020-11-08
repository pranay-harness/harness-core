package io.harness.connector.entities.embedded.gcpconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.gcpconnector.GcpServiceAccountKey")
public class GcpServiceAccountKey implements GcpCredential {
  String secretKeyRef;
}
