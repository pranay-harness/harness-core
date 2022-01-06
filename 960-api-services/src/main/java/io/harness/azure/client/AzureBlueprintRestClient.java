/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.azure.client;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;
import rx.Observable;

public interface AzureBlueprintRestClient {
  String APP_VERSION = "2018-11-01-preview";

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @PUT("{resourceScope}/providers/Microsoft.Blueprint/blueprints/{blueprintName}")
  Observable<Response<ResponseBody>> createOrUpdateBlueprint(@Header("Authorization") String bearerAuthHeader,
      @Path("resourceScope") String resourceScope, @Path("blueprintName") String blueprintName,
      @Body Object blueprintJSON, @Query("api-version") String appVersion);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @GET("{resourceScope}/providers/Microsoft.Blueprint/blueprints/{blueprintName}")
  Observable<Response<ResponseBody>> getBlueprint(@Header("Authorization") String bearerAuthHeader,
      @Path("resourceScope") String resourceScope, @Path("blueprintName") String blueprintName,
      @Query("api-version") String appVersion);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @PUT("{resourceScope}/providers/Microsoft.Blueprint/blueprints/{blueprintName}/artifacts/{artifactName}")
  Observable<Response<ResponseBody>> createOrUpdateArtifact(@Header("Authorization") String bearerAuthHeader,
      @Path("resourceScope") String resourceScope, @Path("blueprintName") String blueprintName,
      @Path("artifactName") String artifactName, @Body Object artifactJSON, @Query("api-version") String appVersion);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @PUT("{resourceScope}/providers/Microsoft.Blueprint/blueprints/{blueprintName}/versions/{versionId}")
  Observable<Response<ResponseBody>> publishBlueprintDefinition(@Header("Authorization") String bearerAuthHeader,
      @Path("resourceScope") String resourceScope, @Path("blueprintName") String blueprintName,
      @Path("versionId") String versionId, @Query("api-version") String appVersion);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @GET("{resourceScope}/providers/Microsoft.Blueprint/blueprints/{blueprintName}/versions/{versionId}")
  Observable<Response<ResponseBody>> getPublishedBlueprintVersion(@Header("Authorization") String bearerAuthHeader,
      @Path("resourceScope") String resourceScope, @Path("blueprintName") String blueprintName,
      @Path("versionId") String versionId, @Query("api-version") String appVersion);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @GET("{resourceScope}/providers/Microsoft.Blueprint/blueprints/{blueprintName}/versions")
  Observable<Response<ResponseBody>> listPublishedBlueprintVersions(@Header("Authorization") String bearerAuthHeader,
      @Path("resourceScope") String resourceScope, @Path("blueprintName") String blueprintName,
      @Query("api-version") String appVersion);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @GET
  Observable<Response<ResponseBody>> listNext(
      @Header("Authorization") String bearerAuthHeader, @Url String nextUrl, @Query("api-version") String appVersion);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @PUT("{resourceScope}/providers/Microsoft.Blueprint/blueprintAssignments/{assignmentName}")
  Observable<Response<ResponseBody>> beginCreateOrUpdateAssignment(@Header("Authorization") String bearerAuthHeader,
      @Path("resourceScope") String resourceScope, @Path("assignmentName") String assignmentName,
      @Body Object assignmentJSON, @Query("api-version") String appVersion);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @GET("{resourceScope}/providers/Microsoft.Blueprint/blueprintAssignments/{assignmentName}")
  Observable<Response<ResponseBody>> getAssignment(@Header("Authorization") String bearerAuthHeader,
      @Path("resourceScope") String resourceScope, @Path("assignmentName") String assignmentName,
      @Query("api-version") String appVersion);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @GET("{resourceScope}/providers/Microsoft.Blueprint/blueprintAssignments")
  Observable<Response<ResponseBody>> listBlueprintAssignments(@Header("Authorization") String bearerAuthHeader,
      @Path("resourceScope") String resourceScope, @Query("api-version") String appVersion);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @GET("{resourceScope}/providers/Microsoft.Blueprint/blueprintAssignments/{assignmentName}/assignmentOperations")
  Observable<Response<ResponseBody>> listAssignmentOperations(@Header("Authorization") String bearerAuthHeader,
      @Path("resourceScope") String resourceScope, @Path("assignmentName") String assignmentName,
      @Query("api-version") String appVersion);

  @Headers({"Content-Type: application/json; charset=utf-8", "accept-language: en-US"})
  @POST("{resourceScope}/providers/Microsoft.Blueprint/blueprintAssignments/{assignmentName}/whoIsBlueprint")
  Observable<Response<ResponseBody>> whoIsBlueprint(@Header("Authorization") String bearerAuthHeader,
      @Path("resourceScope") String resourceScope, @Path("assignmentName") String assignmentName,
      @Query("api-version") String appVersion);
}
