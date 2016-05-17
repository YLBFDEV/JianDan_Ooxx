package com.jiandan.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

public class SimpleClient {
	private static WebClient client = new WebClient(BrowserVersion.CHROME);

	public SimpleClient() {
		client.getOptions().setCssEnabled(false);
		client.getOptions().setJavaScriptEnabled(false);
	}

	public Page getPage(String url) {
		try {
			return client.getPage(url);
		} catch (FailingHttpStatusCodeException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Page getPage(WebRequest request) {
		try {
			return client.getPage(request);
		} catch (FailingHttpStatusCodeException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Page getPage(HttpMethod method, String url, List<NameValuePair> params) {
		try {
			WebRequest request = new WebRequest(new URL(url));
			request.setRequestParameters(params);
			request.setHttpMethod(method);
			return client.getPage(request);
		} catch (FailingHttpStatusCodeException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setTimeout(int seconds) {
		client.getOptions().setTimeout(seconds * 1000);
	}

	public WebClient getWebClient() {
		return client;
	}

	public void setProxy(String host, int port) {
		ProxyConfig config = new ProxyConfig();
		config.setProxyHost(host);
		config.setProxyPort(port);
		client.getOptions().setProxyConfig(config);
	}

	public WebResponse loadResponse(WebRequest request) {
		try {
			return client.loadWebResponse(request);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public WebResponse loadResponse(String url) {
		try {
			WebRequest req = new WebRequest(new URL(url));
			return loadResponse(req);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public String getCookies() {
		Set<Cookie> cookies = client.getCookieManager().getCookies();
		StringBuilder sb = new StringBuilder();
		cookies.forEach(x -> {
			sb.append(x.getName() + "=" + x.getValue());
			sb.append(";");
		});
		return sb.toString();
	}

}