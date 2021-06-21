package io.harness.accesscontrol.principals.serviceaccounts.events;

import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class ServiceAccountMembershipEventFilter implements EventFilter {}
