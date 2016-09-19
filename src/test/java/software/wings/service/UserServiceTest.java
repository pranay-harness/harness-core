package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.ErrorCodes.USER_DOES_NOT_EXIST;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.ROLE_ID;
import static software.wings.utils.WingsTestConstants.ROLE_NAME;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.EmailVerificationToken;
import software.wings.beans.SearchFilter;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
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

  @Inject @InjectMocks private UserService userService;

  @Inject @Named("primaryDatastore") private Datastore datastore;

  @Captor private ArgumentCaptor<EmailData> emailDataArgumentCaptor;
  @Captor private ArgumentCaptor<User> userArgumentCaptor;
  @Captor private ArgumentCaptor<PageRequest<User>> pageRequestArgumentCaptor;
  @Captor private ArgumentCaptor<Query<EmailVerificationToken>> emailVerificationQueryArgumentCaptor;

  @Mock Query<User> query;
  @Mock FieldEnd end;
  @Mock UpdateOperations<User> updateOperations;

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(wingsPersistence.createQuery(User.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
    when(wingsPersistence.createUpdateOperations(User.class)).thenReturn(updateOperations);
    when(updateOperations.add(any(), any())).thenReturn(updateOperations);
    when(wingsPersistence.createQuery(EmailVerificationToken.class))
        .thenReturn(datastore.createQuery(EmailVerificationToken.class));
  }

  /**
   * Test register.
   *
   * @throws Exception the exception
   */
  @Test
  @Ignore
  public void shouldRegisterUser() throws Exception {
    User savedUser = userBuilder.withUuid(USER_ID)
                         .withEmailVerified(false)
                         .withCompanyName(COMPANY_NAME)
                         .withPasswordHash(hashpw(PASSWORD, BCrypt.gensalt()))
                         .build();

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
    assertThat(((Map<String, String>) emailDataArgumentCaptor.getValue().getTemplateModel()).get("url"))
        .startsWith(PORTAL_URL + "/api/users/verify");
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
    User user = anUser().withAppId(APP_ID).withUuid(USER_ID).withEmail(USER_EMAIL).withName(USER_NAME).build();
    UserThreadLocal.set(user);

    userService.update(user);
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

  @Test
  public void shouldThrowExceptionIfUserDoesNotExist() {
    assertThatThrownBy(() -> userService.get("INVALID_USER_ID"))
        .isInstanceOf(WingsException.class)
        .hasMessage(USER_DOES_NOT_EXIST.getCode());
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
   * Test assign role to user.
   */
  @Test
  public void shouldAddRole() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).build());
    when(roleService.get(ROLE_ID)).thenReturn(aRole().withUuid(ROLE_ID).withName(ROLE_NAME).build());

    userService.addRole(USER_ID, ROLE_ID);
    verify(wingsPersistence, times(2)).get(User.class, USER_ID);
    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));
    verify(query).field(Mapper.ID_KEY);
    verify(end).equal(USER_ID);
    verify(updateOperations).add("roles", aRole().withUuid(ROLE_ID).withName(ROLE_NAME).build());
  }

  /**
   * Test revoke role to user.
   */
  @Test
  public void shouldRevokeRole() {
    when(wingsPersistence.get(User.class, USER_ID)).thenReturn(userBuilder.withUuid(USER_ID).build());
    when(roleService.get(ROLE_ID)).thenReturn(aRole().withUuid(ROLE_ID).withName(ROLE_NAME).build());

    userService.revokeRole(USER_ID, ROLE_ID);
    verify(wingsPersistence, times(2)).get(User.class, USER_ID);
    verify(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));
    verify(query).field(Mapper.ID_KEY);
    verify(end).equal(USER_ID);
    verify(updateOperations).removeAll("roles", aRole().withUuid(ROLE_ID).withName(ROLE_NAME).build());
  }
}
