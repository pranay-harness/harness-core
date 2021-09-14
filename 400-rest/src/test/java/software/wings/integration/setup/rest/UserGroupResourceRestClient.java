/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.integration.setup.rest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.utils.WingsIntegrationTestConstants.API_BASE;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;

import software.wings.beans.security.UserGroup;

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
public class UserGroupResourceRestClient {
  @Inject private software.wings.integration.UserResourceRestClient userResourceRestClient;

  public UserGroup getUserGroupByName(Client client, String userToken, String accountId, String userGroupName)
      throws UnsupportedEncodingException {
    WebTarget target = client.target(
        API_BASE + "/userGroups?accountId=" + accountId + "&name=" + URLEncoder.encode(userGroupName, "UTF-8"));
    RestResponse<PageResponse<UserGroup>> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<PageResponse<UserGroup>>>() {});
    return isEmpty(response.getResource()) ? null : response.getResource().get(0);
  }

  public UserGroup createUserGroup(Client client, String userToken, String accountId, UserGroup userGroup) {
    WebTarget target = client.target(API_BASE + "/userGroups?accountId=" + accountId);
    RestResponse<UserGroup> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .post(entity(userGroup, APPLICATION_JSON), new GenericType<RestResponse<UserGroup>>() {});
    assertThat(response.getResource()).isInstanceOf(UserGroup.class);
    return response.getResource();
  }
}
