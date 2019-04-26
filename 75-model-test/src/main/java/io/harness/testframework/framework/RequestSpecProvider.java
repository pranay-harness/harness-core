package io.harness.testframework.framework;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestSpecProvider {
  public RequestSpecification useDefaultSpec() {
    String host = System.getProperty("server.host", "https://localhost");
    String basePath = System.getProperty("server.base", "/api");
    RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
    requestSpecBuilder.setAccept(ContentType.JSON);
    requestSpecBuilder.setContentType(ContentType.JSON);
    requestSpecBuilder.setBaseUri(host);
    requestSpecBuilder.setBasePath(basePath);
    String port = System.getProperty("server.port", "9090");
    if (!port.equals("0000")) {
      requestSpecBuilder.setPort(Integer.parseInt(port));
    }
    logger.info("Querying environment : " + host + basePath + ":" + port);
    if (port.equals("0000")) {
      logger.info(
          "This querying environment would use the default port for the service. This option is good to be used in non local envs such as QA");
    }
    return requestSpecBuilder.build();
  }

  public RequestSpecification useEmailSpec() {
    RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
    requestSpecBuilder.setAccept(ContentType.JSON);
    requestSpecBuilder.setContentType(ContentType.JSON);
    requestSpecBuilder.setBaseUri("https://api.guerrillamail.com");
    requestSpecBuilder.setBasePath("/ajax.php");
    RestAssured.useRelaxedHTTPSValidation();
    return requestSpecBuilder.build();
  }

  public RequestSpecification useMailinatorSpec(String decryptedToken) {
    RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
    requestSpecBuilder.setAccept(ContentType.JSON);
    requestSpecBuilder.setContentType(ContentType.JSON);
    requestSpecBuilder.setBaseUri("https://api.mailinator.com");
    requestSpecBuilder.setBasePath("/api");
    requestSpecBuilder.addQueryParam("token", decryptedToken);
    requestSpecBuilder.addQueryParam("private_domain", "true");
    RestAssured.useRelaxedHTTPSValidation();
    return requestSpecBuilder.build();
  }
}
