package io.harness.event.handler.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.handler.marketo.MarketoRestClient;
import io.harness.event.model.marketo.Error;
import io.harness.event.model.marketo.Lead;
import io.harness.event.model.marketo.Lead.Input;
import io.harness.event.model.marketo.Lead.Input.InputBuilder;
import io.harness.event.model.marketo.LoginResponse;
import io.harness.event.model.marketo.Response;
import io.harness.event.model.marketo.Response.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.common.Constants;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;
import software.wings.utils.Validator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author rktummala on 11/20/18
 */
@Singleton
public class MarketoHelper {
  private static final Logger logger = LoggerFactory.getLogger(MarketoHelper.class);

  @Inject private UserService userService;
  @Inject private AccountService accountService;

  public long registerLead(String accountId, User user, String accessToken, Retrofit retrofit) throws IOException {
    Validator.notNullCheck("User is null while registering the lead", user);
    Account account = accountService.get(accountId);
    Validator.notNullCheck("Account is null for accountId: " + accountId, account);
    LicenseInfo licenseInfo = account.getLicenseInfo();
    Validator.notNullCheck("LicenseInfo is null for accountId: " + accountId, licenseInfo);

    InputBuilder input = Input.builder()
                             .email(user.getEmail())
                             .firstName(getFirstName(user.getName(), user.getEmail()))
                             .lastName(getLastName(user.getName(), user.getEmail()))
                             .company(account.getCompanyName())
                             .Harness_Account_ID__c_lead(accountId)
                             .Free_Trial_Status__c(licenseInfo.getAccountStatus())
                             .Days_Left_in_Trial__c(getDaysLeft(licenseInfo.getExpiryTime()));

    Lead lead =
        Lead.builder().action("createOrUpdate").lookupField("email").input(Arrays.asList(input.build())).build();

    long marketoLeadId = 0L;
    retrofit2.Response<Response> response =
        retrofit.create(MarketoRestClient.class).createLead(accessToken, lead).execute();
    if (!response.isSuccessful()) {
      logger.error(
          "Error while creating lead in marketo. Error code is {}, message is {}", response.code(), response.message());
      return marketoLeadId;
    }

    Response leadResponse = response.body();
    if (!leadResponse.isSuccess()) {
      logger.error("Marketo http response reported failure while creating lead. {}", getErrorMsg(leadResponse));
      return marketoLeadId;
    }

    List<Result> results = leadResponse.getResult();
    if (isEmpty(results)) {
      logger.error("Marketo http response reported empty result while creating lead");
      return marketoLeadId;
    }

    Result result = results.get(0);

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

    marketoLeadId = result.getId();
    if (marketoLeadId > 0L) {
      user.setMarketoLeadId(marketoLeadId);
      userService.update(user);
    }
    return marketoLeadId;
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

    return "" + delta / Constants.DAY;
  }

  public String getErrorMsg(Response response) {
    List<Error> errors = response.getErrors();
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
