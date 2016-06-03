package software.wings.service.intfc;

import software.wings.beans.Infra;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

// TODO: Auto-generated Javadoc

/**
 * The Interface InfraService.
 */
public interface InfraService {
  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Infra> list(PageRequest<Infra> pageRequest);

  /**
   * Save.
   *
   * @param infra the infra
   * @return the infra
   */
  public Infra save(Infra infra);

  /**
   * Delete.
   *
   * @param infraId the infra id
   */
  void delete(String infraId);
}
