package org.jahia.modules.jexperienceProfile.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jahia.modules.jexperience.admin.*;
import org.jahia.modules.jexperienceProfile.service.HttpUtils;
import org.jahia.modules.jexperienceProfile.service.JexperienceProfileService;
import org.jahia.services.render.RenderContext;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import java.util.*;

@Component(service = JexperienceProfileService.class)
public class JexperienceProfileServiceImpl implements JexperienceProfileService  {

    private static final String ipForwardingHeaderName = "X-Forwarded-For";
    private static final Logger logger = LoggerFactory.getLogger(JexperienceProfileServiceImpl.class);
    private static final String APIProfilesPath = "/cxs/profiles/";
    private ContextServerService contextServerService;
    private JExperienceConfigFactory jExperienceConfigFactory;

    public <T> T executePostRequest(String siteKey, String path, String json, Map<String, String> headers, Class<T> tClass)
            throws IOException {
        T t = contextServerService.executePostRequest(siteKey, path, json, null, headers, tClass);

        return t;
    }


    public <T> T executeGetRequest(ContextServerSettings contextServerSettings, String path, List<Cookie> cookies, Map<String,
            String> headers, Class<T> tClass) throws IOException {
        String url = contextServerSettings.getContextServerURL() + path;
        HttpGet httpGetBase = new HttpGet(url);

        long requestStartTime = System.currentTimeMillis();
        if (headers != null) {
            Iterator var7 = headers.entrySet().iterator();

            while(var7.hasNext()) {
                Map.Entry<String, String> entry = (Map.Entry)var7.next();
                httpGetBase.setHeader((String)entry.getKey(), (String)entry.getValue());
            }
        }

        CloseableHttpResponse response;
        CloseableHttpClient adminHttpClient = HttpUtils.initHttpClient(contextServerSettings, false, contextServerSettings.getPoolMaxTotal());
        if (cookies != null) {
            HttpContext localContext = HttpUtils.setupCookies(cookies);
            response = adminHttpClient.execute(httpGetBase, localContext);
        } else {
            response = adminHttpClient.execute(httpGetBase);
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            if (statusCode == 401) {
                throw new ContextServerHttpException("Error invalid username and/or password", (Throwable)null, statusCode);
            } else {
                throw new ContextServerHttpException("Couldn't execute " + httpGetBase + " response: " + EntityUtils.toString(response.getEntity()), (Throwable)null, statusCode);
            }
        } else {
            HttpEntity entity = response.getEntity();
            if (logger.isDebugEnabled()) {
                if (entity != null) {
                    entity = new BufferedHttpEntity(response.getEntity());
                }

                //  logger.debug("POST request " + httpRequestBase + " executed with code: " + statusCode + " and message: " + (entity !=
                //  null ? EntityUtils.toString((HttpEntity)entity) : null));
                long totalRequestTime = System.currentTimeMillis() - requestStartTime;
                logger.debug("Request to Apache Unomi url: " + url + " executed in " + totalRequestTime + "ms");
            }

        if (entity != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            T object = objectMapper.readValue(entity.getContent(), tClass);
            EntityUtils.consume(entity);
            return object;
        } else {
            return null;
        }
    }
    }

    private static Map<String, String> getHeaders(HttpServletRequest httpServletRequest) {
        Map<String, String> headers = new HashMap<>();
        String xff = httpServletRequest.getHeader(ipForwardingHeaderName);
        if (StringUtils.isNotBlank(xff)) {
            headers.put("X-Forwarded-For", xff);
            logger.debug("X-Forwarded-For header value set to " + xff);
        }
        headers.put("User-Agent", httpServletRequest.getHeader("User-Agent"));
        return headers;
    }


    @Override
    public Map<String, Object> getProfileProperties(RenderContext renderContext) throws IOException {
        String siteKey = renderContext.getSite().getSiteKey();
        HttpServletRequest request =renderContext.getRequest();
        ContextServerSettings contextServerSettings = jExperienceConfigFactory.getSettings(siteKey);
        String url = APIProfilesPath+getProfileId(request,siteKey,true);
        return executeGetRequest(contextServerSettings, url,null,
                getHeaders(request),
                new LinkedHashMap<>().getClass()) ;
    }


    /**
     * This method will find the profile id and return it
     * @param httpServletRequest HttpServletRequest
     * @param siteKey siteKey
     * @param checkRequestParameters set to true if we want to look for existing profile id in the request parameters and request attributes
     *                               false otherwise, it will only look for existing profile id from cookies.
     * @return String profile id
     */
    @Override
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

        } else if (httpServletRequest.getCookies() != null && jExperienceConfigFactory != null) {
            ContextServerSettings contextServerSettings = jExperienceConfigFactory.getSettings(siteKey);
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

    @Activate
    public void activate(BundleContext bundleContext) {
        logger.info("activating JexperienceProfileService") ;
    }

    @Reference
    public void setContextServerService(ContextServerService contextServerService) {
        this.contextServerService = contextServerService;
    }


    @Reference
    public void setJExperienceConfigFactory(JExperienceConfigFactory jExperienceConfigFactory) {
        this.jExperienceConfigFactory = jExperienceConfigFactory;
    }
}
