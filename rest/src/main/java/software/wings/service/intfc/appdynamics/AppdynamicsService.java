package software.wings.service.intfc.appdynamics;

import software.wings.beans.SettingAttribute;
import software.wings.service.impl.appdynamics.AppdynamicsBusinessTransaction;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;

import java.io.IOException;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsService {
  List<NewRelicApplication> getApplications(@NotNull String settingId) throws IOException;

  List<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId) throws IOException;

  List<AppdynamicsBusinessTransaction> getBusinessTransactions(@NotNull String settingId, @Valid long appdynamicsAppId)
      throws IOException;

  void validateConfig(@NotNull SettingAttribute settingAttribute);

  List<AppdynamicsMetric> getTierBTMetrics(@NotNull String settingId, long appdynamicsAppId, long tierId)
      throws IOException;

  List<AppdynamicsMetricData> getTierBTMetricData(@NotNull String settingId, long appdynamicsAppId, long tierId,
      @NotNull String btName, int durantionInMinutes) throws IOException;
}
