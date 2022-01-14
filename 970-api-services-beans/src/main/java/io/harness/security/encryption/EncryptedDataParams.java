package io.harness.security.encryption;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
@OwnedBy(PL)
@Value
@Builder
@EqualsAndHashCode
public class EncryptedDataParams {
  @NonNull private String name;
  @NonNull private String value;
}
