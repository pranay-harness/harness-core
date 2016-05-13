package software.wings.beans;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 3/16/16.
 */
@Entity(value = "roles", noClassnameStored = true)
public class Role extends Base {
  private String name;
  private String description;
  @Embedded private List<Permission> permissions;

  public Role() {}

  /**
   * Creates a role object.
   *
   * @param name        role name.
   * @param description role description.
   * @param permissions permissions associated with role.
   */
  public Role(String name, String description, List<Permission> permissions) {
    this.name = name;
    this.description = description;
    this.permissions = permissions;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name.toUpperCase();
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Permission> getPermissions() {
    return permissions;
  }

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
}
