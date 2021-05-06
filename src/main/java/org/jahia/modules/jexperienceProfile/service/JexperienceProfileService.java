package org.jahia.modules.jexperienceProfile.service;

import org.jahia.services.render.RenderContext;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

public interface JexperienceProfileService {

    String getProfileId(HttpServletRequest httpServletRequest, String siteKey, boolean checkRequestParameters);

    Map<String, String> getProfileProperties(RenderContext renderContext) throws IOException;

    }
