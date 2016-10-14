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
  @NotEmpty private String accountId;
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

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, description, accountId, permissions, adminRole);
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
        && Objects.equals(this.accountId, other.accountId) && Objects.equals(this.permissions, other.permissions)
        && Objects.equals(this.adminRole, other.adminRole);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("description", description)
        .add("accountId", accountId)
        .add("permissions", permissions)
        .add("adminRole", adminRole)
        .toString();
  }

  public static final class Builder {
    private String name;
    private String description;
    private String accountId;
    private List<Permission> permissions;
    private boolean adminRole = false;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static Builder aRole() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withPermissions(List<Permission> permissions) {
      this.permissions = permissions;
      return this;
    }

    public Builder withAdminRole(boolean adminRole) {
      this.adminRole = adminRole;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder but() {
      return aRole()
          .withName(name)
          .withDescription(description)
          .withAccountId(accountId)
          .withPermissions(permissions)
          .withAdminRole(adminRole)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public Role build() {
      Role role = new Role();
      role.setName(name);
      role.setDescription(description);
      role.setAccountId(accountId);
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
