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
package org.exoplatform.social.core.storage;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.storage.api.ActivityStreamStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.exoplatform.social.core.storage.streams.StreamContext;
import org.exoplatform.social.core.test.AbstractCoreTest;
import org.exoplatform.social.core.updater.UserActivityStreamUpdaterPlugin;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 13, 2013  
 */
public class LazyActivityStorageTest extends AbstractCoreTest {
  
  private final Log LOG = ExoLogger.getLogger(LazyActivityStorageTest.class);
  
  private IdentityStorage identityStorage;
  private ActivityStorageImpl activityStorageImpl;
  //private ActivityManager activityManager;
  private ActivityStreamStorage streamStorage;
  private List<ExoSocialActivity> tearDownActivityList;

  private Identity rootIdentity;
  private Identity maryIdentity;
  
 
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    identityStorage = (IdentityStorage) getContainer().getComponentInstanceOfType(IdentityStorage.class);
    activityStorageImpl = (ActivityStorageImpl) getContainer().getComponentInstanceOfType(ActivityStorageImpl.class);
    //activityManager = (ActivityManager) getContainer().getComponentInstanceOfType(ActivityManager.class);
    streamStorage = (ActivityStreamStorage) getContainer().getComponentInstanceOfType(ActivityStreamStorage.class);
    
    activityStorageImpl.setInjectStreams(false);
    StreamContext.instanceInContainer().switchSchedulerOnOff(false);
    
    //
    assertNotNull("identityManager must not be null", identityStorage);
    assertNotNull("activityStorage must not be null", activityStorageImpl);
    rootIdentity = new Identity(OrganizationIdentityProvider.NAME, "root");
    maryIdentity = new Identity(OrganizationIdentityProvider.NAME, "mary");
    
    identityStorage.saveIdentity(rootIdentity);
    identityStorage.saveIdentity(maryIdentity);

    tearDownActivityList = new ArrayList<ExoSocialActivity>();
  }

  @Override
  protected void tearDown() throws Exception {
    for (ExoSocialActivity activity : tearDownActivityList) {
      activityStorageImpl.deleteActivity(activity.getId());
    }
    identityStorage.deleteIdentity(rootIdentity);
    identityStorage.deleteIdentity(maryIdentity);
    
    activityStorageImpl.setInjectStreams(true);
    StreamContext.instanceInContainer().switchSchedulerOnOff(true);
    super.tearDown();
  }

  
  public void test3PageFeedLazyMigration() throws Exception {
    final String activityTitle = "activity Title";
    
    for(int i = 0; i < 54; i++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle(activityTitle + i);
      activityStorageImpl.saveActivity(rootIdentity, activity);
      tearDownActivityList.add(activity);
    }
    
    ValueParam param = new ValueParam();
    param.setName("limit");
    param.setValue("20");
    InitParams params = new InitParams();
    params.addParameter(param);
    
    UserActivityStreamUpdaterPlugin updaterPlugin = new UserActivityStreamUpdaterPlugin(params);
    
    assertNotNull(updaterPlugin);
    updaterPlugin.processUpgrade("1.2.x", "4.0");
    
    List<ExoSocialActivity> got = new ArrayList<ExoSocialActivity>();
        
    List<ExoSocialActivity> page1 = activityStorageImpl.getActivityFeed(rootIdentity, 0, 20);
    //PAGE 1
    LOG.info("==========PAGE 1===========");
    got.addAll(page1);
    assertEquals(20, got.size());
    //PAGE 2
    List<ExoSocialActivity> page2 = activityStorageImpl.getActivityFeed(rootIdentity, 20, 20);
    LOG.info("==========PAGE 2===========");
    got.addAll(page2);
    assertEquals(40, got.size());
    //PAGE 3
    List<ExoSocialActivity> page3 = activityStorageImpl.getActivityFeed(rootIdentity, 40, 20);
    LOG.info("==========PAGE 3===========");
    assertEquals(14, page3.size());
    got.addAll(page3);
    assertEquals(54, got.size());
    assertEquals(54, streamStorage.getNumberOfFeed(rootIdentity));
  }
  
  public void test3PageMyActivitiesLazyMigration() throws Exception {
    final String activityTitle = "activity Title";
    
    for(int i = 0; i < 10; i++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle(activityTitle + i);
      activity.setPosterId(rootIdentity.getId());
      activity.setUserId(rootIdentity.getId());
      activityStorageImpl.saveActivity(rootIdentity, activity);
      tearDownActivityList.add(activity);
    }
    //
    StorageUtils.persist(false);
    
    ValueParam param = new ValueParam();
    param.setName("limit");
    param.setValue("5");
    InitParams params = new InitParams();
    params.addParameter(param);
    
    UserActivityStreamUpdaterPlugin updaterPlugin = new UserActivityStreamUpdaterPlugin(params);
    
    assertNotNull(updaterPlugin);
    updaterPlugin.processUpgrade("1.2.x", "4.0");
    
    List<ExoSocialActivity> got = new ArrayList<ExoSocialActivity>();
        
    //PAGE 1
    List<ExoSocialActivity> page1 = activityStorageImpl.getUserActivities(rootIdentity, 0, 10);
    LOG.info("==========PAGE 1===========");
    got.addAll(page1);
    assertEquals(10, got.size());
    assertEquals(10, streamStorage.getNumberOfMyActivities(rootIdentity));
  }
}