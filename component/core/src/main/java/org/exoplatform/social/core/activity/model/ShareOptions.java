/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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
package org.exoplatform.social.core.activity.model;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.social.core.identity.model.Identity;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Apr 8, 2015  
 */
public class ShareOptions {
  
  public enum ShareType {
    SHARE_CONNECTIONS, NO_CHANGE, UNSHARE_CONNECTIONS
  }
  
  private boolean isShareConnections;
  private List<String> spaces;
  private Identity sharer;
  
  public ShareOptions(Identity sharer) {
    this.isShareConnections = false;
    this.spaces = new ArrayList<String>();
    this.setSharer(sharer);
  }
  
  public ShareOptions(boolean isShareConnections, List<String> spaces, Identity sharer) {
    this.isShareConnections = isShareConnections;
    this.spaces = spaces;
    this.setSharer(sharer);
  }
  
  public boolean isShareConnections() {
    return isShareConnections;
  }
  
  public void setShareConnections(boolean isShareConnections) {
    this.isShareConnections = isShareConnections;
  }
  
  public List<String> getSpaces() {
    return spaces;
  }
  
  public void setSpaces(List<String> spaces) {
    this.spaces = spaces;
  }

  public Identity getSharer() {
    return sharer;
  }

  public void setSharer(Identity sharer) {
    this.sharer = sharer;
  }

  public List<String> addedSpaces(ShareOptions newShareOptions) {
    List<String> added = new ArrayList<String>();
    for (String space : newShareOptions.getSpaces()) {
      if (! this.getSpaces().contains(space)) {
        added.add(space);
      }
    }
    return added;
  }

  public List<String> removedSpaces(ShareOptions newShareOptions) {
    List<String> removed = new ArrayList<String>();
    for (String space : this.getSpaces()) {
      if (! newShareOptions.getSpaces().contains(space)) {
        removed.add(space);
      }
    }
    return removed;
  }
  
  public ShareType getShareType(ShareOptions newShareOptions) {
    if (this.isShareConnections() && ! newShareOptions.isShareConnections()) {
      return ShareType.UNSHARE_CONNECTIONS;
    }
    if (! this.isShareConnections() && newShareOptions.isShareConnections()) {
      return ShareType.SHARE_CONNECTIONS;
    }
    return ShareType.NO_CHANGE;
  }
}
