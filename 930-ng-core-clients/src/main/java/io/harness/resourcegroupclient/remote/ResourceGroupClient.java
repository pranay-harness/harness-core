package io.harness.resourcegroupclient.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.resourcegroup.remote.dto.ResourceTypeDTO;
import io.harness.resourcegroupclient.ResourceGroupResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ResourceGroupClient {
  String RESOURCE_GROUP_API = "resourcegroup";
  String RESOURCE_TYPE_API = "resourcetype";

  @GET(RESOURCE_GROUP_API + "/{identifier}")
  Call<ResponseDTO<ResourceGroupResponse>> getResourceGroup(
      @Path(value = NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  @GET(RESOURCE_TYPE_API)
  Call<ResponseDTO<ResourceTypeDTO>> getResourceTypes(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);
}
