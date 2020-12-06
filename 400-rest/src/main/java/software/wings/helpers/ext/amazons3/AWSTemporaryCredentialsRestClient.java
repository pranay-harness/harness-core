package software.wings.helpers.ext.amazons3;

import software.wings.beans.AWSTemporaryCredentials;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface AWSTemporaryCredentialsRestClient {
  @GET("/latest/meta-data/iam/security-credentials/") Call<ResponseBody> getRoleName();

  @GET("/latest/meta-data/iam/security-credentials/{roleName}")
  Call<AWSTemporaryCredentials> getTemporaryCredentials(@Path("roleName") String roleName);
}
