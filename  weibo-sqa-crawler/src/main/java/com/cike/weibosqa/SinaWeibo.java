package com.cike.weibosqa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.alibaba.fastjson.JSONObject;


public class SinaWeibo {
	private static HttpClient client;
	private String username;	//登录帐号(明文)
	private String password;	//登录密码(明文)
	private String su;			//登录帐号(Base64加密)
	private String sp;			//登录密码(各种参数RSA加密后的密文)
	private long servertime;	//初始登录时，服务器返回的时间戳,用以密码加密以及登录用
	private String nonce;		//初始登录时，服务器返回的一串字符，用以密码加密以及登录用
	private String rsakv;		//初始登录时，服务器返回的一串字符，用以密码加密以及登录用
	private String pubkey;		//初始登录时，服务器返回的RSA公钥
	
	private String errInfo;		//登录失败时的错误信息
	private String location;	//登录成功后的跳转连接
	
	public SinaWeibo(String username,String password){
		client = new DefaultHttpClient();
		this.username = username;
		this.password = password;
	}
	
	
	/**
	 * 初始登录信息<br>
	 * 返回false说明初始失败
	 * @return
	 */
	public boolean preLogin(){
		boolean flag = false;
		try {
			su = Base64.encodeBase64String(URLEncoder.encode(username, "UTF-8").getBytes());
			String url = "http://login.sina.com.cn/sso/prelogin.php?entry=weibo&rsakt=mod&checkpin=1&client=ssologin.js(v1.4.5)&_="+getTimestamp();
			url += "&su="+su;
			String content;
			content = HttpTools.getRequest(client, url);
			//System.out.println(content);
			JSONObject json = JSONObject.parseObject(content);
			servertime = json.getLongValue("servertime");
			nonce = json.getString("nonce");
			rsakv = json.getString("rsakv");
			pubkey = json.getString("pubkey");
			flag = encodePwd();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return flag;
	}
	
	/**
	 * 登录
	 * @return true:登录成功
	 */
	public boolean login(){
		if(preLogin()){
			String url = "http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.5)";
			List<NameValuePair> parms = new ArrayList<NameValuePair>();
			parms.add(new BasicNameValuePair("entry","weibo"));
			parms.add(new BasicNameValuePair("gateway","1"));
			parms.add(new BasicNameValuePair("from",""));
			parms.add(new BasicNameValuePair("savestate","7"));
			parms.add(new BasicNameValuePair("useticket","1"));
			parms.add(new BasicNameValuePair("pagerefer","http://login.sina.com.cn/sso/logout.php?entry=miniblog&r=http%3A%2F%2Fweibo.com%2Flogout.php%3Fbackurl%3D%2F"));
			parms.add(new BasicNameValuePair("vsnf","1"));
			parms.add(new BasicNameValuePair("su",su));
			parms.add(new BasicNameValuePair("service","miniblog"));
			parms.add(new BasicNameValuePair("servertime",servertime+""));
			parms.add(new BasicNameValuePair("nonce",nonce));
			parms.add(new BasicNameValuePair("pwencode","rsa2"));
			parms.add(new BasicNameValuePair("rsakv",rsakv));
			parms.add(new BasicNameValuePair("sp",sp));
			parms.add(new BasicNameValuePair("encoding","UTF-8"));
			parms.add(new BasicNameValuePair("prelt","182"));
			parms.add(new BasicNameValuePair("url","http://weibo.com/ajaxlogin.php?framelogin=1&callback=parent.sinaSSOController.feedBackUrlCallBack"));
			parms.add(new BasicNameValuePair("returntype","META"));
			try {
				String content = HttpTools.postRequest(client, url, parms);
				//System.out.println(content);
				String regex = "location\\.replace\\(\"(.+?)\"\\);";
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(content);
				if(m.find()){
					location = m.group(1);
					if(location.contains("reason=")){
						errInfo = location.substring(location.indexOf("reason=")+7);
						errInfo = URLDecoder.decode(errInfo, "GBk");
					}else{
						String result = HttpTools.getRequest(client, location);
						System.out.println(result);
						return true;
					}
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * 密码进行RSA加密<br>
	 * 返回false说明加密失败
	 * @return
	 */
	private boolean encodePwd(){
		ScriptEngineManager sem = new ScriptEngineManager();
		ScriptEngine se = sem.getEngineByName("javascript");
		try {
			se.eval(new FileReader("encoder.js"));
			Invocable invocableEngine = (Invocable) se;
			String callbackvalue=(String) invocableEngine.invokeFunction("encodePwd",pubkey,servertime,nonce,password);
			sp = callbackvalue;
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("加密脚本encoder.sj未找到");
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		errInfo = "密码加密失败";
		return false;
	}
	
	public String getErrInfo() {
		return errInfo;
	}
	
	/**
	 * 获取时间戳
	 * @return
	 */
	private long getTimestamp(){
		Date now = new Date();
		return now.getTime();
	}
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		
		/**
		 * 新浪微博登陆（需要登录获取cookie才能爬取数据）
		 */
		SinaWeibo weibo = new SinaWeibo("15920411869","ws199312231805");
		if(weibo.login()){
			System.out.println("登录成功");
		}else{
			System.out.println("登录失败："+weibo.getErrInfo());
		}

		/*爬取关注/粉丝名单
		 * 
		 for(int i=1;;i++){
			String req = "http://weibo.com/1775020471/myfollow?t=1&f=1&page="+i;
			String file = "C:/Users/Qixuan/Desktop/weibo_data/friends/qixuanfollow"+i+".html";
			
			getHTML(req, file);
			
			System.out.println("第"+i+"页");
			if(!hasNextpage(req))
				break;
		}*/
		
		
		getAllWeibos("1890040815");
        
	}
	
	/**获取一个用户的所有微博
	 * @param uid
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static void getAllWeibos(String uid) throws ClientProtocolException, IOException{
//		String req1 = "http://weibo.com/p/100505"+uid+"/weibo?from=page_100505_home&wvr=5.1&mod=weibomore";
//		String req2 = "http://weibo.com/p/aj/mblog/mbloglist?domain=100505&pre_page=1&page=1&count=15&pagebar=0&max_msign=&filtered_min_id=&pl_name=Pl_Official_LeftProfileFeed__11&id=100505"+uid+"&script_uri=/p/100505"+uid+"/weibo&feed_type=0&from=page_100505&mod=TAB";
//		String req3 = "http://weibo.com/p/aj/mblog/mbloglist?domain=100505&pre_page=1&page=1&count=15&pagebar=1&max_msign=&filtered_min_id=&pl_name=Pl_Official_LeftProfileFeed__11&id=100505"+uid+"&script_uri=/p/100505"+uid+"/weibo&feed_type=0&from=page_100505&mod=TAB";
//		String file1 = "C:/Users/Qixuan/Desktop/weibo_data/weibos/"+uid+"_1.html";
//		String file2 = "C:/Users/Qixuan/Desktop/weibo_data/weibos/"+uid+"_2.html";
//		String file3 = "C:/Users/Qixuan/Desktop/weibo_data/weibos/"+uid+"_3.html";
//		
//		getHTML(req1, file1);
//		getRowWeibos(req2, file2);
//		getRowWeibos(req3, file3);
		
		for(int i=1;;i++){
			String treq1 = "http://weibo.com/p/100505"+uid+"/weibo?page="+i;
			String treq2 = "http://weibo.com/p/aj/mblog/mbloglist?domain=100505&pre_page="+i+"&page="+i+"&count=15&pagebar=0&max_msign=&filtered_min_id=&pl_name=Pl_Official_LeftProfileFeed__11&id=100505"+uid+"&script_uri=/p/100505"+uid+"/weibo&feed_type=0&from=page_100505&mod=TAB";
			String treq3 = "http://weibo.com/p/aj/mblog/mbloglist?domain=100505&pre_page="+i+"&page="+i+"&count=15&pagebar=1&max_msign=&filtered_min_id=&pl_name=Pl_Official_LeftProfileFeed__11&id=100505"+uid+"&script_uri=/p/100505"+uid+"/weibo&feed_type=0&from=page_100505&mod=TAB";
			String tfile1 = "C:/Users/Qixuan/Desktop/weibo_data/weibos/"+uid+"_"+i+"_1.html";
			String tfile2 = "C:/Users/Qixuan/Desktop/weibo_data/weibos/"+uid+"_"+i+"_2.html";
			String tfile3 = "C:/Users/Qixuan/Desktop/weibo_data/weibos/"+uid+"_"+i+"_3.html";

			getHTML(treq1, tfile1);
			getRowWeibos(treq2, tfile2);
			getRowWeibos(treq3, tfile3);
			
//			System.out.println("第"+i+"页");
			
			if(!hasweiboNext(tfile3))
				break;
			
		}
		
//		String req4 = "http://weibo.com/1775020471/myfollow?t=1&page=1";
//		String file4 = "C:/Users/Qixuan/Desktop/weibo_data/friends/qixuan1.html";
//		String req5 = "http://weibo.com/1775020471/myfollow?t=1&page=2";
//		String file5 = "C:/Users/Qixuan/Desktop/weibo_data/friends/qixuan2.html";
		
//		String req4 = "http://weibo.com/1775020471/myfans";
//		String file4 = "C:/Users/Qixuan/Desktop/weibo_data/friends/qixuanfan1.html";
//		String req5 = "http://weibo.com/1775020471/myfans?t=1&f=1&page=2";
//		String file5 = "C:/Users/Qixuan/Desktop/weibo_data/friends/qixuanfan2.html";
//		
//		getHTML(req4, file4);
//		getHTML(req5, file5);
//		getHTML(req1, file1);
//		getRowWeibos(req2, file2);
//		getRowWeibos(req3, file3);
	}
	

	/**判定是否存在下一页
	 * @param htmlurl 需要判定的页面
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static boolean hasNextpage(String htmlurl) throws ClientProtocolException, IOException{
		
		HttpGet httpget = new HttpGet(htmlurl); 

        HttpResponse response = client.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(entity.getContent()));
           String str = null;
           while(null != (str = reader.readLine()) ){
               if(str.contains("下一页")){
            	   httpget.abort();
            	   return true;
               }
            }
        }
        httpget.abort();
		return false;
		
	}
	
	
	
	/**本函数用于爬取一页并保存html（可以用于获取用户第一页的头15条微博）
	 * @param reqstr
	 * @param filename
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static void getHTML(String reqstr, String filename) throws ClientProtocolException, IOException{
		
		// HTTP请求   
        HttpUriRequest request = new HttpGet(reqstr);   
 
        HttpResponse response = client.execute(request);
         
        // 从response中取出HttpEntity对象
        HttpEntity entity = response.getEntity();
        
         
        // 取出服务器返回的数据流
        try {
			InputStream stream = entity.getContent();
			BufferedReader bf = new BufferedReader(
                      new InputStreamReader(stream));

			String s = null;
			StringBuffer sbf = new StringBuffer();
			while ((s=bf.readLine()) != null){
				sbf.append(s);
			}
			String str = sbf.toString();
			
			PrintWriter write = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
			write.print(str);
			write.close();
			
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**判断文本中是否有“下一页”
	 * @param filename
	 * @return
	 * @throws FileNotFoundException 
	 */
	public static boolean hasweiboNext(String filename) throws FileNotFoundException{
		
		Scanner in = new Scanner(new File(filename));
		while(in.hasNext()){
			String str = in.nextLine();
//			System.out.println(str);
			if(str.contains("下一页")){
				in.close();
				return true;
			}
		}
		in.close();
		return false;
	}
	
	
	/** 本函数用于获取滚屏加载的请求
	 * @param reqstr 请求URL
	 * @param filename 存放html的页面
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static void getRowWeibos(String reqstr, String filename) throws ClientProtocolException, IOException{
		
		
		
		// HTTP请求   
        HttpUriRequest request =   
                new HttpGet(reqstr);   
 
        HttpResponse response = client.execute(request);
         
        // 从response中取出HttpEntity对象
        HttpEntity entity = response.getEntity();
        
         
        // 取出服务器返回的数据流
        try {
			InputStream stream = entity.getContent();
			BufferedReader bf = new BufferedReader(
                      new InputStreamReader(stream));

			String s = null;
			StringBuffer sbf = new StringBuffer();
			while ((s=bf.readLine()) != null){
				sbf.append(s);
			}
			String str = sbf.toString();
			JSONObject res = JSONObject.parseObject(str);
			
			PrintWriter write = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
			write.print(res.getString("data"));
			write.close();
			
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
