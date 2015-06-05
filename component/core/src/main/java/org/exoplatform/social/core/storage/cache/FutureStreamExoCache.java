/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.storage.cache;

import java.io.Serializable;

import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.social.core.storage.cache.model.data.AbstractStreamListData;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Apr 15, 2014  
 */
public class FutureStreamExoCache<T, K extends Serializable, V extends AbstractStreamListData<K, T>, C> extends FutureStreamCache<K, V, C> {

  /** . */
  private final ExoCache<K, V> cache;

  public FutureStreamExoCache(Loader<K, V, C> loader, ExoCache<K, V> cache) {
    super(loader);

    //
    this.cache = cache;
  }

  public void clear() {
    cache.clearCache();
  }

  public void remove(K key) {
    cache.remove(key);
  }

  @Override
  protected V get(K key, long offset, long limit) {
    V v = cache.get(key);
    if (v == null) return null;
    //find better way to handle
    int newSize = (int)(offset + limit);
    //Fixed the use case:
    //precondition: there are more than 20 activities
    //1. loads AS
    //2. clear caching by jconsole
    //3. load more by scroll end of the page
    //4. refresh AS
    //Expected: expected shows 20 activities in first page.
    if (v.size() < newSize) return null;
    
    int i =0;
    for(i = (int)offset; i < limit && i < v.size(); i++) {
      if (v.getList().get(i) == null) {
        return null;
      }
    }
    //return (v.size() >= newSize && (v.getList().get((int)offset) != null || v.getList().get(newSize - 1) != null)) ? v : null;
    return v;
  }

  @Override
  protected void put(K key, V entry) {
    cache.put(key, entry);
  }
}