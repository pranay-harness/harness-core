package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.beans.ConfigFile;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.service.intfc.ServiceResourceService;

import java.io.InputStream;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Created by anubhaw on 3/25/16.
 */
@Path("/services")
@Timed
@ExceptionMetered
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ServiceResource {
  @Inject private ServiceResourceService srs;

  @GET
  @Path("{appId}")
  public RestResponse<List<Service>> list(@PathParam("appId") String appId) {
    return new RestResponse<>(srs.list(appId));
  }

  @POST
  @Path("{appId}")
  public RestResponse<Service> save(@PathParam("appId") String appId, Service service) {
    return new RestResponse<>(srs.save(appId, service));
  }

  @PUT
  @Path("{appId}/{serviceId}")
  public RestResponse<Service> update(
      @PathParam("appId") String appId, @PathParam("serviceId") String serviceId, Service service) {
    service.setUuid(serviceId);
    return new RestResponse<>(srs.update(service));
  }

  @POST
  @Path("{serviceId}/configs")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> uploadConfig(@PathParam("serviceId") String serviceId,
      @FormDataParam("fileName") String fileName, @FormDataParam("relativePath") String relativePath,
      @FormDataParam("md5") String md5, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    ConfigFile configFile = new ConfigFile(serviceId, fileName, relativePath, md5);
    String fileId = srs.saveFile(configFile, uploadedInputStream, CONFIGS);
    return new RestResponse<>(fileId);
  }

  @GET
  @Path("{serviceId}/configs")
  public RestResponse<List<ConfigFile>> fetchConfigs(@PathParam("serviceId") String serviceId) {
    return new RestResponse<>(srs.getConfigs(serviceId));
  }

  @GET
  @Path("{serviceId}/configs/{configId}")
  public RestResponse<ConfigFile> fetchConfig(@PathParam("configId") String configId) {
    return new RestResponse<>(srs.getConfig(configId));
  }

  @PUT
  @Path("{serviceId}/configs/{configId}")
  @Consumes(MULTIPART_FORM_DATA)
  public void updateConfig(@PathParam("serviceId") String serviceId, @PathParam("configId") String configId,
      @FormDataParam("fileName") String fileName, @FormDataParam("relativePath") String relativePath,
      @FormDataParam("md5") String md5, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    ConfigFile configFile = new ConfigFile(serviceId, fileName, relativePath, md5);
    configFile.setUuid(configId);
    srs.updateFile(configFile, uploadedInputStream, CONFIGS);
  }
}
