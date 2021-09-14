/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.authenticationservice.recaptcha;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface ReCaptchaClient {
  @POST("recaptcha/api/siteverify?")
  Call<VerificationStatus> siteverify(@Query("secret") String secret, @Query("response") String captchaResponse);
}
