/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.jexperienceProfile.service;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jahia.modules.jexperience.admin.ContextServerHttpException;
import org.jahia.modules.jexperience.admin.ContextServerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * @author dgaillard
 */
public class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static CloseableHttpClient initHttpClient(ContextServerSettings contextServerSettings, boolean withTimeOut, int poolMaxTotal) {
        long requestStartTime = System.currentTimeMillis();
        BasicCredentialsProvider credsProvider = null;
        if (contextServerSettings.getContextServerUsername() != null && contextServerSettings.getContextServerPassword() != null) {
            credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(contextServerSettings.getContextServerUsername(), contextServerSettings.getContextServerPassword()));
        }
        HttpClientBuilder httpClientBuilder = HttpClients.custom().useSystemProperties().setDefaultCredentialsProvider(credsProvider);

        if (contextServerSettings.getContextServerTrustAllCertificates()) {
            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs,
                                                   String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs,
                                                   String authType) {
                    }
                }}, new SecureRandom());

                Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER))
                        .build();

                PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
                poolingHttpClientConnectionManager.setMaxTotal(poolMaxTotal);

                httpClientBuilder.setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                        .setConnectionManager(poolingHttpClientConnectionManager);

            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                logger.error("Error creating SSL Context", e);
            }
        } else {
            PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
            poolingHttpClientConnectionManager.setMaxTotal(poolMaxTotal);
            httpClientBuilder.setConnectionManager(poolingHttpClientConnectionManager);
        }

        int timeout;
        if (withTimeOut && contextServerSettings.getTimeoutInMilliseconds() != -1) {
            timeout = (int) contextServerSettings.getTimeoutInMilliseconds();
        } else {
            timeout = 30000;
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);

        if (logger.isDebugEnabled()) {
            long totalRequestTime = System.currentTimeMillis() - requestStartTime;
            logger.debug("Init HttpClient executed in " + totalRequestTime + "ms");
        }

        return httpClientBuilder.build();
    }

    public static void closeHttpClient(CloseableHttpClient httpClient) {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            logger.error("Could not close httpClient: " + httpClient, e);
        }
    }

    public static HttpEntity executeGetRequest(CloseableHttpClient httpClient, String url, List<Cookie> cookies, Map<String, String> headers) throws IOException {
        HttpGet httpGet = new HttpGet(url);

        return getHttpEntity(httpClient, url, cookies, headers, httpGet);
    }

    public static HttpEntity executeDeleteRequest(CloseableHttpClient httpClient, String url, List<Cookie> cookies, Map<String, String> headers) throws IOException {
        HttpDelete httpDelete = new HttpDelete(url);

        return getHttpEntity(httpClient, url, cookies, headers, httpDelete);
    }

    public static HttpEntity executePostRequest(CloseableHttpClient httpClient, String url, String jsonData, List<Cookie> cookies, Map<String, String> headers) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("accept", "application/json");

        if (jsonData != null) {
            StringEntity input = new StringEntity(jsonData);
            input.setContentType("application/json");
            httpPost.setEntity(input);
        }

        return getHttpEntity(httpClient, url, cookies, headers, httpPost);
    }

    private static HttpEntity getHttpEntity(CloseableHttpClient httpClient, String url, List<Cookie> cookies, Map<String, String> headers, HttpRequestBase httpRequestBase) throws IOException {
        long requestStartTime = System.currentTimeMillis();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpRequestBase.setHeader(entry.getKey(), entry.getValue());
            }
        }

        CloseableHttpResponse response;
        if (cookies != null) {
            HttpContext localContext = setupCookies(cookies);
            response = httpClient.execute(httpRequestBase, localContext);
        } else {
            response = httpClient.execute(httpRequestBase);
        }

        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 400) {
            if (statusCode == HttpServletResponse.SC_UNAUTHORIZED) {
                throw new ContextServerHttpException("Error invalid username and/or password", null, statusCode);
            } else {
                throw new ContextServerHttpException("Couldn't execute " + httpRequestBase + " response: " + EntityUtils.toString(response.getEntity()), null, statusCode);
            }
        }

        HttpEntity entity = response.getEntity();
        if (logger.isDebugEnabled()) {
            if (entity !=null) {
                entity = new BufferedHttpEntity(response.getEntity());
            }
            logger.debug("POST request " + httpRequestBase + " executed with code: " + statusCode + " and message: " + (entity!=null?EntityUtils.toString(entity):null));

            long totalRequestTime = System.currentTimeMillis() - requestStartTime;
            logger.debug("Request to Apache Unomi url: " + url + " executed in " + totalRequestTime + "ms");
        }

        return entity;
    }

    public static HttpContext setupCookies(List<Cookie> cookies) {
        HttpContext localContext = new BasicHttpContext();
        if (cookies != null) {
            CookieStore cookieStore = new BasicCookieStore();
            for (Cookie cookie : cookies) {
                BasicClientCookie basicClientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());

                if (cookie.getDomain() != null && cookie.getDomain().length() > 0) {
                    basicClientCookie.setDomain(cookie.getDomain());
                }
                if (cookie.getPath() != null && cookie.getPath().length() > 0) {
                    basicClientCookie.setPath(cookie.getPath());
                }

                cookieStore.addCookie(basicClientCookie);
            }
            localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
        }
        return localContext;
    }
}
