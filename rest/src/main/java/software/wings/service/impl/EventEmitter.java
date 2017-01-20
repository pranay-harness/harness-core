package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Singleton;

import org.atmosphere.cpr.MetaBroadcaster;
import software.wings.beans.Application;
import software.wings.beans.Event;
import software.wings.security.PermissionAttribute.PermissionScope;
import software.wings.service.intfc.AppService;

import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 8/16/16.
 */
@Singleton
public class EventEmitter {
  private MetaBroadcaster metaBroadcaster;
  @Inject private AppService appService;

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
    if ("*".equals(event.getOrgId()) && !"all".equals(event.getAppId())) {
      Application application = appService.get(event.getAppId());
      event.setOrgId(application.getAccountId());
    }
    metaBroadcaster.broadcastTo("/stream/" + channel.getTarget() + "/" + event.getOrgId() + "/" + event.getAppId() + "/"
            + event.getEnvId() + "/" + event.getServiceId() + "/" + channel.getChannelName(),
        event);
    if (isNotBlank(event.getUuid())) {
      metaBroadcaster.broadcastTo("/stream/" + channel.getTarget() + "/" + event.getOrgId() + "/" + event.getAppId()
              + "/" + event.getEnvId() + "/" + event.getServiceId() + "/" + channel.getChannelName() + "/"
              + event.getUuid(),
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
    ARTIFACTS("artifacts", "ui", "ARTIFACT:ALL"), /**
                                                   * Activities channel.
                                                   */
    ACTIVITIES("activities", "ui", "ENVIRONMENT:ALL"),

    DELEGATES("delegates", "ui", "");

    private String channelName;
    private String permission;
    private String target;
    private PermissionScope scope = PermissionScope.APP;

    Channel(String channelName, String target, String permission) {
      this.channelName = channelName;
      this.permission = permission;
      this.target = target;
    }

    Channel(String channelName, String target, String permission, PermissionScope scope) {
      this.channelName = channelName;
      this.permission = permission;
      this.scope = scope;
      this.target = target;
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

    public String getTarget() {
      return target;
    }
  }
}
