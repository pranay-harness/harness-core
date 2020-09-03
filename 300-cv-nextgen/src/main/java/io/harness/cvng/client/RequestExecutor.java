package io.harness.cvng.client;

import com.google.inject.Singleton;

import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

@Singleton
public class RequestExecutor {
  // TODO: enhance it to handle exceptions, stacktraces and retries based on response code and exception from manager.
  public <U> U execute(Call<U> request) {
    try {
      Response<U> response = request.clone().execute();
      if (response.isSuccessful()) {
        return response.body();
      } else {
        throw new IllegalStateException("Response code: " + response.code() + ", Message: " + response.message()
            + ", Error: " + response.errorBody().string());
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
