package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.atmosphere.cpr.MetaBroadcaster;
import software.wings.beans.Event;

/**
 * Created by peeyushaggarwal on 8/16/16.
 */
public class EventEmitter {
  private MetaBroadcaster metaBroadcaster;

  /**
   * Instantiates a new Event emitter.
   *
   * @param metaBroadcaster the broadcaster factory
   */
  public EventEmitter(MetaBroadcaster metaBroadcaster) {
    this.metaBroadcaster = metaBroadcaster;
  }

  /**
   * Send.
   *
   * @param channel the channel
   * @param event   the event
   */
  public void send(Channel channel, Event event) {
    metaBroadcaster.broadcastTo("/stream/" + event.getOrgId() + "/" + event.getAppId() + "/" + event.getEnvId() + "/"
            + event.getServiceId() + "/" + channel.getChannelName(),
        event);
    if (isNotBlank(event.getUuid())) {
      metaBroadcaster.broadcastTo("/stream/" + event.getOrgId() + "/" + event.getAppId() + "/" + event.getEnvId() + "/"
              + event.getServiceId() + "/" + channel.getChannelName() + "/" + event.getUuid(),
          event);
    }
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
