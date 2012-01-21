package de.siteof.resource.netty;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;

import de.siteof.resource.IResource;
import de.siteof.resource.event.IResourceListener;
import de.siteof.resource.event.RedirectResourceLoaderEvent;
import de.siteof.resource.event.ResourceLoaderEvent;

public abstract class NettyClientHandler<T> extends SimpleChannelUpstreamHandler {

	private static final Log log = LogFactory.getLog(NettyClientHandler.class);

	private boolean readingChunks;
	private final IResource resource;
	private final IResourceListener<ResourceLoaderEvent<T>> listener;
	private boolean redirect;
	
	public NettyClientHandler(IResource resource,
			IResourceListener<ResourceLoaderEvent<T>> listener) {
		this.resource = resource;
		this.listener = listener;
	}
	
	protected void fireResourceEvent(ResourceLoaderEvent<T> event) {
		listener.onResourceEvent(event);
	}
	
	protected void redirectTo(String location) {
		fireResourceEvent(new RedirectResourceLoaderEvent<T>(resource,
				location));
	}
	
	protected abstract void contentReceived(ChannelBuffer content, boolean complete);

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object message;
		if (!readingChunks) {
			HttpResponse response = (HttpResponse) e.getMessage();

			if (log.isInfoEnabled()) {
				log.info("status=[" + response.getStatus() + "]");
				log.info("version=[" + response.getProtocolVersion() + "]");
			}

			if (log.isInfoEnabled()) {
				if (!response.getHeaderNames().isEmpty()) {				
					for (String name : response.getHeaderNames()) {
						for (String value : response.getHeaders(name)) {
							log.info("header, name=[" + name + "], value=[" + value + "]");
						}
					}
				}
			}
			
			HttpResponseStatus status = response.getStatus();
			if ((HttpResponseStatus.MOVED_PERMANENTLY.equals(status)) ||
					(HttpResponseStatus.FOUND.equals(status))) {
				String location = response.getHeader("Location");
				if (!StringUtils.isEmpty(location)) {
					this.redirect = true;
					redirectTo(location);
				}
			}

			if (response.isChunked()) {
				readingChunks = true;
				log.info("CHUNKED CONTENT {");
			} else {
				ChannelBuffer content = response.getContent();
				if (content.readable()) {
					if (log.isInfoEnabled()) {
						log.info("content=[\n" +
								content.toString(CharsetUtil.UTF_8) +
								"\n]");
					}
					if (!redirect) {
						contentReceived(content, true);
					}
				}
			}
		} else {
			HttpChunk chunk = (HttpChunk) e.getMessage();
			if (chunk.isLast()) {
				readingChunks = false;
				if (log.isInfoEnabled()) {
					log.info("} END OF CHUNKED CONTENT");
				}
				if (!redirect) {
					ChannelBuffer content = chunk.getContent();
					contentReceived(content, true);
				}
			} else {
				if (log.isInfoEnabled()) {
					log.info("chunk=[" + chunk.getContent().toString(CharsetUtil.UTF_8) + "]");
				}
				if (!redirect) {
					ChannelBuffer content = chunk.getContent();
					contentReceived(content, false);
				}
			}
		}
	}

	public IResource getResource() {
		return resource;
	}
}
