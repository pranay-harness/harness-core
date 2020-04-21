package software.wings.security.encryption;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Set;

@RequiredArgsConstructor
@Value
@Getter
public class SecretParentsUpdateDetail {
  @NonNull String secretId;
  @NonNull Set<EncryptedDataParent> parentsToAdd;
  @NonNull Set<EncryptedDataParent> parentsToRemove;
}