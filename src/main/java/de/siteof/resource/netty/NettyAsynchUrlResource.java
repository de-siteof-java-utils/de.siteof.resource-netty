package de.siteof.resource.netty;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.siteof.resource.AbstractResource;
import de.siteof.resource.ICookieManager;
import de.siteof.resource.event.IResourceListener;
import de.siteof.resource.event.ResourceLoaderEvent;
import de.siteof.task.ITaskManager;

public class NettyAsynchUrlResource extends AbstractResource {

	private static final Log log = LogFactory.getLog(NettyAsynchUrlResource.class);

	private final ICookieManager cookieManager;

	public NettyAsynchUrlResource(String name, ICookieManager cookieManager,
			ITaskManager taskManager) {
		super(name, taskManager);
		this.cookieManager = cookieManager;
	}

	@Override
	public long getSize() {
		return 0;
	}

	@Override
	public InputStream getResourceAsStream() throws IOException {
		return null;
	}

	@Override
	public void getResourceBytes(
			final IResourceListener<ResourceLoaderEvent<byte[]>> listener)
			throws IOException {
//		try {
			ByteArrayNettyResourceClient client = new ByteArrayNettyResourceClient(listener);
			client.execute(this);
//			
//			final URI uri = new URI(this.getName());
//
//			String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
//			final String host = uri.getHost() == null ? "localhost" : uri.getHost();
//			int port = uri.getPort();
//			if (port == -1) {
//				if (scheme.equalsIgnoreCase("http")) {
//					port = 80;
//				} else if (scheme.equalsIgnoreCase("https")) {
//					port = 443;
//				}
//			}
//
//			if (!scheme.equalsIgnoreCase("http")
//					&& !scheme.equalsIgnoreCase("https")) {
//				listener.onResourceEvent(new ResourceLoaderEvent<byte[]>(this,
//						new IOException("unsupported scheme: " + scheme)));
//			}
//
//			boolean ssl = scheme.equalsIgnoreCase("https");
//
//			// Configure the client.
//			final ClientBootstrap bootstrap = new ClientBootstrap(NettyUtils.getChannelFactory());
//
//			// Set up the event pipeline factory.
//			bootstrap.setPipelineFactory(new NettyClientPipelineFactory(
//					this, listener, ssl));
//			
//			// Start the connection attempt.
//			ChannelFuture future = bootstrap.connect(new InetSocketAddress(host,
//					port));
////			notifyDownloadStart();
//
//			future.addListener(new ChannelFutureListener() {
//				
//				@Override
//				public void operationComplete(ChannelFuture future) throws Exception {
//					if (future.isDone()) {
//						if (!future.isSuccess()) {
//							Throwable e = future.getCause();
//							log.warn("request failed due to " + e, e);
//							listener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
//									NettyAsynchUrlResource.this, e));
//						} else {
//							Channel channel = future.getChannel();
//							
//							// Prepare the HTTP request.
//							HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1,
//									HttpMethod.GET, uri.toASCIIString());
//							request.setHeader(HttpHeaders.Names.HOST, host);
//							request.setHeader(HttpHeaders.Names.CONNECTION,
//									HttpHeaders.Values.CLOSE);
//							request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING,
//									HttpHeaders.Values.GZIP);
//
//							// Set some example cookies.
//							CookieEncoder httpCookieEncoder = new CookieEncoder(false);
//							httpCookieEncoder.addCookie("my-cookie", "foo");
//							httpCookieEncoder.addCookie("another-cookie", "bar");
//							request.setHeader(HttpHeaders.Names.COOKIE, httpCookieEncoder.encode());
//
//							// Send the HTTP request.
//							channel.write(request);
//							
//							// Wait for the server to close the connection.
//							channel.getCloseFuture().addListener(new ChannelFutureListener() {							
//								@Override
//								public void operationComplete(ChannelFuture future) throws Exception {								
////									final String redirectUrl = redirectUrlHolder.get();
////									if (redirectUrl != null) {
////										if (redirectCount >= 10) {
////											log.error("maximum redirects reached");
////											notifyDownloadEnd(false);
////										} else {
////											taskManager.addTask(new AbstractTask() {			
////												@Override
////												public void execute() throws Exception {
////													doDownload(new URI(redirectUrl), redirectCount + 1);
////												}
////											});
////										}
////									} else {
////										notifyDownloadEnd(true);
////									}
////									listener.onResourceEvent(new ResourceLoaderEvent<byte[]>(
////											new byte[0], true));
//								}
//							});
//						}
//					}
//				}
//			});

//		} catch (URISyntaxException e) {
//			listener.onResourceEvent(new ResourceLoaderEvent<byte[]>(this, e));
//		}
	}

}
