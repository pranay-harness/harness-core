package software.wings.service.impl.yaml.handler;

import static java.lang.String.format;

import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlHelper;

import java.util.List;

/**
 * Base class for all yaml handlers. Each yaml bean would have a handler that takes care of toYaml(), fromYaml(), etc.
 *
 * @author rktummala on 10/16/17
 */
public abstract class BaseYamlHandler<Y extends BaseYaml, B extends Object> {
  public abstract Y toYaml(B bean, String appId);

  public abstract B upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException;

  public abstract boolean validate(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext);

  public abstract Class getYamlClass();

  public abstract B get(String accountId, String yamlFilePath);

  public abstract B createFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException;

  public abstract B updateFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException;

  protected void ensureValidChange(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      throw new HarnessException(format("Invalid yaml change [%s]", changeContext));
    }
  }

  protected ChangeContext.Builder cloneFileChangeContext(ChangeContext<Y> context, BaseYaml yaml) {
    Change change = context.getChange();
    Change.Builder clonedChange = change.clone();
    clonedChange.withFileContent(YamlHelper.toYamlString(yaml));

    ChangeContext.Builder clonedContext = context.toBuilder();
    clonedContext.withChange(clonedChange.build());
    clonedContext.withYaml(yaml);
    return clonedContext;
  }

  protected Object createOrUpdateFromYaml(boolean isCreate, BaseYamlHandler yamlHandler, ChangeContext context,
      List<ChangeContext> changeSetContext) throws HarnessException {
    if (isCreate) {
      return yamlHandler.createFromYaml(context, changeSetContext);
    } else {
      return yamlHandler.updateFromYaml(context, changeSetContext);
    }
  }
}
