package io.harness.secrets;

import java.time.Duration;

public interface SecretsDelegateCacheHelperService {
  Duration initializeCacheExpiryTTL();
}
