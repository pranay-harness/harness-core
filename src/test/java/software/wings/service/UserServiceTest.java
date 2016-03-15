package software.wings.service;

import org.junit.Test;
import org.mongodb.morphia.Datastore;
import software.wings.beans.User;
import software.wings.dl.MongoConnectionFactory;

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
    user.setEmail("anubhaw@gmail.com");
    user.setName("Anubhaw Srivastava");
    user.setPasswordHash("password");
    user.setToken("DummyTokenShouldNotBeStored");
    user.setLastLogin(System.currentTimeMillis());
    userService.register(user);
  }

  @Test
  public void testMatchPassword() throws Exception {
    long startTime = System.currentTimeMillis();
    System.out.println(
        userService.matchPassword("password", "$2a$10$ygoANZ1GfZf09oUDCcDLuO1cWt7x2XDl/Dq3J.sYgkC51KDEMK64C"));
    System.out.println(System.currentTimeMillis() - startTime);
  }
}