package software.wings.graphql.datafetcher.secrets;

import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SecretController {
  @Inject SecretManager secretManager;
}
