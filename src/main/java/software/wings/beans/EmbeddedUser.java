package software.wings.beans;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 10/6/16.
 */
public class EmbeddedUser {
  private String uuid;
  private String name;
  private String email;

  /**
   * Getter for property 'uuid'.
   *
   * @return Value for property 'uuid'.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Setter for property 'uuid'.
   *
   * @param uuid Value to set for property 'uuid'.
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   * Getter for property 'name'.
   *
   * @return Value for property 'name'.
   */
  public String getName() {
    return name;
  }

  /**
   * Setter for property 'name'.
   *
   * @param name Value to set for property 'name'.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Getter for property 'email'.
   *
   * @return Value for property 'email'.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Setter for property 'email'.
   *
   * @param email Value to set for property 'email'.
   */
  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, name, email);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final EmbeddedUser other = (EmbeddedUser) obj;
    return Objects.equals(this.uuid, other.uuid) && Objects.equals(this.name, other.name)
        && Objects.equals(this.email, other.email);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("uuid", uuid).add("name", name).add("email", email).toString();
  }

  public static final class Builder {
    private String uuid;
    private String name;
    private String email;

    private Builder() {}

    public static Builder anEmbeddedUser() {
      return new Builder();
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withEmail(String email) {
      this.email = email;
      return this;
    }

    public Builder but() {
      return anEmbeddedUser().withUuid(uuid).withName(name).withEmail(email);
    }

    public EmbeddedUser build() {
      EmbeddedUser embeddedUser = new EmbeddedUser();
      embeddedUser.setUuid(uuid);
      embeddedUser.setName(name);
      embeddedUser.setEmail(email);
      return embeddedUser;
    }
  }
}
