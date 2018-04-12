package software.wings.service.intfc;

import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;

import javax.validation.Valid;

public interface WebHookService {
  WebHookResponse execute(String token, @Valid WebHookRequest webHookRequest);
  WebHookResponse executeByEvent(String token, String webhookEventPayload);
}
