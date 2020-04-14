package io.harness.commandlibrary.client;

import io.harness.beans.PageResponse;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.api.dto.CommandStoreDTO;
import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO;
import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandVersionEntity;

import java.util.List;
import java.util.Map;

public interface CommandLibraryServiceHttpClient {
  @GET("command-stores") Call<RestResponse<List<CommandStoreDTO>>> getCommandStores();

  @GET("command-stores/{commandStoreId}/commands/categories")
  Call<RestResponse<List<String>>> getCommandCategories(@Path("commandStoreId") String commandStoreId);

  @GET("command-stores/{commandStoreId}/commands")
  Call<RestResponse<PageResponse<CommandDTO>>> listCommands(@Path("commandStoreId") String commandStoreId,
      @Query("cl_implementation_version") Integer clImplementationVersion, @Query("category") String category,
      @QueryMap Map<String, Object> pageRequestParams);

  @GET("command-stores/{commandStoreId}/commands/{commandId}")
  Call<RestResponse<CommandDTO>> getCommandDetails(
      @Path("commandStoreId") String commandStoreId, @Path("commandId") String commandId);

  @POST("command-stores/{commandStoreId}/commands")
  Call<RestResponse<CommandEntity>> saveCommand(
      @Path("commandStoreId") String commandStoreId, @Body CommandEntity commandEntity);

  @GET("command-stores/{commandStoreId}/commands/{commandId}/versions/{version}")
  Call<RestResponse<EnrichedCommandVersionDTO>> getVersionDetails(@Path("commandStoreId") String commandStoreId,
      @Path("commandId") String commandId, @Path("version") String version);

  @POST("command-stores/{commandStoreId}/commands/{commandId}/versions")
  Call<RestResponse<CommandVersionEntity>> saveCommandVersion(@Path("commandStoreId") String commandStoreId,
      @Path("commandId") String commandId, @Body CommandVersionEntity commandVersionEntity);
}
