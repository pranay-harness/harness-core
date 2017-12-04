package software.wings.utils;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.util.AbstractBroadcasterProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.core.maintenance.MaintenanceController;
import software.wings.core.maintenance.MaintenanceListener;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 1/11/17.
 */
public class HazelcastBroadcaster extends AbstractBroadcasterProxy implements MaintenanceListener {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public static HazelcastInstance HAZELCAST_INSTANCE;

  @Inject private MaintenanceController maintenanceController;

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
    maintenanceController.register(this);
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
    if (messageListenerRegistrationId != null && topic != null) {
      topic.removeMessageListener(messageListenerRegistrationId);
      messageListenerRegistrationId = null;
      logger.info("Removed message listener from topic");
    }
  }

  @Override
  public Broadcaster addAtmosphereResource(AtmosphereResource resource) {
    Broadcaster result = super.addAtmosphereResource(resource);
    if (!isMaintenance()) {
      addMessageListener();
    }
    return result;
  }

  @Override
  public Broadcaster removeAtmosphereResource(AtmosphereResource resource) {
    Broadcaster result = super.removeAtmosphereResource(resource);
    if (isEmpty(getAtmosphereResources())) {
      removeMessageListener();
    }
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

  @Override
  public void onEnterMaintenance() {
    logger.info("Entering maintenance mode");
    removeMessageListener();
  }

  @Override
  public void onLeaveMaintenance() {
    logger.info("Leaving maintenance mode");
    addMessageListener();
  }
}
