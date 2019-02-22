package software.wings.security.authentication.oauth;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.sso.OauthSettings;
import software.wings.security.authentication.AuthenticationUtil;
import software.wings.security.saml.SSORequest;
import software.wings.service.impl.SSOSettingServiceImpl;

public class OauthOptions {
  @Inject GithubClientImpl githubClient;
  @Inject LinkedinClientImpl linkedinClient;
  @Inject GoogleClientImpl googleClient;
  @Inject AzureClientImpl azureClient;
  @Inject AuthenticationUtil authenticationUtil;
  @Inject SSOSettingServiceImpl ssoSettingService;
  @Inject BitbucketClient bitbucketClient;
  @Inject GitlabClient gitlabClient;

  public enum SupportedOauthProviders { github, linkedin, google, azure, bitbucket, gitlab }

  public String getRedirectURI(SupportedOauthProviders oauthProvider) {
    return getOauthProvider(oauthProvider).getRedirectUrl().toString();
  }

  private OauthClient getOauthProvider(SupportedOauthProviders oauthProvider) {
    switch (oauthProvider) {
      case github:
        return githubClient;
      case linkedin:
        return linkedinClient;
      case google:
        return googleClient;
      case azure:
        return azureClient;
      case bitbucket:
        return bitbucketClient;
      case gitlab:
        return gitlabClient;
      default:
        throw new InvalidRequestException(String.format("Oauth provider %s not supported.", oauthProvider));
    }
  }

  public SSORequest oauthProviderRedirectionUrl(User user) {
    Account primaryAccount = authenticationUtil.getPrimaryAccount(user);
    OauthSettings oauthSettings = ssoSettingService.getOauthSettingsByAccountId(primaryAccount.getUuid());
    String redirectUrl = oauthSettings.getPublicSSOSettings().getUrl();
    SSORequest SSORequest = new SSORequest();
    SSORequest.setIdpRedirectUrl(redirectUrl);
    return SSORequest;
  }
}
