package software.wings.service.impl.yaml.handler;

import com.google.inject.Inject;

import io.harness.exception.HarnessException;
import io.harness.persistence.PersistentEntity;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.ChangeContext.Builder;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHelper;
import software.wings.yaml.BaseEntityYaml;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlHelper;

import java.util.List;

/**
 * Base class for all yaml handlers. Each yaml bean would have a handler that takes care of toYaml(), fromYaml(), etc.
 *
 * @author rktummala on 10/16/17
 */
public abstract class BaseYamlHandler<Y extends BaseYaml, B extends Object> {
  @Inject(optional = true) protected HarnessTagYamlHelper harnessTagYamlHelper;

  public abstract void delete(ChangeContext<Y> changeContext) throws HarnessException;

  public abstract Y toYaml(B bean, String appId);

  public abstract B upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException;

  public abstract Class getYamlClass();

  public abstract B get(String accountId, String yamlFilePath);

  protected Builder cloneFileChangeContext(ChangeContext<Y> context, BaseYaml yaml) {
    Change change = context.getChange();
    Change.Builder clonedChange = change.toBuilder();
    clonedChange.withFileContent(YamlHelper.toYamlString(yaml));

    Builder clonedContext = context.toBuilder();
    clonedContext.withChange(clonedChange.build());
    clonedContext.withYaml(yaml);
    return clonedContext;
  }

  protected String getHarnessApiVersion() {
    return "1.0";
  }

  protected void updateYamlWithAdditionalInfo(PersistentEntity entity, String appId, BaseEntityYaml yaml) {
    harnessTagYamlHelper.updateYamlWithHarnessTagLinks(entity, appId, yaml);
  }
}
