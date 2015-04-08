/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.social.core.chromattic.entity;

import org.chromattic.api.annotations.Id;
import org.chromattic.api.annotations.Name;
import org.chromattic.api.annotations.Path;
import org.chromattic.api.annotations.PrimaryType;
import org.chromattic.api.annotations.Property;
import org.exoplatform.social.core.storage.query.PropertyLiteralExpression;


@PrimaryType(name = "soc:sharer")
public abstract class SharerEntity implements NamedEntity {

  @Name
  public abstract String getName();
  public abstract void setName(String name);
  
  @Path
  public abstract String getPath();
  
  @Id
  public abstract String getId();
  
  @Property(name = "soc:myConnection")
  public abstract Boolean getMyConnection();
  public abstract void setMyConnection(Boolean myConnection);
  public static final PropertyLiteralExpression<String> myConnection =
      new PropertyLiteralExpression<String>(String.class, "soc:myConnection");
  
  @Property(name = "soc:spaces")
  public abstract String[] getSpaces();
  public abstract void setMySpace(String[] mySpaces);
  public static final PropertyLiteralExpression<String> mySpaces =
      new PropertyLiteralExpression<String>(String.class, "soc:spaces");
  
  
  @Override
  public String toString() {
    return String.format("SharerEntity{path=%s, name=%s}",
                         getPath(),
                         getName());
  }
}