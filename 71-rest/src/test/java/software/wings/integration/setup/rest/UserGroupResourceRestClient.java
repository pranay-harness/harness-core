package software.wings.integration.setup.rest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsIntegrationTestConstants.API_BASE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.RestResponse;
import software.wings.beans.security.UserGroup;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * @author rktummala on 09/25/18
 */
@Singleton
public class UserGroupResourceRestClient {
  private static final Logger logger = LoggerFactory.getLogger(UserGroupResourceRestClient.class);

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
