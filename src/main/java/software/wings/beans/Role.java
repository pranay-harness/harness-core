package software.wings.beans;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.ArrayList;
import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 3/16/16.
 */
@Entity(value = "roles", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class Role extends Base {
  private String name;
  private String description;
  @Embedded private List<Permission> permissions;

  /**
   * Instantiates a new role.
   */
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
}
