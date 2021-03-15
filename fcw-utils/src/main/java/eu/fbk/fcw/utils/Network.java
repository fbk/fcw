package eu.fbk.fcw.utils;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by alessio on 07/02/17.
 */

public class Network {

    private static final Logger LOGGER = LoggerFactory.getLogger(Network.class);

    synchronized public static String request(String requestURL, @Nullable Map<String, String> postMap)
            throws IOException {
        LOGGER.debug(requestURL.toString());

        HttpRequestBase request;

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        if (postMap == null) {
            request = new HttpGet(requestURL);
        } else {
            request = new HttpPost(requestURL);

            List<NameValuePair> nvps = new ArrayList<>();
            for (String key : postMap.keySet()) {
                nvps.add(new BasicNameValuePair(key, postMap.get(key)));
            }

            ((HttpPost) request).setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
        }

        CloseableHttpResponse response = httpClient.execute(request);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        String inputLine;
        StringBuilder a = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            a.append(inputLine);
        }
        in.close();
        return a.toString();

    }

    synchronized public static String request(String requestURL, @Nullable String post)
            throws IOException {
        LOGGER.debug(requestURL.toString());

        HttpRequestBase request;

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        if (post == null) {
            request = new HttpGet(requestURL);
        } else {
            request = new HttpPost(requestURL);

            ((HttpPost) request).setEntity(new StringEntity(post));
        }

        CloseableHttpResponse response = httpClient.execute(request);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        String inputLine;
        StringBuilder a = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            a.append(inputLine);
        }
        in.close();
        return a.toString();

    }

}
