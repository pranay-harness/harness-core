package software.wings.service.impl.yaml.handler.defaults;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.NameValuePair;
import software.wings.beans.defaults.Defaults;
import software.wings.beans.defaults.Defaults.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.NameValuePairYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class Defaults1YamlHandler extends BaseYamlHandler<Yaml, Defaults> {
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject DefaultVariablesHelper defaultsHelper;

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {}

  @Override
  public Yaml toYaml(Defaults bean, String appId) {
    List<NameValuePair> nameValuePairList = bean.getNameValuePairList();

    // properties
    NameValuePairYamlHandler nameValuePairYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.NAME_VALUE_PAIR);
    List<NameValuePair.Yaml> nameValuePairYamlList =
        nameValuePairList.stream()
            .map(nameValuePair -> nameValuePairYamlHandler.toYaml(nameValuePair, appId))
            .collect(Collectors.toList());
    YamlType yamlType = GLOBAL_APP_ID.equals(appId) ? YamlType.ACCOUNT_DEFAULTS : YamlType.APPLICATION_DEFAULTS;

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .defaults(nameValuePairYamlList)
        .type(yamlType.name())
        .build();
  }

  @Override
  public Defaults upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    YamlType yamlType = changeContext.getYamlType();
    String accountId = changeContext.getChange().getAccountId();
    String appId = null;
    switch (yamlType) {
      case APPLICATION_DEFAULTS:

        break;
      case ACCOUNT_DEFAULTS:
        appId = GLOBAL_APP_ID;
        break;
      default:
        break;
    }
    //    defaultsHelper.saveOrUpdateDefaults();

    //    return defaultsHelper.getCurrentDefaultVariables(appId, accountId);
    return null;
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String type = changeContext.getYaml().getType();
    return !isEmpty(type);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Defaults get(String accountId, String yamlFilePath) {
    return null;
  }
}
