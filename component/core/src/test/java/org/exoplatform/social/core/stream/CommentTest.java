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
package org.exoplatform.social.core.stream;

import java.util.List;
import java.util.ListIterator;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.storage.cache.model.key.IdentityKey;
import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.streams.event.DataChangeMerger;
import org.exoplatform.social.core.storage.streams.event.StreamChange;
import org.exoplatform.social.core.stream.data.ActivityDataBuilder;
import org.exoplatform.social.core.stream.data.CachedActivityData;
import org.exoplatform.social.core.stream.data.CachedRelationshipData;
import org.exoplatform.social.core.stream.data.CommentDataBuilder;
/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 20, 2014  
 */
public class CommentTest extends BaseTest {
  
  @Override
  protected void tearDown() throws Exception {
    DataChangeMerger.reset();
  }
  
  @Override
  public void initData() {
    if (CachedActivityData.feedSize(demo) == 0) {
      ActivityDataBuilder.initMore(50, demo).inject();
      List<ExoSocialActivity> feed = CachedActivityData.feed(demo, 0, 20);
      ListIterator<ExoSocialActivity> it = feed.listIterator(feed.size() - 1);
      while (it.hasPrevious()) {
        ExoSocialActivity activity = it.previous();
        CommentDataBuilder.initMore(10, activity, demo).inject();
      }
    }
  }
  
  @Override
  public void initConnecions() {
    List<IdentityKey> demoConnections = CachedRelationshipData.getConnections(demo);
    if (demoConnections == null) {
      CachedRelationshipData.addRelationship(demo, john);
      CachedRelationshipData.addRelationship(demo, mary);
    }
  }

  public void testAddComments() throws Exception {
    List<ExoSocialActivity> feed = CachedActivityData.feed(demo, 0, 20);
    ExoSocialActivity first = feed.get(0);
    ExoSocialActivity second = feed.get(1);
    assertTrue(first.getTitle().indexOf("49") > 0);
    assertTrue(second.getTitle().indexOf("48") > 0);
    List<ExoSocialActivity> comments = CachedActivityData.getComments(first.getId());
    assertEquals(10, comments.size());

    {
      //test What's hot feature
      CommentDataBuilder.initMore(5, second, demo).inject();
      comments = CachedActivityData.getComments(second.getId());
      assertEquals(15, comments.size());
      
      //
      feed = CachedActivityData.feed(demo, 0, 20);
      first = feed.get(0);
      assertTrue(first.getTitle().indexOf("48") > 0);
      
      List<StreamChange<StreamKey, String>> changes = CachedActivityData.feedChangeList(demo);
      assertEquals(69, changes.size());

      //
      changes = CachedActivityData.feedChangeList(demo, StreamChange.Kind.MOVE);
      assertEquals(19, changes.size());
    }
    
    CachedActivityData.reset();
  }
  
  public void testAddCommentRelationship() throws Exception {
    CachedActivityData.reset();
    //
    ActivityDataBuilder.initMore(1, demo).inject();
    
    List<StreamChange<StreamKey, String>> changes = CachedActivityData.feedChangeList(demo);
    
    printDebug(changes);
    
    assertEquals(1, changes.size());

    changes = CachedActivityData.feedChangeList(john, StreamChange.Kind.ADD);
    assertEquals(1, changes.size());
    
    CachedActivityData.reset();
  }
  
  private void printDebug(List<StreamChange<StreamKey, String>> changes) {
    for(StreamChange<StreamKey, String> change : changes) {
      System.out.println(change.toString());
    }
  }
  
  public void testComplex() throws Exception {
    CachedActivityData.reset();
    //
    ActivityDataBuilder.initMore(2, demo).inject();
    
    List<StreamChange<StreamKey, String>> changes = CachedActivityData.feedChangeList(demo, StreamChange.Kind.ADD);
    assertEquals(2, changes.size());

    {
      //john
      changes = CachedActivityData.feedChangeList(john, StreamChange.Kind.ADD);
      assertEquals(2, changes.size());
      
      changes = CachedActivityData.connectionsChangeList(john, StreamChange.Kind.ADD);
      assertEquals(2, changes.size());
    }
    
    {
      //mary
      changes = CachedActivityData.feedChangeList(mary, StreamChange.Kind.ADD);
      assertEquals(2, changes.size());
      
      changes = CachedActivityData.connectionsChangeList(mary, StreamChange.Kind.ADD);
      assertEquals(2, changes.size());
      
    }
    
    //remove here
    List<ExoSocialActivity> feed = CachedActivityData.feed(demo, 0, 20);
    ExoSocialActivity first = feed.get(0);
    CachedActivityData.removeActivity(first.getId());
    changes = CachedActivityData.feedChangeList(john, StreamChange.Kind.ADD);
    assertEquals(1, changes.size());
    //
    changes = CachedActivityData.feedChangeList(john, StreamChange.Kind.DELETE);
    assertEquals(0, changes.size());
    
    CachedActivityData.reset();
  }
  
  
}
