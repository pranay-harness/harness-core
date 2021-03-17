package io.harness.cvng.state;

import com.google.inject.Singleton;
import groovy.util.logging.Slf4j;
import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;
@Singleton
@Slf4j
public class CVNGRequestExecutor {
  public <U> U execute(Call<U> request) {
    try {
      Response<U> response = request.clone().execute();
      if (response.isSuccessful()) {
        return response.body();
      } else {
        String errorBody = response.errorBody().string();
        throw new IllegalStateException(
            "Code: " + response.code() + ", message: " + response.message() + ", body: " + errorBody);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
