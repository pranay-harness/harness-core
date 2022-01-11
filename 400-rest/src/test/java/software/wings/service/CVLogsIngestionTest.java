/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMillis;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.rules.Integration;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by rsingh on 7/20/18.
 */
@Integration
@Slf4j
public class CVLogsIngestionTest extends WingsBaseTest {
  CloseableHttpClient httpclient = HttpClients.createDefault();

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  @Ignore("This is used by QA to generate load for sumo")
  public void generateSumoLog() throws Exception {
    String sumoUrl = System.getProperty("sumo.url");
    if (isEmpty(sumoUrl)) {
      throw new IllegalArgumentException("sumo.url not provided");
    }

    String hostsArg = System.getProperty("sumo.hosts");
    if (isEmpty(sumoUrl)) {
      throw new IllegalArgumentException("sumo.hosts not provided");
    }

    String[] hosts = hostsArg.split(",");
    while (true) {
      for (String host : hosts) {
        HttpPost httpPost = new HttpPost(sumoUrl);
        httpPost.setEntity(new StringEntity("This is a new error \t This is a new exception"));
        httpPost.setHeader("X-Sumo-Name", host);
        httpPost.setHeader("X-Sumo-Host", host);
        log.info("sending log exception to sumo ");
        CloseableHttpResponse response = httpclient.execute(httpPost);
        log.info("status log sumo exception: " + response.getStatusLine());
        response.close();
        sleep(ofMillis(1000));
      }
    }
  }
}
