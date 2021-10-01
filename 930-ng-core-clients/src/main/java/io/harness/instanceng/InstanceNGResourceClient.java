package io.harness.instanceng;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.HarnessServiceInfoNG;
import io.harness.ng.core.dto.ResponseDTO;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@OwnedBy(HarnessTeam.CE)
public interface InstanceNGResourceClient {
  final String INSTANCENG = "instanceng";
  final String INSTANCE_INFO_NAMESPACE = "instanceInfoNamespace";
  final String INSTANCE_INFO_POD_NAME = "instanceInfoPodName";

  @GET(INSTANCENG + "/")
  Call<ResponseDTO<Optional<HarnessServiceInfoNG>>> getInstanceNGData(
      @NotNull @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @Query(INSTANCE_INFO_POD_NAME) String instanceInfoPodName,
      @NotNull @Query(INSTANCE_INFO_NAMESPACE) String instanceInfoNamespace);
}
