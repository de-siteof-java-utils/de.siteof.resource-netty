package de.siteof.resource.netty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

import de.siteof.resource.ICookieManager;
import de.siteof.resource.IResource;
import de.siteof.resource.event.IResourceListener;
import de.siteof.resource.event.RedirectResourceLoaderEvent;
import de.siteof.resource.event.ResourceLoaderEvent;
import de.siteof.resource.netty.util.NettyUtils;
import de.siteof.task.AbstractTask;
import de.siteof.task.ITaskManager;
import de.siteof.task.SingleThreadTaskManager;

public abstract class AbstractNettyResourceClient<T> {

	private static final Log log = LogFactory.getLog(AbstractNettyResourceClient.class);

	private static ITaskManager redirectTaskManager = new SingleThreadTaskManager();

	private final ICookieManager cookieManager;
	private final IResourceListener<ResourceLoaderEvent<T>> listener;
	private final AtomicReference<String> redirectUrlHolder = new AtomicReference<String>();
	private final AtomicBoolean done = new AtomicBoolean();

	protected AbstractNettyResourceClient(
			ICookieManager cookieManager,
			IResourceListener<ResourceLoaderEvent<T>> listener) {
		this.cookieManager = cookieManager;
		this.listener = createProxyListener(listener);
	}

	private IResourceListener<ResourceLoaderEvent<T>> createProxyListener(
			final IResourceListener<ResourceLoaderEvent<T>> listener) {
		return new IResourceListener<ResourceLoaderEvent<T>>() {
			@Override
			public void onResourceEvent(ResourceLoaderEvent<T> event) {
				if (log.isDebugEnabled()) {
					log.debug("event=" + event);
				}
				if (event instanceof RedirectResourceLoaderEvent) {
					String redirectUrl = ((RedirectResourceLoaderEvent<?>) event).getRedirectUrl();
					redirectUrlHolder.set(redirectUrl);
				} else {
					if (redirectUrlHolder.get() == null) {
						if ((event.isComplete()) || (event.isFailed())) {
							if (done.compareAndSet(false, true)) {
								listener.onResourceEvent(event);
							} else {
								log.warn("response already complete, but received event=" + event);
							}
						} else {
							listener.onResourceEvent(event);
						}
					}
				}
			}
		};
	}

	private void error(IResource resource, Throwable e) {
		if (!done.compareAndSet(false, true)) {
			listener.onResourceEvent(new ResourceLoaderEvent<T>(resource, e));
		}
	}

	protected abstract ResourceLoaderEvent<T> getCompleteEvent(IResource resource);

	private void complete(IResource resource) {
		if (!done.compareAndSet(false, true)) {
			listener.onResourceEvent(this.getCompleteEvent(resource));
		}
	}

	public void execute(IResource resource) {
		execute(resource, resource.getName(), 0);
	}

	private void execute(IResource resource, String url, int redirectCount) {
		try {
			execute(resource, new URI(url), redirectCount);
		} catch (URISyntaxException e) {
			error(resource, e);
		}
	}

	protected abstract ChannelHandler createChannelHandler(IResource resource,
			IResourceListener<ResourceLoaderEvent<T>> listener);

	private void addCookies(String url, HttpRequest request) {
		if (this.cookieManager != null) {
			String[] cookies = this.cookieManager.getCookiesForName(url);
			if (cookies != null) {
				for (int i = 0; i < cookies.length; i++) {
					request.addHeader("Cookie", cookies[i]);
				}
			}
		}
//		CookieEncoder httpCookieEncoder = new CookieEncoder(false);
//		httpCookieEncoder.addCookie("my-cookie", "foo");
//		httpCookieEncoder.addCookie("another-cookie", "bar");
//		request.setHeader(HttpHeaders.Names.COOKIE, httpCookieEncoder.encode());
	}

	private void execute(final IResource resource, final URI uri, final int redirectCount) {
		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
		final String host = uri.getHost() == null ? "localhost" : uri.getHost();
		int port = uri.getPort();
		if (port == -1) {
			if (scheme.equalsIgnoreCase("http")) {
				port = 80;
			} else if (scheme.equalsIgnoreCase("https")) {
				port = 443;
			}
		}

		if (!scheme.equalsIgnoreCase("http")
				&& !scheme.equalsIgnoreCase("https")) {
			error(resource, new IOException("unsupported scheme: " + scheme));
		}

		boolean ssl = scheme.equalsIgnoreCase("https");

		// Configure the client.
		final ClientBootstrap bootstrap = new ClientBootstrap(NettyUtils.getChannelFactory());

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new NettyClientPipelineFactory(ssl) {
			@Override
			protected ChannelHandler createChannelHandler() {
				return AbstractNettyResourceClient.this.createChannelHandler(
						resource, listener);
			}
		});

		// Start the connection attempt.
		ChannelFuture future = bootstrap.connect(new InetSocketAddress(host,
				port));
//			notifyDownloadStart();

		future.addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isDone()) {
					if (!future.isSuccess()) {
						Throwable e = future.getCause();
						log.warn("request failed due to " + e, e);
						error(resource, e);
					} else {
						Channel channel = future.getChannel();

						// Prepare the HTTP request.
						HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
								HttpMethod.GET, uri.toASCIIString());
						request.setHeader(HttpHeaders.Names.HOST, host);
						request.setHeader(HttpHeaders.Names.CONNECTION,
								HttpHeaders.Values.CLOSE);
						request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING,
								HttpHeaders.Values.GZIP);

						// Set some example cookies.
						addCookies(uri.toASCIIString(), request);

						// Send the HTTP request.
						channel.write(request);

						// Wait for the server to close the connection.
						channel.getCloseFuture().addListener(new ChannelFutureListener() {
							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
									final String redirectUrl = redirectUrlHolder.getAndSet(null);
									if (redirectUrl != null) {
										if (redirectCount >= 10) {
											log.error("maximum redirects reached");
											error(resource, new IOException("maximum redirects reached"));
										} else {
											redirectTaskManager.addTask(new AbstractTask() {
												@Override
												public void execute() throws Exception {
													AbstractNettyResourceClient.this.execute(
															resource, redirectUrl, redirectCount + 1);
												}
											});
										}
									} else {
										complete(resource);
									}
							}
						});
					}
				}
			}
		});
	}

}
