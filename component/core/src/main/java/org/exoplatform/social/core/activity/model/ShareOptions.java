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
  
  private boolean isShareConnections;
  private List<String> spaceIds;
  private Identity sharer;
  
  public ShareOptions(Identity sharer) {
    this.isShareConnections = false;
    this.spaceIds = new ArrayList<String>();
    this.setSharer(sharer);
  }
  
  public ShareOptions(boolean isShareConnections, List<String> spaceIds, Identity sharer) {
    this.isShareConnections = isShareConnections;
    this.spaceIds = spaceIds;
    this.setSharer(sharer);
  }
  
  public boolean isShareConnections() {
    return isShareConnections;
  }
  
  public void setShareConnections(boolean isShareConnections) {
    this.isShareConnections = isShareConnections;
  }
  
  public List<String> getSpaceIds() {
    return spaceIds;
  }
  
  public void setSpaceIds(List<String> spaceIds) {
    this.spaceIds = spaceIds;
  }

  public Identity getSharer() {
    return sharer;
  }

  public void setSharer(Identity sharer) {
    this.sharer = sharer;
  }


}
