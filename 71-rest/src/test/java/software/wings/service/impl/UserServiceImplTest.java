package software.wings.service.impl;

import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.MOHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.UserInvite.UserInviteBuilder.anUserInvite;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SignupService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

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
    Mockito.verify(signupService, times(1)).sendEmail(any(), any(), any());
    Mockito.verify(eventPublishHelper, times(1)).publishTrialUserSignupEvent(any(), any(), any(), any(), any());
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
    List<User> userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "", 0, 30, false);
    assertThat(userList.get(2).getName()).isEqualTo("pqr");

    map.put("sort[0][field]", Arrays.asList("email"));
    when(uriInfo.getQueryParameters(true)).thenReturn(map);
    userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "", 0, 30, false);
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

    List<User> userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "ab", 0, 30, false);
    assertThat(userList.size()).isEqualTo(1);
    assertThat(userList.get(0).getName()).isEqualTo("pqr");

    userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "PqR", 0, 30, false);
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
    List<User> userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "PqR", 0, 30, false);
    assertThat(userList.get(1).getName()).isEqualTo("pqr");

    userList = userServiceImpl.listUsers(pageRequest, "ACCOUNT_ID", "fgh", 0, 30, false);
    assertThat(userList.size()).isEqualTo(0);
  }
}