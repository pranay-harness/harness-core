/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import com.amazonaws.AmazonWebServiceClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class CloseableAmazonWebServiceClient<T extends AmazonWebServiceClient> implements AutoCloseable {
  private T amazonWebServiceClient;

  public CloseableAmazonWebServiceClient(T amazonWebServiceClient) {
    this.amazonWebServiceClient = amazonWebServiceClient;
  }

  @Override
  public void close() throws Exception {
    if (amazonWebServiceClient != null) {
      try {
        amazonWebServiceClient.shutdown();
      } catch (Exception e) {
        /*
        Eating the exception here as failure to shutdown AmazonWebServiceClient should not fail the workflow
        This is done keeping in mind that there is no change in behavior post addition of AmazonWebServiceClient
        shutdown.
         */
        log.warn("Failed to shutdown AmazonWebServiceClient. Possible leak of resources", e);
      }
    }
  }

  public T getClient() {
    return amazonWebServiceClient;
  }
}
