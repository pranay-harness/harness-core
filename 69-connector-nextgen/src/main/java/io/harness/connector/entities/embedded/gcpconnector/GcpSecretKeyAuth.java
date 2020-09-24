package io.harness.connector.entities.embedded.gcpconnector;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("connector.entities.embedded.gcpconnector.GcpSecretKeyAuth")
public class GcpSecretKeyAuth implements GcpAuth {
  String secretKeyRef;
}
