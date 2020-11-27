package io.harness.ng.core;

import io.harness.beans.DecryptableEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NGAccessWithEncryptionConsumer {
  private NGAccess ngAccess;
  private DecryptableEntity decryptableEntity;
}
