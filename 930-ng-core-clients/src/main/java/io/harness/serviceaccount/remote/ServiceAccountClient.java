package io.harness.serviceaccount.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.serviceaccount.ServiceAccountDTO;

import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ServiceAccountClient {
  String SERVICE_ACCOUNTS_API = "serviceaccount";

  @GET(SERVICE_ACCOUNTS_API)
  Call<ResponseDTO<List<ServiceAccountDTO>>> listServiceAccounts(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotEmpty String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) @NotEmpty String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.IDENTIFIER_KEY) List<String> serviceAccountIdentifiers);
}
