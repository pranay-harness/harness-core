package io.harness.perpetualtask;

import static java.lang.String.format;

import com.google.api.client.util.Base64;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import io.harness.beans.DelegateTask;
import io.harness.network.Http;
import io.harness.perpetualtask.https.HttpsPerpetualTaskParams;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class HttpsPerpetualTaskServiceClientImpl implements HttpsPerpetualTaskServiceClient {
  private HttpsClient httpsClient;

  public HttpsPerpetualTaskServiceClientImpl(HttpsPerpetualTaskClientEntrypoint entryPoint) {
    this.httpsClient = buildHttpsClient(entryPoint);
  }

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    if (clientContext == null || clientContext.getClientParams() == null) {
      return null;
    }

    try {
      Response<ResponseBody> response = httpsClient.getTaskParams(clientContext.getClientParams()).execute();
      if (response != null && response.body() != null) {
        return HttpsPerpetualTaskParams.newBuilder()
            .setTaskParams(ByteString.copyFrom(response.body().bytes()))
            .build();
      }
      return null;
    } catch (IOException ex) {
      logger.error("Failed to fetch task params.", ex);
    }

    return null;
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    throw new UnsupportedOperationException();
  }

  public interface HttpsClient {
    @FormUrlEncoded @POST("/") Call<ResponseBody> getTaskParams(@FieldMap Map<String, String> body);
  }

  private HttpsClient buildHttpsClient(HttpsPerpetualTaskClientEntrypoint entrypoint) {
    OkHttpClient client =
        Http.getUnsafeOkHttpClientBuilder(entrypoint.getUrl(), 15, 15)
            .connectionPool(new ConnectionPool())
            .retryOnConnectionFailure(true)
            .addInterceptor(chain -> {
              Request.Builder request = chain.request().newBuilder().addHeader("Authorization",
                  "Basic "
                      + Base64.encodeBase64String(format("%s:%s", entrypoint.getBasicAuthCredentials().getUsername(),
                            entrypoint.getBasicAuthCredentials().getPassword())
                                                      .getBytes(StandardCharsets.UTF_8)));
              return chain.proceed(request.build());
            })
            .build();

    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(entrypoint.getUrl())
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(client)
                                  .build();
    return retrofit.create(HttpsClient.class);
  }
}
