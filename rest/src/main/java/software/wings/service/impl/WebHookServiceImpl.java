package software.wings.service.impl;

import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.service.intfc.WebHookService;

public class WebHookServiceImpl implements WebHookService {
  @Override
  public WebHookResponse execute(String token, WebHookRequest webHookRequest) {
    // validate token
    // fetch artifact stream and trigger
    // validate webHookRequest
    // wait for artifact to appear
    // trigger execution
    // return response;
    return new WebHookResponse();
  }
}
