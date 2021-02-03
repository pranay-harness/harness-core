package io.harness.accesscontrol.rolebindings;

import java.util.Optional;
import javax.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;

public interface RoleBindingService {
  String create(@Valid RoleBindingDTO roleBindingDTO);

  Optional<RoleBindingDTO> get(@NotEmpty String identifier, @NotEmpty String parentIdentifier);

  String update(@Valid RoleBindingDTO roleBindingDTO);

  void delete(@NotEmpty String identifier, @NotEmpty String parentIdentifier);
}
