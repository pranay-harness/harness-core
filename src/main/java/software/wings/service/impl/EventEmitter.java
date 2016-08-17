package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import software.wings.beans.Event;

import java.util.Optional;

/**
 * Created by peeyushaggarwal on 8/16/16.
 */
public class EventEmitter {
  private BroadcasterFactory broadcasterFactory;

  /**
   * Instantiates a new Event emitter.
   *
   * @param broadcasterFactory the broadcaster factory
   */
  public EventEmitter(BroadcasterFactory broadcasterFactory) {
    this.broadcasterFactory = broadcasterFactory;
  }

  /**
   * Send.
   *
   * @param channel the channel
   * @param event   the event
   */
  public void send(Channel channel, Event event) {
    if (isNotBlank(event.getUuid())
        && broadcasterFactory.lookup("/stream/" + channel.getChannelName() + "/" + event.getUuid()) != null) {
      broadcasterFactory.lookup("/stream/" + channel.getChannelName() + "/" + event.getUuid()).broadcast(event);
    }
    Optional.ofNullable(broadcasterFactory.lookup("/stream/" + channel.getChannelName()))
        .ifPresent(o -> ((Broadcaster) o).broadcast(event));
  }

  /**
   * The enum Channel.
   */
  public enum Channel {
    /**
     * Artifacts channel.
     */
    ARTIFACTS("artifacts");

    private String channelName;

    Channel(String channelName) {
      this.channelName = channelName;
    }

    /**
     * Getter for property 'channelName'.
     *
     * @return Value for property 'channelName'.
     */
    public String getChannelName() {
      return channelName;
    }

    /**
     * Setter for property 'channelName'.
     *
     * @param channelName Value to set for property 'channelName'.
     */
    public void setChannelName(String channelName) {
      this.channelName = channelName;
    }
  }
}
