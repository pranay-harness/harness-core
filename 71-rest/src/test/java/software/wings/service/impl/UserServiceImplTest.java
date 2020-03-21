package software.wings.service.impl;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static software.wings.beans.UserInvite.UserInviteBuilder.anUserInvite;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
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
import software.wings.beans.UserInvite;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SignupService;

public class UserServiceImplTest extends WingsBaseTest {
  @Mock AccountService accountService;
  @Inject @InjectMocks UserServiceImpl userServiceImpl;
  @Mock SignupService signupService;
  @Mock EventPublishHelper eventPublishHelper;

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
}