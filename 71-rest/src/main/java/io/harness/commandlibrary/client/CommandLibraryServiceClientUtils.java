package io.harness.commandlibrary.client;

import io.harness.exception.GeneralException;
import io.harness.network.SafeHttpCall;
import lombok.experimental.UtilityClass;
import retrofit2.Call;

import java.io.IOException;

@UtilityClass
public class CommandLibraryServiceClientUtils {
  public static <T> T executeHttpRequest(Call<T> call) {
    try {
      return SafeHttpCall.execute(call);
    } catch (IOException e) {
      throw new GeneralException("error while executing request", e);
    }
  }
}
