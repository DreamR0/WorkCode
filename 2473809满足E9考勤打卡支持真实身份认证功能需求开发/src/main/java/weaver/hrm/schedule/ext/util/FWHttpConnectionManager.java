package weaver.hrm.schedule.ext.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

public class FWHttpConnectionManager {
		
		
		private static HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		
		static {
	}

	public static HttpConnectionManager getInstance() {
		return connectionManager;
	}
	
	public static HttpClient getHttpClient() {
		 HttpClient client = new HttpClient();
         client.getHttpConnectionManager().getParams().setSoTimeout(10*1000);
         client.getHttpConnectionManager().getParams().setConnectionTimeout(10*1000);
		
		return client;
	}

}
