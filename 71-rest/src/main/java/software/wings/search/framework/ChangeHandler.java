package software.wings.search.framework;

import software.wings.search.framework.changestreams.ChangeEvent;

/**
 * The changeHandler interface each search entity
 * would implement.
 *
 * @author utkarsh
 */

public interface ChangeHandler { boolean handleChange(ChangeEvent changeEvent); }
