package com.jiandan;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.jiandan.utils.RegexUtils;
import com.jiandan.utils.SimpleClient;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.gargoylesoftware.htmlunit.util.UrlUtils;

public class JianDan {
	private static SimpleClient client = new SimpleClient();
	private static String savePath = "D:/meizi/";
	private static final String BASE_URL = "http://jandan.net/ooxx/";

	static class Downloader {
		private volatile int currentPage;
		private BlockingQueue<String> imgUrls = new LinkedBlockingQueue<String>(1000);
		private volatile boolean hasNext = true;

		public Downloader() {
			File f = new File(savePath);
			if (f.isFile() || !f.exists()) {
				f.mkdir();
			}
			WebRequest request = getWebRequest(BASE_URL);
			HtmlPage page;
			try {
				page = (HtmlPage) client.getPage(request);
			} catch (Exception e) {
				checkHuman(request);
				page = (HtmlPage) client.getPage(request);
			}
			HtmlElement comments = page.getDocumentElement();
			HtmlElement currentComment = comments.getOneHtmlElementByAttribute("span", "class", "current-comment-page");
			currentPage = Integer.parseInt(RegexUtils.getMatchedString("\\[(\\d+)\\]", currentComment.asText(), 1));
			getImageUrls(comments);
		}

		/**
		 * 当前页面的HtmlElement
		 * 
		 * @param element
		 * @return
		 */
		public void getImageUrls(HtmlElement element) {
			DomNodeList<HtmlElement> elements = element.getOneHtmlElementByAttribute("ol", "class", "commentlist")
					.getElementsByTagName("li");
			System.out.println(elements.size());
			elements.forEach(e -> {
				List<String> imgs = RegexUtils
						.getMatchedList("<a href=\"(.*?)\" target=\"_blank\" class=\"view_img_link\">", e.asXml(), 1);
				imgUrls.addAll(imgs);
			});
		}

		public boolean hasNext() {
			return hasNext;
		}

		public String next() {
			return UrlUtils.resolveUrl(BASE_URL, "page-" + (--currentPage));
		}

		public void download() {
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (true) {
						goNextPage(next());
						if (imgUrls.size() > 800) {
							System.out.println("还有：" + imgUrls.size() + "个，你们先下着，我休息会。。。");
							try {
								TimeUnit.SECONDS.sleep(30);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						if (!hasNext)
							break;
					}

				}
			}, "productor").start();
			// 消费者
			for (int i = 0; i < 25; i++) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						while (true) {
							try {
								String url = imgUrls.take();
								downloadImage(url);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							if (!hasNext && imgUrls.size() == 0) {
								break;
							}
						}
					}

				}, "consumer_" + i).start();
			}

		}

		public void goNextPage(String url) {
			System.out.println("--------currentPage ----------: " + currentPage);
			WebRequest req = getWebRequest(url);
			HtmlPage page;
			try {
				page = (HtmlPage) client.getPage(req);
				if (page == null || StringUtils.contains(page.asText(), "就看到这里了")) {
					hasNext = false;
				}
				HtmlElement comments = page.getDocumentElement().getOneHtmlElementByAttribute("div", "id", "comments");
				getImageUrls(comments);
			} catch (Exception e) {
				checkHuman(url);
				goNextPage(url);
			}
		}
	}

	/**
	 * 检测是否是人类
	 * 
	 * @param url
	 * @return
	 */
	public static boolean checkHuman(String url) {
		try {
			WebRequest req = getWebRequest(url);
			return checkHuman(req);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 检测是否是人类
	 * 
	 * @param request
	 * @return
	 */
	public static boolean checkHuman(WebRequest request) {
		String url = "http://jandan.net/block.php?action=check_human";
		String resp = client.loadResponse(request).getContentAsString();
		String hash = RegexUtils.getMatchedString("<input type=\"hidden\" name=\"hash\" value=\"(.*?)\" >", resp, 1);
		if (StringUtils.isBlank(hash)) {
			return false;
		}
		WebRequest req = getWebRequest(url);
		req.setAdditionalHeader("Referer", "http://jandan.net/block.php?from=" + request.getUrl().toString());
		req.setAdditionalHeader("Origin", "http://jandan.net");
		req.setAdditionalHeader("Cache-Control", "max-age=0");
		req.setAdditionalHeader("Cookie", client.getCookies());
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new NameValuePair("hash", hash));
		params.add(new NameValuePair("from", request.getUrl().toString()));
		req.setRequestParameters(params);
		req.setHttpMethod(HttpMethod.POST);
		Page p = client.getPage(req);
		String checkResult = p.getWebResponse().getContentAsString();
		if (StringUtils.contains(checkResult, "验证失败")) {
			System.out.println("好吧，我承认我不是人类");
			return false;
		}
		System.out.println("人家明明就是人类嘛！");
		return true;
	}

	/**
	 * 根据url构造请求
	 * 
	 * @param url
	 * @return
	 */
	public static WebRequest getWebRequest(String url) {
		try {
			WebRequest request = new WebRequest(new URL(url));
			request.setAdditionalHeader("Host", "jandan.net");
			request.setAdditionalHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			request.setAdditionalHeader("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6");
			request.setAdditionalHeader("Connection", "keep-alive");
			request.setAdditionalHeader("Accept-Encoding", "gzip, deflate, sdch");
			request.setAdditionalHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.94 Safari/537.36");
			request.setAdditionalHeader("Upgrade-Insecure-Requests", "1");
			return request;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 将图片下载到本地
	 * 
	 * @param imageUrl
	 */
	public static void downloadImage(String imageUrl) {
		String fileName = StringUtils.substring(imageUrl, imageUrl.lastIndexOf("/"));
		fileName = Paths.get(savePath, fileName).toString();
		if (Files.exists(Paths.get(fileName))) {
			return;
		}
		try (InputStream input = client.getPage(imageUrl).getWebResponse().getContentAsStream();
				BufferedInputStream bis = new BufferedInputStream(input);
				FileOutputStream output = new FileOutputStream(fileName);
				BufferedOutputStream bos = new BufferedOutputStream(output);) {
			byte[] buff = new byte[4096];
			int len;
			while ((len = bis.read(buff, 0, 4096)) != -1) {
				bos.write(buff, 0, len);
			}
			bos.flush();
		} catch (Exception e) {
			if (checkHuman(imageUrl)) {
				downloadImage(imageUrl);
			} else {
				try {
					Files.deleteIfExists(Paths.get(fileName));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		System.out.println("完成下载:" + imageUrl);
	}

	public void startDownload() {
		client.getWebClient().getCookieManager().clearCookies();
		client.setTimeout(300);
		Downloader dlr = new Downloader();
		dlr.download();
	}
}
