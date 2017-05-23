package software.wings.service.intfc.appdynamics;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.appdynamics.AppdynamicsApplication;
import software.wings.service.impl.appdynamics.AppdynamicsBusinessTransaction;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsNode;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.utils.validation.Create;

import java.io.IOException;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsService {
  List<AppdynamicsApplication> getApplications(@NotNull String settingId) throws IOException;

  List<AppdynamicsTier> getTiers(String settingId, int appdynamicsAppId) throws IOException;

  List<AppdynamicsNode> getNodes(String settingId, int appdynamicsAppId, int tierId) throws IOException;

  List<AppdynamicsBusinessTransaction> getBusinessTransactions(@NotNull String settingId, @Valid long appdynamicsAppId)
      throws IOException;

  void validateConfig(@NotNull SettingAttribute settingAttribute);

  List<AppdynamicsMetric> getTierBTMetrics(@NotNull String settingId, int appdynamicsAppId, int tierId)
      throws IOException;

  List<AppdynamicsMetricData> getTierBTMetricData(@NotNull String settingId, int appdynamicsAppId, int tierId,
      @NotNull String btName, int durantionInMinutes) throws IOException;

  @ValidationGroups(Create.class)
  Boolean saveMetricData(@NotNull String accountId, @Valid long appdynamicsAppId, @Valid long tierId,
      @Valid List<AppdynamicsMetricData> metricData) throws IOException;
}
