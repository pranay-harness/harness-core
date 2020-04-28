package io.harness.commandlibrary.client;

import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.rest.RestResponse;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO;

import java.util.Map;

public interface CommandLibraryServiceHttpClient {
  @GET("command-stores") Call<Object> getCommandStores(@QueryMap Map<String, Object> pageRequestParams);

  @GET("command-stores/{commandStoreName}/commands/categories")
  Call<Object> getCommandCategories(
      @Path("commandStoreName") String commandStoreName, @QueryMap Map<String, Object> pageRequestParams);

  @GET("command-stores/{commandStoreName}/commands")
  Call<Object> listCommands(
      @Path("commandStoreName") String commandStoreName, @QueryMap Map<String, Object> pageRequestParams);

  @GET("command-stores/{commandStoreName}/commands/{commandName}")
  Call<RestResponse<CommandDTO>> getCommandDetails(@Path("commandStoreName") String commandStoreName,
      @Path("commandName") String commandName, @QueryMap Map<String, Object> pageRequestParams);

  @GET("command-stores/{commandStoreName}/commands/{commandName}/versions/{version}")
  Call<RestResponse<EnrichedCommandVersionDTO>> getVersionDetails(@Path("commandStoreName") String commandStoreName,
      @Path("commandName") String commandName, @Path("version") String version,
      @QueryMap Map<String, Object> pageRequestParams);

  @Multipart
  @POST("command-stores/{commandStoreName}/commands")
  Call<Object> publishCommand(@Path("commandStoreName") String commandStoreName, @Part MultipartBody.Part file,
      @QueryMap Map<String, Object> pageRequestParams);
}
