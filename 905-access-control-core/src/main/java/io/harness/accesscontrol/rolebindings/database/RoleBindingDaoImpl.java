package io.harness.accesscontrol.rolebindings.database;

import io.harness.accesscontrol.rolebindings.RoleBindingDTO;
import io.harness.exception.DuplicateFieldException;

import com.google.inject.Inject;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;

public class RoleBindingDaoImpl implements RoleBindingDao {
  private final RoleBindingRepository roleBindingRepository;

  @Inject
  public RoleBindingDaoImpl(RoleBindingRepository roleBindingRepository) {
    this.roleBindingRepository = roleBindingRepository;
  }

  @Override
  public String create(io.harness.accesscontrol.rolebindings.RoleBindingDTO roleBindingDTO) {
    RoleBinding roleBinding = RoleBindingMapper.fromDTO(roleBindingDTO);
    try {
      return roleBindingRepository.save(roleBinding).getIdentifier();
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldException(
          String.format("A role binding with identifier %s in this scope %s is already present",
              roleBinding.getIdentifier(), roleBinding.getParentIdentifier()));
    }
  }

  @Override
  public Optional<io.harness.accesscontrol.rolebindings.RoleBindingDTO> get(
      String identifier, String parentIdentifier) {
    Optional<RoleBinding> roleBinding =
        roleBindingRepository.findByIdentifierAndParentIdentifier(identifier, parentIdentifier);
    return roleBinding.flatMap(r -> Optional.of(RoleBindingMapper.toDTO(r)));
  }

  @Override
  public String update(RoleBindingDTO permissionDTO) {
    return null;
  }

  @Override
  public void delete(String identifier, String parentIdentifier) {}
}
