package de.siteof.resource.netty;

import java.io.IOException;

import de.siteof.resource.AbstractUrlResourceLoader;
import de.siteof.resource.ICookieManager;
import de.siteof.resource.IResource;
import de.siteof.resource.IResourceLoader;
import de.siteof.task.ITaskManager;

public class NettyAsynchUrlResourceLoader extends AbstractUrlResourceLoader {

	private final ICookieManager cookieManager;

	public NettyAsynchUrlResourceLoader(IResourceLoader parent,
			ICookieManager cookieManager, ITaskManager taskManager) {
		super(parent, taskManager);
		this.cookieManager = cookieManager;
	}

	@Override
	protected IResource getUrlResource(String name) throws IOException {
		return new NettyAsynchUrlResource(name, this.getCookieManager(), this.getTaskManager());
	}

	public ICookieManager getCookieManager() {
		return cookieManager;
	}

}
