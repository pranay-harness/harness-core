package software.wings.service.impl.yaml.handler;

import static java.lang.String.format;

import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.ChangeContext.Builder;
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
  public abstract void delete(ChangeContext<Y> changeContext) throws HarnessException;

  public abstract Y toYaml(B bean, String appId);

  public abstract B upsertFromYaml(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException;

  public abstract boolean validate(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext);

  public abstract Class getYamlClass();

  public abstract B get(String accountId, String yamlFilePath);

  protected void ensureValidChange(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (!validate(changeContext, changeSetContext)) {
      throw new HarnessException(format("Invalid yaml change [%s]", changeContext));
    }
  }

  public B getPrevious(ChangeContext<Y> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    return get(accountId, yamlFilePath);
  }

  protected Builder cloneFileChangeContext(ChangeContext<Y> context, BaseYaml yaml) {
    Change change = context.getChange();
    Change.Builder clonedChange = change.clone();
    clonedChange.withFileContent(YamlHelper.toYamlString(yaml));

    Builder clonedContext = context.toBuilder();
    clonedContext.withChange(clonedChange.build());
    clonedContext.withYaml(yaml);
    return clonedContext;
  }
}
