package de.siteof.resource.netty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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
		byte[] data = this.getResourceBytes();
		InputStream in;
		if (data != null) {
			in = new ByteArrayInputStream(data);
		} else {
			in = null;
		}
		return in;
	}

	@Override
	public void getResourceAsStream(
			IResourceListener<ResourceLoaderEvent<InputStream>> listener)
			throws IOException {
		InputStreamNettyResourceClient client = new InputStreamNettyResourceClient(
				this.getTaskManager(), cookieManager, listener);
		client.execute(this);
	}

	@Override
	public void getResourceBytes(
			final IResourceListener<ResourceLoaderEvent<byte[]>> listener)
			throws IOException {
		ByteArrayNettyResourceClient client = new ByteArrayNettyResourceClient(
				cookieManager, listener);
		client.execute(this);
	}

	@Override
	public byte[] getResourceBytes() throws IOException {
		byte[] data = null;
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final AtomicReference<ResourceLoaderEvent<byte[]>> result =
				new AtomicReference<ResourceLoaderEvent<byte[]>>();
			final CountDownLatch requestCount = new CountDownLatch(1);
			this.getResourceBytes(new IResourceListener<ResourceLoaderEvent<byte[]>>() {
				@Override
				public void onResourceEvent(ResourceLoaderEvent<byte[]> event) {
					result.set(event);
					if (!event.isFailed()) {
						byte[] data = event.getResult();
						if ((data != null) && (data.length > 0)) {
							try {
								out.write(data);
							} catch (IOException e) {
								log.error("failed to write bytes due to " + e, e);
								// change the event to a failed event
								event = new ResourceLoaderEvent<byte[]>(event.getResource(),
										new IOException("failed to write bytes due to", e));
							}
						}
					}
					if ((event.isComplete()) || (event.isFailed())) {
						requestCount.countDown();
					}
				}});
			requestCount.await();
			ResourceLoaderEvent<byte[]> event = result.get();
			if (event.isComplete()) {
				data = out.toByteArray();
			} else {
				Throwable cause = event.getCause();
				if (cause != null) {
					throw new IOException("failed to retrieve bytes - " + cause, cause);
				} else {
					throw new IOException("failed to retrieve bytes, name=[" + this.getName() + "]");
				}
			}
		} catch (InterruptedException e) {
			log.warn("request interrupted, name=[" + this.getName() + "]");
		}
		return data;
	}

}
