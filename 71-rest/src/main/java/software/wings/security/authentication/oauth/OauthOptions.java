package software.wings.security.authentication.oauth;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.sso.OauthSettings;
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.security.authentication.OauthProviderType;
import software.wings.security.saml.SSORequest;
import software.wings.service.impl.SSOSettingServiceImpl;

public class OauthOptions {
  @Inject GithubClientImpl githubClient;
  @Inject LinkedinClientImpl linkedinClient;
  @Inject GoogleClientImpl googleClient;
  @Inject AzureClientImpl azureClient;
  @Inject AuthenticationUtils authenticationUtils;
  @Inject SSOSettingServiceImpl ssoSettingService;
  @Inject BitbucketClient bitbucketClient;
  @Inject GitlabClient gitlabClient;

  public String getRedirectURI(OauthProviderType oauthProvider) {
    return getOauthProvider(oauthProvider).getRedirectUrl().toString();
  }

  public OauthClient getOauthProvider(OauthProviderType oauthProvider) {
    switch (oauthProvider) {
      case GITHUB:
        return githubClient;
      case LINKEDIN:
        return linkedinClient;
      case GOOGLE:
        return googleClient;
      case AZURE:
        return azureClient;
      case BITBUCKET:
        return bitbucketClient;
      case GITLAB:
        return gitlabClient;
      default:
        throw new InvalidRequestException(String.format("Oauth provider %s not supported.", oauthProvider));
    }
  }

  public SSORequest oauthProviderRedirectionUrl(User user) {
    Account primaryAccount = authenticationUtils.getPrimaryAccount(user);
    OauthSettings oauthSettings = ssoSettingService.getOauthSettingsByAccountId(primaryAccount.getUuid());
    String displayName = oauthSettings.getPublicSSOSettings().getDisplayName();
    OauthProviderType oauthProviderType = OauthProviderType.valueOf(displayName.toUpperCase());
    OauthClient oauthProvider = getOauthProvider(oauthProviderType);
    SSORequest ssoRequest = new SSORequest();
    ssoRequest.setIdpRedirectUrl(oauthProvider.getRedirectUrl().toString());
    ssoRequest.setOauthProviderType(oauthProviderType);
    return ssoRequest;
  }
}
