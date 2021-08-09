package io.harness.accesscontrol.commons.events;

import static io.harness.eventsframework.EventsFrameworkConstants.USERMEMBERSHIP;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PL)
public class UserMembershipEventListenerService extends EventListenerService {
  @Inject
  public UserMembershipEventListenerService(UserMembershipEventListener userMembershipEventListener) {
    super(userMembershipEventListener);
  }

  @Override
  public String getServiceName() {
    return USERMEMBERSHIP;
  }
}