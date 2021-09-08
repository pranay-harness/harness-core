package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessModule._360_CG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.MOHIT;
import static io.harness.rule.OwnerRule.NANDAN;
import static io.harness.rule.OwnerRule.VOJIN;

import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.UserInvite.UserInviteBuilder.anUserInvite;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SignupService;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
@OwnedBy(PL)
@TargetModule(_360_CG_MANAGER)
public class UserServiceImplTest extends WingsBaseTest {
  @Mock AccountService accountService;
  @Inject @InjectMocks UserServiceImpl userServiceImpl;
  @Mock SignupService signupService;
  @Mock EventPublishHelper eventPublishHelper;
  @Inject WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testTrialSignup() {
    String email = "email@email.com";
    UserInvite userInvite =
        anUserInvite().withEmail(email).withCompanyName("companyName").withAccountName("accountName").build();
    userInvite.setPassword("somePassword".toCharArray());
    when(signupService.getUserInviteByEmail(Matchers.eq(email))).thenReturn(null);
    userServiceImpl.trialSignup(userInvite);
    // Verifying that the mail is sent and event is published when a new user sign ups for trial
    Mockito.verify(accountService, times(1)).validateAccount(any(Account.class));
    Mockito.verify(signupService, times(1)).sendTrialSignupVerificationEmail(any(), any());
    Mockito.verify(eventPublishHelper, times(1)).publishTrialUserSignupEvent(any(), any(), any());
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testValidateAccountName() {
    Mockito.doNothing().when(accountService).validateAccount(any(Account.class));
    userServiceImpl.validateAccountName("correctName", "correctAccountName");
    Mockito.verify(accountService, times(1)).validateAccount(any(Account.class));
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testValidateAccountNameWhenInvalidName() {
    doThrow(new InvalidRequestException("")).when(accountService).validateAccount(any(Account.class));
    userServiceImpl.validateAccountName("someInvalidName", "someInvalidName");
    Mockito.verify(accountService, times(1)).validateAccount(any(Account.class));
  }

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  public void testGetUserCountForPendingAccounts() {
    Account account = anAccount().withUuid("ACCOUNT_ID").build();
    wingsPersistence.save(account);
    User user = anUser().pendingAccounts(Arrays.asList(account)).build();
    wingsPersistence.save(user);
    assertThat(userServiceImpl.getTotalUserCount("ACCOUNT_ID", true)).isEqualTo(1);
  }

  private void setup() {
    Account account = anAccount().withUuid("ACCOUNT_ID").build();
    wingsPersistence.save(account);
    User user1 = User.Builder.anUser()
                     .uuid(UUIDGenerator.generateUuid())
                     .accounts(Collections.singletonList(account))
                     .email("aBc@harness.io")
                     .name("pqr")
                     .build();
    User user2 = User.Builder.anUser()
                     .uuid(UUIDGenerator.generateUuid())
                     .accounts(Collections.singletonList(account))
                     .email("xYz@harness.io")
                     .name("eFg")
                     .build();
    User user3 = User.Builder.anUser()
                     .uuid(UUIDGenerator.generateUuid())
                     .accounts(Collections.singletonList(account))
                     .email("pqR@harness.io")
                     .name("lMN")
                     .build();
    wingsPersistence.save(user1);
    wingsPersistence.save(user2);
    wingsPersistence.save(user3);
  }

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  public void shouldSortUsers() {
    setup();

    PageRequest pageRequest = mock(PageRequest.class);
    MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
    UriInfo uriInfo = mock(UriInfo.class);
    when(pageRequest.getUriInfo()).thenReturn(uriInfo);

    map.put("sort[0][direction]", Arrays.asList("ASC"));
    map.put("sort[0][field]", Arrays.asList("name"));
    when(uriInfo.getQueryParameters(true)).thenReturn(map);
    List<User> userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "", 0, 30, false, true);
    assertThat(userList.get(2).getName()).isEqualTo("pqr");

    map.put("sort[0][field]", Arrays.asList("email"));
    when(uriInfo.getQueryParameters(true)).thenReturn(map);
    userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "", 0, 30, false, true);
    assertThat(userList.get(2).getName()).isEqualTo("eFg");
  }

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  public void shouldSearchUsers() {
    setup();

    PageRequest pageRequest = mock(PageRequest.class);
    MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
    UriInfo uriInfo = mock(UriInfo.class);
    when(pageRequest.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getQueryParameters(true)).thenReturn(map);

    List<User> userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "ab", 0, 30, false, true);
    assertThat(userList.size()).isEqualTo(1);
    assertThat(userList.get(0).getName()).isEqualTo("pqr");

    userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "PqR", 0, 30, false, true);
    assertThat(userList.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = MOHIT)
  @Category(UnitTests.class)
  public void shouldSearchAndSortUsers() {
    setup();

    PageRequest pageRequest = mock(PageRequest.class);
    MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
    UriInfo uriInfo = mock(UriInfo.class);
    when(pageRequest.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getQueryParameters(true)).thenReturn(map);

    map.put("sort[0][direction]", Arrays.asList("DESC"));
    map.put("sort[0][field]", Arrays.asList("email"));
    List<User> userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "PqR", 0, 30, false, true);
    assertThat(userList.get(1).getName()).isEqualTo("pqr");

    userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "fgh", 0, 30, false, true);
    assertThat(userList.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = VOJIN)
  @Category(UnitTests.class)
  public void sanitizeUserNameTest_without_malicious_content() {
    String name1 = "Vojin Đukić";
    String name2 = "Peter O'Toole";
    String name3 = "You <p>user login</p> is <strong>owasp-user01</strong>";
    String expectedName3 = "You user login is owasp-user01";
    assertThat(userServiceImpl.sanitizeUserName(name1)).isEqualTo(name1);
    assertThat(userServiceImpl.sanitizeUserName(name2)).isEqualTo(name2);
    assertThat(userServiceImpl.sanitizeUserName(name3)).isEqualTo(expectedName3);
  }

  @Test
  @Owner(developers = NANDAN)
  @Category(UnitTests.class)
  public void sanitizeUserNameTest_with_malicious_content() {
    String name1 = "<script>alert(22);</script>" + USER_NAME + "<img src='#' onload='javascript:alert(23);'>";
    String name2 = "'''><img src=x onerror=alert(1)> '''><img srx onerror=alert(1)>";
    String name3 = "</h2>special offer <a href=www.attacker.site>" + USER_NAME + "</a><h2>";

    String expectedName2 = "'''> '''>";
    String expectedName3 = "special offer " + USER_NAME;
    assertThat(userServiceImpl.sanitizeUserName(name1)).isEqualTo(USER_NAME);
    assertThat(userServiceImpl.sanitizeUserName(name2)).isEqualTo(expectedName2);
    assertThat(userServiceImpl.sanitizeUserName(name3)).isEqualTo(expectedName3);
  }
}
