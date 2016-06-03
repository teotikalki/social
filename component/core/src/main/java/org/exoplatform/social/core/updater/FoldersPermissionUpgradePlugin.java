package org.exoplatform.social.core.updater;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.util.Date;

/**
 * Created by kmenzli on 03/06/16.
 */
public class FoldersPermissionUpgradePlugin extends UpgradeProductPlugin {

    private static final Log LOG = ExoLogger.getLogger(FoldersPermissionUpgradePlugin.class.getName());

    private static final String[] readPerm = {PermissionType.READ};

    private RepositoryService repositoryService;

    private UserHandler userHandler;

    private String targetNodePath = "production";

    private String dropPermissions = "";

    private String addPermissions = "";

    private String targetWorkspace = "social";

    private final String TARGET_NODE_NAME = "target-node-path";
    private final String PERMISSIONS_TO_DROP = "permissions-to-drop";
    private final String PERMISSIONS_TO_ADD = "permissions-to-add";
    private final String TARGET_WORKSPACE = "target-workspace";



    public FoldersPermissionUpgradePlugin(RepositoryService repoService,OrganizationService orgService, InitParams params) {
        super(params);

        this.repositoryService = repoService;

        this.userHandler = orgService.getUserHandler();

        targetNodePath = params.getValueParam(TARGET_NODE_NAME).getValue();

        ValueParam targetNodeNameParam = params.getValueParam(TARGET_NODE_NAME);
        if (targetNodeNameParam != null) {
            this.targetNodePath = targetNodeNameParam.getValue();
        }
        ValueParam dropPermissionsParam = params.getValueParam(PERMISSIONS_TO_DROP);
        if (targetNodeNameParam != null) {
            this.dropPermissions = dropPermissionsParam.getValue();
        }
        ValueParam addPermissionsParam = params.getValueParam(PERMISSIONS_TO_ADD);
        if (targetNodeNameParam != null) {
            this.addPermissions = addPermissionsParam.getValue();
        }
        ValueParam targetWorkspaceParam = params.getValueParam(TARGET_WORKSPACE);
        if (targetNodeNameParam != null) {
            this.targetWorkspace = targetWorkspaceParam.getValue();
        }
    }
    @Override
    public void processUpgrade(String oldVersion, String newVersion) {

        LOG.info("Start {} .............", this.getClass().getName());
        //--- JCR session to modify target nodes
        Session jcrSession = null;
        //--- JCR node to hold the target
        Node node = null;
        //--- Init sessionProvider
        SessionProvider sessionProvider = SessionProvider.createSystemProvider();
        //--- Time spent to update avatar permission for all users registered on eXo
        long startTime;
        //--- Offset ot load users
        int limit = 100;
        //--- index
        int index =0;

        try {

            //--- Compute permissions to drop
            String[] dropPermTab = dropPermissions.split(",");
            //--- Compute permissions to drop
            String[] addPermTab = addPermissions.split(",");

            //--- Get target workspace
            jcrSession = repositoryService.getCurrentRepository().getSystemSession(targetWorkspace);
            //--- Get target node
            node = (Node) jcrSession.getItem(targetNodePath);

            //--- Get node ACL
            AccessControlList acl = ((ExtendedNode) node).getACL();
            //--- Drop permissions phase
            for (int i = 0; i < dropPermTab.length; i++) {
                for (AccessControlEntry entry : acl.getPermissionEntries()) {
                    if (entry.getIdentity().equalsIgnoreCase(dropPermTab[i])) {
                        ((ExtendedNode) node).removePermission(entry.getIdentity());
                    }
                }
            }
            //--- Save drop permissions operation
            node.getSession().save();
            LOG.info("Permissions "+dropPermissions+"has been dropped successfully from node "+targetNodePath);

            //--- Add permissions phase

            if (addPermTab != null) {
                //--- Populate the permission Map with ACL coming from the configuration file
                for (int i = 0; i < addPermTab.length; i++) {
                    //--- set the new permission
                    ((ExtendedNode)node).setPermission(addPermTab[i],readPerm);
                }
                //--- Save add permissions operation
                node.getSession().save();
                LOG.info("Permissions "+addPermissions+"has been added successfully to node "+targetNodePath);
            }

            //--- Allow all users to access to avatar node of each user
            startTime = new Date().getTime();

            //--- Total number of users registered in the platform
            long allUsers = 0;

            try {
                allUsers = userHandler.findAllUsers().getSize();
            } catch (Exception E) {
                if(LOG.isErrorEnabled()) {
                    LOG.error(E.getMessage());
                }

            }

            if (LOG.isInfoEnabled()) {
                LOG.info("===> Amount of users to process is {} users", allUsers);
            }

            int globalOffSet = 0;
            while(allUsers >= globalOffSet){
                globalOffSet = index * limit;
                updateAvatarPermissions(jcrSession,globalOffSet, limit);
                index++;
            }
            long finishTime = new Date().getTime();
            if (LOG.isInfoEnabled()) {
                LOG.info("User's avatar update completed, Take: " + (finishTime - startTime) / 1000 + "s");
            }


        } catch(Exception re){
            if(LOG.isErrorEnabled()) {
                LOG.error(re.getMessage());
            }
        } finally {
            sessionProvider.close();
        }

        LOG.info("Finish upgrading {} permissions", targetNodePath);

    }

    /**
     * Update avatar permission of each user registered on eXo
     * @param session JCR session used to update node
     * @param offset
     * @param limit
     */
    private void updateAvatarPermissions(Session session, long offset, long limit){
        LOG.info("Update avatars, check from " + offset + " to " + limit);
        try {
            String sql = "SELECT * FROM soc:identitydefinition WHERE soc:isDeleted = 'false' AND jcr:path like '/production/soc:providers/soc:organization/%' ORDER BY soc:remoteId ASC";
            NodeIterator iterator = queryNodes(session, sql, offset, limit);
            Node aNode = null;
            while (iterator.hasNext()) {
                aNode = iterator.nextNode();
                String nodeName = aNode.getName();
                //check soc:profile
                Node profileNode = aNode.getNode("soc:profile");
                //--- Check if the current user has added an avatar to his profile
                if(profileNode.hasNode("soc:avatar")){

                    Node avatarNode = profileNode.getNode("soc:avatar");
                    //--- set the new permission
                    ((ExtendedNode)avatarNode).setPermission("any",readPerm);
                    //--- Save add permissions operation
                    profileNode.save();
                    if (LOG.isDebugEnabled()) {
                        LOG.info("Avatar permissions of user "+nodeName+"has been updated successfully");
                    }

                }

            }

        } catch (Exception e) {
            LOG.error("Error while collecting soc:identitydefinition", e);
        }finally {
        }
    }

    /**
     *  Run a JCR query to get users based on  criteria
     * @param session JCR session used to update node
     * @param statement criteria
     * @param offset
     * @param limit
     * @return
     */
    private NodeIterator queryNodes(Session session, String statement, long offset, long limit) {
        //
        if (statement == null) return null;
        try {
            QueryManager queryMgr = session.getWorkspace().getQueryManager();
            Query query = queryMgr.createQuery(statement, Query.SQL);
            if (query instanceof QueryImpl) {
                QueryImpl impl = (QueryImpl) query;
                if (limit > 0) {
                    impl.setOffset(offset);
                    impl.setLimit(limit);
                }
                return impl.execute().getNodes();
            }

            return query.execute().getNodes();
        } catch (Exception ex) {
            LOG.error("Query is failed!.", ex);
            return null;
        }
    }

    @Override
    public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
        return VersionComparator.isAfter(newVersion, previousVersion);
    }
}
