package io.harness.dashboard;

import static io.harness.beans.PageRequest.DEFAULT_PAGE_SIZE;
import static io.harness.beans.PageRequest.DEFAULT_UNLIMITED;
import static io.harness.beans.PageRequest.PageRequestBuilder;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.dashboard.DashboardSettings.keys;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.security.auth.DashboardAuthHandler;

import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class DashboardSettingsServiceImpl implements DashboardSettingsService {
  @Inject private WingsPersistence persistence;
  @Inject private DashboardAuthHandler dashboardAuthHandler;

  @Override
  public DashboardSettings get(@NotNull String accountId, @NotNull String id) {
    DashboardSettings dashboardSettings = persistence.get(DashboardSettings.class, id);
    dashboardAuthHandler.setAccessFlags(asList(dashboardSettings));
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
    if (isNotEmpty(dashboardSettings.getPermissions())) {
      updateOperations.set(keys.permissions, dashboardSettings.getPermissions());
    } else {
      updateOperations.unset(keys.permissions);
    }

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
  public PageResponse<DashboardSettings> getDashboardSettingSummary(
      @NotNull String accountId, @NotNull PageRequest pageRequest) {
    pageRequest = sanitizePageRequest(pageRequest);
    pageRequest.addFilter(DashboardSettings.keys.accountId, Operator.EQ, accountId);
    pageRequest.addFieldsExcluded(DashboardSettings.keys.data);
    PageResponse pageResponse = persistence.query(DashboardSettings.class, pageRequest);
    dashboardAuthHandler.setAccessFlags(pageResponse.getResponse());
    return pageResponse;
  }

  private PageRequest sanitizePageRequest(PageRequest pageRequest) {
    if (pageRequest == null) {
      pageRequest =
          PageRequestBuilder.aPageRequest().withLimit(Integer.toString(DEFAULT_PAGE_SIZE)).withOffset("0").build();
    }
    pageRequest.setOffset(pageRequest.getLimit() != null
            ? Integer.toString(Integer.max(Integer.parseInt(pageRequest.getOffset()), 0))
            : "0");
    pageRequest.setLimit(pageRequest.getLimit() != null
            ? Integer.toString(Integer.min(Integer.parseInt(pageRequest.getLimit()), DEFAULT_UNLIMITED))
            : Integer.toString(DEFAULT_PAGE_SIZE));
    return pageRequest;
  }
}
