/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.modules.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface ModulesClient {
  String MODULES_API = "ng/modules";

  @GET(MODULES_API)
  Call<RestResponse<List<ModuleType>>> getEnabledModules(@Query(value = "accountId") String accountId);
}
