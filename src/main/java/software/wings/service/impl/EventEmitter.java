package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.atmosphere.cpr.MetaBroadcaster;
import software.wings.beans.Event;
import software.wings.security.PermissionAttribute.PermissionScope;

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
    ARTIFACTS("artifacts", "ARTIFACT:ALL"), /**
                                             * Activities channel.
                                             */
    ACTIVITIES("activities", "ENVIRONMENT:ALL");

    private String channelName;
    private String permission;
    private PermissionScope scope = PermissionScope.APP;

    Channel(String channelName, String permission) {
      this.channelName = channelName;
      this.permission = permission;
    }

    Channel(String channelName, String permission, PermissionScope scope) {
      this.channelName = channelName;
      this.permission = permission;
      this.scope = scope;
    }

    /**
     * Gets channel by channel name.
     *
     * @param channelName the channel name
     * @return the channel by channel name
     */
    public static Channel getChannelByChannelName(String channelName) {
      for (Channel channel : values()) {
        if (channel.getChannelName().equalsIgnoreCase(channelName)) {
          return channel;
        }
      }
      return null;
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
     * Gets permission.
     *
     * @return the permission
     */
    public String getPermission() {
      return permission;
    }

    /**
     * Sets permission.
     *
     * @param permission the permission
     */
    public void setPermission(String permission) {
      this.permission = permission;
    }

    /**
     * Gets scope.
     *
     * @return the scope
     */
    public PermissionScope getScope() {
      return scope;
    }
  }
}
