/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.schema.resource;

import static io.harness.exception.WingsException.USER;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.schema.YamlBaseUrlService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;

@Api("/yamlschema")
@Path("yamlschema")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@NextGenManagerAuth
@Deprecated
public class YamlSchemaResource {
  @Inject YamlBaseUrlService yamlBaseUrlService;

  @GET
  @Path("schemafile")
  @ApiOperation(value = "Get Schema for the given entity type", nickname = "schemafile")
  //  @PublicApi
  @Encoded
  @Produces(APPLICATION_OCTET_STREAM)
  public Response getSchemaFile(@QueryParam("filename") String fileName) {
    try {
      String fileNameDecoded = URLDecoder.decode(fileName, Charsets.UTF_8.name());
      if (fileNameDecoded.endsWith("/")) {
        fileNameDecoded = fileNameDecoded.substring(0, fileNameDecoded.length() - 1);
      }
      try (InputStream inputStream = getResourceAsStream(fileNameDecoded)) {
        File file = File.createTempFile("temp", ".json");
        copyInputStreamToFile(inputStream, file);
        String fileToString = FileUtils.readFileToString(file, Charsets.UTF_8.name());
        String updatedString = modifyPlaceholders(fileToString);
        File updatedFile = File.createTempFile("manual", ".json");
        FileUtils.writeStringToFile(updatedFile, updatedString, Charsets.UTF_8.name());
        return buildResponse(fileNameDecoded, updatedFile);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  Response buildResponse(String fileNameDecoded, File updatedFile) {
    ResponseBuilder response = Response.ok(updatedFile, APPLICATION_OCTET_STREAM);
    response.header("Content-Disposition", "attachment; filename=" + fileNameDecoded);
    return response.build();
  }

  @VisibleForTesting
  InputStream getResourceAsStream(String fileNameDecoded) {
    return getClass().getClassLoader().getResourceAsStream(fileNameDecoded);
  }

  // todo(abhinav): ensure that things dont go to infinite loops
  private String modifyPlaceholders(String fileToString) {
    try {
      final URL baseUrl = new URL(yamlBaseUrlService.getBaseUrl());
      final String host = baseUrl.getHost();
      final int port = baseUrl.getPort();
      final String mainPath = getClass().getAnnotation(Api.class).value();
      final String urlPath =
          YamlSchemaResource.class.getMethod("getSchemaFile", String.class).getAnnotation(Path.class).value();
      Annotation[][] parameterAnnotations =
          YamlSchemaResource.class.getMethod("getSchemaFile", String.class).getParameterAnnotations();
      final String queryParam = ((QueryParam) parameterAnnotations[0][0]).value();
      final String portInUrl = port != -1 ? (":" + port) : "";
      String urlWithQueryParam = host + portInUrl + mainPath + "/" + urlPath + "?" + queryParam + "=";
      String url = baseUrl.getProtocol() + "://" + URLEncoder.encode(urlWithQueryParam, Charsets.UTF_8.name());
      Pattern pattern = Pattern.compile("<<(.*?)>>");
      Matcher matcher = pattern.matcher(fileToString);
      while (matcher.find()) {
        String a = matcher.group();
        String c = a.substring(2, a.length() - 2);
        fileToString = fileToString.replace(a, url + URLEncoder.encode(c, Charsets.UTF_8.name()));
      }
      return fileToString;
    } catch (Exception e) {
      throw new InvalidRequestException(e.getMessage(), USER);
    }
  }

  @VisibleForTesting
  void copyInputStreamToFile(InputStream inputStream, File file) {
    try (FileOutputStream outputStream = new FileOutputStream(file)) {
      int read;
      byte[] bytes = new byte[1024];
      while ((read = inputStream.read(bytes)) != -1) {
        outputStream.write(bytes, 0, read);
      }
    } catch (Exception e) {
      throw new InvalidRequestException(e.getMessage(), USER);
    }
  }
}
