package software.wings.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.util.AbstractBroadcasterProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by peeyushaggarwal on 1/11/17.
 */
@SuppressFBWarnings("MS_CANNOT_BE_FINAL")
public class HazelcastBroadcaster extends AbstractBroadcasterProxy {
  private static final Logger logger = LoggerFactory.getLogger(HazelcastBroadcaster.class);

  public static HazelcastInstance HAZELCAST_INSTANCE;

  private final AtomicBoolean isClosed = new AtomicBoolean();
  private ITopic topic;
  private String messageListenerRegistrationId;

  public HazelcastBroadcaster() {}

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
    topic = HAZELCAST_INSTANCE.getTopic(getID());
    config.shutdownHook(() -> {
      HazelcastBroadcaster.HAZELCAST_INSTANCE.shutdown();
      isClosed.set(true);
    });
  }

  private synchronized void addMessageListener() {
    if (isNotEmpty(getAtmosphereResources()) && messageListenerRegistrationId == null) {
      messageListenerRegistrationId =
          topic.addMessageListener(message -> broadcastReceivedMessage(message.getMessageObject()));
      logger.info("Added message listener to topic");
    }
  }

  private synchronized void removeMessageListener() {
    if (isEmpty(getAtmosphereResources()) && messageListenerRegistrationId != null && topic != null) {
      topic.removeMessageListener(messageListenerRegistrationId);
      messageListenerRegistrationId = null;
      logger.info("Removed message listener from topic");
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
    logger.info("Subscribing to: {}", getID());
  }

  @Override
  public void outgoingBroadcast(Object message) {
    topic.publish(message);
  }
}
