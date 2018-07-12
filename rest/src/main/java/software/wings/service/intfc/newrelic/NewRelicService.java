package software.wings.service.intfc.newrelic;

import software.wings.APMFetchConfig;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicApplicationInstance;
import software.wings.service.impl.newrelic.NewRelicMetric;
import software.wings.sm.StateType;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 8/28/17.
 */
public interface NewRelicService {
  void validateConfig(@NotNull SettingAttribute settingAttribute, @NotNull StateType stateType);
  void validateAPMConfig(SettingAttribute settingAttribute, APMValidateCollectorConfig config);
  List<NewRelicApplication> getApplications(@NotNull String settingId, @NotNull StateType stateType);
  String fetch(String accountId, String serverConfigId, APMFetchConfig url);
  List<NewRelicApplicationInstance> getApplicationInstances(
      @NotNull String settingId, long applicationId, @NotNull StateType stateType);
  List<NewRelicMetric> getTxnsWithData(String settingId, long applicationId);
}
