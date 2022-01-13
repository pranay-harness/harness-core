/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.instanceng;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.HarnessServiceInfoNG;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.Optional;
import javax.validation.constraints.NotNull;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.CE)
public interface InstanceNGResourceClient {
  String INSTANCENG = "instanceng";
  String INSTANCE_INFO_NAMESPACE = "instanceInfoNamespace";
  String INSTANCE_INFO_POD_NAME = "instanceInfoPodName";

  @GET(INSTANCENG + "/")
  Call<ResponseDTO<Optional<HarnessServiceInfoNG>>> getInstanceNGData(
      @NotNull @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Query(INSTANCE_INFO_POD_NAME) String instanceInfoPodName,
      @NotNull @Query(INSTANCE_INFO_NAMESPACE) String instanceInfoNamespace);
}
