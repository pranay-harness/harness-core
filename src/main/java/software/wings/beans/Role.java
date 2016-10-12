package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 3/16/16.
 */
@Entity(value = "roles", noClassnameStored = true)
public class Role extends Base {
  @Indexed(unique = true) @NotEmpty private String name;
  private String description;
  @Embedded private List<Permission> permissions;
  private boolean adminRole = false;

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name.toUpperCase();
  }

  /**
   * Gets description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Gets permissions.
   *
   * @return the permissions
   */
  public List<Permission> getPermissions() {
    return permissions;
  }

  /**
   * Sets permissions.
   *
   * @param permissions the permissions
   */
  public void setPermissions(List<Permission> permissions) {
    this.permissions = permissions;
  }

  /**
   * Adds permission to role.
   *
   * @param permission permission to add.
   */
  public void addPermission(Permission permission) {
    if (permissions == null) {
      permissions = new ArrayList<>();
    }
    permissions.add(permission);
  }

  /**
   * Is admin role boolean.
   *
   * @return the boolean
   */
  public boolean isAdminRole() {
    return adminRole;
  }

  /**
   * Sets admin role.
   *
   * @param adminRole the admin role
   */
  public void setAdminRole(boolean adminRole) {
    this.adminRole = adminRole;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, description, permissions, adminRole);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final Role other = (Role) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description)
        && Objects.equals(this.permissions, other.permissions) && Objects.equals(this.adminRole, other.adminRole);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("description", description)
        .add("permissions", permissions)
        .add("adminRole", adminRole)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String description;
    private List<Permission> permissions;
    private boolean adminRole = false;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A role builder.
     *
     * @return the builder
     */
    public static Builder aRole() {
      return new Builder();
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With description builder.
     *
     * @param description the description
     * @return the builder
     */
    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    /**
     * With permissions builder.
     *
     * @param permissions the permissions
     * @return the builder
     */
    public Builder withPermissions(List<Permission> permissions) {
      this.permissions = permissions;
      return this;
    }

    /**
     * With admin role builder.
     *
     * @param adminRole the admin role
     * @return the builder
     */
    public Builder withAdminRole(boolean adminRole) {
      this.adminRole = adminRole;
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
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aRole()
          .withName(name)
          .withDescription(description)
          .withPermissions(permissions)
          .withAdminRole(adminRole)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build role.
     *
     * @return the role
     */
    public Role build() {
      Role role = new Role();
      role.setName(name);
      role.setDescription(description);
      role.setPermissions(permissions);
      role.setAdminRole(adminRole);
      role.setUuid(uuid);
      role.setAppId(appId);
      role.setCreatedBy(createdBy);
      role.setCreatedAt(createdAt);
      role.setLastUpdatedBy(lastUpdatedBy);
      role.setLastUpdatedAt(lastUpdatedAt);
      return role;
    }
  }
}
