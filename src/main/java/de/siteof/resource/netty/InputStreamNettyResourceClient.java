package de.siteof.resource.netty;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;

import de.siteof.resource.ICookieManager;
import de.siteof.resource.IResource;
import de.siteof.resource.event.IResourceListener;
import de.siteof.resource.event.ResourceLoaderEvent;
import de.siteof.task.AbstractTask;
import de.siteof.task.ITaskManager;

public class InputStreamNettyResourceClient extends
		AbstractNettyResourceClient<InputStream> {

	private class ByteArrayNettyClientHandler extends NettyClientHandler<InputStream> {

		private final AtomicReference<InputStream> in = new AtomicReference<InputStream>();
		private final AtomicReference<OutputStream> out = new AtomicReference<OutputStream>();

		public ByteArrayNettyClientHandler(IResource resource,
				IResourceListener<ResourceLoaderEvent<InputStream>> listener) {
			super(resource, listener);
		}

		@Override
		protected void contentReceived(final ChannelHandlerContext context,
				ChannelBuffer content, boolean complete) {
			try {
				byte[] data = getContentBytes(content, complete);

				if ((data != null) && (data.length > 0)) {
					InputStream in = this.in.get();
					if (in != null) {
						OutputStream out = this.out.get();
						out.write(data);
					} else {
						final PipedOutputStream out = new PipedOutputStream();
						this.out.set(out);
						final Object inputCreatedLock = new Object();

						taskManager.addTask(new AbstractTask() {
							@Override
							public void execute() throws Exception {
								InputStream in = new PipedInputStream(out);
								in = new FilterInputStream(in) {

									@Override
									public int available() throws IOException {
										return 0;
									}

									@Override
									public void close() throws IOException {
										try {
											super.close();
										} catch (Exception e) {
											log.debug("failed to close input stream - " + e, e);
										}
										try {
											cancel(context);
										} catch (Exception e) {
											log.debug("failed to cancel connection - " + e, e);
										}
									}
								};
								ByteArrayNettyClientHandler.this.in.set(in);
								synchronized (inputCreatedLock) {
									inputCreatedLock.notify();
								}
								fireResourceEvent(new ResourceLoaderEvent<InputStream>(
										ByteArrayNettyClientHandler.this.getResource(),
										in, true));
							}
						});

						// wait until the input stream is created
						synchronized (inputCreatedLock) {
							inputCreatedLock.wait();
						}

						out.write(data);
					}
				}

				if (complete) {
					contentComplete();
				}
			} catch (Exception e) {
				log.error("failed due to " + e, e);
				cancel(context);
			}
		}

		@Override
		protected void contentComplete() {
			log.debug("complete");
			InputStream in = this.in.get();
			if (in == null) {
				// no content event sent yet, sent a dummy input stream
				fireResourceEvent(new ResourceLoaderEvent<InputStream>(this.getResource(),
						new ByteArrayInputStream(new byte[0]), true));
			}
			OutputStream out = this.out.get();
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					log.error("close failed", e);
				}
			}
		}

	}

	private static final Log log = LogFactory.getLog(InputStreamNettyResourceClient.class);

	private final ITaskManager taskManager;

	protected InputStreamNettyResourceClient(
			ITaskManager taskManager,
			ICookieManager cookieManager,
			IResourceListener<ResourceLoaderEvent<InputStream>> listener) {
		super(cookieManager, listener);
		this.taskManager = taskManager;
	}

	@Override
	protected ChannelHandler createChannelHandler(IResource resource,
			IResourceListener<ResourceLoaderEvent<InputStream>> listener) {
		return new ByteArrayNettyClientHandler(resource, listener);
	}

	@Override
	protected ResourceLoaderEvent<InputStream> getCompleteEvent(IResource resource) {
		return new ResourceLoaderEvent<InputStream>(resource, null, true);
	}

}
