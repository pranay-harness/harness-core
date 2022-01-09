/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.authenticationservice.recaptcha;

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
