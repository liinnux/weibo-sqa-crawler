package com.cike.weibosqa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class UtoCN {

	public static void main(String[] args) throws ClientProtocolException, IOException {
			// 核心应用类   
	       	HttpClient httpClient = new DefaultHttpClient();   
	  
	        // HTTP请求   
	        HttpUriRequest request =   
	                new HttpGet("http://weibo.com/p/aj/mblog/mbloglist?domain=100505&pre_page=1&page=1&count=15&pagebar=1&max_msign=&filtered_min_id=&pl_name=Pl_Official_LeftProfileFeed__11&id=1005051220291284&script_uri=/p/1005051220291284/weibo&feed_type=0&from=page_100505&mod=TAB");   
	 
	        HttpResponse response = httpClient.execute(request);
	         
	        // 从response中取出HttpEntity对象
	        HttpEntity entity = response.getEntity();
	        
	         
	        // 取出服务器返回的数据流
	        try {
				InputStream stream = entity.getContent();
				BufferedReader bf = new BufferedReader(
	                      new InputStreamReader(stream));

				String s = null;
				while ((s=bf.readLine()) != null)
					System.out.println(s);
				
				
				
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
			System.out.println(UcdtoString("\u52a0\u8f7d\u5931\u8d25\uff0c"));
		

	}
	
	public static String UcdtoString(String ucd) {
		String str=ucd; 
		try {
			str = new String(str.getBytes("Unicode"),"UTF-16");
			return str;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		
	}
}
