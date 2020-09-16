package io.harness.event.handler.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.handler.marketo.MarketoRestClient;
import io.harness.event.model.marketo.Error;
import io.harness.event.model.marketo.GetLeadResponse;
import io.harness.event.model.marketo.GetLeadResponse.Result;
import io.harness.event.model.marketo.Lead;
import io.harness.event.model.marketo.Lead.LeadBuilder;
import io.harness.event.model.marketo.LeadRequestWithEmail;
import io.harness.event.model.marketo.LeadRequestWithId;
import io.harness.event.model.marketo.LoginResponse;
import io.harness.event.model.marketo.Response;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Retrofit;
import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;
import software.wings.beans.UserInvite;
import software.wings.beans.utm.UtmInfo;
import software.wings.service.intfc.SignupService;
import software.wings.service.intfc.UserService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * @author rktummala on 11/20/18
 */
@OwnedBy(PL)
@Singleton
@Slf4j
public class MarketoHelper {
  @Inject private UserService userService;
  @Inject private SignupService signupService;
  @Inject private Utils utils;

  public long createOrUpdateLead(Account account, String userName, String email, String accessToken,
      String oauthProvider, Retrofit retrofit, UtmInfo utmInfo) throws IOException, URISyntaxException {
    notNullCheck("Email is null while registering the lead", email);

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
            utils.getErrorMsg(existingLeadResponse.getErrors()));
      }
    } else {
      logger.error("Marketo http response reported null while looking up lead");
    }

    UserInvite userInvite;
    if (account != null) {
      userInvite = userService.getUserInviteByEmailAndAccount(email, account.getUuid());
    } else {
      userInvite = signupService.getUserInviteByEmail(email);
    }

    String userInviteUrl = utils.getUserInviteUrl(userInvite, account);

    retrofit2.Response<Response> createOrUpdateResponse;
    if (existingLeadId > 0) {
      try {
        createOrUpdateResponse = updateLead(retrofit, existingLeadId, email, userName, account, userInviteUrl,
            accessToken, oauthProvider, userInvite.getUtmInfo(), userInvite);
      } catch (Exception ex) {
        // Retrying with email if id was invalid, we have cases where the lead was manually deleted from marketo
        createOrUpdateResponse = createLead(
            retrofit, email, userName, account, userInviteUrl, accessToken, oauthProvider, utmInfo, userInvite);
      }
    } else {
      createOrUpdateResponse = createLead(
          retrofit, email, userName, account, userInviteUrl, accessToken, oauthProvider, utmInfo, userInvite);
    }

    return processLeadResponse(createOrUpdateResponse);
  }

  private retrofit2.Response<Response> createLead(Retrofit retrofit, String email, String userName, Account account,
      String userInviteUrl, String accessToken, String oauthProvider, UtmInfo utmInfo, UserInvite userInvite)
      throws IOException {
    logger.info("Creating lead with email: {} in marketo with oauth provider {}", email, oauthProvider);

    Lead lead = buildLead(email, userName, account, userInviteUrl, oauthProvider, utmInfo, userInvite);

    LeadRequestWithEmail leadRequestWithEmail =
        LeadRequestWithEmail.builder().action("createOrUpdate").lookupField("email").input(Arrays.asList(lead)).build();

    retrofit2.Response<Response> response =
        retrofit.create(MarketoRestClient.class).createLead(accessToken, leadRequestWithEmail).execute();
    logger.info("Created lead with email: {} in marketo with oauth provider {}", email, oauthProvider);
    return response;
  }

  private retrofit2.Response<Response> updateLead(Retrofit retrofit, int existingLeadId, String email, String userName,
      Account account, String userInviteUrl, String accessToken, String oauthProvider, UtmInfo utmInfo,
      UserInvite userInvite) throws IOException {
    logger.info("Updating lead {} to marketo", existingLeadId);

    Lead lead = buildLead(email, userName, account, userInviteUrl, oauthProvider, utmInfo, userInvite);

    LeadRequestWithId leadRequestWithId =
        LeadRequestWithId.builder().action("createOrUpdate").lookupField("id").input(Arrays.asList(lead)).build();
    retrofit2.Response<Response> response =
        retrofit.create(MarketoRestClient.class).updateLead(accessToken, leadRequestWithId).execute();
    logger.info("Updated lead {} to marketo", existingLeadId);
    return response;
  }

  private Lead buildLead(String email, String userName, Account account, String userInviteUrl, String oauthProvider,
      UtmInfo utmInfo, UserInvite userInvite) {
    LeadBuilder leadBuilder = Lead.builder();
    leadBuilder.email(email)
        .firstName(utils.getFirstName(userName, email))
        .lastName(utils.getLastName(userName, email));

    if (account != null) {
      if (isNotEmpty(oauthProvider)) {
        // In case of sso, make the company name null because it's populated by harness using email.
        leadBuilder.company(null).Harness_Account_ID__c_lead(account.getUuid());
      } else {
        leadBuilder.company(account.getCompanyName()).Harness_Account_ID__c_lead(account.getUuid());
      }
      LicenseInfo licenseInfo = account.getLicenseInfo();
      if (licenseInfo != null) {
        leadBuilder.Free_Trial_Status__c(licenseInfo.getAccountStatus())
            .Days_Left_in_Trial__c(utils.getDaysLeft(licenseInfo.getExpiryTime()));
      }
    } else if (userInvite != null) {
      leadBuilder.company(userInvite.getCompanyName());
    }

    if (isNotEmpty(userInviteUrl)) {
      leadBuilder.Freemium_Invite_URL__c(userInviteUrl);
    }

    if (isNotEmpty(oauthProvider)) {
      leadBuilder.SSO_Freemium_Type__c(oauthProvider);
    }

    if (utmInfo != null) {
      leadBuilder.UTM_Source__c(utmInfo.getUtmSource());
      leadBuilder.UTM_Content__c(utmInfo.getUtmContent());
      leadBuilder.UTM_Medium__c(utmInfo.getUtmMedium());
      leadBuilder.UTM_Term__c(utmInfo.getUtmTerm());
      leadBuilder.UTM__c(utmInfo.getUtmCampaign());
    }

    if (userInvite != null && isNotEmpty(userInvite.getFreemiumProducts())) {
      leadBuilder.freemiumProducts(userInvite.getFreemiumProducts());
    }

    if (userInvite != null && userInvite.getFreemiumAssistedOption() != null) {
      leadBuilder.freemiumAssistedOption(userInvite.getFreemiumAssistedOption());
    }
    return leadBuilder.build();
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
      logger.error("Marketo http response reported failure while creating lead. {}",
          utils.getErrorMsg(leadResponse.getErrors()));
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
