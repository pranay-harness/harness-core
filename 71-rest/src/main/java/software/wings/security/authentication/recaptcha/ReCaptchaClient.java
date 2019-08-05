package software.wings.security.authentication.recaptcha;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ReCaptchaClient {
  @POST("recaptcha/api/siteverify?")
  Call<VerificationStatus> siteverify(@Query("secret") String secret, @Query("response") String captchaResponse);
}
