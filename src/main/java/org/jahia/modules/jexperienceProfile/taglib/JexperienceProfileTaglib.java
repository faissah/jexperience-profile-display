package org.jahia.modules.jexperienceProfile.taglib;

import org.apache.felix.utils.collections.StringArrayMap;
import org.jahia.modules.jexperienceProfile.service.JexperienceProfileService;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.render.RenderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class JexperienceProfileTaglib {

    private static Logger logger = LoggerFactory.getLogger(JexperienceProfileTaglib.class);

    public static Map<String, String> getProfileProps(RenderContext renderContext) throws IOException {

        JexperienceProfileService jExperienceProfileService = BundleUtils.getOsgiService(JexperienceProfileService.class, null);
        Map<String, String> profileProps = new StringArrayMap<>();

        profileProps = jExperienceProfileService.getProfileProperties(renderContext);
        return profileProps;

    }



}
