package software.wings.helpers.ext.servicenow;

import com.fasterxml.jackson.databind.JsonNode;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ServiceNowRestClient {
  @GET("api/now/table/incident?sysparm_limit=1")
  Call<JsonNode> validateConnection(@Header("Authorization") String authorization);

  @Headers("Content-Type: application/json")
  @POST("api/now/table/{ticket-type}")
  Call<JsonNode> createTicket(@Header("Authorization") String authorization, @Path("ticket-type") String ticketType,
      @Query("sysparm_display_value") String displayValue, @Query("sysparm_fields") String returnFields,
      @Body Object jsonBody);

  @Headers("Content-Type: application/json")
  @POST("api/now/import/{staging-table-name}")
  Call<JsonNode> createImportSet(@Header("Authorization") String authorization,
      @Path("staging-table-name") String stagingTableName, @Query("sysparm_display_value") String displayValue,
      @Body Object jsonBody);

  @Headers("Content-Type: application/json")
  @PATCH("api/now/table/{ticket-type}/{ticket-id}")
  Call<JsonNode> updateTicket(@Header("Authorization") String authorization, @Path("ticket-type") String ticketType,
      @Path("ticket-id") String ticketId, @Query("sysparm_display_value") String displayValue,
      @Query("sysparm_fields") String returnFields, @Body Object jsonBody);

  @GET("api/now/table/{ticket-type}")
  Call<JsonNode> fetchChangeTasksFromCR(@Header("Authorization") String authorization,
      @Path("ticket-type") String ticketType, @Query("sysparm_fields") String returnFields,
      @Query("sysparm_query") String query);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHstate%5EnameSTARTSWITHincident%5EinactiveSTARTSWITHfalse%5ElanguageSTARTSWITHen")
  Call<JsonNode>
  getIncidentStates(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHrisk%5EnameSTARTSWITHchange_request%5EinactiveSTARTSWITHfalse%5ElanguageSTARTSWITHen")
  Call<JsonNode>
  getRisk(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHpriority%5EnameSTARTSWITHchange_request%5EinactiveSTARTSWITHfalse%5ElanguageSTARTSWITHen")
  Call<JsonNode>
  getPriority(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHimpact%5EinactiveSTARTSWITHfalse%5ElanguageSTARTSWITHen")
  Call<JsonNode>
  getImpact(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHurgency%5EinactiveSTARTSWITHfalse%5ElanguageSTARTSWITHen")
  Call<JsonNode>
  getUrgency(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHstate%5EnameSTARTSWITHproblem%5EinactiveSTARTSWITHfalse%5ElanguageSTARTSWITHen")
  Call<JsonNode>
  getProblemStates(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHstate%5EnameSTARTSWITHchange_request%5EinactiveSTARTSWITHfalse%5ElanguageSTARTSWITHen")
  Call<JsonNode>
  getChangeRequestStates(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHstate%5EnameSTARTSWITHchange_task%5EinactiveSTARTSWITHfalse%5ElanguageSTARTSWITHen")
  Call<JsonNode>
  getChangeTaskStates(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHtype%5EnameSTARTSWITHchange_request%5EinactiveSTARTSWITHfalse%5ElanguageSTARTSWITHen")
  Call<JsonNode>
  getChangeRequestTypes(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHchange_task_type%5EnameSTARTSWITHchange_task%5EinactiveSTARTSWITHfalse%5ElanguageSTARTSWITHen")
  Call<JsonNode>
  getChangeTaskTypes(@Header("Authorization") String authorization);

  @GET("/api/now/table/{ticketType}")
  Call<JsonNode> getIssue(@Header("Authorization") String authorization, @Path("ticketType") String ticketType,
      @Query("sysparm_query") String query, @Query("sysparm_display_value") String displayValue);

  @GET("api/now/doc/table/schema/{ticketType}")
  Call<JsonNode> getAdditionalFields(
      @Header("Authorization") String authorization, @Path("ticketType") String ticketType);
}
