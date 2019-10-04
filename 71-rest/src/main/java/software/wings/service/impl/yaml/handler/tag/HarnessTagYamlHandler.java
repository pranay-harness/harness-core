package software.wings.service.impl.yaml.handler.tag;

import com.google.inject.Inject;

import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTag.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.intfc.HarnessTagService;

import java.util.List;

public class HarnessTagYamlHandler extends BaseYamlHandler<Yaml, List<HarnessTag>> {
  @Inject private HarnessTagService harnessTagService;

  @Override
  public Yaml toYaml(List<HarnessTag> harnessTags, String appId) {
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .tag(harnessTagYamlHelper.getHarnessTagsYamlList(harnessTags))
        .build();
  }

  @Override
  public List<HarnessTag> upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();
    Yaml yaml = changeContext.getYaml();

    harnessTagYamlHelper.upsertHarnessTags(yaml, accountId, changeContext.getChange().isSyncFromGit());
    return harnessTagService.listTags(accountId);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) {
    String accountId = changeContext.getChange().getAccountId();
    Yaml yaml = changeContext.getYaml();

    harnessTagYamlHelper.deleteTags(yaml, accountId, changeContext.getChange().isSyncFromGit());
  }

  @Override
  public List<HarnessTag> get(String accountId, String yamlFilePath) {
    return harnessTagService.listTags(accountId);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
