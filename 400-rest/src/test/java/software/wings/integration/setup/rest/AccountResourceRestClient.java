/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration.setup.rest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.utils.WingsIntegrationTestConstants.API_BASE;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;

import software.wings.beans.Account;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;

/**
 * @author rktummala on 09/25/18
 */
@Singleton
@Slf4j
public class AccountResourceRestClient {
  @Inject private software.wings.integration.UserResourceRestClient userResourceRestClient;

  public Account getAccountByName(Client client, String userToken, String accountId, String accountName)
      throws UnsupportedEncodingException {
    WebTarget target = client.target(
        API_BASE + "/users/accounts?accountId=" + accountId + "&name=" + URLEncoder.encode(accountName, "UTF-8"));
    RestResponse<PageResponse<Account>> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<PageResponse<Account>>>() {});
    return isEmpty(response.getResource()) ? null : response.getResource().get(0);
  }

  public Account createAccount(Client client, String userToken, Account account) {
    WebTarget target = client.target(API_BASE + "/users/account");
    RestResponse<Account> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .post(entity(account, APPLICATION_JSON), new GenericType<RestResponse<Account>>() {});
    assertThat(response.getResource()).isInstanceOf(Account.class);
    return response.getResource();
  }
}
