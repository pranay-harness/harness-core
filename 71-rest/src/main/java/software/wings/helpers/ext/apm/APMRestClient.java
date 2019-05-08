package software.wings.helpers.ext.apm;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

import java.util.Map;

public interface APMRestClient {
  @GET
  Call<Object> validate(@Url String url, @HeaderMap Map<String, String> headers, @QueryMap Map<String, String> options);

  @POST
  Call<Object> validatePost(@Url String url, @HeaderMap Map<String, String> headers,
      @QueryMap Map<String, String> options, @Body String body);

  @GET
  Call<Object> collect(@Url String url, @HeaderMap Map<String, Object> headers, @QueryMap Map<String, Object> options);

  @PUT
  Call<Object> putCollect(@Url String url, @HeaderMap Map<String, Object> headers,
      @QueryMap Map<String, Object> options, @Body String body);

  @POST
  Call<Object> postCollect(@Url String url, @HeaderMap Map<String, Object> headers,
      @QueryMap Map<String, Object> options, @Body Map<String, Object> body);
}
