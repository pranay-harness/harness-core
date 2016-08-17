package software.wings.beans;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 8/16/16.
 */
public class Event {
  private Type type;
  private String uuid;

  /**
   * Gets type.
   *
   * @return the type
   */
  public Type getType() {
    return type;
  }

  /**
   * Sets type.
   *
   * @param type the type
   */
  public void setType(Type type) {
    this.type = type;
  }

  /**
   * Gets uuid.
   *
   * @return the uuid
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
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

  /**
   * The enum Type.
   */
  public enum Type {
    /**
     * Create type.
     */
    CREATE, /**
             * Update type.
             */
    UPDATE, /**
             * Delete type.
             */
    DELETE
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private Type type;
    private String uuid;

    private Builder() {}

    /**
     * An event builder.
     *
     * @return the builder
     */
    public static Builder anEvent() {
      return new Builder();
    }

    /**
     * With type builder.
     *
     * @param type the type
     * @return the builder
     */
    public Builder withType(Type type) {
      this.type = type;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anEvent().withType(type).withUuid(uuid);
    }

    /**
     * Build event.
     *
     * @return the event
     */
    public Event build() {
      Event event = new Event();
      event.setType(type);
      event.setUuid(uuid);
      return event;
    }
  }
}
