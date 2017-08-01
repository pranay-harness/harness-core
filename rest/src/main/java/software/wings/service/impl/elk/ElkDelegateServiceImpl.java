package software.wings.service.impl.elk;

import org.apache.commons.codec.binary.Base64;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.ElkConfig;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.elk.ElkRestClient;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.utils.JsonUtils;

import java.nio.charset.StandardCharsets;

/**
 * Created by rsingh on 8/01/17.
 */
public class ElkDelegateServiceImpl implements ElkDelegateService {
  @Override
  public void validateConfig(ElkConfig elkConfig) {
    try {
      final Call<ElkAuthenticationResponse> request =
          getElkRestClient(elkConfig).authenticate(getHeaderWithCredentials(elkConfig));
      final Response<ElkAuthenticationResponse> response = request.execute();
      if (response.isSuccessful()) {
        return;
      }

      throw new WingsException(
          JsonUtils.asObject(response.errorBody().string(), ElkAuthenticationResponse.class).getError().getReason());
    } catch (Throwable t) {
      throw new WingsException(t.getMessage());
    }
  }

  private ElkRestClient getElkRestClient(final ElkConfig elkConfig) {
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl("http://" + elkConfig.getHost() + ":" + elkConfig.getPort() + "/")
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .build();
    return retrofit.create(ElkRestClient.class);
  }

  private String getHeaderWithCredentials(ElkConfig elkConfig) {
    return "Basic "
        + Base64.encodeBase64String(String.format("%s:%s", elkConfig.getUsername(), new String(elkConfig.getPassword()))
                                        .getBytes(StandardCharsets.UTF_8));
  }
}
