package org.jahia.modules.jexperienceProfile.service.impl;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jexperience.admin.Constants;
import org.jahia.modules.jexperience.admin.ContextServerService;
import org.jahia.modules.jexperience.admin.ContextServerSettings;
import org.jahia.modules.jexperience.admin.internal.ContextServerSettingsService;
import org.jahia.modules.jexperience.tag.WemFunctions;
import org.jahia.modules.jexperienceProfile.service.JexperienceProfileService;
import org.jahia.services.render.RenderContext;
import org.json.JSONException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component(service = JexperienceProfileService.class, immediate = true)
public abstract class JexperienceProfileServiceImpl implements JexperienceProfileService {

    private static final String ipForwardingHeaderName = "X-Forwarded-For";
    private static final Logger logger = LoggerFactory.getLogger(JexperienceProfileServiceImpl.class);
    private static final String APIProfilesPath = "/cxs/profiles/";

    private ContextServerService contextServerService;



    public <T> T executePostRequest(String siteKey, String path, String json, Class<T> tClass)
            throws IOException {
        T t = contextServerService.executePostRequest(siteKey, path, json, null, null, tClass);

        return t;
    }


    public Map<String, String> getProfileProperties(RenderContext renderContext) throws IOException {
        String siteKey = renderContext.getSite().getSiteKey();
        HttpServletRequest request =renderContext.getRequest();

        String url = APIProfilesPath+getProfileId(request,siteKey,true);
        return executePostRequest(siteKey,  url,  null,  new LinkedHashMap<>().getClass()) ;

    }


    /**
     * This method will find the profile id and return it
     * @param httpServletRequest HttpServletRequest
     * @param siteKey siteKey
     * @param checkRequestParameters set to true if we want to look for existing profile id in the request parameters and request attributes
     *                               false otherwise, it will only look for existing profile id from cookies.
     * @return String profile id
     */
    public String getProfileId(HttpServletRequest httpServletRequest, String siteKey, boolean checkRequestParameters) {
        HttpSession session = httpServletRequest.getSession();
        if (checkRequestParameters && httpServletRequest.getAttribute(Constants.WEM_PROFILE_ID) != null) {
            String wemProfileId = (String) httpServletRequest.getAttribute(Constants.WEM_PROFILE_ID);
            if (StringUtils.isNotBlank(wemProfileId)) {
                session.setAttribute(Constants.WEM_PROFILE_ID, wemProfileId);
            } else {
                logger.warn("Empty wemProfileId found in request attributes!");
            }
        } else if (checkRequestParameters && httpServletRequest.getParameter(Constants.WEM_PROFILE_ID) != null) {
            String wemProfileId = httpServletRequest.getParameter(Constants.WEM_PROFILE_ID);
            if (StringUtils.isNotBlank(wemProfileId)) {
                session.setAttribute(Constants.WEM_PROFILE_ID, wemProfileId);
            } else {
                logger.warn("Empty wemProfileId found in request URL parameters");
            }
        } else if (httpServletRequest.getCookies() != null && ContextServerSettingsService.getInstance() != null) {
            ContextServerSettings contextServerSettings = ContextServerSettingsService.getInstance().getSettings(siteKey);
            if (contextServerSettings != null) {
                for (Cookie cookie : httpServletRequest.getCookies()) {
                    String cookieName = cookie.getName();
                    String cookieValue = cookie.getValue();

                    if (cookieName.equals(contextServerSettings.getContextServerCookieName())
                            || (cookieName.equals("wem-profile-id") && !cookieValue.equals("undefined"))) {
                        if (StringUtils.isNotBlank(cookieValue)) {
                            logger.debug("Found profile ID=" + cookieValue + " in cookies");
                            session.setAttribute(Constants.WEM_PROFILE_ID, cookieValue);
                        } else {
                            logger.warn("Found empty profile ID in cookie {} !", cookieName.equals(contextServerSettings.getContextServerCookieName())
                                    ? contextServerSettings.getContextServerCookieName() : Constants.WEM_PROFILE_ID_COOKIE);
                        }
                        break;
                    }
                }
            }
        } else {
            session.removeAttribute(Constants.WEM_PROFILE_ID);
        }

        return (String) session.getAttribute(Constants.WEM_PROFILE_ID);
    }


    @Reference
    public void setContextServerService(ContextServerService contextServerService) {
        this.contextServerService = contextServerService;
    }
}
