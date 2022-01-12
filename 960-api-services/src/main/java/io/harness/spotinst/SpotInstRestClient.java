/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.spotinst;

import io.harness.spotinst.model.SpotInstDeleteElastiGroupResponse;
import io.harness.spotinst.model.SpotInstListElastiGroupInstancesHealthResponse;
import io.harness.spotinst.model.SpotInstListElastiGroupsResponse;
import io.harness.spotinst.model.SpotInstScaleDownElastiGroupResponse;
import io.harness.spotinst.model.SpotInstUpdateElastiGroupResponse;

import java.util.Map;
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

public interface SpotInstRestClient {
  @GET("aws/ec2/group")
  @Headers("Content-Type: application/json")
  Call<SpotInstListElastiGroupsResponse> listAllElastiGroups(@Header("Authorization") String authorization,
      @Query("minCreatedAt") long minCreatedAt, @Query("maxCreatedAt") long maxCreatedAt,
      @Query("accountId") String spotInstAccountId);

  @GET("aws/ec2/group/{groupId}")
  @Headers("Content-Type: application/json")
  Call<SpotInstListElastiGroupsResponse> listElastiGroup(@Header("Authorization") String authorization,
      @Path("groupId") String elastiGroupId, @Query("accountId") String spotInstAccountId);

  @GET("aws/ec2/group/{groupId}")
  @Headers("Content-Type: application/json")
  Call<Object> getElastigroupJson(@Header("Authorization") String authorization, @Path("groupId") String elastigroupId,
      @Query("accountId") String spotInstAccountId);

  @POST("aws/ec2/group")
  @Headers("Content-Type: application/json")
  Call<SpotInstListElastiGroupsResponse> createElastiGroup(@Header("Authorization") String authorization,
      @Query("accountId") String spotInstAccountId, @Body Map<String, Object> group);

  @PUT("aws/ec2/group/{groupId}")
  @Headers("Content-Type: application/json")
  Call<SpotInstUpdateElastiGroupResponse> updateElastiGroup(@Header("Authorization") String authorization,
      @Path("groupId") String elastiGroupId, @Query("accountId") String spotInstAccountId,
      @Body Map<String, Object> group);

  @PUT("aws/ec2/group/{groupId}/capacity")
  @Headers("Content-Type: application/json")
  Call<SpotInstUpdateElastiGroupResponse> updateElastiGroupCapacity(@Header("Authorization") String authorization,
      @Path("groupId") String elastiGroupId, @Query("accountId") String spotInstAccountId,
      @Body Map<String, Object> groupCapacityConfig);

  @DELETE("aws/ec2/group/{groupId}")
  @Headers("Content-Type: application/json")
  Call<SpotInstDeleteElastiGroupResponse> deleteElastiGroup(@Header("Authorization") String authorization,
      @Path("groupId") String elastiGroupId, @Query("accountId") String spotInstAccountId);

  @PUT("aws/ec2/group/{groupId}/scale/up")
  @Headers("Content-Type: application/json")
  Call<Void> scaleUpElastiGroup(@Header("Authorization") String authorization, @Path("groupId") String elastiGroupId,
      @Query("accountId") String spotInstAccountId, @Query("adjustment") int adjustment);

  @PUT("aws/ec2/group/{groupId}/scale/down")
  @Headers("Content-Type: application/json")
  Call<SpotInstScaleDownElastiGroupResponse> scaleDownElastiGroup(@Header("Authorization") String authorization,
      @Path("groupId") String elastiGroupId, @Query("accountId") String spotInstAccountId,
      @Query("adjustment") int adjustment);

  @GET("aws/ec2/group/{groupId}/instanceHealthiness")
  @Headers("Content-Type: application/json")
  Call<SpotInstListElastiGroupInstancesHealthResponse> listElastiGroupInstancesHealth(
      @Header("Authorization") String authorization, @Path("groupId") String elastiGroupId,
      @Query("accountId") String spotInstAccountId);
}
