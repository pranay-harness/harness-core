package io.harness.ng.helpcenter.services;

import software.wings.beans.ZendeskSsoLoginResponse;

public interface ZendeskService {
  ZendeskSsoLoginResponse generateZendeskSsoJwt(String returnToUrl);
}
