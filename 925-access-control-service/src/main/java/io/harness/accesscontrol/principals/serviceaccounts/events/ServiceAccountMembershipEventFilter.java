package io.harness.accesscontrol.principals.serviceaccounts.events;

import static io.harness.eventsframework.EventsFrameworkConstants.USERMEMBERSHIP;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;

import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;

import com.google.inject.Singleton;
import java.util.Map;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class ServiceAccountMembershipEventFilter implements EventFilter {}
