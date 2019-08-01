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
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.Map;

public interface SpotInstRestClient {
  @GET("aws/ec2/group")
  @Headers("Content-Type: application/json")
  Call<SpotInstListElastiGroupsResponse> listAllElastiGroups(@Header("Authorization") String authorization,
      @Query("minCreatedAt") long minCreatedAt, @Query("maxCreatedAt") long maxCreatedAt,
      @Query("accountId") String spotInstAccountId);

  @POST("aws/ec2/group")
  @Headers("Content-Type: application/json")
  Call<SpotInstListElastiGroupsResponse> createElastiGroup(@Header("Authorization") String authorization,
      @Query("accountId") String spotInstAccountId, @Body Map<String, Object> group);

  @PUT("aws/ec2/group/{groupId}")
  @Headers("Content-Type: application/json")
  Call<SpotInstUpdateElastiGroupResponse> updateElastiGroup(@Header("Authorization") String authorization,
      @Query("accountId") String spotInstAccountId, @Path("groupId") String elastiGroupId,
      @Body Map<String, Object> group);

  @DELETE("aws/ec2/group/{groupId}")
  @Headers("Content-Type: application/json")
  Call<SpotInstDeleteElastiGroupResponse> deleteElastiGroup(@Header("Authorization") String authorization,
      @Query("accountId") String spotInstAccountId, @Path("groupId") String elastiGroupId);

  @PUT("aws/ec2/group/{groupId}/scale/up")
  @Headers("Content-Type: application/json")
  Call<Void> scaleUpElastiGroup(@Header("Authorization") String authorization,
      @Query("accountId") String spotInstAccountId, @Path("groupId") String elastiGroupId,
      @Query("adjustment") int adjustment);

  @PUT("aws/ec2/group/{groupId}/scale/down")
  @Headers("Content-Type: application/json")
  Call<SpotInstScaleDownElastiGroupResponse> scaleDownElastiGroup(@Header("Authorization") String authorization,
      @Query("accountId") String spotInstAccountId, @Path("groupId") String elastiGroupId,
      @Query("adjustment") int adjustment);

  @GET("aws/ec2/group/{groupId}/instanceHealthiness")
  @Headers("Content-Type: application/json")
  Call<SpotInstListElastiGroupInstancesHealthResponse> listElastiGroupInstancesHealth(
      @Header("Authorization") String authorization, @Query("accountId") String spotInstAccountId,
      @Path("groupId") String elastiGroupId);
}
