/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.instancesync;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.ng.core.dto.ResponseDTO;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncResourceClient {
  String INSTANCE_SYNC = "instancesync";

  @POST(INSTANCE_SYNC + "/response")
  Call<ResponseDTO<Boolean>> sendPerpetualTaskResponse(
      @NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotEmpty @Query(NGCommonEntityConstants.PERPETUAL_TASK_ID) String perpetualTaskId,
      @NotNull @Body DelegateResponseData instanceSyncPerpetualTaskResponse);
}
