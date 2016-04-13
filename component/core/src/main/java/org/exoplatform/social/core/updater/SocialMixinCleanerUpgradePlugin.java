/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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
package org.exoplatform.social.core.updater;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.transaction.TransactionService;

/**
 * Created by The eXo Platform SAS Author : Boubaker Khanfir
 * bkhanfir@exoplatform.com April 16, 2016
 */
public class SocialMixinCleanerUpgradePlugin extends UpgradeProductPlugin {

  private static final int TRANSACTION_TIMEOUT_IN_SECONDS = 3600;
  private static final String PLUGIN_PROCEED_VERSION = "4.3.1";
  private static final String QUERY = "select distinct PARENT_ID as id from JCR_SITEM" + " where container_name = 'social' and" + " (  NAME= '[http://www.exoplatform.com/jcr/exo/1.0]name' "
      + " or NAME= '[http://www.exoplatform.com/jcr/exo/1.0]title'" + " or NAME= '[http://www.exoplatform.com/jcr/exo/1.0]titlePublished'"
      + " or NAME= '[http://www.exoplatform.com/jcr/exo/1.0]index'" + " or NAME= '[http://www.exoplatform.com/jcr/publication/1.1/]liveDate'"
      + " or NAME= '[http://www.exoplatform.com/jcr/exo/1.0]owner'" + " or NAME= '[http://www.exoplatform.com/jcr/exo/1.0]dateCreated'"
      + " or NAME= '[http://www.exoplatform.com/jcr/exo/1.0]dateModified'" + " or NAME= '[http://www.exoplatform.com/jcr/exo/1.0]lastModifiedDate'"
      + " or NAME= '[http://www.exoplatform.com/jcr/exo/1.0]lastModifier'" + ");";

  private static final Log log = ExoLogger.getLogger(SocialMixinCleanerUpgradePlugin.class);
  private final static String[] MIXIN_NAMES = new String[] { "exo:modify", "exo:datetime", "exo:owneable", "exo:sortable" };

  private static final int FETCH_SIZE = 1000;

  private final PortalContainer portalContainer;
  private final RepositoryService repositoryService;
  private final TransactionService txService;
  private final String workspaceName;

  public SocialMixinCleanerUpgradePlugin(PortalContainer portalContainer, RepositoryService repositoryService, TransactionService txService, InitParams initParams) {
    super(initParams);
    ValueParam workspaceValueParam = initParams.getValueParam("workspace");
    if (workspaceValueParam != null) {
      workspaceName = workspaceValueParam.getValue();
    } else {
      workspaceName = null;
    }

    this.repositoryService = repositoryService;
    this.txService = txService;
    this.portalContainer = portalContainer;
  }

  /**
   * @return true if current version is uppper or equals to than 4.3.1, else
   *         return false
   */
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    return VersionComparator.isAfter(newVersion, PLUGIN_PROCEED_VERSION) || VersionComparator.isSame(newVersion, PLUGIN_PROCEED_VERSION);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        while (!portalContainer.isStarted()) {
          try {
            // Wait 10 seconds
            Thread.sleep(10000);
          } catch (InterruptedException e) {
            log.warn("An interruption on wait was thrown, ignore it.");
          }
        }

        log.info("Start migration");

        Connection jdbcConn = null;
        SessionProvider sessionProvider = null;
        Statement stmt = null;
        ResultSet rs = null;
        ManageableRepository currentRepo = null;

        long totalCount = 0;

        try {
          WorkspaceContainerFacade containerFacade = repositoryService.getCurrentRepository().getWorkspaceContainer(workspaceName);
          if (containerFacade == null) {
            throw new IllegalStateException("Workspace with name '" + workspaceName + "' wasn't found");
          }

          // Get JCR Session
          sessionProvider = SessionProvider.createSystemProvider();
          currentRepo = ((RepositoryImpl) repositoryService.getCurrentRepository());
          Session session = sessionProvider.getSession(workspaceName, currentRepo);

          // Get JDBC Result Set
          JDBCWorkspaceDataContainer dataContainer = (JDBCWorkspaceDataContainer) containerFacade.getComponent(JDBCWorkspaceDataContainer.class);
          jdbcConn = dataContainer.connFactory.getJdbcConnection();
          stmt = jdbcConn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
          stmt.setFetchSize(FETCH_SIZE);
          stmt.setQueryTimeout(TRANSACTION_TIMEOUT_IN_SECONDS * 24);

          WorkspaceEntry wEntry = getWorkspaceEntry(currentRepo);
          String query = QUERY.replace("JCR_SITEM", DBInitializerHelper.getItemTableName(wEntry));
          query = query.replace("'social'", "'" + workspaceName + "'");
          rs = stmt.executeQuery(query);

          // Begin transaction
          UserTransaction transaction = beginTransaction();
          while (rs.next()) {
            String id = rs.getString(1);
            id = id.replaceAll(workspaceName, "");

            try {
              Node node = null;
              try {
                // Test if node exists
                try {
                  node = ((SessionImpl) session).getNodeByIdentifier(id);
                } catch (ItemNotFoundException e) {
                  log.warn("Item not found with id: '{}'", id);
                  continue;
                }
                // Avoid changing Root Node of Workspace
                if (node.getPath().equals("/")) {
                  continue;
                }

                boolean proceeded = false;

                // FIXME: JCR-2442, remove mixin on node using a property
                // definition of name "*" leads to an exception.
                // That's why we exclude 'soc:profiledefinition'
                if (node.isNodeType("soc:profiledefinition")) {
                  log.debug("Ignore id: {}, nodetype = {}.", id, node.getPrimaryNodeType().getName());
                } else {
                  // Remove all mixins from nodes
                  for (String mixinName : MIXIN_NAMES) {
                    if (StringUtils.isBlank(mixinName) || !node.isNodeType(mixinName)) {
                      continue;
                    }
                    // Ignore deletion of 'exo:datetime' and 'exo:modify' from nodes of type 'soc:spaceref'
                    // Those mixins are used to sort spaces by access time
                    if (node.isNodeType("soc:spaceref") && (("exo:datetime".equals(mixinName) || "exo:modify".equals(mixinName)))) {
                      log.debug("Ignore id: {}, nodetype = {}, remove mixin '{}'", id, node.getPrimaryNodeType().getName(), mixinName);
                    } else {
                      log.debug("Proceed id: {}, nodetype = {}, remove mixin {}", id, node.getPrimaryNodeType().getName(), mixinName);
                      node.removeMixin(mixinName);
                      node.save();
                      proceeded = true;
                    }
                  }
                }
                if(proceeded) {
                  totalCount++;
                }
              } catch (Exception e) {
                log.warn("Error updating node with id " + id, e);
                if (node != null) {
                  node.refresh(false);
                }
              }
              if (totalCount % FETCH_SIZE == 0) {
                session.save();
                transaction.commit();
                log.info("Migration in progress, proceeded nodes count = {}", totalCount);

                transaction = beginTransaction();
              }
            } catch (Exception e) {
              transaction.rollback();
              transaction = beginTransaction();
            }
          }
          session.save();
          transaction.commit();
        } catch (Exception e) {
          log.error(e);
        } finally {
          if (sessionProvider != null) {
            sessionProvider.close();
          }
          try {
            if (rs != null) {
              rs.close();
            }
            if (stmt != null) {
              stmt.close();
            }
            if (jdbcConn != null) {
              jdbcConn.close();
            }
          } catch (SQLException e) {
            log.error("Cannot close connection", e);
          }
        }
        log.info("Migration finished, proceeded nodes count = {}", totalCount);
      }
    }, "SocialMixinCleanerUpgradePlugin").start();
  }

  private WorkspaceEntry getWorkspaceEntry(ManageableRepository currentRepo) {
    WorkspaceEntry wEntry = null;
    for (WorkspaceEntry entry : currentRepo.getConfiguration().getWorkspaceEntries()) {
      if (entry.getName().equals(workspaceName)) {
        wEntry = entry;
        break;
      }
    }
    if (wEntry == null) {
      throw new IllegalStateException("Worksapce \"" + workspaceName + "\" was not found.");
    }
    return wEntry;
  }

  private UserTransaction beginTransaction() throws SystemException, NotSupportedException {
    UserTransaction transaction;
    transaction = txService.getUserTransaction();
    transaction.setTransactionTimeout(TRANSACTION_TIMEOUT_IN_SECONDS);
    transaction.begin();
    return transaction;
  }

}
