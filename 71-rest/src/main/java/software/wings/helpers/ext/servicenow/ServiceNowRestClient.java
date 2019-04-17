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
  @PATCH("api/now/table/{ticket-type}/{ticket-id}")
  Call<JsonNode> updateTicket(@Header("Authorization") String authorization, @Path("ticket-type") String ticketType,
      @Path("ticket-id") String ticketId, @Query("sysparm_display_value") String displayValue,
      @Query("sysparm_fields") String returnFields, @Body Object jsonBody);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHstate%5EnameSTARTSWITHincident%5EinactiveSTARTSWITHfalse")
  Call<JsonNode>
  getIncidentStates(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHrisk%5EnameSTARTSWITHchange_request%5EinactiveSTARTSWITHfalse")
  Call<JsonNode>
  getRisk(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHpriority%5EnameSTARTSWITHchange_request%5EinactiveSTARTSWITHfalse")
  Call<JsonNode>
  getPriority(@Header("Authorization") String authorization);

  @GET("/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHimpact%5EinactiveSTARTSWITHfalse")
  Call<JsonNode> getImpact(@Header("Authorization") String authorization);

  @GET("/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHurgency%5EinactiveSTARTSWITHfalse")
  Call<JsonNode> getUrgency(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHstate%5EnameSTARTSWITHproblem%5EinactiveSTARTSWITHfalse")
  Call<JsonNode>
  getProblemStates(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHstate%5EnameSTARTSWITHchange_request%5EinactiveSTARTSWITHfalse")
  Call<JsonNode>
  getChangeRequestStates(@Header("Authorization") String authorization);

  @GET(
      "/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHtype%5EnameSTARTSWITHchange_request%5EinactiveSTARTSWITHfalse")
  Call<JsonNode>
  getChangeRequestTypes(@Header("Authorization") String authorization);

  @GET("/api/now/table/{ticketType}")
  Call<JsonNode> getIssueId(@Header("Authorization") String authorization, @Path("ticketType") String ticketType,
      @Query("sysparm_query") String query);

  @GET("/api/now/table/{ticketType}")
  Call<JsonNode> getIssueStatus(@Header("Authorization") String authorization, @Path("ticketType") String ticketType,
      @Query("sysparm_query") String query, @Query("sysparm_display_value") String displayValue);
}
