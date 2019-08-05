package software.wings.security.authentication.recaptcha;

import com.google.inject.Singleton;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.annotation.concurrent.ThreadSafe;

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
