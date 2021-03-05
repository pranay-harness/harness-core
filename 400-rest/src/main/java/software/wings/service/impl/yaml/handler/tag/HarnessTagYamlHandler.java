package software.wings.service.impl.yaml.handler.tag;

import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTagYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.intfc.HarnessTagService;

import com.google.inject.Inject;
import java.util.List;

public class HarnessTagYamlHandler extends BaseYamlHandler<HarnessTagYaml, List<HarnessTag>> {
  @Inject private HarnessTagService harnessTagService;

  @Override
  public HarnessTagYaml toYaml(List<HarnessTag> harnessTags, String appId) {
    return HarnessTagYaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .tag(harnessTagYamlHelper.getHarnessTagsYamlList(harnessTags))
        .build();
  }

  @Override
  public List<HarnessTag> upsertFromYaml(
      ChangeContext<HarnessTagYaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();
    HarnessTagYaml yaml = changeContext.getYaml();

    harnessTagYamlHelper.upsertHarnessTags(yaml, accountId, changeContext.getChange().isSyncFromGit());
    return harnessTagService.listTags(accountId);
  }

  @Override
  public void delete(ChangeContext<HarnessTagYaml> changeContext) {
    String accountId = changeContext.getChange().getAccountId();
    HarnessTagYaml yaml = changeContext.getYaml();

    harnessTagYamlHelper.deleteTags(yaml, accountId, changeContext.getChange().isSyncFromGit());
  }

  @Override
  public List<HarnessTag> get(String accountId, String yamlFilePath) {
    return harnessTagService.listTags(accountId);
  }

  @Override
  public Class getYamlClass() {
    return HarnessTagYaml.class;
  }
}
