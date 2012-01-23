package de.siteof.resource.netty;

import java.io.IOException;
import java.io.InputStream;

import de.siteof.resource.AbstractResource;
import de.siteof.resource.ICookieManager;
import de.siteof.resource.event.IResourceListener;
import de.siteof.resource.event.ResourceLoaderEvent;
import de.siteof.task.ITaskManager;

public class NettyAsynchUrlResource extends AbstractResource {

//	private static final Log log = LogFactory.getLog(NettyAsynchUrlResource.class);

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
		ByteArrayNettyResourceClient client = new ByteArrayNettyResourceClient(
				cookieManager, listener);
		client.execute(this);
	}

}
