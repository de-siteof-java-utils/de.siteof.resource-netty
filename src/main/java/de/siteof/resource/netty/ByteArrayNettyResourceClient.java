package de.siteof.resource.netty;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandler;

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
		protected void contentReceived(ChannelBuffer content, boolean complete) {
			fireResourceEvent(new ResourceLoaderEvent<byte[]>(this.getResource(),
					content.array(), true));
		}
		
	}
	
	protected ByteArrayNettyResourceClient(
			IResourceListener<ResourceLoaderEvent<byte[]>> listener) {
		super(listener);
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
