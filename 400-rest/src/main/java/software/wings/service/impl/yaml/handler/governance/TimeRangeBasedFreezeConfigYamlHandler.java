package software.wings.service.impl.yaml.handler.governance;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.exception.InvalidRequestException;
import io.harness.governance.ApplicationFilter;
import io.harness.governance.ApplicationFilterYaml;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.governance.TimeRangeBasedFreezeConfig.TimeRangeBasedFreezeConfigBuilder;
import io.harness.governance.TimeRangeBasedFreezeConfig.Yaml;
import io.harness.validation.Validator;

import software.wings.beans.security.UserGroup;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.resources.stats.model.TimeRange;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class TimeRangeBasedFreezeConfigYamlHandler
    extends GovernanceFreezeConfigYamlHandler<Yaml, TimeRangeBasedFreezeConfig> {
  @Inject YamlHelper yamlHelper;
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject EnvironmentService environmentService;
  @Inject UserGroupService userGroupService;
  @Inject GovernanceConfigService governanceConfigService;

  @Override
  public Yaml toYaml(TimeRangeBasedFreezeConfig bean, String accountId) {
    ApplicationFilterYamlHandler applicationFilterYamlHandler;

    List<ApplicationFilterYaml> appFiltersYaml = new ArrayList<>();

    for (ApplicationFilter applicationFilter : bean.getAppSelections()) {
      applicationFilterYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.APPLICATION_FILTER, applicationFilter.getFilterType().name());
      appFiltersYaml.add(applicationFilterYamlHandler.toYaml(applicationFilter, accountId));
    }

    TimeRange.Yaml timeRangeYaml = TimeRange.Yaml.builder()
                                       .from(String.valueOf(bean.getTimeRange().getFrom()))
                                       .to(String.valueOf(bean.getTimeRange().getTo()))
                                       .build();

    return Yaml.builder()
        .name(bean.getName())
        .type("TIME_RANGE_BASED_FREEZE_CONFIG")
        .description(bean.getDescription())
        .applicable(bean.isApplicable())
        .appSelections(appFiltersYaml)
        .userGroups(getUserGroupNames(bean.getUserGroups(), accountId))
        .timeRange(timeRangeYaml)
        .build();
  }

  @Override
  public TimeRangeBasedFreezeConfig upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    TimeRangeBasedFreezeConfigBuilder timeRangeBasedFreezeConfig = TimeRangeBasedFreezeConfig.builder();
    toBean(timeRangeBasedFreezeConfig, changeContext, changeSetContext);
    return timeRangeBasedFreezeConfig.build();
  }

  @Override
  public Class getYamlClass() {
    return TimeRangeBasedFreezeConfig.Yaml.class;
  }

  private List<String> getUserGroupNames(List<String> userGroupList, String accountId) {
    List<String> userGroupNames = new ArrayList<>();
    for (String userGroupId : userGroupList) {
      UserGroup userGroup = userGroupService.get(accountId, userGroupId);
      if (userGroup != null) {
        userGroupNames.add(userGroup.getName());
      }
    }
    return userGroupNames;
  }

  private List<String> getUserGroupIds(List<String> userGroupNames, String accountId) {
    if (hasNone(userGroupNames)) {
      throw new InvalidRequestException("User Groups cannot be empty");
    }
    List<String> userGroupIds = new ArrayList<>();
    for (String userGroupName : userGroupNames) {
      UserGroup userGroup = userGroupService.getByName(accountId, userGroupName);
      if (userGroup == null) {
        throw new InvalidRequestException("Invalid User Group name: " + userGroupName);
      }
      userGroupIds.add(userGroup.getUuid());
    }
    return userGroupIds;
  }

  private void toBean(
      TimeRangeBasedFreezeConfigBuilder bean, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();
    Validator.notNullCheck("AccountId not present", accountId);

    TimeRangeBasedFreezeConfig.Yaml yaml = changeContext.getYaml();
    Validator.notNullCheck("Name is required", yaml.getName());
    TimeRange.Yaml timeRangeYaml = yaml.getTimeRange();

    bean.timeRange(validateTimeRangeYaml(timeRangeYaml));
    bean.name(yaml.getName());
    bean.applicable(yaml.isApplicable());
    bean.description(yaml.getDescription());
    bean.userGroups(getUserGroupIds(yaml.getUserGroups(), accountId));

    ApplicationFilterYamlHandler applicationFilterYamlHandler;

    List<ApplicationFilter> applicationFilters = new ArrayList<>();
    for (ApplicationFilterYaml entry : yaml.getAppSelections()) {
      applicationFilterYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.APPLICATION_FILTER, entry.getFilterType().name());
      ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, entry);
      applicationFilters.add(applicationFilterYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext));
    }

    bean.appSelections(applicationFilters);
  }

  private TimeRange validateTimeRangeYaml(TimeRange.Yaml timeRangeYaml) {
    Validator.notNullCheck("Time Range cannot be empty", timeRangeYaml);
    Validator.notNullCheck("From time cannot be empty", timeRangeYaml.getFrom());
    Validator.notNullCheck("To time cannot be empty", timeRangeYaml.getTo());

    long from;
    long to;

    try {
      from = Long.parseLong(timeRangeYaml.getFrom());
      to = Long.parseLong(timeRangeYaml.getTo());
    } catch (NumberFormatException exception) {
      throw new InvalidRequestException("Incorrect format for Time Range. Please enter epoch time");
    }

    // time zone from DB document
    return new TimeRange(from, to, null);
  }
}
