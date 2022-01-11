/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.managerclient;

import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;

public class SafeHttpCall {
  public static <T> T execute(Call<T> call) throws IOException {
    Response<T> response = null;
    try {
      response = call.execute();
      return response.body();
    } finally {
      if (response != null && !response.isSuccessful()) {
        response.errorBody().close();
      }
    }
  }
}
