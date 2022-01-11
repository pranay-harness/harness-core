/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.K8sEventCollectionBundle;
import io.harness.ng.core.dto.ResponseDTO;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(CE)
public interface K8sWatchTaskResourceClient {
  String K8S_WATCH_TASK_RESOURCE_ENDPOINT = "ccm/perpetual-task";

  String TASK_ID = "taskId";
  String ACCOUNT_ID = "accountIdentifier";

  @POST(K8S_WATCH_TASK_RESOURCE_ENDPOINT + "/create")
  Call<ResponseDTO<String>> create(
      @NotEmpty @Query(ACCOUNT_ID) String accountId, @NotNull @Body K8sEventCollectionBundle k8sEventCollectionBundle);

  @POST(K8S_WATCH_TASK_RESOURCE_ENDPOINT + "/reset")
  Call<ResponseDTO<Boolean>> reset(@NotEmpty @Query(ACCOUNT_ID) String accountId,
      @NotEmpty @Query(TASK_ID) String taskId, @NotNull @Body K8sEventCollectionBundle k8sEventCollectionBundle);

  @GET(K8S_WATCH_TASK_RESOURCE_ENDPOINT + "/delete")
  Call<ResponseDTO<Boolean>> delete(
      @NotEmpty @Query(ACCOUNT_ID) String accountId, @NotEmpty @Query(TASK_ID) String taskId);
}
