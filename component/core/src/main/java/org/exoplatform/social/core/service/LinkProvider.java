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
package org.exoplatform.social.core.service;

import org.apache.commons.lang.Validate;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;

/**
 * The <code>LinkProvider</code> class builds and provides default links and links of users, spaces and activities.
 * Links be built base on specific provided information as name or id of target user or space.
 * In case of wrong when building, default links will be returned.    
 */
public class LinkProvider {
  public static final String RESOURCE_URL = "/social-resources";
  public static final String JAVASCRIPT_RESOURCE_URL = RESOURCE_URL + "/javascript/";
  public static final String PROFILE_DEFAULT_AVATAR_URL = RESOURCE_URL + "/skin/images/ShareImages/UserAvtDefault.png";
  public static final String SPACE_DEFAULT_AVATAR_URL = RESOURCE_URL + "/skin/images/ShareImages/SpaceAvtDefault.png";
  public static final String HAS_CONNECTION_ICON =
          RESOURCE_URL + "/skin/social/webui/UIManageUsers/DefaultSkin/background/StatusIcon.png";
  public static final String WAITING_CONFIRMATION_ICON =
          RESOURCE_URL + "/skin/social/webui/UIManageUsers/DefaultSkin/background/WaittingConfirm.png";
  public static final String SPACE_MANAGER_ICON =
          RESOURCE_URL + "/skin/social/webui/UIManageSpaces/DefaultSkin/background/Manager.png";
  public static final String SPACE_MEMBER_ICON =
          RESOURCE_URL + "/skin/social/webui/UIManageSpaces/DefaultSkin/background/Member.png";
  public static final String SPACE_WAITING_CONFIRM_ICON =
          RESOURCE_URL + "/skin/social/webui/UIManageSpaces/DefaultSkin/background/WaitingConfirm.png";
  public static final String STARTER_ACTIVITY_AVATAR = RESOURCE_URL + "/skin/images/Activity/starterAvt.png";
  public static final String STARTER_ACTIVITY_IMAGE = RESOURCE_URL + "/skin/images/Activity/starterActImg.png";

  public static final String ROUTE_DELIMITER = "/";
  
  private static IdentityManager identityManager;
  private static Log             LOG = ExoLogger.getLogger(LinkProvider.class);

  /**
   * Hacks for unit test to work.
   */
  public static String DEFAULT_PORTAL_OWNER = "classic";

  /**
   * Constructor with parameter to inject the default portal owner name
   * @param params The params get from configuration.
   * 
   * @since 1.2.0 GA
   */
  public LinkProvider(InitParams params){
    if(params.getValueParam("predefinedOwner") != null) DEFAULT_PORTAL_OWNER = params.getValueParam("predefinedOwner").getValue();
  }

  /**
   * Gets the uri link to space profile by its pretty name.
   *
   * @param prettyName The pretty name of space.
   * @return the uri link to space
   * @LevelAPI Platform
   * @since 1.2.0 GA
   */
  public static String getSpaceUri(final String prettyName) {
    SpaceService spaceService = getSpaceService();
    Space space = spaceService.getSpaceByPrettyName(prettyName);
    RequestContext ctx = RequestContext.getCurrentInstance();
    if (ctx != null) {
      NodeURL nodeURL =  ctx.createURL(NodeURL.TYPE);
      NavigationResource resource = new NavigationResource(SiteType.GROUP, space.getGroupId(), space.getUrl());
      return nodeURL.setResource(resource).toString();
    }
    else {
      return null;
    }
  }

  /**
   * Gets the uri link to user profile by user's name.
   *
   * @param username The name of user (remoteId)
   * @return the uri link to user profile
   * @LevelAPI Platform
   */
  public static String getProfileUri(final String username) {
    return buildProfileUri(username, null, null);
  }

  /**
   * Gets the uri link to user profile by user's name and owner portal.
   *
   * @param username The name of user (remoteId)
   * @param portalOwner The portal owner (classic, public..)
   * @return the uri link to user profile
   * @LevelAPI Platform
   */
  public static String getProfileUri(final String username, final String portalOwner) {
    return buildProfileUri(username, null, portalOwner);
  }

  /**
   * Gets tag <a> with a link to profile of userName.
   *
   * @param username The name of user (remoteId)
   * @return tag <a> with a link to profile of userName
   * @LevelAPI Platform
   */
  public static String getProfileLink(final String username) {
    return getProfileLink(username, null);
  }

  /**
   * Gets tag <a> with a link to profile of userName on portalName.
   *
   * @param username The name of user (remoteId)
   * @param portalOwner The portal owner (classic, public..)
   * @return tag <a> with a link to profile of userName on portalName
   * @LevelAPI Platform
   */
  public static String getProfileLink(final String username, final String portalOwner) {
    Identity identity = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, true);
    Validate.notNull(identity, "Identity must not be null.");
    return "<a href=\"" + buildProfileUri(identity.getRemoteId(), null, portalOwner)
    + "\" target=\"_parent\">" + identity.getProfile().getFullName() + "</a>";
  }

  /**
   * Gets absolute profile uri of userName
   *
   * @param username The name of user (remoteId)
   * @param portalName The name of current portal
   * @param portalOwner The portal owner (classic, public..)
   * @param host The name of provided host
   * @return absolute profile uri of userName
   * @LevelAPI Platform
   */
  public static String getAbsoluteProfileUrl(final String userName, final String portalName,
                                             final String portalOwner, final String host) {
    return host + buildProfileUri(userName, portalName, portalOwner);
  }

  /**
   * Gets activity link of user; remoteId should be the id name. 
   * For example: root
   * 
   * @param remoteId The name of user (remoteId)
   * @return Link to activity of provided user.
   * @LevelAPI Platform
   */
  public static String getUserActivityUri(final String remoteId) {
    return getActivityUri(OrganizationIdentityProvider.NAME,remoteId);
  }

  /**
   * Gets connections link of user; remoteId should be the id name. 
   * For example: root
   * 
   * @param remoteId The name of user (remoteId)
   * @return The link point to connections of provided user.
   * @LevelAPI Platform
   */
  public static String getUserConnectionsUri(final String remoteId) {
    return getBaseUri(null, null) + "/connections/all-people" + ROUTE_DELIMITER + remoteId;
  }
  
  /**
   * Gets connections link of user; remoteId should be the id name. 
   * For example: root
   * 
   * @param remoteId The name of user (remoteId)
   * @return The link to network of provided user who has connection with current user. 
   */
  public static String getUserConnectionsYoursUri(final String remoteId) {
    return getBaseUri(null, null) + "/connections/network" + ROUTE_DELIMITER + remoteId;
  }
  
  /**
   * Gets profile link of user; remoteId should be the id name. 
   * For example: root
   *
   * @param remoteId The name of user (remoteId)
   * @return The link to profile of provided user.
   * @LevelAPI Platform
   */
  public static String getUserProfileUri(final String remoteId) {
    return getBaseUri(null, null) + "/profile" + ROUTE_DELIMITER + remoteId;
  }

  /**
   * Gets activity link of space or user; remoteId should be the id name. 
   * For example: organization:root or space:abc_def.
   *
   * @param providerId The provider information.
   * @param remoteId The id of target identity.
   * @return The link to activity of provided user on provided provider.
   * @LevelAPI Platform
   */
  public static String getActivityUri(final String providerId, final String remoteId) {
    final String prefix = getBaseUri(null, null) + "/";
    if (providerId.equals(OrganizationIdentityProvider.NAME)) {
      return String.format("%sactivities/%s",prefix,remoteId);
    } else if (providerId.equals(SpaceIdentityProvider.NAME)) {
      return String.format("/%s/g/:spaces:%s/%s",getPortalName(null),remoteId,remoteId);
    } else {
      LOG.warn("Failed to getActivityLink with providerId: " + providerId);
    }
    return null;
  }

  /**
   * Gets activity link of the space.
   *
   * @param remoteId The id of target space.
   * @param groupId The group id of target space.
   * @return The link to activity of space
   * @LevelAPI Platform
   * @since 1.2.8
   */
  public static String getActivityUriForSpace(final String remoteId, final String groupId) {
    return String.format("/%s/g/:spaces:%s/%s", getPortalName(null), groupId, remoteId);
  }
  
  /**
   * Builds avatar image uri from avatarAttachment.
   *
   * @param avatarAttachment The attachment used to build link to avatar.
   * @return The link to avatar of provided attachment.
   * @LevelAPI Platform
   */
  public static String buildAvatarImageUri(final AvatarAttachment avatarAttachment) {
    return buildAvatarImageUri(PortalContainer.getInstance(), avatarAttachment);
  }

  /**
   * Gets link to avatar of provided space.
   *
   * @param space The target object to get its avatar base on attachment information.
   * @return The link to avatar of target space.
   * @LevelAPI Platform
   * @since 1.2.0-GA
   */
  public static String buildAvatarImageUri(final Space space) {
    return buildAvatarImageUri(space.getAvatarAttachment());
  }

  /**
   * Gets url of avatar from identity name.
   *
   * @param identityName The name of target identity to build link to avatar.
   * @return Link to avatar of target provided identity name.
   * @LevelAPI Platform
   * @since 1.2.0-GA
   */
  public static String buildAvatarImageUri(final String identityName) {
    return String.format(
        "/rest/jcr/repository/social/production/soc:providers/soc:space/soc:%s/soc:profile/soc:avatar",
        identityName);
  }

  /**
   * Escapes jcr special characters.
   *
   * @param string
   * @return
   * @LevelAPI Platform
   */
  public static String escapeJCRSpecialCharacters(String string) {
    if (string == null) {
      return null;
    }
    return string.replace("[", "%5B").replace("]", "%5D").replace(":", "%3A");
  }
  
  /**
   * Gets avatar image uri of profile in a portalContainer.
   *
   * @param profile
   * @param portalContainer
   * @return null or an url if available
   * @LevelAPI Provisional
   * @deprecated use {@link Profile#getAvatarUrl()}. Will be removed by 4.0.x
   */
  public static String getAvatarImageSource(final PortalContainer portalContainer, final Profile profile) {
    final AvatarAttachment avatarAttachment = (AvatarAttachment) profile.getProperty(Profile.AVATAR);
    if (avatarAttachment != null) {
      return buildAvatarImageUri(portalContainer, avatarAttachment);
    }
    return null;
  }
  
  /**
   * Gets avatar image uri of profile.
   *
   * @param profile
   * @return null or an url if available
   * @LevelAPI Provisional
   * @deprecated use {@link Profile#getAvatarUrl()}. Will be removed by 4.0.x
   */
  public static String getAvatarImageSource(final Profile profile) {
    String avatarUrl = profile.getAvatarUrl();
    if (avatarUrl != null) {
      return avatarUrl;
    }

    final AvatarAttachment avatarAttachment = (AvatarAttachment) profile.getProperty(Profile.AVATAR);
    if (avatarAttachment != null) {
      avatarUrl = buildAvatarImageUri(avatarAttachment);
      profile.setAvatarUrl(avatarUrl);
      getIdentityManager().saveProfile(profile);
      return avatarUrl;
    }
    return null;
  }

  /**
   * Builds avatar image uri from avatarAttachment.
   *
   * @param container
   * @param avatarAttachment
   * @return url to avatar
   */
  private static String buildAvatarImageUri(final PortalContainer container, final AvatarAttachment avatarAttachment) {
    String avatarUrl = null;
    try {
      if (avatarAttachment != null) {
        final String repository = ((RepositoryService) container.getComponentInstanceOfType(RepositoryService.class)).
        getCurrentRepository().getConfiguration().getName();


        avatarUrl = "/" + container.getRestContextName() + "/jcr/" + repository + "/"
        + avatarAttachment.getWorkspace() + avatarAttachment.getDataPath() + "/?upd="
        + avatarAttachment.getLastModified();
        avatarUrl = escapeJCRSpecialCharacters(avatarUrl);
      }
    } catch (Exception e) {
      LOG.warn("Failed to build avatar url from avatar attachment for: " + e.getMessage());
    }
    return avatarUrl;
  }

  /**
   * Builds profile uri from userName and portalOwner.
   *
   * @param userName
   * @param portalName
   * @param portalOwner
   *        The portal owner (classic, public..)
   *        
   * @return profile uri
   */
  private static String buildProfileUri(final String userName, final String portalName, String portalOwner) {
    if(portalOwner == null || portalOwner.trim().length() == 0) portalOwner = DEFAULT_PORTAL_OWNER;
    return getBaseUri(portalName, portalOwner) + "/profile" + ROUTE_DELIMITER + userName;
  }

  /**
   * Builds profile uri from userName and portalName and portalOwner.
   *
   * @param portalName
   * @param portalOwner
   *        The portal owner (classic, public..)
   *        
   * @return
   */
  private static String getBaseUri(final String portalName, String portalOwner) {
    return "/" + getPortalName(portalName) + "/" + getPortalOwner(portalOwner);
  }

  /**
   * Gets IdentityManager instance.
   *
   * @return identityManager
   */
  private static IdentityManager getIdentityManager() {
    if (LinkProvider.identityManager == null) {
      LinkProvider.identityManager = (IdentityManager) PortalContainer.getInstance()
      .getComponentInstanceOfType(IdentityManager.class);
    }
    return LinkProvider.identityManager;
  }

  /**
   * Gets the space service.
   * 
   * @return
   * @since 1.2.2
   */
  private static SpaceService getSpaceService() {
    return (SpaceService) PortalContainer.getInstance().getComponentInstanceOfType(SpaceService.class);
  }
  
  /**
   * Gets portal owner, if parameter is null or "", the method return default portal owner.
   *
   * @param portalOwner
   *        The portal owner (classic, public..)
   * @return portalOwner
   */
  private static String getPortalOwner(String portalOwner) {
    if (portalOwner == null || portalOwner.trim().length() == 0) {
      try {
        return Util.getPortalRequestContext().getPortalOwner();
      } catch (Exception e) {
        return DEFAULT_PORTAL_OWNER;
      }
    }
    return portalOwner;
  }

  /**
   * Gets portal owner, if parameter is null or "", the method return default portal owner
   * 
   * @param portalName
   * @return portalName
   */
  private static String getPortalName(String portalName) {
    if (portalName == null || portalName.trim().length() == 0) {
      return PortalContainer.getCurrentPortalContainerName();
    }
    return portalName;
  }
}
