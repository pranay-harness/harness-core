package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.HarnessException;

import software.wings.beans.DockerConfig;
import software.wings.beans.DockerConfigYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * @author rktummala on 11/19/17
 */
@OwnedBy(CDC)
@Singleton
public class DockerRegistryConfigYamlHandler extends ArtifactServerYamlHandler<DockerConfigYaml, DockerConfig> {
  @Override
  public DockerConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    DockerConfig dockerConfig = (DockerConfig) settingAttribute.getValue();
    DockerConfigYaml yaml;
    List<String> delegateSelectors = getDelegateSelectors(dockerConfig.getDelegateSelectors());
    if (dockerConfig.hasCredentials()) {
      yaml = DockerConfigYaml.builder()
                 .harnessApiVersion(getHarnessApiVersion())
                 .type(dockerConfig.getType())
                 .url(dockerConfig.getDockerRegistryUrl())
                 .username(dockerConfig.getUsername())
                 .delegateSelectors(delegateSelectors)
                 .password(getEncryptedYamlRef(dockerConfig.getAccountId(), dockerConfig.getEncryptedPassword()))
                 .build();
    } else {
      yaml = DockerConfigYaml.builder()
                 .harnessApiVersion(getHarnessApiVersion())
                 .type(dockerConfig.getType())
                 .url(dockerConfig.getDockerRegistryUrl())
                 .delegateSelectors(delegateSelectors)
                 .build();
    }

    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<DockerConfigYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    DockerConfigYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    List<String> delegateSelectors = getDelegateSelectors(yaml.getDelegateSelectors());
    DockerConfig config = DockerConfig.builder()
                              .accountId(accountId)
                              .dockerRegistryUrl(yaml.getUrl())
                              .encryptedPassword(yaml.getPassword())
                              .username(yaml.getUsername())
                              .delegateSelectors(delegateSelectors)
                              .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  private List<String> getDelegateSelectors(List<String> delegateSelectors) {
    return isNotEmpty(delegateSelectors)
        ? delegateSelectors.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList())
        : new ArrayList<>();
  }

  @Override
  public Class getYamlClass() {
    return DockerConfigYaml.class;
  }
}
