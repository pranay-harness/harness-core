package software.wings.security.authentication.recaptcha;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import javax.annotation.concurrent.ThreadSafe;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@OwnedBy(PL)
@Singleton
@ThreadSafe
public class ReCaptchaClientBuilder {
  private ReCaptchaClient client;

  public synchronized ReCaptchaClient getInstance() {
    if (null != client) {
      return client;
    }

    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl("https://www.google.com/")
                            .client(new OkHttpClient())
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();

    this.client = retrofit.create(ReCaptchaClient.class);
    return client;
  }
}
