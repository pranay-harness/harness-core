package io.harness.accesscontrol.principals.serviceaccounts.events;

import io.harness.accesscontrol.commons.events.EventConsumer;
import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class ServiceAccountMembershipEventConsumer implements EventConsumer {
  private final ServiceAccountMembershipEventFilter serviceAccountMembershipEventFilter;
  private final ServiceAccountMembershipEventHandler serviceAccountMembershipEventHandler;

  @Inject
  public ServiceAccountMembershipEventConsumer(ServiceAccountMembershipEventFilter serviceAccountMembershipEventFilter,
      ServiceAccountMembershipEventHandler serviceAccountMembershipEventHandler) {
    this.serviceAccountMembershipEventFilter = serviceAccountMembershipEventFilter;
    this.serviceAccountMembershipEventHandler = serviceAccountMembershipEventHandler;
  }

  @Override
  public EventFilter getEventFilter() {
    return serviceAccountMembershipEventFilter;
  }

  @Override
  public EventHandler getEventHandler() {
    return serviceAccountMembershipEventHandler;
  }
}
