/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
