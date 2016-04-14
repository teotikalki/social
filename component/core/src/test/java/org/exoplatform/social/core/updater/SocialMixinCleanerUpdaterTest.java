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
package org.exoplatform.social.core.updater;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.NodeIterator;
import javax.jcr.query.Query;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.transaction.TransactionService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.test.AbstractCoreTest;

public class SocialMixinCleanerUpdaterTest extends AbstractCoreTest {
  private static final String     WS_NAME = "portal-test";

  private final Log               LOG     = ExoLogger.getLogger(SocialMixinCleanerUpdaterTest.class);

  private List<ExoSocialActivity> tearDownActivityList;

  private Identity                rootIdentity;

  private Identity                johnIdentity;

  private Identity                maryIdentity;

  private Identity                demoIdentity;

  private Identity                ghostIdentity;

  private Identity                raulIdentity;

  private Identity                jameIdentity;

  private Identity                paulIdentity;

  private IdentityManager         identityManager;

  private ActivityManager         activityManager;

  private RepositoryService       repositoryService;

  private TransactionService      transactionService;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    identityManager = getContainer().getComponentInstanceOfType(IdentityManager.class);
    activityManager = getContainer().getComponentInstanceOfType(ActivityManager.class);
    repositoryService = getContainer().getComponentInstanceOfType(RepositoryService.class);
    transactionService = getContainer().getComponentInstanceOfType(TransactionService.class);
    tearDownActivityList = new ArrayList<ExoSocialActivity>();

    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", true);
    johnIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "john", true);
    maryIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "mary", true);
    demoIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "demo", true);
    ghostIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "ghost", true);
    raulIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "raul", true);
    jameIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "jame", true);
    paulIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "paul", true);
  }

  public void testUpgrade() throws Exception {
    assertEquals("Used workspace is different", session.getWorkspace().getName(), WS_NAME);

    createActivity("Test root activity", rootIdentity);
    createActivity("Test john activity", johnIdentity);
    createActivity("Test mary activity", maryIdentity);
    createActivity("Test demo activity", demoIdentity);
    createActivity("Test ghost activity", ghostIdentity);
    createActivity("Test raul activity", raulIdentity);
    createActivity("Test jame activity", jameIdentity);
    createActivity("Test paul activity", paulIdentity);

    Query query = session.getWorkspace().getQueryManager().createQuery("select * from exo:sortable", Query.SQL);
    NodeIterator nodeIterator = query.execute().getNodes();
    assertTrue("No nodes was found with mixin exo:sortable", nodeIterator.getSize() > 1);

    LOG.info("Cleanup '{}' social nodes.", nodeIterator.getSize());

    InitParams initParams = new InitParams();

    ValueParam workspaceParam = new ValueParam();
    workspaceParam.setName("workspace");
    workspaceParam.setValue(WS_NAME);

    ValueParam groupIdParam = new ValueParam();
    groupIdParam.setName("product.group.id");
    groupIdParam.setValue("org.exoplatform.social");

    ValuesParam mixinsParam = new ValuesParam();
    mixinsParam.setName("mixins.to.clean");
    mixinsParam.setValues(Collections.singletonList("exo:sortable"));

    ValuesParam mixinsExceptionParam = new ValuesParam();
    mixinsExceptionParam.setName("mixins.clean.exception");
    mixinsExceptionParam.setValues(Collections.singletonList("exo:sortable;soc:profiledefinition"));

    initParams.addParam(workspaceParam);
    initParams.addParam(groupIdParam);
    initParams.addParam(mixinsParam);
    initParams.addParam(mixinsExceptionParam);

    SocialMixinCleanerUpgradePlugin socialMixinCleanerUpgradePlugin = new SocialMixinCleanerUpgradePlugin(PortalContainer.getInstance(),
                                                                                                          repositoryService,
                                                                                                          transactionService,
                                                                                                          initParams);
    socialMixinCleanerUpgradePlugin.processUpgrade(null, null);

    while (!socialMixinCleanerUpgradePlugin.isUpgradeFinished()) {
      Thread.sleep(1000);
    }

    // Get the number of nodes that wasn't updated
    query = session.getWorkspace().getQueryManager().createQuery("select * from soc:profiledefinition", Query.SQL);
    // exceptional nodes = (soc:profiledefinition COUNT + 1 for root node)
    long exceptionalNodesCount = query.execute().getNodes().getSize() + 1;

    query = session.getWorkspace().getQueryManager().createQuery("select * from exo:sortable", Query.SQL);
    nodeIterator = query.execute().getNodes();
    LOG.info("Not cleaned up social nodes: '{}'.", nodeIterator.getSize());

    assertFalse("Social nodes wasn't cleaned up. It seems that there are some remaining nodes that uses exo:sortable",
                nodeIterator.getSize() > exceptionalNodesCount);
  }

  private void createActivity(String activityTitle, Identity userIdentity) {
    String userId = userIdentity.getId();
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    // test for reserving order of map values for i18n activity
    Map<String, String> templateParams = new LinkedHashMap<String, String>();
    templateParams.put("key1", "value 1");
    templateParams.put("key2", "value 2");
    templateParams.put("key3", "value 3");
    activity.setTemplateParams(templateParams);
    activity.setTitle(activityTitle);
    activity.setUserId(userId);

    //
    activity.isHidden(false);
    activity.isLocked(true);
    activityManager.saveActivityNoReturn(userIdentity, activity);
    tearDownActivityList.add(activity);
  }

  @Override
  public void tearDown() throws Exception {
    for (ExoSocialActivity activity : tearDownActivityList) {
      try {
        activityManager.deleteActivity(activity.getId());
      } catch (Exception e) {
        LOG.warn("can not delete activity with id: " + activity.getId());
      }
    }

    identityManager.deleteIdentity(rootIdentity);
    identityManager.deleteIdentity(johnIdentity);
    identityManager.deleteIdentity(maryIdentity);
    identityManager.deleteIdentity(demoIdentity);
    identityManager.deleteIdentity(ghostIdentity);
    identityManager.deleteIdentity(jameIdentity);
    identityManager.deleteIdentity(raulIdentity);
    identityManager.deleteIdentity(paulIdentity);
    super.tearDown();
  }

}
