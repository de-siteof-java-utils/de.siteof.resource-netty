package de.siteof.resource.netty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;

import de.siteof.resource.ICookieManager;
import de.siteof.resource.IResource;
import de.siteof.resource.event.IResourceListener;
import de.siteof.resource.event.ResourceLoaderEvent;

public class ByteArrayNettyResourceClient extends
		AbstractNettyResourceClient<byte[]> {

	private class ByteArrayNettyClientHandler extends NettyClientHandler<byte[]> {

		public ByteArrayNettyClientHandler(IResource resource,
				IResourceListener<ResourceLoaderEvent<byte[]>> listener) {
			super(resource, listener);
		}

		@Override
		protected void contentReceived(ChannelHandlerContext context,
				ChannelBuffer content, boolean complete) {
			byte[] data = getContentBytes(content, complete);
			fireResourceEvent(new ResourceLoaderEvent<byte[]>(this.getResource(),
					data, complete));
		}

		@Override
		protected void contentComplete() {
			log.debug("complete");
			fireResourceEvent(new ResourceLoaderEvent<byte[]>(this.getResource(),
					new byte[0], true));
		}

	}

	private static final Log log = LogFactory.getLog(ByteArrayNettyResourceClient.class);

	protected ByteArrayNettyResourceClient(
			ICookieManager cookieManager,
			IResourceListener<ResourceLoaderEvent<byte[]>> listener) {
		super(cookieManager, listener);
	}

	@Override
	protected ChannelHandler createChannelHandler(IResource resource,
			IResourceListener<ResourceLoaderEvent<byte[]>> listener) {
		return new ByteArrayNettyClientHandler(resource, listener);
	}

	@Override
	protected ResourceLoaderEvent<byte[]> getCompleteEvent(IResource resource) {
		return new ResourceLoaderEvent<byte[]>(resource, new byte[0], true);
	}

}
