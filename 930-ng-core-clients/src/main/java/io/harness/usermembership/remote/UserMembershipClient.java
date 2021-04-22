package io.harness.usermembership.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface UserMembershipClient {
  String USER_MEMBERSHIP_API = "users/usermembership";

  @GET(USER_MEMBERSHIP_API)
  Call<ResponseDTO<Boolean>> isUserInScope(@Query(NGCommonEntityConstants.USER_ID) String userId,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);
}
