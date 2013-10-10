package com.cike.weibosqa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.alibaba.fastjson.JSONObject;


public class HtmlParserTest {
	
	static String filename = "C:/Users/Qixuan/Desktop/weibo_data/weibos/2047706491_1.html";
	static String filename2 = "C:/Users/Qixuan/Desktop/weibo_data/getdown.html";
	public static void main(String[] args) throws ParserException, FileNotFoundException {
		
//		dealWithFitstPage(filename);
//		dealWithNextPages(filename2);
//		dealFriends("C:/Users/Qixuan/Desktop/weibo_data/friends/qixuan1.html");
//		dealFans("C:/Users/Qixuan/Desktop/weibo_data/friends/qixuanfan1.html");
		
		
		/*处理粉丝以及所关注用户的uid，存成txt
		 * 
		 * 
		List<String> followlist = new ArrayList<String>();
		
		int i = 1;
		File file = new File(getFollowname(i));
		while(file.exists()){
			followlist.addAll(dealFriends(getFollowname(i)));
			i++;
			file = new File(getFollowname(i));
		}
		
		List<String> fanlist = new ArrayList<String>();
		
		int j = 1;
		File file2 = new File(getFanname(j));
		while(file2.exists()){
			fanlist.addAll(dealFans(getFanname(j)));
			j++;
			file2 = new File(getFanname(j));
		}
		
		PrintWriter writer1 = new PrintWriter(new File("C:/Users/Qixuan/Desktop/weibo_data/friends/fans.txt"));
		PrintWriter writer2 = new PrintWriter(new File("C:/Users/Qixuan/Desktop/weibo_data/friends/follows.txt"));
		
		for(String a: followlist)
			writer2.println(a);
		for(String a: fanlist)
			writer1.println(a);
		
		writer1.close();
		writer2.close();*/
	}
	
	
	public static String getFanname(int num){
		return "C:/Users/Qixuan/Desktop/weibo_data/friends/qixuanfan"+num+".html";
	}
	
	public static String getFollowname(int num){
		return "C:/Users/Qixuan/Desktop/weibo_data/friends/qixuanfollow"+num+".html";
	}
	
	
	/**处理一页的 “我的粉丝” 的信息（一页19条）
	 * @param filename html文件路径
	 * @return
	 * @throws ParserException
	 */
	public static List<String> dealFans(String filename) throws ParserException{
		
		List<String> friendlist = new ArrayList<String>();
		
		Parser parser = new Parser(filename);
		parser.setEncoding("UTF-8");
		NodeFilter filter = new TagNameFilter("script");
		NodeList list = parser.extractAllNodesThatMatch(filter);
		/**
		 * 这里需要注意！！！直接取第7条不一定安全
		 */
		Node node = list.elementAt(6);
		String fullstr = node.getLastChild().getText();
		/**
		 * 这里需要注意！！！依赖于页面的模式。。。
		 */
		String jsstr = fullstr.substring(41, fullstr.length()-1);
		JSONObject json = JSONObject.parseObject(jsstr);
		String htmlstr = json.getString("html");
		Parser parser2 = new Parser(htmlstr);
		parser2.setEncoding("UTF-8");
        NodeFilter filter2 = new HasAttributeFilter("class","clearfix S_line5");
        NodeList list2 = parser2.extractAllNodesThatMatch(filter2);
        for(int i=0;i<list2.size();i++){
        	String str = list2.elementAt(i).getText();
        	String reg = "uid=[0-9]+";	//uid=XXXXXXXXX的正则表达式
            //使用正则表达式进行字符串匹配
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(str);
            if (matcher.find()) {
                String[] tsl = matcher.group().split("=");
                friendlist.add(tsl[1]);
//                System.out.println(tsl[1]);
            }
        }
        
        return friendlist;
	} 
	
	/**处理一页的 “我关注的人” 的信息（一页30条）
	 * @param filename html文件路径
	 * @return
	 * @throws ParserException
	 */
	public static List<String> dealFriends(String filename) throws ParserException{
		
		List<String> friendlist = new ArrayList<String>();
		
		Parser parser = new Parser(filename);
		parser.setEncoding("UTF-8");
		NodeFilter filter = new TagNameFilter("script");
		NodeList list = parser.extractAllNodesThatMatch(filter);
		/**
		 * 这里需要注意！！！直接取第六条不一定安全
		 */
		Node node = list.elementAt(6);
		String fullstr = node.getLastChild().getText();
		/**
		 * 这里需要注意！！！依赖于页面的模式。。。
		 */
		String jsstr = fullstr.substring(41, fullstr.length()-1);
		JSONObject json = JSONObject.parseObject(jsstr);
		String htmlstr = json.getString("html");
		
		Parser parser2 = new Parser(htmlstr);
		parser2.setEncoding("UTF-8");
        NodeFilter filter2 = new HasAttributeFilter("class","S_link2");
        NodeFilter filter3 = new HasAttributeFilter("node-type","set_group");
        NodeList list2 = parser2.extractAllNodesThatMatch(filter2);
        NodeList list3 = list2.extractAllNodesThatMatch(filter3);
        for(int i=0;i<list3.size();i++){
        	String str = list3.elementAt(i).getText();
        	String reg = "uid=[0-9]+";	//uid=XXXXXXXXX的正则表达式
            //使用正则表达式进行字符串匹配
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(str);
            if (matcher.find()) {
                String[] tsl = matcher.group().split("=");
                friendlist.add(tsl[1]);
//                System.out.println(tsl[1]);
            }
        }
        
        return friendlist;
	} 
	
	
	/**处理微博内容首页HTML，需要从script提取html信息
	 * @param filename
	 * @throws ParserException 
	 */
	public static String dealWithFitstPage(String filename) throws ParserException{
		Parser parser = new Parser(filename);
		parser.setEncoding("UTF-8");
		NodeFilter filter = new TagNameFilter("script");
		NodeList list = parser.extractAllNodesThatMatch(filter);
		/**
		 * 这里需要注意！！！直接取最后一条不一定安全
		 */
		Node node = list.elementAt(list.size()-1);
		String fullstr = node.getLastChild().getText();
		/**
		 * 这里需要注意！！！依赖于页面的模式。。。
		 */
		String jsstr = fullstr.substring(8, fullstr.length()-1);
		JSONObject json = JSONObject.parseObject(jsstr);
		String htmlstr = json.getString("html");
//		System.out.println(htmlstr);
		return dealWithNextPages(htmlstr);
	}
	
	/**处理微博内容简单html页面（script内部部分）
	 * @param file 文件名或者String皆可
	 * @throws ParserException
	 */
	public static String dealWithNextPages(String file) throws ParserException{
		Parser parser = new Parser(file);
		parser.setEncoding("UTF-8");
        NodeFilter filter = new HasAttributeFilter("class","WB_text");       
        NodeList list = parser.extractAllNodesThatMatch(filter);       
        StringBuffer strb = new StringBuffer();
        for (int i = 0; i < list.size(); i++) {           
        	Node node = list.elementAt(i);
        	NodeFilter filter2 = new NodeClassFilter(TextNode.class);
        	NodeList list2 = node.getChildren().extractAllNodesThatMatch(filter2);
        	for (int j = 0; j < list2.size(); j++) {
        		String buffer = list2.elementAt(j).getText().trim();
        		if(buffer.trim().length()>0)
        			strb.append(buffer);
//        			System.out.println(buffer);
        	}
        }
        System.out.println(strb);
        return strb.toString();
	}
	
}