/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.service.rest;

import javax.ws.rs.core.Response;
import junit.framework.AssertionFailedError;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.tools.ByteArrayContainerResponseWriter;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.service.test.AbstractResourceTest;

public class PeopleRestServiceTest  extends AbstractResourceTest {
  static private PeopleRestService peopleRestService;
  private Identity rootIdentity;
  private Identity johnIdentity;
  private RelationshipManager relationshipManager;
  private IdentityStorage identityStorage;

  public void setUp() throws Exception {
    super.setUp();
    peopleRestService = new PeopleRestService();
    identityStorage = (IdentityStorage) getContainer().getComponentInstanceOfType(IdentityStorage.class);
    relationshipManager = (RelationshipManager) getContainer().getComponentInstanceOfType(RelationshipManager.class);
    identityStorage.saveIdentity(rootIdentity);
    identityStorage.saveIdentity(johnIdentity);
    peopleRestService = new PeopleRestService();
    registry(peopleRestService);
  }

  public void tearDown() throws Exception {
    super.tearDown();
    identityStorage.deleteIdentity(rootIdentity);
    identityStorage.deleteIdentity(johnIdentity);
    unregistry(peopleRestService);
  }

  public void testSuggestUsernames() throws Exception {
      ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
      Relationship relationship1 = new Relationship(rootIdentity, johnIdentity, Relationship.Type.PENDING);
      relationshipManager.update(relationship1);
      startSessionAs("root");
      Relationship rootDemo = relationshipManager.get(rootIdentity, johnIdentity);
      ContainerResponse response = service("GET", "/social/people/suggest.json", "", null, null, writer);
      assertNotNull(response);
      assertEquals(200, response.getStatus());
      assertEquals("application/json;charset=utf-8", response.getContentType().toString());
      if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode())
          throw new AssertionFailedError("Service not found");
      endSession();
      relationshipManager.delete(relationship1);
  }
}
