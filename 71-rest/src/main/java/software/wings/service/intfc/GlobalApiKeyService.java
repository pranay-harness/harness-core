package software.wings.service.intfc;

import software.wings.beans.GlobalApiKey.ProviderType;

public interface GlobalApiKeyService {
  String generate(ProviderType providerType);

  String get(ProviderType providerType);

  void delete(ProviderType providerType);
}
