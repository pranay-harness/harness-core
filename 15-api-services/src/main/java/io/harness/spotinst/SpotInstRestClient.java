package io.harness.spotinst;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.spotinst.model.SpotInstDeleteElastiGroupResponse;
import io.harness.spotinst.model.SpotInstListElastiGroupsResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SpotInstRestClient {
  @GET("aws/ec2/group")
  Call<SpotInstListElastiGroupsResponse> listAllElastiGroups(@Header("Content-Type") String contentType,
      @Header("Authorization") String authorization, @Query("minCreatedAt") long minCreatedAt,
      @Query("maxCreatedAt") long maxCreatedAt, @Query("accountId") String spotInstAccountId);

  @POST("aws/ec2/group")
  Call<JsonNode> createElastiGroup(@Header("Content-Type") String contentType,
      @Header("Authorization") String authorization, @Query("accountId") String spotInstAccountId,
      @Body Object jsonPayload);

  @DELETE("aws/ec2/group/{groupId}")
  Call<SpotInstDeleteElastiGroupResponse> deleteElastiGroup(@Header("Content-Type") String contentType,
      @Header("Authorization") String authorization, @Query("accountId") String spotInstAccountId,
      @Path("groupId") String elastiGroupId);
}
