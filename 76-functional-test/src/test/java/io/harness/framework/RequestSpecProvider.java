package io.harness.framework;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public class RequestSpecProvider {
  public RequestSpecification useDefaultSpec() {
    RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
    requestSpecBuilder.setAccept(ContentType.JSON);
    requestSpecBuilder.setContentType(ContentType.JSON);
    requestSpecBuilder.setBaseUri(System.getProperty("server.host", "https://localhost"));
    requestSpecBuilder.setBasePath(System.getProperty("server.base", "/api"));
    String port = System.getProperty("server.port", "9090");
    requestSpecBuilder.setPort(Integer.parseInt(port));
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
