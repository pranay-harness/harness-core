/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.principals.usergroups.events;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.USER_GROUP;

import io.harness.accesscontrol.commons.events.EventConsumer;
import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(PL)
@Singleton
public class UserGroupEventConsumer implements EventConsumer {
  private final UserGroupEventFilter userGroupEventFilter;
  private final UserGroupEventHandler userGroupEventHandler;
  public static final String USER_GROUP_ENTITY_TYPE = USER_GROUP;

  @Inject
  public UserGroupEventConsumer(
      UserGroupEventFilter userGroupEventFilter, UserGroupEventHandler userGroupEventHandler) {
    this.userGroupEventFilter = userGroupEventFilter;
    this.userGroupEventHandler = userGroupEventHandler;
  }

  @Override
  public EventFilter getEventFilter() {
    return userGroupEventFilter;
  }

  @Override
  public EventHandler getEventHandler() {
    return userGroupEventHandler;
  }
}
