package io.harness.connector.entities.embedded.gcpconnector;

import io.harness.delegate.beans.connector.gcpconnector.GcpAuthType;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldNameConstants(innerTypeName = "GcpDetailsKeys")
@TypeAlias("io.harness.connector.entities.embedded.gcpconnector.GcpDetails")
public class GcpDetails implements GcpCredential {
  GcpAuthType authType;
  GcpAuth auth;
}
