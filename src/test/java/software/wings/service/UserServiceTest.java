package software.wings.service;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseUnitTest;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Permission;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;

import java.util.Collections;
import javax.inject.Inject;

/**
 * Created by anubhaw on 3/9/16.
 */
@Ignore
public class UserServiceTest extends WingsBaseUnitTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;
  @Inject private RoleService roleService;

  @Test
  public void testRegister() throws Exception {
    User user = new User();
    user.setEmail("user1@wings.software");
    user.setName("John Doe");
    user.setPasswordHash("password");
    userService.register(user);

    user.setEmail("user2@wings.software");
    user.setUuid(null);
    userService.register(user);
    user.setEmail("user3@wings.software");
    user.setUuid(null);
    userService.register(user);
  }

  @Test
  public void testCreateRole() {
    Permission permission = new Permission("ALL", "ALL", "ALL", "ALL");
    Role role = new Role("ADMIN", "Administrator role. It can access resource and perform any action",
        Collections.singletonList(permission));
    role.setUuid("BFB4B4F079EB449C9B421D1BB720742E");
    wingsPersistence.save(role);
    permission = new Permission("APP", "ALL", "ALL", "ALL");
    role = new Role("APP_ALL", "APP access", Collections.singletonList(permission));
    role.setUuid("2C496ED72DDC48FEA51E5C3736DD33B9");
    wingsPersistence.save(role);
  }

  @Test
  public void testAssignRoleToUser() {
    Role role = new Role();
    role.setUuid("35D7D2C04A164655AB732B963A5DD308");
    Query<User> updateQuery =
        wingsPersistence.createQuery(User.class).field(ID_KEY).equal("D3BB4DEA57D043BCA73597CCDE01E637");
    UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class).add("roles", role);
    wingsPersistence.update(updateQuery, updateOperations);
    PageResponse<User> list = userService.list(new PageRequest<User>());
    userService.addRole("51968DC229D7479EAA1D8B56D6C8EB6D", "BFB4B4F079EB449C9B421D1BB720742E");
    userService.addRole("51968DC229D7479EAA1D8B56D6C8EB6D", "2C496ED72DDC48FEA51E5C3736DD33B9");
    userService.addRole("1AF8F38C83394D67B03AC13E704C8186", "BFB4B4F079EB449C9B421D1BB720742E");
    userService.addRole("4D92F1B445EB4C2C8BD0C2898AF95F03", "BFB4B4F079EB449C9B421D1BB720742E");
    userService.addRole("4D92F1B445EB4C2C8BD0C2898AF95F03", "2C496ED72DDC48FEA51E5C3736DD33B9");
  }

  @Test
  public void testRevokeRoleToUser() {
    userService.revokeRole("51968DC229D7479EAA1D8B56D6C8EB6D", "AFBC5F9953BB4F20A56B84CE845EF7A3");
  }

  @Test
  public void deleteRole() {
    roleService.delete("2C496ED72DDC48FEA51E5C3736DD33B9");
  }

  @Test
  public void testMatchPassword() throws Exception {
    long startTime = System.currentTimeMillis();
    System.out.println(
        userService.matchPassword("password", "$2a$10$ygoANZ1GfZf09oUDCcDLuO1cWt7x2XDl/Dq3J.sYgkC51KDEMK64C"));
    System.out.println(System.currentTimeMillis() - startTime);
  }

  @Test
  public void testHelper() {
    User user = wingsPersistence.get(User.class, "D3BB4DEA57D043BCA73597CCDE01E637");
    System.out.println(user);
  }
}
