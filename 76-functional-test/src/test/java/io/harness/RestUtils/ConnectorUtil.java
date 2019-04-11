package io.harness.RestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;

import com.google.inject.Singleton;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.restassured.path.json.JsonPath;
import software.wings.beans.SettingAttribute;
import software.wings.beans.config.ArtifactoryConfig;

@Singleton
public class ConnectorUtil {
  public static String createArtifactoryConnector(String bearerToken, String connectorName, String accountId) {
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName(connectorName)
            .withCategory(CONNECTOR)
            .withAccountId(accountId)
            .withValue(
                ArtifactoryConfig.builder()
                    .accountId(accountId)
                    .artifactoryUrl("https://harness.jfrog.io/harness")
                    .username("admin")
                    .password(new ScmSecret().decryptToCharArray(new SecretName("artifactory_connector_password")))
                    .build())
            .build();

    JsonPath setAttrResponse = SettingsUtil.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    return setAttrResponse.getString("resource.uuid").trim();
  }
}