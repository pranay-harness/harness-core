package software.wings.service.intfc;

import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;

public interface WebHookService { WebHookResponse execute(String token, WebHookRequest webHookRequest); }
