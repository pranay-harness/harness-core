package software.wings.service.intfc;

import software.wings.beans.Infra;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

public interface InfraService {
  PageResponse<Infra> list(PageRequest<Infra> pageRequest);

  public Infra save(Infra infra);

  void delete(String infraId);
}
