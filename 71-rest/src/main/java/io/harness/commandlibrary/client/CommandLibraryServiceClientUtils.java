package io.harness.commandlibrary.client;

import static javax.ws.rs.core.Response.status;

import io.harness.exception.GeneralException;
import io.harness.network.SafeHttpCall;
import lombok.experimental.UtilityClass;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import javax.ws.rs.core.Response.ResponseBuilder;

@UtilityClass
public class CommandLibraryServiceClientUtils {
  public static <T> T executeHttpRequest(Call<T> call) {
    try {
      return SafeHttpCall.executeWithExceptions(call);
    } catch (IOException e) {
      throw new GeneralException("error while executing request", e);
    }
  }

  public static <T> javax.ws.rs.core.Response executeAndCreatePassThroughResponse(Call<T> call) {
    try {
      final Response<T> response = call.execute();
      if (response.isSuccessful()) {
        return createSuccessResponse(response);
      }
      return createFailureResponse(response);

    } catch (Exception e) {
      throw new GeneralException("error while processing request", e);
    }
  }

  private static <T> javax.ws.rs.core.Response createFailureResponse(Response<T> response) throws IOException {
    final ResponseBody errorBody = response.errorBody();
    final ResponseBuilder responeBuilder = status(response.code());
    if (errorBody != null) {
      responeBuilder.type(errorBody.contentType().toString()).entity(errorBody.string());
    }
    return responeBuilder.build();
  }

  private static <T> javax.ws.rs.core.Response createSuccessResponse(Response<T> response) {
    return status(response.code()).entity(response.body()).build();
  }
}
