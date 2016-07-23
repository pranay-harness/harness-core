package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.name.Named;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.EmailVerificationToken;
import software.wings.beans.Permission;
import software.wings.beans.Role;
import software.wings.beans.SearchFilter;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by anubhaw on 3/9/16.
 */
public class UserServiceTest extends WingsBaseTest {
  private final User.Builder userBuilder =
      anUser().withAppId(APP_ID).withEmail(USER_EMAIL).withName(USER_NAME).withPassword(PASSWORD);
  @Mock private EmailNotificationService<EmailData> emailDataNotificationService;
  @Mock private RoleService roleService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MainConfiguration configuration;

  @Inject @InjectMocks private UserService userService;

  @Inject @Named("primaryDatastore") private Datastore datastore;

  @Captor private ArgumentCaptor<EmailData> emailDataArgumentCaptor;
  @Captor private ArgumentCaptor<User> userArgumentCaptor;
  @Captor private ArgumentCaptor<PageRequest<User>> pageRequestArgumentCaptor;
  @Captor private ArgumentCaptor<Query<EmailVerificationToken>> emailVerificationQueryArgumentCaptor;

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(wingsPersistence.createQuery(User.class)).thenReturn(datastore.createQuery(User.class));
    when(wingsPersistence.createQuery(EmailVerificationToken.class))
        .thenReturn(datastore.createQuery(EmailVerificationToken.class));
  }

  /**
   * Test register.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldRegisterUser() throws Exception {
    User savedUser = userBuilder.withUuid(USER_ID)
                         .withEmailVerified(false)
                         .withCompanyName(COMPANY_NAME)
                         .withPasswordHash(hashpw(PASSWORD, BCrypt.gensalt()))
                         .build();
    when(configuration.getPortal().getAllowedDomains()).thenReturn(asList("wings.software"));
    when(configuration.getPortal().getCompanyName()).thenReturn("COMPANY_NAME");
    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(wingsPersistence.saveAndGet(eq(User.class), any(User.class))).thenReturn(savedUser);
    when(wingsPersistence.saveAndGet(eq(EmailVerificationToken.class), any(EmailVerificationToken.class)))
        .thenReturn(new EmailVerificationToken(USER_ID));

    userService.register(userBuilder.build());

    verify(wingsPersistence).saveAndGet(eq(User.class), userArgumentCaptor.capture());
    assertThat(BCrypt.checkpw(PASSWORD, userArgumentCaptor.getValue().getPasswordHash())).isTrue();
    assertThat(userArgumentCaptor.getValue().isEmailVerified()).isFalse();
    assertThat(userArgumentCaptor.getValue().getCompanyName()).isEqualTo(COMPANY_NAME);

    verify(emailDataNotificationService).send(emailDataArgumentCaptor.capture());
    assertThat(emailDataArgumentCaptor.getValue().getTo().get(0)).isEqualTo(USER_EMAIL);
    assertThat(emailDataArgumentCaptor.getValue().getTemplateName()).isEqualTo("signup");
    assertThat(((Map) emailDataArgumentCaptor.getValue().getTemplateModel()).get("name")).isEqualTo(USER_NAME);
    assertThat(((Map<String, String>) emailDataArgumentCaptor.getValue().getTemplateModel())
                   .get("url")
                   .startsWith(PORTAL_URL + "/api/users/verify"))
        .isTrue();
  }

  /**
   * Should match password.
   */
  @Test
  public void shouldMatchPassword() {
    String hashpw = hashpw(PASSWORD, BCrypt.gensalt());
    assertThat(userService.matchPassword(PASSWORD, hashpw)).isTrue();
  }

  /**
   * Should update user.
   */
  @Test
  public void shouldUpdateUser() {
    userService.update(anUser().withAppId(APP_ID).withUuid(USER_ID).withEmail(USER_EMAIL).withName(USER_NAME).build());
    verify(wingsPersistence).updateFields(User.class, USER_ID, ImmutableMap.of("name", USER_NAME));
    verify(wingsPersistence).get(User.class, APP_ID, USER_ID);
  }

  /**
   * Should list users.
   */
  @Test
  public void shouldListUsers() {
    PageRequest<User> request = new PageRequest<>();
    request.addFilter("appId", GLOBAL_APP_ID, EQ);
    userService.list(request);
    verify(wingsPersistence).query(eq(User.class), pageRequestArgumentCaptor.capture());
    SearchFilter filter = (SearchFilter) pageRequestArgumentCaptor.getValue().getFilters().get(0);
    assertThat(filter.getFieldName()).isEqualTo("appId");
    assertThat(filter.getFieldValues()).containsExactly(GLOBAL_APP_ID);
    assertThat(filter.getOp()).isEqualTo(EQ);
  }

  /**
   * Should delete user.
   */
  @Test
  public void shouldDeleteUser() {
    userService.delete(USER_ID);
    verify(wingsPersistence).delete(User.class, USER_ID);
  }

  /**
   * Should fetch user.
   */
  @Test
  public void shouldFetchUser() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).build());
    User user = userService.get(USER_ID);
    verify(wingsPersistence).get(User.class, USER_ID);
    assertThat(user).isEqualTo(userBuilder.withUuid(USER_ID).build());
  }

  /**
   * Should verify email.
   */
  @Test
  public void shouldVerifyEmail() {
    when(wingsPersistence.executeGetOneQuery(emailVerificationQueryArgumentCaptor.capture()))
        .thenReturn(EmailVerificationToken.Builder.anEmailVerificationToken()
                        .withUuid("TOKEN_ID")
                        .withUserId(USER_ID)
                        .withToken("TOKEN")
                        .build());
    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    userService.verifyEmail("TOKEN");
    assertThat(emailVerificationQueryArgumentCaptor.getValue().getQueryObject().get("appId")).isEqualTo(GLOBAL_APP_ID);
    assertThat(emailVerificationQueryArgumentCaptor.getValue().getQueryObject().get("token")).isEqualTo("TOKEN");
    verify(wingsPersistence).updateFields(User.class, USER_ID, ImmutableMap.of("emailVerified", true));
    verify(wingsPersistence).delete(EmailVerificationToken.class, "TOKEN_ID");
  }

  /**
   * Should send email.
   *
   * @throws EmailException    the email exception
   * @throws TemplateException the template exception
   * @throws IOException       the io exception
   */
  @Test
  public void shouldSendEmail() throws EmailException, TemplateException, IOException {
    emailDataNotificationService.send(asList("anubhaw@gmail.com"), asList(), "wings-test", "hi");
  }

  /**
   * Test create role.
   */
  @Test
  @Ignore
  public void shouldCreateRole() {
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

  /**
   * Test assign role to user.
   */
  @Test
  @Ignore
  public void shouldAssignRoleToUser() {
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

  /**
   * Test revoke role to user.
   */
  @Test
  @Ignore
  public void testRevokeRoleToUser() {
    userService.revokeRole("51968DC229D7479EAA1D8B56D6C8EB6D", "AFBC5F9953BB4F20A56B84CE845EF7A3");
  }

  /**
   * Delete role.
   */
  @Test
  @Ignore
  public void deleteRole() {
    roleService.delete("2C496ED72DDC48FEA51E5C3736DD33B9");
  }
}
