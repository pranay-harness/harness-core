package io.harness.accesscontrol.rolebindings.database;

import io.harness.accesscontrol.rolebindings.RoleBindingDTO;

import java.util.Optional;
import javax.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleBindingDao {
  String create(@Valid io.harness.accesscontrol.rolebindings.RoleBindingDTO roleBindingDTO);

  Optional<io.harness.accesscontrol.rolebindings.RoleBindingDTO> get(
      @NotEmpty String identifier, @NotEmpty String parentIdentifier);

  String update(@Valid RoleBindingDTO permissionDTO);

  void delete(@NotEmpty String identifier, @NotEmpty String parentIdentifier);
}