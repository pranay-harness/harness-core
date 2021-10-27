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
