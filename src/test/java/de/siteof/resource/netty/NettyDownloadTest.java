package de.siteof.resource.netty;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import junit.framework.Assert;

import org.junit.Test;

import de.siteof.httpclient.test.AbstractResourceDownloaderTest;
import de.siteof.httpclient.test.TestDownloadParameters;
import de.siteof.httpclient.test.TestServlet;
import de.siteof.resource.CookieManager;
import de.siteof.resource.ICookieManager;
import de.siteof.resource.IResourceLoader;
import de.siteof.task.ITaskManager;
import de.siteof.task.ThreadPoolTaskManager;

public class NettyDownloadTest extends AbstractResourceDownloaderTest {

	private IResourceLoader getNettyResourceLoader() {
		return getNettyResourceLoader(new CookieManager());
	}

	private IResourceLoader getNettyResourceLoader(ICookieManager cookieManager) {
		ITaskManager taskManager = new ThreadPoolTaskManager(50);
		taskManager.start();

		IResourceLoader resourceLoader = new NettyAsynchUrlResourceLoader(null, cookieManager, taskManager);
		return resourceLoader;
	}

	@Test
	public void testResourceDownloaderNetty() throws Exception {
		TestDownloadParameters parameters = getTestParameters();
		setServletResponse(parameters);

		IResourceLoader resourceLoader = getNettyResourceLoader();

		doTestResourceDownloader(resourceLoader, parameters);
	}

	@Test
	public void testResourceDownloaderBinary() throws Exception {
		TestDownloadParameters parameters = withBinaryData(getTestParameters(), 1024);
		setServletResponse(parameters);

		IResourceLoader resourceLoader = getNettyResourceLoader();

		doTestResourceDownloader(resourceLoader, parameters);
	}

	@Test
	public void testResourceDownloaderMinimumSize() throws Exception {
		TestDownloadParameters parameters = withBinaryData(getTestParameters(), 1024);
		parameters.minimumSize = parameters.data.length + 1;
		parameters.expectFail = true;
		setServletResponse(parameters);

		IResourceLoader resourceLoader = getNettyResourceLoader();

		doTestResourceDownloader(resourceLoader, parameters);
	}

	@Test
	public void testResourceDownloaderLargeBinary() throws Exception {
		TestDownloadParameters parameters = withBinaryData(getTestParameters(), 10 * 1024);
		setServletResponse(parameters);

		IResourceLoader resourceLoader = getNettyResourceLoader();

		doTestResourceDownloader(resourceLoader, parameters);
	}

	@Test
	public void testResourceDownloaderLargeString() throws Exception {
		TestDownloadParameters parameters = withData(getTestParameters(), getStringData(10 * 1024));
		setServletResponse(parameters);

		IResourceLoader resourceLoader = getNettyResourceLoader();

		doTestResourceDownloader(resourceLoader, parameters);
	}

	@Test
	public void testResourceDownloaderNettyWithRedirect() throws Exception {
		TestDownloadParameters parameters = withRedirect(getTestParameters());
		setServletResponse(parameters);

		IResourceLoader resourceLoader = getNettyResourceLoader();

		doTestResourceDownloader(resourceLoader, parameters);
	}

	@Test
	public void testResourceDownloaderNettyWithRedirectFile() throws Exception {
		TestDownloadParameters parameters = withRedirect(getTestParameters());
		setServletResponse(parameters);

		IResourceLoader resourceLoader = getNettyResourceLoader();

		doTestResourceDownloaderFile(resourceLoader, parameters, parameters.filename);
	}

	@Test
	public void testResourceDownloaderNettyWithCookies() throws Exception {
		TestDownloadParameters parameters = withRedirect(getTestParameters());
		setServletResponse(parameters);

		List<HttpCookie> expectedCookies = Arrays.asList(
				new HttpCookie("cookie1", "value1"),
				new HttpCookie("cookie2", "value2"));
		List<String> expectedCookieNames = new ArrayList<String>(expectedCookies.size());

		ICookieManager cookieManager = new CookieManager();
		for (HttpCookie cookie: expectedCookies) {
			cookie.setDomain("localhost");
			cookie.setPath("/");
			cookieManager.setSessionCookie(cookie);
			expectedCookieNames.add(cookie.getName());
		}
		Collections.sort(expectedCookieNames);

		IResourceLoader resourceLoader = getNettyResourceLoader(cookieManager);

		doTestResourceDownloaderFile(resourceLoader, parameters, parameters.filename);
		Cookie[] cookies = TestServlet.getLastRequestCookies(parameters.path);
		Assert.assertNotNull("cookies", cookies);
		Map<String, Cookie> cookieMap = new HashMap<String, Cookie>();
		for (Cookie cookie: cookies) {
			cookieMap.put(cookie.getName(), cookie);
		}
		List<String> actualCookieNames = new ArrayList<String>(cookieMap.keySet());
		Collections.sort(actualCookieNames);
		Assert.assertEquals("cookieNames", expectedCookieNames, actualCookieNames);
		for (HttpCookie expectedCookie: expectedCookies) {
			Cookie actualCookie = cookieMap.get(expectedCookie.getName());
			Assert.assertNotNull("cookies[" + expectedCookie.getName() + "]", actualCookie);
			Assert.assertEquals("cookies[" + expectedCookie.getName() + "].value",
					expectedCookie.getValue(), actualCookie.getValue());
		}
	}

	@Test
	public void testResourceDownloaderNettyWithRedirectFileNoFilename() throws Exception {
		TestDownloadParameters parameters = withRedirect(getTestParameters());
		parameters.filename = null;
		setServletResponse(parameters);

		IResourceLoader resourceLoader = getNettyResourceLoader();

		doTestResourceDownloaderFile(resourceLoader, parameters, "TestServlet");
	}

	@Test
	public void testResourceLoaderInputStream() throws Exception {
		TestDownloadParameters parameters = withData(getTestParameters(), getStringData(10 * 1024));
		setServletResponse(parameters);

		IResourceLoader resourceLoader = getNettyResourceLoader();

		doTestResourceLoaderStream(resourceLoader, parameters);
	}

}
