package io.harness.event.handler.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.event.model.EventType.COMMUNITY_TO_PAID;
import static io.harness.event.model.EventType.COMPLETE_USER_REGISTRATION;
import static io.harness.event.model.EventType.FIRST_DELEGATE_REGISTERED;
import static io.harness.event.model.EventType.FIRST_DEPLOYMENT_EXECUTED;
import static io.harness.event.model.EventType.FIRST_ROLLED_BACK_DEPLOYMENT;
import static io.harness.event.model.EventType.FIRST_VERIFIED_DEPLOYMENT;
import static io.harness.event.model.EventType.FIRST_WORKFLOW_CREATED;
import static io.harness.event.model.EventType.LICENSE_UPDATE;
import static io.harness.event.model.EventType.NEW_TRIAL_SIGNUP;
import static io.harness.event.model.EventType.SETUP_2FA;
import static io.harness.event.model.EventType.SETUP_CV_24X7;
import static io.harness.event.model.EventType.SETUP_IP_WHITELISTING;
import static io.harness.event.model.EventType.SETUP_RBAC;
import static io.harness.event.model.EventType.SETUP_SSO;
import static io.harness.event.model.EventType.TRIAL_TO_COMMUNITY;
import static io.harness.event.model.EventType.TRIAL_TO_PAID;
import static io.harness.event.model.EventType.USER_INVITED_FROM_EXISTING_ACCOUNT;
import static io.harness.exception.WingsException.USER;
import static software.wings.common.Constants.ACCOUNT_ID;
import static software.wings.common.Constants.EMAIL_ID;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.handler.EventHandler;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.handler.marketo.MarketoRestClient;
import io.harness.event.listener.EventListener;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.event.model.marketo.Campaign;
import io.harness.event.model.marketo.Id;
import io.harness.event.model.marketo.LoginResponse;
import io.harness.event.model.marketo.Response;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;
import software.wings.utils.Validator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rktummala on 11/20/18
 */
@Singleton
@Slf4j
public class MarketoHandler implements EventHandler {
  @Inject private UserService userService;
  @Inject private AccountService accountService;
  @Inject private MarketoHelper marketoHelper;
  @Inject private PersistentLocker persistentLocker;

  private MarketoConfig marketoConfig;

  // key = event type, value = campaign id
  private Map<EventType, Long> campaignRegistry;
  private Retrofit retrofit;

  @Inject
  public MarketoHandler(MarketoConfig marketoConfig, EventListener eventListener) {
    this.marketoConfig = marketoConfig;
    if (isMarketoEnabled()) {
      campaignRegistry = Maps.newHashMap();
      registerCampaignRegistry();
      registerEventHandlers(eventListener);
      retrofit = new Retrofit.Builder()
                     .baseUrl(marketoConfig.getUrl())
                     .addConverterFactory(JacksonConverterFactory.create())
                     .client(Http.getUnsafeOkHttpClient(marketoConfig.getUrl()))
                     .build();
    }
  }

  public boolean isMarketoEnabled() {
    return marketoConfig.isEnabled();
  }

  private void registerCampaignRegistry() {
    //    1898 user invited from existing account USER_INVITED_FROM_EXISTING_ACCOUNT
    //    1800 completed account signup           COMPLETE_USER_REGISTRATION
    //    1802 created first workflow             FIRST_WORKFLOW_CREATED
    //    1803 first deployment                   FIRST_DEPLOYMENT_EXECUTED
    //    1804 verified deployment                FIRST_VERIFIED_DEPLOYMENT
    //    1805 initiated rollback                 FIRST_ROLLED_BACK_DEPLOYMENT
    //    1806 CV 24X7                            SETUP_CV_24X7
    //    1807 2 FA                               SETUP_2FA
    //    1808 SSO setup                          SETUP_SSO
    //    1809 IP Whitelisting                    SETUP_IP_WHITELISTING
    //    1810 RBAC                               SETUP_RBAC

    //    1801 installed delegate                 FIRST_DELEGATE_REGISTERED
    //    1811 Trial to Paid                      TRIAL_TO_PAID
    //    1812 Trial to Community                 TRIAL_TO_COMMUNITY
    //    1813 Community to Paid                  COMMUNITY_TO_PAID

    //    2015 User invited for trial signup

    campaignRegistry.put(USER_INVITED_FROM_EXISTING_ACCOUNT, 1898L);
    campaignRegistry.put(COMPLETE_USER_REGISTRATION, 1800L);
    campaignRegistry.put(FIRST_DELEGATE_REGISTERED, 1801L);
    campaignRegistry.put(FIRST_WORKFLOW_CREATED, 1802L);
    campaignRegistry.put(FIRST_DEPLOYMENT_EXECUTED, 1803L);
    campaignRegistry.put(FIRST_VERIFIED_DEPLOYMENT, 1804L);
    campaignRegistry.put(FIRST_ROLLED_BACK_DEPLOYMENT, 1805L);
    campaignRegistry.put(SETUP_CV_24X7, 1806L);
    campaignRegistry.put(SETUP_2FA, 1807L);
    campaignRegistry.put(SETUP_SSO, 1808L);
    campaignRegistry.put(SETUP_IP_WHITELISTING, 1809L);
    campaignRegistry.put(SETUP_RBAC, 1810L);
    campaignRegistry.put(TRIAL_TO_PAID, 1811L);
    campaignRegistry.put(TRIAL_TO_COMMUNITY, 1812L);
    campaignRegistry.put(COMMUNITY_TO_PAID, 1813L);
    campaignRegistry.put(NEW_TRIAL_SIGNUP, 2015L);
  }

  private void registerEventHandlers(EventListener eventListener) {
    eventListener.registerEventHandler(this,
        Sets.newHashSet(USER_INVITED_FROM_EXISTING_ACCOUNT, COMPLETE_USER_REGISTRATION, FIRST_DELEGATE_REGISTERED,
            FIRST_WORKFLOW_CREATED, FIRST_DEPLOYMENT_EXECUTED, FIRST_VERIFIED_DEPLOYMENT, FIRST_ROLLED_BACK_DEPLOYMENT,
            SETUP_CV_24X7, SETUP_2FA, SETUP_SSO, SETUP_IP_WHITELISTING, SETUP_RBAC, TRIAL_TO_PAID, TRIAL_TO_COMMUNITY,
            COMMUNITY_TO_PAID, NEW_TRIAL_SIGNUP, LICENSE_UPDATE));
  }

  @Override
  public void handleEvent(Event event) {
    if (event == null) {
      logger.error("Event is null");
      return;
    }

    EventType eventType = event.getEventType();
    if (eventType == null) {
      logger.error("Event type is null");
      return;
    }

    try {
      EventData eventData = event.getEventData();
      if (eventData == null) {
        logger.error("Event data is null");
        return;
      }

      Map<String, String> properties = eventData.getProperties();

      if (isEmpty(properties)) {
        logger.error("Event data properties are null");
        return;
      }

      String accessToken = getAccessToken(marketoConfig.getClientId(), marketoConfig.getClientSecret());

      if (NEW_TRIAL_SIGNUP.equals(eventType)) {
        String email = properties.get(EMAIL_ID);
        if (isEmpty(email)) {
          logger.error("User email is empty");
          return;
        }

        long marketoLeadId = reportLead(email, accessToken, true);
        if (marketoLeadId > 0) {
          reportCampaignEvent(eventType, accessToken, Arrays.asList(Id.builder().id(marketoLeadId).build()));
        }
        return;
      }

      String accountId = properties.get(ACCOUNT_ID);
      if (isEmpty(accountId)) {
        logger.error("Account is empty");
        return;
      }

      Account account = accountService.get(accountId);
      Validator.notNullCheck("Account is null for accountId:" + accountId, account);

      switch (eventType) {
        case COMMUNITY_TO_PAID:
        case TRIAL_TO_COMMUNITY:
        case TRIAL_TO_PAID:
        case FIRST_DELEGATE_REGISTERED:
          reportToAllUsers(account, accessToken, eventType);
          return;
        case LICENSE_UPDATE:
          updateAllUsersInMarketo(account, accessToken);
          return;
        default:
          break;
      }

      String email = properties.get(EMAIL_ID);
      if (isEmpty(email)) {
        logger.error("User email is empty");
        return;
      }

      User user = userService.getUserByEmail(email);
      if (user == null) {
        logger.error("User not found for email {}", email);
        return;
      }

      List<Account> accounts = user.getAccounts();
      if (isEmpty(accounts)) {
        logger.info("User {} is not assigned to any accounts", email);
        return;
      } else {
        if (accounts.size() > 1) {
          // At this point, only harness users can be assigned to more than one account.
          // Marketo and Salesforce follow the model one user - one account.
          logger.info("User {} is associated with more than one account, skipping marketo publish", email);
          return;
        }
      }

      switch (eventType) {
        case USER_INVITED_FROM_EXISTING_ACCOUNT:
        case COMPLETE_USER_REGISTRATION:
          registerLeadAndReportCampaign(account, user, accessToken, event);
          break;
        default:
          reportCampaignEvent(account, eventType, accessToken, user);
          break;
      }

    } catch (Exception ex) {
      logger.error("Error while sending event to marketo for event {}", eventType, ex);
    }
  }

  private void reportToAllUsers(Account account, String accessToken, EventType eventType) throws IOException {
    List<User> usersOfAccount = userService.getUsersOfAccount(account.getUuid());
    if (isEmpty(usersOfAccount)) {
      return;
    }

    List<Id> leadIdList = usersOfAccount.stream()
                              .filter(user -> user.getMarketoLeadId() != 0L)
                              .map(user -> Id.builder().id(user.getMarketoLeadId()).build())
                              .collect(Collectors.toList());
    reportCampaignEvent(eventType, accessToken, leadIdList);
  }

  private void updateAllUsersInMarketo(Account account, String accessToken) {
    List<User> usersOfAccount = userService.getUsersOfAccount(account.getUuid());
    if (isEmpty(usersOfAccount)) {
      return;
    }

    usersOfAccount.stream().filter(user -> user.getMarketoLeadId() != 0L).forEach(user -> {
      try {
        reportLead(account, user, accessToken, false);
      } catch (IOException | URISyntaxException e) {
        logger.error("Error while updating license to all users in marketo", e);
      }
    });
  }

  private void registerLeadAndReportCampaign(Account account, User user, String accessToken, Event event)
      throws IOException, URISyntaxException {
    long marketoLeadId = user.getMarketoLeadId();
    if (marketoLeadId == 0L) {
      reportLead(account, user, accessToken, true);
    }
    // Getting the latest copy since we had a sleep of 10 seconds.
    user = userService.getUserFromCacheOrDB(user.getUuid());
    reportCampaignEvent(account, event.getEventType(), accessToken, user);
  }

  public long reportLead(Account account, User user, String accessToken, boolean wait)
      throws IOException, URISyntaxException {
    long marketoLeadId = marketoHelper.createOrUpdateLead(
        account, user.getName(), user.getEmail(), accessToken, user.getOauthProvider(), retrofit);
    if (marketoLeadId > 0) {
      if (marketoLeadId != user.getMarketoLeadId()) {
        updateUser(user, marketoLeadId);
      }

      if (wait) {
        // Sleeping for 10 secs as a work around for marketo issue.
        // Marketo can't process trigger campaign with a lead just created.
        try {
          Thread.sleep(10000);
        } catch (InterruptedException ex) {
          logger.warn("Exception while waiting 10 seconds for marketo to catchup");
        }
      }
    }
    return marketoLeadId;
  }

  public long reportLead(String email, String accessToken, boolean wait) throws IOException, URISyntaxException {
    long marketoLeadId = marketoHelper.createOrUpdateLead(null, null, email, accessToken, null, retrofit);

    // Sleeping for 10 secs as a work around for marketo issue.
    // Marketo can't process trigger campaign with a lead just created.
    if (marketoLeadId > 0 && wait) {
      try {
        Thread.sleep(10000);
      } catch (InterruptedException ex) {
        logger.warn("Exception while waiting 10 seconds for marketo to catchup");
      }
    }
    return marketoLeadId;
  }

  private User updateUser(User user, EventType eventType) {
    try (AcquiredLock lock =
             persistentLocker.waitToAcquireLock(user.getEmail(), Duration.ofMinutes(2), Duration.ofMinutes(4))) {
      User latestUser = userService.getUserFromCacheOrDB(user.getUuid());
      Set<String> reportedMarketoCampaigns = latestUser.getReportedMarketoCampaigns();
      if (reportedMarketoCampaigns == null) {
        reportedMarketoCampaigns = new HashSet<>();
      }
      reportedMarketoCampaigns.add(eventType.name());
      latestUser.setReportedMarketoCampaigns(reportedMarketoCampaigns);
      return userService.update(latestUser);
    }
  }

  private User updateUser(User user, long marketoLeadId) {
    if (marketoLeadId == 0L) {
      return user;
    }

    try (AcquiredLock lock =
             persistentLocker.waitToAcquireLock(user.getEmail(), Duration.ofMinutes(2), Duration.ofMinutes(4))) {
      User latestUser = userService.getUserFromCacheOrDB(user.getUuid());
      if (latestUser != null) {
        latestUser.setMarketoLeadId(marketoLeadId);
        return userService.update(latestUser);
      } else {
        throw new WingsException("Invalid user for the given id:" + user.getUuid(), USER);
      }
    }
  }

  public boolean reportCampaignEvent(EventType eventType, String accessToken, List<Id> leadIdList) throws IOException {
    logger.info("Reporting campaign for event {} with leads {}", eventType, leadIdList);
    if (isEmpty(leadIdList)) {
      logger.error("No Leads reported for event {}", eventType);
      return false;
    }

    long campaignId = campaignRegistry.get(eventType);
    if (campaignId == 0) {
      logger.warn("No Campaign found for event type {}", eventType);
      return false;
    }

    Campaign campaign = Campaign.builder().input(Campaign.Input.builder().leads(leadIdList).build()).build();

    retrofit2.Response<Response> response =
        retrofit.create(MarketoRestClient.class).triggerCampaign(campaignId, accessToken, campaign).execute();

    if (!response.isSuccessful()) {
      logger.error("Error while triggering campaign to Marketo for eventType {}. Response code is {}", eventType,
          response.code());
      return false;
    }

    Response campaignResponse = response.body();

    if (campaignResponse == null) {
      logger.error("Marketo trigger campaign response was null for eventType {}", eventType);
      return false;
    }

    if (!campaignResponse.isSuccess()) {
      logger.error("Marketo http response reported failure for eventType {}, {}", eventType,
          marketoHelper.getErrorMsg(campaignResponse.getErrors()));
      return false;
    }

    logger.info("Reported campaign for event {} with leads {}", eventType, leadIdList);
    return true;
  }

  private void reportCampaignEvent(Account account, EventType eventType, String accessToken, User user)
      throws IOException, URISyntaxException {
    String userId = user.getUuid();
    long marketoLeadId = user.getMarketoLeadId();
    if (marketoLeadId == 0L) {
      marketoLeadId = reportLead(account, user, accessToken, true);
      if (marketoLeadId == 0L) {
        logger.error("Invalid lead id reported for user {}", userId);
        return;
      }

      // Getting the latest copy since we had a sleep of 10 seconds.
      user = userService.getUserFromCacheOrDB(userId);
    }

    boolean reported =
        reportCampaignEvent(eventType, accessToken, Arrays.asList(Id.builder().id(marketoLeadId).build()));
    if (reported) {
      updateUser(user, eventType);
    }
  }

  public String getAccessToken(String clientId, String clientSecret) throws IOException {
    retrofit2.Response<LoginResponse> response =
        retrofit.create(MarketoRestClient.class).login(clientId, clientSecret).execute();

    if (!response.isSuccessful()) {
      throw new IOException(response.message());
    }

    LoginResponse loginResponse = response.body();

    if (loginResponse == null) {
      throw new IOException("Login response is null");
    }

    return loginResponse.getAccess_token();
  }
}
