package software.wings.security.authentication.oauth;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

public interface OauthClient {
  String getName();

  URI getRedirectUrl();

  OauthUserInfo execute(String code, String state) throws InterruptedException, ExecutionException, IOException;
}
