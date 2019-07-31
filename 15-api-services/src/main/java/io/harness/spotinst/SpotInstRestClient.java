package io.harness.spotinst;

import io.harness.spotinst.model.SpotInstDeleteElastiGroupResponse;
import io.harness.spotinst.model.SpotInstListElastiGroupInstancesHealthResponse;
import io.harness.spotinst.model.SpotInstListElastiGroupsResponse;
import io.harness.spotinst.model.SpotInstScaleDownElastiGroupResponse;
import io.harness.spotinst.model.SpotInstUpdateElastiGroupResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SpotInstRestClient {
  @GET("aws/ec2/group")
  Call<SpotInstListElastiGroupsResponse> listAllElastiGroups(@Header("Content-Type") String contentType,
      @Header("Authorization") String authorization, @Query("minCreatedAt") long minCreatedAt,
      @Query("maxCreatedAt") long maxCreatedAt, @Query("accountId") String spotInstAccountId);

  @POST("aws/ec2/group")
  Call<SpotInstListElastiGroupsResponse> createElastiGroup(@Header("Content-Type") String contentType,
      @Header("Authorization") String authorization, @Query("accountId") String spotInstAccountId,
      @Body Object jsonPayload);

  @PUT("aws/ec2/group/{groupId}")
  Call<SpotInstUpdateElastiGroupResponse> updateElastiGroup(@Header("Content-Type") String contentType,
      @Header("Authorization") String authorization, @Query("accountId") String spotInstAccountId,
      @Path("groupId") String elastiGroupId, @Body Object jsonPayload);

  @DELETE("aws/ec2/group/{groupId}")
  Call<SpotInstDeleteElastiGroupResponse> deleteElastiGroup(@Header("Content-Type") String contentType,
      @Header("Authorization") String authorization, @Query("accountId") String spotInstAccountId,
      @Path("groupId") String elastiGroupId);

  @PUT("aws/ec2/group/{groupId}/scale/up")
  Call<Void> scaleUpElastiGroup(@Header("Content-Type") String contentType,
      @Header("Authorization") String authorization, @Query("accountId") String spotInstAccountId,
      @Path("groupId") String elastiGroupId, @Query("adjustment") int adjustment);

  @PUT("aws/ec2/group/{groupId}/scale/down")
  Call<SpotInstScaleDownElastiGroupResponse> scaleDownElastiGroup(@Header("Content-Type") String contentType,
      @Header("Authorization") String authorization, @Query("accountId") String spotInstAccountId,
      @Path("groupId") String elastiGroupId, @Query("adjustment") int adjustment);

  @GET("aws/ec2/group/{groupId}/instanceHealthiness")
  Call<SpotInstListElastiGroupInstancesHealthResponse> listElastiGroupInstancesHealth(
      @Header("Content-Type") String contentType, @Header("Authorization") String authorization,
      @Query("accountId") String spotInstAccountId, @Path("groupId") String elastiGroupId);
}
