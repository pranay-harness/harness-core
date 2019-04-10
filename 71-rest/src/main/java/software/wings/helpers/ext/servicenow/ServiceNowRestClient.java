package software.wings.helpers.ext.servicenow;

import com.fasterxml.jackson.databind.JsonNode;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ServiceNowRestClient {
  @GET("api/now/table/incident?sysparm_limit=1")
  Call<JsonNode> validateConnection(@Header("Authorization") String authorization);

  @GET("/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHstate%5EnameSTARTSWITHincident")
  Call<JsonNode> getIncidentStates(@Header("Authorization") String authorization);

  @GET("/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHstate%5EnameSTARTSWITHproblem")
  Call<JsonNode> getProblemStates(@Header("Authorization") String authorization);

  @GET("/api/now/table/sys_choice?sysparm_query=elementSTARTSWITHstate%5EnameSTARTSWITHchange_request")
  Call<JsonNode> getChangeRequestStates(@Header("Authorization") String authorization);

  @GET("/api/now/table/{ticketType}")
  Call<JsonNode> getIssueId(@Header("Authorization") String authorization, @Path("ticketType") String ticketType,
      @Query("sysparm_query") String query);

  @GET("/api/now/table/{ticketType}")
  Call<JsonNode> getIssueStatus(@Header("Authorization") String authorization, @Path("ticketType") String ticketType,
      @Query("sysparm_query") String query, @Query("sysparm_display_value") String displayVale);
}
