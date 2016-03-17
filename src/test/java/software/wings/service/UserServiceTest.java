package software.wings.service;

import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import software.wings.beans.AuthToken;
import software.wings.beans.Permission;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.MongoConnectionFactory;

import java.util.Collections;

/**
 * Created by anubhaw on 3/9/16.
 */

public class UserServiceTest {
  private Datastore getDataStore() {
    MongoConnectionFactory factory = new MongoConnectionFactory();
    factory.setDb("wings");
    factory.setHost("localhost");
    return factory.getDatastore();
  }

  UserService userService = new UserService(getDataStore());

  @Test
  public void testRegister() throws Exception {
    User user = new User();
    user.setEmail("user1@wings.software");
    user.setName("John Doe");
    user.setPasswordHash("password");
    userService.register(user);
  }

  @Test
  public void testMatchPassword() throws Exception {
    long startTime = System.currentTimeMillis();
    System.out.println(
        userService.matchPassword("password", "$2a$10$ygoANZ1GfZf09oUDCcDLuO1cWt7x2XDl/Dq3J.sYgkC51KDEMK64C"));
    System.out.println(System.currentTimeMillis() - startTime);
  }

  @Test
  public void testCreatePermission() {
    getDataStore().save(new Permission("DEPLOYMENT", "CREATE", "ALL", "ALL"));
  }

  @Test
  public void testCreateRole() {
    Role role = new Role();
    role.setName("ADMIN");
    role.setDescription("Administrator role. It can access resource and perform any action");
    Permission permission = new Permission("ALL", "ALL", "ALL", "ALL");
    getDataStore().save(permission);
    role.setPermissions(Collections.singletonList(permission));
    getDataStore().save(role);
  }
}