package software.wings.security.authentication.oauth;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

@OwnedBy(PL)
public interface OauthClient {
  String getName();

  URI getRedirectUrl();

  OauthUserInfo execute(String code, String state) throws InterruptedException, ExecutionException, IOException;
}
