package software.wings.beans;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 8/16/16.
 */
public class Event {
  private Type type;
  private String uuid;

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, uuid);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Event other = (Event) obj;
    return Objects.equals(this.type, other.type) && Objects.equals(this.uuid, other.uuid);
  }

  public enum Type { CREATE, UPDATE, DELETE }

  public static final class Builder {
    private Type type;
    private String uuid;

    private Builder() {}

    public static Builder anEvent() {
      return new Builder();
    }

    public Builder withType(Type type) {
      this.type = type;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder but() {
      return anEvent().withType(type).withUuid(uuid);
    }

    public Event build() {
      Event event = new Event();
      event.setType(type);
      event.setUuid(uuid);
      return event;
    }
  }
}
