package software.wings.utils;

/**
 * Created by peeyushaggarwal on 1/11/17.
 */

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.util.AbstractBroadcasterProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public class HazelcastBroadcaster extends AbstractBroadcasterProxy {
  private static final Logger logger = LoggerFactory.getLogger(HazelcastBroadcaster.class);
  public static HazelcastInstance HAZELCAST_INSTANCE;
  private final AtomicBoolean isClosed = new AtomicBoolean();
  private ITopic topic;
  private String messageListenerRegistrationId;

  public HazelcastBroadcaster() {}

  public Broadcaster initialize(String id, AtmosphereConfig config) {
    return this.initialize(id, URI.create("http://localhost:6379"), config);
  }

  public Broadcaster initialize(String id, URI uri, AtmosphereConfig config) {
    super.initialize(id, uri, config);
    this.setUp();
    return this;
  }

  public void setUp() {
    this.topic = HAZELCAST_INSTANCE.getTopic(this.getID());
    this.config.shutdownHook(() -> {
      HazelcastBroadcaster.HAZELCAST_INSTANCE.shutdown();
      HazelcastBroadcaster.this.isClosed.set(true);
    });
  }

  private synchronized void addMessageListener() {
    if (this.getAtmosphereResources().size() > 0 && this.messageListenerRegistrationId == null) {
      this.messageListenerRegistrationId = this.topic.addMessageListener(
          message -> HazelcastBroadcaster.this.broadcastReceivedMessage(message.getMessageObject()));
      logger.info("Added message listener to topic");
    }
  }

  private synchronized void removeMessageListener() {
    if (this.getAtmosphereResources().size() == 0 && this.messageListenerRegistrationId != null
        && this.getTopic() != null) {
      this.getTopic().removeMessageListener(this.messageListenerRegistrationId);
      this.messageListenerRegistrationId = null;
      logger.info("Removed message listener from topic");
    }
  }

  public Broadcaster addAtmosphereResource(AtmosphereResource resource) {
    Broadcaster result = super.addAtmosphereResource(resource);
    this.addMessageListener();
    return result;
  }

  public Broadcaster removeAtmosphereResource(AtmosphereResource resource) {
    Broadcaster result = super.removeAtmosphereResource(resource);
    this.removeMessageListener();
    return result;
  }

  public synchronized void setID(String id) {
    super.setID(id);
    this.setUp();
  }

  public void destroy() {
    if (!this.isClosed.get()) {
      this.topic.destroy();
      this.topic = null;
    }

    super.destroy();
  }

  public void incomingBroadcast() {
    logger.info("Subscribing to: {}", this.getID());
  }

  public void outgoingBroadcast(Object message) {
    this.topic.publish(message);
  }

  protected ITopic getTopic() {
    return this.topic;
  }
}
