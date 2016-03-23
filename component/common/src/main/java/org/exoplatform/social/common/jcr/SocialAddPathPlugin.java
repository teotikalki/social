package org.exoplatform.social.common.jcr;

import java.util.Arrays;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.hierarchy.impl.AddPathPlugin;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig;

public class SocialAddPathPlugin extends AddPathPlugin {

  private RepositoryService repoService;
  
  public SocialAddPathPlugin(InitParams params, RepositoryService repoService) {
    super(params);
    this.repoService = repoService;
  }

  @Override
  public HierarchyConfig getPaths() {
    if (hasWorkspace()) {
      return super.getPaths();      
    } else {
      return null;
    }
  }

  private boolean hasWorkspace() {
    try {
      String[] names = repoService.getCurrentRepository().getWorkspaceNames();

      if (names.length == 0 || super.getPaths().getWorkspaces().isEmpty()) {
        return false;
      }
      for (String workspace : super.getPaths().getWorkspaces()) {
        if (!Arrays.asList(names).contains(workspace)) {
          return false;
        }
      }
      return true;
    } catch (Exception ex) {
      //workaround, too soon to check for workspace exists
      //this ex happen when NodeHierchyCreator#addPlugin is called
      return true;
    }
  }
  
}
