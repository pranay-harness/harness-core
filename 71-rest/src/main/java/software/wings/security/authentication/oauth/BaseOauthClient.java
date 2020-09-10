package software.wings.security.authentication.oauth;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.fabric8.utils.Strings;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import software.wings.security.JWT_CATEGORY;
import software.wings.security.SecretManager;

import java.net.URI;
import java.net.URISyntaxException;

@OwnedBy(PL)
@Slf4j
public class BaseOauthClient {
  private static final String STATE_KEY = "state";

  private SecretManager secretManager;

  public BaseOauthClient(SecretManager secretManager) {
    this.secretManager = secretManager;
  }

  public URI appendStateToURL(URIBuilder uriBuilder) throws URISyntaxException {
    String jwtSecret = secretManager.generateJWTToken(null, JWT_CATEGORY.OAUTH_REDIRECT);
    logger.info("Status appending to oauth url is [{}]", jwtSecret);
    uriBuilder.addParameter(STATE_KEY, jwtSecret);
    return uriBuilder.build();
  }

  public void verifyState(String state) {
    try {
      logger.info("The status received is: [{}]", state);
      secretManager.verifyJWTToken(state, JWT_CATEGORY.OAUTH_REDIRECT);
    } catch (Exception ex) {
      logger.warn("State verification failed in oauth.", ex);
      throw new WingsException("Oauth failed because of state mismatch");
    }
  }

  protected void populateEmptyFields(OauthUserInfo oauthUserInfo) {
    String email = oauthUserInfo.getEmail();
    String handle = email.substring(0, email.indexOf('@'));
    logger.info("Populating the name, from email. Email is {} and the new name is {} ", email, handle);
    oauthUserInfo.setLogin(Strings.isNullOrBlank(oauthUserInfo.getLogin()) ? handle : oauthUserInfo.getEmail());
    oauthUserInfo.setName(Strings.isNullOrBlank(oauthUserInfo.getName()) ? handle : oauthUserInfo.getName());
  }
}
