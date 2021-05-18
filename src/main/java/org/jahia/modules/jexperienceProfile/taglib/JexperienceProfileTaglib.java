package org.jahia.modules.jexperienceProfile.taglib;

import org.apache.felix.utils.collections.StringArrayMap;
import org.jahia.modules.jexperienceProfile.service.JexperienceProfileService;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.render.RenderContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JexperienceProfileTaglib {

    private static Logger logger = LoggerFactory.getLogger(JexperienceProfileTaglib.class);

    public static JSONObject getProfileProps(RenderContext renderContext) throws IOException, JSONException {
        Map<String, Object> profileProps = new HashMap<String, Object>();
        LinkedHashMap<String,String> properties = new LinkedHashMap<String,String>();
        JexperienceProfileService JexperienceProfileService = BundleUtils.getOsgiService(JexperienceProfileService.class, null);

        BundleContext bundleContext = FrameworkUtil.getBundle(JexperienceProfileTaglib.class).getBundleContext();
        if (bundleContext != null) {
            ServiceReference<JexperienceProfileService> jexperienceProfileServiceServiceReference = bundleContext.getServiceReference(JexperienceProfileService.class);
            if (jexperienceProfileServiceServiceReference != null) {
                JexperienceProfileService jExperienceProfileService = bundleContext.getService(jexperienceProfileServiceServiceReference);
                profileProps = jExperienceProfileService.getProfileProperties(renderContext);
                properties = (LinkedHashMap<String, String>) profileProps.get("properties");
            }

        }

        return new JSONObject(properties);

    }



}
