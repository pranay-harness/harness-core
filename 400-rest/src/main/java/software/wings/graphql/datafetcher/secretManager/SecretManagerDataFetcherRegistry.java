package software.wings.graphql.datafetcher.secretManager;

import static io.harness.security.encryption.EncryptionType.VAULT;

import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptionType;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import java.util.EnumMap;
import java.util.Optional;

@Singleton
public class SecretManagerDataFetcherRegistry {
  private final Injector injector;
  private final EnumMap<EncryptionType, SecretManagerDataFetchers> registeredDataFetchers;

  @Inject
  public SecretManagerDataFetcherRegistry(Injector injector) {
    this.injector = injector;
    this.registeredDataFetchers = new EnumMap<>(EncryptionType.class);
    this.registeredDataFetchers.put(VAULT, SecretManagerDataFetchers.HASHICORP_VAULT_DATA_FETCHER);
  }

  public SecretManagerMutationDataFetcher getDataFetcher(EncryptionType encryptionType) {
    return Optional.ofNullable(this.registeredDataFetchers.get(encryptionType))
        .flatMap(type
            -> Optional.of(this.injector.getInstance(
                Key.get(SecretManagerMutationDataFetcher.class, Names.named(type.getName())))))
        .orElseThrow(() -> new InvalidRequestException("Secret manager type is not supported"));
  }
}
