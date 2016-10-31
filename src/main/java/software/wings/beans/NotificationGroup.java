package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 10/30/16.
 */
@Entity(value = "notificationGroups", noClassnameStored = true)
public class NotificationGroup extends Base {
  private String name;

  private Map<NotificationChannelType, List<String>> addressesByChannelType = new HashMap<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<NotificationChannelType, List<String>> getAddressesByChannelType() {
    return addressesByChannelType;
  }

  public void setAddressesByChannelType(Map<NotificationChannelType, List<String>> addressesByChannelType) {
    this.addressesByChannelType = addressesByChannelType;
  }

  public static final class NotificationGroupBuilder {
    private String name;
    private Map<NotificationChannelType, List<String>> addressesByChannelType = new HashMap<>();
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private NotificationGroupBuilder() {}

    public static NotificationGroupBuilder aNotificationGroup() {
      return new NotificationGroupBuilder();
    }

    public NotificationGroupBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public NotificationGroupBuilder addAddressesByChannelType(
        NotificationChannelType notificationChannelType, List<String> addresses) {
      this.addressesByChannelType.put(notificationChannelType, addresses);
      return this;
    }

    public NotificationGroupBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public NotificationGroupBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public NotificationGroupBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public NotificationGroupBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public NotificationGroupBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public NotificationGroupBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public NotificationGroup build() {
      NotificationGroup notificationGroup = new NotificationGroup();
      notificationGroup.setName(name);
      notificationGroup.setAddressesByChannelType(addressesByChannelType);
      notificationGroup.setUuid(uuid);
      notificationGroup.setAppId(appId);
      notificationGroup.setCreatedBy(createdBy);
      notificationGroup.setCreatedAt(createdAt);
      notificationGroup.setLastUpdatedBy(lastUpdatedBy);
      notificationGroup.setLastUpdatedAt(lastUpdatedAt);
      return notificationGroup;
    }
  }
}
