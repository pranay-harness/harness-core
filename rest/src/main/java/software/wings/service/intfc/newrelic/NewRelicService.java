package software.wings.service.intfc.newrelic;

import software.wings.beans.SettingAttribute;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.sm.StateType;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 8/28/17.
 */
public interface NewRelicService {
  void validateConfig(@NotNull SettingAttribute settingAttribute, @NotNull StateType stateType);
  List<NewRelicApplication> getApplications(@NotNull String settingId, @NotNull StateType stateType);
}
