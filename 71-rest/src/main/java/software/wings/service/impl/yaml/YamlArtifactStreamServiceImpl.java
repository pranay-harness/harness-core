package software.wings.service.impl.yaml;

import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import software.wings.beans.Base;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.yaml.YamlArtifactStreamService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;

/**
 * @author rktummala on 10/09/17
 *
 */
@Singleton
public class YamlArtifactStreamServiceImpl implements YamlArtifactStreamService {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private YamlGitService yamlGitSyncService;
  @Inject private YamlService yamlSyncService;
  @Inject private AppService appService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private SettingsService settingsService;

  @Override
  public RestResponse<YamlPayload> getArtifactStreamYaml(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);

    if (artifactStream != null) {
      if (!appId.equals(GLOBAL_APP_ID)) {
        return YamlHelper.getYamlRestResponse(yamlGitSyncService, artifactStreamId,
            appService.getAccountIdByAppId(appId), getArtifactStreamYamlObject(artifactStream),
            artifactStream.getName() + YAML_EXTENSION);
      } else {
        return YamlHelper.getYamlRestResponse(
            getArtifactStreamYamlObject(artifactStream), artifactStream.getName() + YAML_EXTENSION);
      }
    }

    throw new WingsException(ErrorCode.GENERAL_YAML_ERROR, USER)
        .addParam("message", "ArtifactStream with this Id: '" + artifactStreamId + "' was not found!");
  }

  @Override
  public ArtifactStream.Yaml getArtifactStreamYamlObject(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (artifactStream != null) {
      return getArtifactStreamYamlObject(artifactStream);
    }
    return null;
  }

  @Override
  public ArtifactStream.Yaml getArtifactStreamYamlObject(String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (artifactStream != null) {
      return getArtifactStreamYamlObject(artifactStream);
    }
    return null;
  }

  @Override
  public String getArtifactStreamYamlString(ArtifactStream artifactStream) {
    return YamlHelper.toYamlString(getArtifactStreamYamlObject(artifactStream));
  }

  @Override
  public String getArtifactStreamYamlString(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (artifactStream != null) {
      return getArtifactStreamYamlString(artifactStream);
    }
    return null;
  }

  @Override
  public String getArtifactStreamYamlString(String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    if (artifactStream != null) {
      return getArtifactStreamYamlString(artifactStream);
    }
    return null;
  }

  private ArtifactStream.Yaml getArtifactStreamYamlObject(ArtifactStream artifactStream) {
    return (ArtifactStream.Yaml) yamlHandlerFactory
        .getYamlHandler(YamlType.ARTIFACT_STREAM, artifactStream.getArtifactStreamType())
        .toYaml(artifactStream, artifactStream.getAppId());
  }

  @Override
  public RestResponse<Base> updateArtifactStream(
      String appId, String artifactStreamId, YamlPayload yamlPayload, boolean deleteEnabled) {
    String accountId = null;
    if (!appId.equals(GLOBAL_APP_ID)) {
      accountId = appService.getAccountIdByAppId(appId);
    } else {
      ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
      if (artifactStream != null) {
        SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
        if (settingAttribute != null) {
          accountId = settingAttribute.getAccountId();
        }
      }
    }
    return yamlSyncService.update(yamlPayload, accountId);
  }
}
