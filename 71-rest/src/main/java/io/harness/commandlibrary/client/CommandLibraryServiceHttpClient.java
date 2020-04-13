package io.harness.commandlibrary.client;

import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface CommandLibraryServiceHttpClient { @GET("command-stores") Call<RestResponse<String>> getHelloWorld(); }
