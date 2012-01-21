package de.siteof.resource.netty;

import java.io.IOException;

import de.siteof.resource.AsynchUrlResourceLoader;
import de.siteof.resource.ICookieManager;
import de.siteof.resource.IResource;
import de.siteof.resource.IResourceLoader;
import de.siteof.task.ITaskManager;

public class NettyAsynchUrlResourceLoader extends AsynchUrlResourceLoader {

	public NettyAsynchUrlResourceLoader(IResourceLoader parent,
			ICookieManager cookieManager, ITaskManager taskManager) {
		super(parent, cookieManager, taskManager);
	}

	@Override
	protected IResource getUrlResource(String name) throws IOException {
		return new NettyAsynchUrlResource(name, this.getCookieManager(), this.getTaskManager());
	}

}
