package de.siteof.resource.netty;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;

import de.siteof.resource.IResource;
import de.siteof.resource.IResourceMetaData;
import de.siteof.resource.ResourceMetaData;
import de.siteof.resource.event.IResourceListener;
import de.siteof.resource.event.MetaResourceLoaderEvent;
import de.siteof.resource.event.RedirectResourceLoaderEvent;
import de.siteof.resource.event.ResourceLoaderEvent;

public abstract class NettyClientHandler<T> extends SimpleChannelUpstreamHandler {

	private static final Log log = LogFactory.getLog(NettyClientHandler.class);

	private boolean readingChunks;
	private final IResource resource;
	private final IResourceListener<ResourceLoaderEvent<T>> listener;
	private boolean redirect;

	private final AtomicLong dataReceived = new AtomicLong();

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

	private String cleanFilename(String s) {
		s = s.replace('\\', '/');
		int i = s.lastIndexOf('/');
		if (i >= 0) {
			s = s.substring(i + 1);
		}
		return s;
	}

	protected void onMetaData(IResourceMetaData metaData) {
		fireResourceEvent(new MetaResourceLoaderEvent<T>(resource,
				metaData));
	}

	protected abstract void contentReceived(ChannelHandlerContext context,
			ChannelBuffer content, boolean complete);

	protected abstract void contentComplete();

	protected byte[] getContentBytes(ChannelBuffer content, boolean complete) {
		int readableBytes = content.readableBytes();
		byte[] data = content.array();
		long total = dataReceived.addAndGet(data.length);
		if (log.isDebugEnabled()) {
			log.debug("data.length=" + data.length + ", readableBytes=" + readableBytes +
					", total=" + total + ", complete=" + complete);
		}
		return data;
	}

	private String getHeader(HttpResponse response, String name) {
		String result = response.getHeader(name);
		if (result == null) {
			for (String n: response.getHeaderNames()) {
				if (n.equalsIgnoreCase(name)) {
					result = response.getHeader(n);
				}
			}
		}
		return result;
	}

	private String stripQuotes(String s) {
		String result;
		if ((s.length() >= 2) && (s.startsWith("\"")) && (s.endsWith("\""))) {
			result = s.substring(1, s.length() - 1);
		} else {
			result = s;
		}
		return result;
	}

	@Override
	public void messageReceived(ChannelHandlerContext context, MessageEvent e)
			throws Exception {
		if (!readingChunks) {
			HttpResponse response = (HttpResponse) e.getMessage();

			if (log.isDebugEnabled()) {
				log.debug("status=[" + response.getStatus() + "]");
				log.debug("version=[" + response.getProtocolVersion() + "]");
			}

			if (log.isDebugEnabled()) {
				if (!response.getHeaderNames().isEmpty()) {
					for (String name : response.getHeaderNames()) {
						for (String value : response.getHeaders(name)) {
							log.info("header, name=[" + name + "], value=[" + value + "]");
						}
					}
				}
			}

			ResourceMetaData metaData = new ResourceMetaData();
			metaData.setLength(HttpHeaders.getContentLength(response));
			metaData.setContentType(HttpHeaders.getHeader(response, "Content-Type"));

			HttpResponseStatus status = response.getStatus();
			if ((HttpResponseStatus.MOVED_PERMANENTLY.equals(status)) ||
					(HttpResponseStatus.FOUND.equals(status))) {
				String location = getHeader(response, "Location");
				if (!StringUtils.isEmpty(location)) {
					this.redirect = true;
					redirectTo(location);
				}
			} else {
				String contentDiposition = getHeader(response, "Content-Disposition");
				if (contentDiposition != null) {
					int separatorIndex = contentDiposition.indexOf(';');
					if (separatorIndex >= 0) {
						String dispositionType = contentDiposition.substring(0, separatorIndex).trim();
						String dispositionParameter = contentDiposition.substring(separatorIndex + 1).trim();
						if ("attachment".equalsIgnoreCase(dispositionType)) {
							separatorIndex = dispositionParameter.indexOf('=');
							if (separatorIndex >= 0) {
								String parameterName = dispositionParameter.substring(0, separatorIndex).trim();
								String parameterValue = dispositionParameter.substring(separatorIndex + 1).trim();
								if ("filename".equalsIgnoreCase(parameterName)) {
									parameterValue = stripQuotes(parameterValue);
									if (!parameterValue.isEmpty()) {
										metaData.setName(cleanFilename(parameterValue));
									}
								}
							}
						}
					}
				}
			}

			onMetaData(metaData);

			if (response.isChunked()) {
				readingChunks = true;
				log.debug("CHUNKED CONTENT {");
			} else {
				ChannelBuffer content = response.getContent();
				if (content.readable()) {
					if (log.isDebugEnabled()) {
						log.debug("content=[\n" +
								content.toString(CharsetUtil.UTF_8) +
								"\n]");
					}
					if (!redirect) {
						contentReceived(context, content, true);
					}
				}
			}
		} else {
			HttpChunk chunk = (HttpChunk) e.getMessage();
			if (chunk.isLast()) {
				readingChunks = false;
				if (log.isDebugEnabled()) {
					log.debug("} END OF CHUNKED CONTENT");
				}
				if (!redirect) {
					contentComplete();
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("chunk=[" + chunk.getContent().toString(CharsetUtil.UTF_8) + "]");
				}
				if (!redirect) {
					ChannelBuffer content = chunk.getContent();
					contentReceived(context, content, false);
				}
			}
		}
	}

	public IResource getResource() {
		return resource;
	}
}
