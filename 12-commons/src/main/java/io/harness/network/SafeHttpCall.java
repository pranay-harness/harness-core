package io.harness.network;

import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

/**
 * Created by peeyushaggarwal on 3/16/17.
 */
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
