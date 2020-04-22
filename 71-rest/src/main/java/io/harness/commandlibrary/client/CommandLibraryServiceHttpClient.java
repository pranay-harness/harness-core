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

  @GET("command-stores/{commandStoreName}/commands/categories")
  Call<RestResponse<List<String>>> getCommandCategories(@Path("commandStoreName") String commandStoreName);

  @GET("command-stores/{commandStoreName}/commands")
  Call<RestResponse<PageResponse<CommandDTO>>> listCommands(@Path("commandStoreName") String commandStoreName,
      @Query("cl_implementation_version") Integer clImplementationVersion, @Query("category") String category,
      @QueryMap Map<String, Object> pageRequestParams);

  @GET("command-stores/{commandStoreName}/commands/{commandName}")
  Call<RestResponse<CommandDTO>> getCommandDetails(
      @Path("commandStoreName") String commandStoreName, @Path("commandName") String commandName);

  @POST("command-stores/{commandStoreName}/commands")
  Call<RestResponse<CommandEntity>> saveCommand(
      @Path("commandStoreName") String commandStoreName, @Body CommandEntity commandEntity);

  @GET("command-stores/{commandStoreName}/commands/{commandName}/versions/{version}")
  Call<RestResponse<EnrichedCommandVersionDTO>> getVersionDetails(@Path("commandStoreName") String commandStoreName,
      @Path("commandName") String commandName, @Path("version") String version);

  @POST("command-stores/{commandStoreName}/commands/{commandName}/versions")
  Call<RestResponse<CommandVersionEntity>> saveCommandVersion(@Path("commandStoreName") String commandStoreName,
      @Path("commandName") String commandName, @Body CommandVersionEntity commandVersionEntity);
}
