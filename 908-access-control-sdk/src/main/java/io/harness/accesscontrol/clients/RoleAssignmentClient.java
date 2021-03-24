package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface RoleAssignmentClient {
  @POST("/roleassignments/multi")
  Call<ResponseDTO<List<RoleAssignmentResponseDTO>>> createMulti(@Query("accountIdentifier") String accountIdentifier,
      @Query("orgIdentifier") String orgIdentifier, @Query("projectIdentifier") String projectIdentifier,
      @Body RoleAssignmentCreateRequestDTO roleAssignmentCreateRequestDTO);

  @POST("/roleassignments")
  Call<ResponseDTO<RoleAssignmentResponseDTO>> create(@Query("accountIdentifier") String accountIdentifier,
      @Query("orgIdentifier") String orgIdentifier, @Query("projectIdentifier") String projectIdentifier,
      @Body RoleAssignmentDTO roleAssignmentDTO);
}
