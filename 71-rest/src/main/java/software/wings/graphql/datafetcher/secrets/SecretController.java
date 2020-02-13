package software.wings.graphql.datafetcher.secrets;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.security.SecretManager;

@Slf4j
@Singleton
public class SecretController {
  @Inject SecretManager secretManager;
}
