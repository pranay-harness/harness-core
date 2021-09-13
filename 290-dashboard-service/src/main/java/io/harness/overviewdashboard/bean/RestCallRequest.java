package io.harness.overviewdashboard.bean;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;

import lombok.Builder;
import lombok.Value;
import retrofit2.Call;

@Value
@Builder
@OwnedBy(PL)
public class RestCallRequest<T> {
  Call<ResponseDTO<T>> request;
  OverviewDashboardRequestType requestType;
}
