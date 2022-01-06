/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration.setup.rest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.shell.AccessType.KEY;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.CGConstants.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsIntegrationTestConstants.API_BASE;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_DC_KEY;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_DC_NAME;
import static software.wings.utils.WingsIntegrationTestConstants.SEED_SSH_KEY;
import static software.wings.utils.WingsIntegrationTestConstants.defaultSshKey;
import static software.wings.utils.WingsIntegrationTestConstants.defaultSshUserName;
import static software.wings.utils.WingsIntegrationTestConstants.sshKeyName;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;

import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.integration.UserResourceRestClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

@Singleton
public class SettingsResourceRestClient {
  @Inject private UserResourceRestClient userResourceRestClient;

  private ConcurrentHashMap<String, SettingAttribute> cachedEntity = new ConcurrentHashMap<>();

  public SettingAttribute getSettingAttribute(Client client, String userToken, String accountId, String settingId) {
    WebTarget target = client.target(API_BASE + "/settings/" + settingId + "?accountId=" + accountId);

    RestResponse<SettingAttribute> response = userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
                                                  .get(new GenericType<RestResponse<SettingAttribute>>() {});
    assertThat(response.getResource()).isNotNull().isInstanceOf(SettingAttribute.class);
    return response.getResource();
  }

  public SettingAttribute getSettingAttributeByName(Client client, String userToken, String accountId, String name) {
    WebTarget target = client.target(API_BASE + "/settings?accountId=" + accountId);

    RestResponse<PageResponse<SettingAttribute>> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<PageResponse<SettingAttribute>>>() {});
    return isEmpty(response.getResource()) ? null : response.getResource().get(0);
  }

  public SettingAttribute seedDataCenter(Client client) {
    return cachedEntity.computeIfAbsent(SEED_DC_KEY, key -> fetchOrCreatreDataCenter(client));
  }

  public SettingAttribute seedSshKey(Client client) {
    return cachedEntity.computeIfAbsent(SEED_SSH_KEY, key -> fetchOrCreatreSshKey(client));
  }

  private SettingAttribute fetchOrCreatreDataCenter(Client client) {
    Account seedAccount = userResourceRestClient.getSeedAccount(client);
    SettingAttribute seedDc = getSettingAttributeByName(
        client, userResourceRestClient.getUserToken(client), seedAccount.getUuid(), SEED_DC_NAME);
    if (seedDc == null) {
      seedDc =
          createDataCenter(client, userResourceRestClient.getUserToken(client), seedAccount.getUuid(), SEED_DC_NAME);
    }
    return seedDc;
  }

  private SettingAttribute fetchOrCreatreSshKey(Client client) {
    Account seedAccount = userResourceRestClient.getSeedAccount(client);
    SettingAttribute sshKey = getSettingAttributeByName(
        client, userResourceRestClient.getUserToken(client), seedAccount.getUuid(), sshKeyName);
    if (sshKey == null) {
      SettingAttribute settingAttribute = aSettingAttribute()
                                              .withAccountId(seedAccount.getUuid())
                                              .withAppId(GLOBAL_APP_ID)
                                              .withEnvId(GLOBAL_ENV_ID)
                                              .withName(sshKeyName)
                                              .withValue(aHostConnectionAttributes()
                                                             .withConnectionType(SSH)
                                                             .withAccessType(KEY)
                                                             .withAccountId(seedAccount.getUuid())
                                                             .withUserName(defaultSshUserName)
                                                             .withKey(defaultSshKey.toCharArray())
                                                             .build())
                                              .build();

      sshKey = createSettingAttribute(
          client, userResourceRestClient.getUserToken(client), seedAccount.getUuid(), settingAttribute);
    }
    return sshKey;
  }

  public SettingAttribute createDataCenter(Client client, String userToken, String accountId, String dataCenterName) {
    SettingAttribute settingAttribute = aSettingAttribute().withValue(aPhysicalDataCenterConfig().build()).build();
    return createSettingAttribute(client, userToken, accountId, settingAttribute);
  }

  private SettingAttribute createSettingAttribute(
      Client client, String userToken, String accountId, SettingAttribute settingAttribute) {
    WebTarget target = client.target(API_BASE + "/settings/?accountId=" + accountId);

    RestResponse<SettingAttribute> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .post(entity(settingAttribute, APPLICATION_JSON), new GenericType<RestResponse<SettingAttribute>>() {});
    assertThat(response.getResource()).isNotNull().isInstanceOf(SettingAttribute.class);
    String settingId = response.getResource().getUuid();
    assertThat(settingId).isNotEmpty();
    SettingAttribute ret = getSettingAttribute(client, userToken, accountId, settingId);
    assertThat(ret)
        .isNotNull()
        .hasFieldOrPropertyWithValue("uuid", settingId)
        .hasFieldOrPropertyWithValue("name", settingAttribute.getName())
        .hasFieldOrPropertyWithValue("value", settingAttribute.getValue());
    return settingAttribute;
  }
}
