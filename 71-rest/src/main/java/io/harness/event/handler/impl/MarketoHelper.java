package io.harness.event.handler.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.handler.marketo.MarketoRestClient;
import io.harness.event.model.marketo.Error;
import io.harness.event.model.marketo.GetLeadResponse;
import io.harness.event.model.marketo.GetLeadResponse.Result;
import io.harness.event.model.marketo.LeadRequestWithEmail;
import io.harness.event.model.marketo.LeadRequestWithId;
import io.harness.event.model.marketo.LeadRequestWithId.Lead;
import io.harness.event.model.marketo.LeadRequestWithId.Lead.LeadBuilder;
import io.harness.event.model.marketo.LoginResponse;
import io.harness.event.model.marketo.Response;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Retrofit;
import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;
import software.wings.service.intfc.UserService;
import software.wings.utils.Validator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * @author rktummala on 11/20/18
 */
@Singleton
@Slf4j
public class MarketoHelper {
  @Inject private UserService userService;

  public long createOrUpdateLead(Account account, String userName, String email, String accessToken,
      String oauthProvider, Retrofit retrofit) throws IOException, URISyntaxException {
    Validator.notNullCheck("Email is null while registering the lead", email);

    int existingLeadId = 0;
    retrofit2.Response<GetLeadResponse> response =
        retrofit.create(MarketoRestClient.class).getLead(accessToken, "email", email).execute();
    GetLeadResponse existingLeadResponse = response.body();

    if (existingLeadResponse != null) {
      if (existingLeadResponse.isSuccess()) {
        List<Result> resultList = existingLeadResponse.getResult();
        if (isNotEmpty(resultList)) {
          Result result = resultList.get(0);
          existingLeadId = result.getId();
        }
      } else {
        logger.error("Marketo http response reported failure while looking up lead. {}",
            getErrorMsg(existingLeadResponse.getErrors()));
      }
    } else {
      logger.error("Marketo http response reported null while looking up lead");
    }

    String userInviteUrl = getUserInviteUrl(email, account);
    retrofit2.Response<Response> createOrUpdateResponse;
    if (existingLeadId > 0) {
      try {
        createOrUpdateResponse =
            updateLead(retrofit, existingLeadId, email, userName, account, userInviteUrl, accessToken, oauthProvider);
      } catch (Exception ex) {
        // Retrying with email if id was invalid, we have cases where the lead was manually deleted from marketo
        createOrUpdateResponse =
            createLead(retrofit, email, userName, account, userInviteUrl, accessToken, oauthProvider);
      }
    } else {
      createOrUpdateResponse =
          createLead(retrofit, email, userName, account, userInviteUrl, accessToken, oauthProvider);
    }

    return processLeadResponse(createOrUpdateResponse);
  }

  private retrofit2.Response<Response> createLead(Retrofit retrofit, String email, String userName, Account account,
      String userInviteUrl, String accessToken, String oauthProvider) throws IOException {
    logger.info("Creating lead with email: {} in marketo with oauth provider {}", email, oauthProvider);
    LeadRequestWithEmail.Lead.LeadBuilder leadBuilderWithEmail = LeadRequestWithEmail.Lead.builder();
    leadBuilderWithEmail.email(email).firstName(getFirstName(userName, email)).lastName(getLastName(userName, email));

    if (account != null) {
      leadBuilderWithEmail.company(account.getCompanyName()).Harness_Account_ID__c_lead(account.getUuid());
      LicenseInfo licenseInfo = account.getLicenseInfo();
      if (licenseInfo != null) {
        leadBuilderWithEmail.Free_Trial_Status__c(licenseInfo.getAccountStatus())
            .Days_Left_in_Trial__c(getDaysLeft(licenseInfo.getExpiryTime()));
      }
    }

    if (isNotEmpty(userInviteUrl)) {
      leadBuilderWithEmail.Freemium_Invite_URL__c(userInviteUrl);
    }

    if (isNotEmpty(oauthProvider)) {
      leadBuilderWithEmail.SSO_Freemium_Type__c(oauthProvider);
    }

    LeadRequestWithEmail leadRequestWithEmail = LeadRequestWithEmail.builder()
                                                    .action("createOrUpdate")
                                                    .lookupField("email")
                                                    .input(Arrays.asList(leadBuilderWithEmail.build()))
                                                    .build();

    retrofit2.Response<Response> response =
        retrofit.create(MarketoRestClient.class).createLead(accessToken, leadRequestWithEmail).execute();
    logger.info("Created lead with email: {} in marketo with oauth provider {}", email, oauthProvider);
    return response;
  }

  private retrofit2.Response<Response> updateLead(Retrofit retrofit, int existingLeadId, String email, String userName,
      Account account, String userInviteUrl, String accessToken, String oauthProvider) throws IOException {
    logger.info("Updating lead {} to marketo", existingLeadId);

    LeadBuilder leadBuilderWithId = Lead.builder();
    leadBuilderWithId.id(existingLeadId);
    leadBuilderWithId.email(email).firstName(getFirstName(userName, email)).lastName(getLastName(userName, email));

    if (account != null) {
      leadBuilderWithId.company(account.getCompanyName()).Harness_Account_ID__c_lead(account.getUuid());

      LicenseInfo licenseInfo = account.getLicenseInfo();
      if (licenseInfo != null) {
        leadBuilderWithId.Free_Trial_Status__c(licenseInfo.getAccountStatus())
            .Days_Left_in_Trial__c(getDaysLeft(licenseInfo.getExpiryTime()));
      }
    }

    if (isNotEmpty(userInviteUrl)) {
      leadBuilderWithId.Freemium_Invite_URL__c(userInviteUrl);
    }

    if (isNotEmpty(oauthProvider)) {
      leadBuilderWithId.SSO_Freemium_Type__c(oauthProvider);
    }

    LeadRequestWithId leadRequestWithId = LeadRequestWithId.builder()
                                              .action("createOrUpdate")
                                              .lookupField("id")
                                              .input(Arrays.asList(leadBuilderWithId.build()))
                                              .build();
    retrofit2.Response<Response> response =
        retrofit.create(MarketoRestClient.class).updateLead(accessToken, leadRequestWithId).execute();
    logger.info("Updated lead {} to marketo", existingLeadId);
    return response;
  }

  private long processLeadResponse(retrofit2.Response<Response> response) {
    long marketoLeadId = 0L;
    if (!response.isSuccessful()) {
      logger.error(
          "Error while creating lead in marketo. Error code is {}, message is {}", response.code(), response.message());
      return marketoLeadId;
    }

    Response leadResponse = response.body();
    if (!leadResponse.isSuccess()) {
      logger.error(
          "Marketo http response reported failure while creating lead. {}", getErrorMsg(leadResponse.getErrors()));
      return marketoLeadId;
    }

    List<Response.Result> results = leadResponse.getResult();
    if (isEmpty(results)) {
      logger.error("Marketo http response reported empty result while creating lead");
      return marketoLeadId;
    }

    Response.Result result = results.get(0);

    String status = result.getStatus();
    if (!("updated".equalsIgnoreCase(status) || "created".equalsIgnoreCase(status))) {
      List<Error> reasons = result.getReasons();
      if (isEmpty(reasons)) {
        logger.error("Marketo reported status {} for lead creation. No error reported in response", status);
      } else {
        Error error = reasons.get(0);
        logger.error("Marketo reported status {} for lead creation. Error code is: {}, message is: {}", status,
            error.getCode(), error.getMessage());
      }
    }

    logger.info("Marketo returned lead id {}", result.getId());
    return result.getId();
  }

  private String getUserInviteUrl(String email, Account account) throws URISyntaxException {
    if (account != null) {
      return userService.getUserInviteUrl(email, account);
    } else {
      return userService.getUserInviteUrl(email);
    }
  }

  private String getFirstName(String name, String email) {
    if (isEmpty(name) || name.equals(email)) {
      return null;
    }

    String[] words = name.split(" ");
    int numberOfWords = words.length;
    if (numberOfWords == 1 || numberOfWords == 2) {
      return words[0];
    } else {
      return name.substring(0, name.lastIndexOf(words[numberOfWords - 1]) - 1);
    }
  }

  private String getLastName(String name, String email) {
    if (isEmpty(name) || name.equals(email)) {
      return null;
    }

    String[] words = name.split(" ");
    int numberOfWords = words.length;
    if (numberOfWords == 1) {
      return words[0];
    } else {
      return words[numberOfWords - 1];
    }
  }

  private String getDaysLeft(long expiryTime) {
    long delta = expiryTime - System.currentTimeMillis();
    if (delta <= 0) {
      return "0";
    }

    return "" + delta / Duration.ofDays(1).toMillis();
  }

  public String getErrorMsg(List<Error> errors) {
    if (isEmpty(errors)) {
      return "No error msg reported in response";
    }

    Error error = errors.get(0);
    StringBuilder builder = new StringBuilder(32);
    return builder.append("error code is: ")
        .append(error.getCode())
        .append(" , error msg is: ")
        .append(error.getMessage())
        .toString();
  }

  public String getAccessToken(String clientId, String clientSecret, Retrofit retrofit) throws IOException {
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
