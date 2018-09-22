package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;

import javax.validation.Valid;
import javax.ws.rs.core.HttpHeaders;

public interface WebHookService {
  WebHookResponse execute(@NotEmpty String token, @Valid WebHookRequest webHookRequest);
  WebHookResponse executeByEvent(@NotEmpty(message = "Token can not be empty") String token,
      @NotEmpty(message = "Payload can not be empty") String webhookEventPayload, HttpHeaders httpHeaders);
}
