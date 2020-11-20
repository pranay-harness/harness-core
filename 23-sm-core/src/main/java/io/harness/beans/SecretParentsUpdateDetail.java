package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Set;

@OwnedBy(PL)
@RequiredArgsConstructor
@Value
@Getter
public class SecretParentsUpdateDetail {
  @NonNull String secretId;
  @NonNull Set<EncryptedDataParent> parentsToAdd;
  @NonNull Set<EncryptedDataParent> parentsToRemove;
}
