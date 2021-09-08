package software.wings.security.authentication;

import static io.harness.eventsframework.EventsFrameworkConstants.SAML_AUTHORIZATION_ASSERTION;

import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.samlauthorization.samlauthorizationdata.SamlAuthorizationDTO;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class NgSamlAuthorizationEventPublisher {
  @Inject @Named(SAML_AUTHORIZATION_ASSERTION) private Producer eventProducer;

  public void publishSamlAuthorizationAssertion(
      SamlUserAuthorization samlUserAuthorization, String accountIdentifier, String ssoId) {
    SamlAuthorizationDTO samlAuthorizationDTO = SamlAuthorizationDTO.newBuilder()
                                                    .setAccountIdentifier(accountIdentifier)
                                                    .setSsoId(ssoId)
                                                    .setEmail(samlUserAuthorization.getEmail())
                                                    .addAllUserGroups(samlUserAuthorization.getUserGroups())
                                                    .build();

    eventProducer.send(Message.newBuilder()
                           .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier))
                           .setData(samlAuthorizationDTO.toByteString())
                           .build());
  }
}
