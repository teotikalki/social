/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.social.portlet;

import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.social.portlet.profilelist.UIProfileList;
import org.exoplatform.social.portlet.profile.UIProfile;
import org.exoplatform.social.portlet.dashboard.UISocialDashboard;
import org.exoplatform.social.portlet.activities.UIActivities;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.application.PortalRequestContext;

@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class,
    template = "app:/groovy/portal/webui/component/UIMainProfilePortlet.gtmpl"
)
public class UIMainProfilePortlet extends UIPortletApplication {

  public UIMainProfilePortlet() throws Exception {
    addChild(UIProfileList.class, null, null);
    addChild(UIProfile.class, null, null);
    addChild(UISocialDashboard.class, null, null);
    addChild(UIActivities.class, null, null);
  }
}
