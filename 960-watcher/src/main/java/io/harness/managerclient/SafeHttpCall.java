package io.harness.managerclient;

import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;

public class SafeHttpCall {
  public static <T> T execute(Call<T> call) throws IOException {
    Response<T> response = null;
    try {
      response = call.execute();
      return response.body();
    } finally {
      if (response != null && !response.isSuccessful()) {
        response.errorBody().close();
      }
    }
  }
}
