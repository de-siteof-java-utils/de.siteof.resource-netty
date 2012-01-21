package de.siteof.resource.netty.util;

import java.util.concurrent.Executors;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

public class NettyUtils {
	
	private static ChannelFactory channelFactory;

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(NettyUtils.class.getName() + "-Shutdown") {
			@Override
			public void run() {
				NettyUtils.shutdown();
			}
			
		});
	}
	
	public static synchronized ChannelFactory getChannelFactory() {
		if (channelFactory == null) {
			channelFactory = new NioClientSocketChannelFactory(
					Executors.newCachedThreadPool(),
					Executors.newCachedThreadPool());
		}
		return channelFactory;
	}
	
	public static synchronized void shutdown() {
		if (channelFactory != null) {
			channelFactory.releaseExternalResources();
		}
	}

}
