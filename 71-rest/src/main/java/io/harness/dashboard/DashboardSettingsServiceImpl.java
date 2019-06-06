package io.harness.dashboard;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;

import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class DashboardSettingsServiceImpl implements DashboardSettingsService {
  @Inject private WingsPersistence persistence;
  int maxQueryLimit = 100;
  @Override
  public DashboardSettings get(@NotNull String accountId, @NotNull String id) {
    DashboardSettings dashboardSettings = persistence.get(DashboardSettings.class, id);
    if (dashboardSettings != null && !dashboardSettings.getAccountId().equals(accountId)) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED);
    }
    return dashboardSettings;
  }

  @Override
  public DashboardSettings createDashboardSettings(
      @NotNull String accountId, @NotNull DashboardSettings dashboardSettings) {
    dashboardSettings.setAccountId(accountId);
    return get(accountId, persistence.save(dashboardSettings));
  }

  @Override
  public DashboardSettings updateDashboardSettings(
      @NotNull String accountId, @NotNull DashboardSettings dashboardSettings) {
    String id = dashboardSettings.getUuid();
    if (id == null) {
      throw new WingsException(ErrorCode.INVALID_DASHBOARD_UPDATE_REQUEST, USER);
    }
    UpdateOperations<DashboardSettings> updateOperations = persistence.createUpdateOperations(DashboardSettings.class);
    updateOperations.set(DashboardSettings.keys.data, dashboardSettings.getData());
    updateOperations.set(DashboardSettings.keys.name, dashboardSettings.getName());
    updateOperations.set(DashboardSettings.keys.description, dashboardSettings.getDescription());
    persistence.update(dashboardSettings, updateOperations);
    return get(accountId, id);
  }

  @Override
  public boolean deleteDashboardSettings(@NotNull String accountId, @NotNull String dashboardSettingsId) {
    DashboardSettings dashboardSettings = get(accountId, dashboardSettingsId);
    if (dashboardSettings != null && dashboardSettings.getAccountId().equals(accountId)) {
      return persistence.delete(DashboardSettings.class, dashboardSettingsId);
    }
    return false;
  }

  @Override
  public PageResponse<DashboardSettings> getDashboardSettingSummary(@NotNull String accountId, int offset, int limit) {
    offset = Integer.max(offset, 0);
    limit = Integer.min(limit, maxQueryLimit);

    return persistence.query(DashboardSettings.class,
        PageRequestBuilder.aPageRequest()
            .withLimit(Integer.toString(limit))
            .withOffset(Integer.toString(offset))
            .addFilter(DashboardSettings.keys.accountId, Operator.EQ, accountId)
            .addFieldsExcluded(DashboardSettings.keys.data)
            .build());
  }
}
