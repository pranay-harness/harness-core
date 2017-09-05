package software.wings.service.intfc.newrelic;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.utils.validation.Create;

import java.io.IOException;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 8/28/17.
 */
public interface NewRelicService {
  void validateConfig(@NotNull SettingAttribute settingAttribute);

  List<NewRelicApplication> getApplications(@NotNull String settingId);

  @ValidationGroups(Create.class)
  boolean saveMetricData(@NotNull String accountId, String applicationId,
      @Valid List<NewRelicMetricDataRecord> metricData) throws IOException;
}
