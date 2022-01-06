/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.stream.hazelcast;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.util.AbstractBroadcasterProxy;

/**
 * Created by peeyushaggarwal on 1/11/17.
 */
@NoArgsConstructor
@Slf4j
public class HazelcastBroadcaster extends AbstractBroadcasterProxy {
  public static final AtomicReference<HazelcastInstance> HAZELCAST_INSTANCE = new AtomicReference<>();

  private final AtomicBoolean isClosed = new AtomicBoolean();
  private ITopic topic;
  private String messageListenerRegistrationId;

  @Override
  public Broadcaster initialize(String id, AtmosphereConfig config) {
    return initialize(id, URI.create("http://localhost:6379"), config);
  }

  @Override
  public Broadcaster initialize(String id, URI uri, AtmosphereConfig config) {
    super.initialize(id, uri, config);
    setUp();
    return this;
  }

  public void setUp() {
    topic = HAZELCAST_INSTANCE.get().getTopic(getID());
    config.shutdownHook(() -> {
      HazelcastBroadcaster.HAZELCAST_INSTANCE.get().shutdown();
      isClosed.set(true);
    });
  }

  private synchronized void addMessageListener() {
    if (isNotEmpty(getAtmosphereResources()) && messageListenerRegistrationId == null) {
      messageListenerRegistrationId =
          topic.addMessageListener(message -> broadcastReceivedMessage(message.getMessageObject()));
      log.info("Added message listener to topic");
    }
  }

  private synchronized void removeMessageListener() {
    if (isEmpty(getAtmosphereResources()) && messageListenerRegistrationId != null && topic != null) {
      topic.removeMessageListener(messageListenerRegistrationId);
      messageListenerRegistrationId = null;
      log.info("Removed message listener from topic");
    }
  }

  @Override
  public Broadcaster addAtmosphereResource(AtmosphereResource resource) {
    Broadcaster result = super.addAtmosphereResource(resource);
    addMessageListener();
    return result;
  }

  @Override
  public Broadcaster removeAtmosphereResource(AtmosphereResource resource) {
    Broadcaster result = super.removeAtmosphereResource(resource);
    removeMessageListener();
    return result;
  }

  @Override
  public synchronized void setID(String id) {
    super.setID(id);
    setUp();
  }

  @Override
  public void destroy() {
    if (!isClosed.get()) {
      topic.destroy();
      topic = null;
    }
    super.destroy();
  }

  @Override
  public void incomingBroadcast() {
    log.info("Subscribing to: {}", getID());
  }

  @Override
  public void outgoingBroadcast(Object message) {
    topic.publish(message);
  }
}
