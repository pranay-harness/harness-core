/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.resource;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.Tester;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SampleBean;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("test")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@OwnedBy(DX)
public class TestResource {
  @Inject Tester tester;

  @POST
  public ResponseDTO<SampleBean> get() {
    final SampleBean save = tester.save();
    return ResponseDTO.newResponse(save);
  }
}
