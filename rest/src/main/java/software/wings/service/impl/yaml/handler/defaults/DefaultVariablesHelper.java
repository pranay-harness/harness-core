package software.wings.service.impl.yaml.handler.defaults;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.Base.GLOBAL_ENV_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.collections.CollectionUtils;
import software.wings.beans.NameValuePair;
import software.wings.beans.SettingAttribute;
import software.wings.beans.StringValue;
import software.wings.beans.defaults.Defaults.Yaml;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Takes care of converting Account and Application level defaults to Defaults entity object.
 * This was needed since we map entity object to a yaml typically, but in this case, we map a list of default variables
 * to a Defaults.yaml. Also, all the application and account defaults are stored as a list of setting attributes instead
 * of an entity.
 * @author rktummala on 1/17/18
 */
public class DefaultVariablesHelper {
  @Inject SettingsService settingsService;
  @Inject private YamlPushService yamlPushService;

  public List<NameValuePair.Yaml> convertToNameValuePairYamlList(List<SettingAttribute> settingAttributes) {
    if (CollectionUtils.isEmpty(settingAttributes)) {
      return Lists.newArrayList();
    }

    return settingAttributes.stream()
        .map(settingAttribute -> {
          String value = null;
          SettingValue settingValue = settingAttribute.getValue();
          if (settingValue != null) {
            value = ((StringValue) settingValue).getValue();
          }

          return NameValuePair.Yaml.builder().name(settingAttribute.getName()).value(value).build();
        })
        .collect(toList());
  }

  @SuppressFBWarnings({"NP_NULL_ON_SOME_PATH", "UC_USELESS_OBJECT"}) // TODO
  public void saveOrUpdateDefaults(Yaml updatedYaml, String appId, String accountId, boolean syncFromGit)
      throws HarnessException {
    List<SettingAttribute> previousDefaultValues = getCurrentDefaultVariables(appId, accountId);
    List<NameValuePair.Yaml> previousDefaultYamls = convertToNameValuePairYamlList(previousDefaultValues);

    // what are the defaults changes? Which are additions and which are deletions?
    List<NameValuePair.Yaml> varsToAdd = new ArrayList<>();
    List<NameValuePair.Yaml> varsToDelete = new ArrayList<>();
    List<NameValuePair.Yaml> varsToUpdate = new ArrayList<>();

    List<NameValuePair.Yaml> defaults = updatedYaml.getDefaults();

    if (defaults != null) {
      // initialize the defaults to add from the after
      for (NameValuePair.Yaml cv : defaults) {
        varsToAdd.add(cv);
      }
    }

    if (previousDefaultYamls != null) {
      // initialize the defaults to delete from the before, and remove the befores from the defaults to add list
      for (NameValuePair.Yaml cv : previousDefaultYamls) {
        varsToDelete.add(cv);
        varsToAdd.remove(cv);
      }
    }

    if (defaults != null) {
      // remove the afters from the defaults to delete list
      for (NameValuePair.Yaml cv : defaults) {
        varsToDelete.remove(cv);

        if (previousDefaultYamls != null && previousDefaultYamls.contains(cv)) {
          NameValuePair.Yaml beforeCV = null;
          for (NameValuePair.Yaml bcv : previousDefaultYamls) {
            if (bcv.equals(cv)) {
              beforeCV = bcv;
              break;
            }
          }
          if (!cv.getValue().equals(beforeCV.getValue())) {
            varsToUpdate.add(cv);
          }
        }
      }
    }

    Map<String, SettingAttribute> defaultVarMap = previousDefaultValues.stream().collect(
        Collectors.toMap(defaultVar -> defaultVar.getName(), defaultVar -> defaultVar));

    // do deletions
    varsToDelete.forEach(defaultVar -> {
      if (defaultVarMap.containsKey(defaultVar.getName())) {
        settingsService.delete(appId, defaultVarMap.get(defaultVar.getName()).getUuid(), false, syncFromGit);
      }
    });

    // save the new variables
    varsToAdd.forEach(
        defaultVar -> settingsService.save(createNewSettingAttribute(accountId, appId, defaultVar), false));

    try {
      // update the existing variables
      varsToUpdate.forEach(defaultVar -> {
        SettingAttribute settingAttribute = defaultVarMap.get(defaultVar.getName());
        if (settingAttribute != null) {
          SettingValue settingValue = settingAttribute.getValue();
          if (settingValue != null) {
            StringValue stringValue = (StringValue) settingValue;
            stringValue.setValue(defaultVar.getValue());
            settingsService.update(settingAttribute, false);
          }
        }
      });
    } catch (WingsException ex) {
      throw new HarnessException(ex);
    }

    yamlPushService.pushYamlChangeSet(accountId, appId, ChangeType.MODIFY, syncFromGit);
  }

  private SettingAttribute createNewSettingAttribute(String accountId, String appId, NameValuePair.Yaml defaultVar) {
    SettingValue settingValue = StringValue.Builder.aStringValue().withValue(defaultVar.getValue()).build();
    return SettingAttribute.Builder.aSettingAttribute()
        .withAppId(appId)
        .withName(defaultVar.getName())
        .withValue(settingValue)
        .withAccountId(accountId)
        .withEnvId(GLOBAL_ENV_ID)
        .build();
  }

  public List<SettingAttribute> getCurrentDefaultVariables(String appId, String accountId) {
    return settingsService.getSettingAttributesByType(
        accountId, appId, GLOBAL_ENV_ID, SettingVariableTypes.STRING.name());
  }

  public void deleteDefaultVariables(String accountId, String appId) {
    settingsService.deleteSettingAttributesByType(accountId, appId, GLOBAL_ENV_ID, SettingVariableTypes.STRING.name());
  }
}
