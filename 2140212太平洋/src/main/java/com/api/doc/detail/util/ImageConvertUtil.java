package com.api.doc.detail.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.alibaba.fastjson.JSON;
import com.api.doc.yozo.weboffice.util.YozoWebOfficeUtil;
import weaver.conn.RecordSet;
import weaver.docs.pdf.docpreview.ConvertPDFUtil;
import weaver.file.FileSecurityUtil;
import weaver.file.ImageFileManager;
import weaver.file.util.FileDeleteUtil;
import weaver.general.BaseBean;
import weaver.general.GCONST;
import weaver.general.StaticObj;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.system.SystemComInfo;
import weaver.upgradetool.httpclient.org.apache.http.client.HttpClient;
import weaver.upgradetool.httpclient.org.apache.http.client.methods.HttpPost;
import weaver.upgradetool.httpclient.org.apache.http.impl.client.DefaultHttpClient;
import weaver.upgradetool.httpcore.org.apache.http.*;
import weaver.upgradetool.httpcore.org.apache.http.util.EntityUtils;
import weaver.upgradetool.httpmime.org.apache.http.entity.mime.HttpMultipartMode;
import weaver.upgradetool.httpmime.org.apache.http.entity.mime.MultipartEntity;
import weaver.upgradetool.httpmime.org.apache.http.entity.mime.content.FileBody;
import weaver.upgradetool.httpmime.org.apache.http.entity.mime.content.StringBody;
import weaver.wps.WebOfficeUtil;
import weaver.wps.doccenter.utils.Config;
import weaver.wps.view.linux.utils.PropUtil;
import DBstep.iMsgServer2000;

import com.alibaba.fastjson.JSONObject;
import com.api.doc.detail.service.DocViewPermission;


public class ImageConvertUtil extends BaseBean{

	public static String DOC_ACC_TABLE = "ImageFile";
	public static String EMAIL_ACC_TABLE = "MailResourceFile";
	public static String SOCIAL_ACC_TABLE = "SoCialFile";  //em附件
	public static String CONVERT_TYPE_HTML = "svg";
	public static String CONVERT_TYPE_JPG = "jpg";
	public static String CONVERT_TYPE_HTML_FLAG = "HtmlConvert";
	public static String CONVERT_TYPE_JPG_FLAG = "ImgConvert";

	public static String YOZO_METHOD_OPEN = YozoWebOfficeUtil.YOZO_METHOD_OPEN;
	public static String YOZO_METHOD_SAVE = YozoWebOfficeUtil.YOZO_METHOD_SAVE;
	public static String YOZO_METHOD_CLOSE = YozoWebOfficeUtil.YOZO_METHOD_CLOSE;
	public static String YOZO_METHOD_ISOPEN = YozoWebOfficeUtil.YOZO_METHOD_ISOPEN;
	public static String YOZO_METHOD_AUTHINFO = YozoWebOfficeUtil.YOZO_METHOD_AUTHINFO;

	public Map<String,Object> convert(int fileid,User user,String clientAddress,Map<String,String> otherParams,String convertType,String from)
			throws Exception{
		if(!EMAIL_ACC_TABLE.equals(from)){
			from = DOC_ACC_TABLE;
		}

		Map<String,Object> apidatas = new HashMap<String,Object>();
		apidatas.put("result", -1);
		RecordSet rs = new RecordSet();
		int imgids = -1;
		int height = 0;
		int width = 0;
		for(int i = 0 ;  i < 5 ; i ++){
			String sql = "select imgids,height,width from IMG_IMAGEFILE where imagefileid=? and convertype=? and ecologyTable=? order by imgids";
			rs.executeQuery(sql,fileid,convertType,from);
			if(rs.next()){
			    imgids = Util.getIntValue(rs.getString("imgids"),0);
			    height = Util.getIntValue(rs.getString("height"),0);
			    width = Util.getIntValue(rs.getString("width"),0);
				if(imgids <= 0){
					Thread.sleep(2000);
				}else{
					//writeLog("^^^^^^^^^^^^^^^^^^^^" + 0);
					apidatas.put("result", 0);
					List<Map<String,String>> data = new ArrayList<Map<String,String>>();
					Map<String,String> d0 = new HashMap<String,String>();
					d0.put("id",imgids + "");
					d0.put("wi",width + "");
					d0.put("hi",height + "");
					data.add(d0);
					while(rs.next()){
						Map<String,String> d = new HashMap<String,String>();
						d.put("id",Util.getIntValue(rs.getString("imgids"),0) + "");
						d.put("wi",Util.getIntValue(rs.getString("width"),0) + "");
						d.put("hi",Util.getIntValue(rs.getString("height"),0) + "");
						data.add(d);
					}
					apidatas.put("data",data);
					break;
				}
			}else{
				//writeLog("^^^^^^^^^^^^^^^^^^^^" + 1);
				apidatas = toConvert(fileid,user,clientAddress,convertType,from);
				break;
			}

		}
		return apidatas;
	}

	public String getConvertIp(){
		//ImageConvertUtil icu = new ImageConvertUtil();
		//return icu.isWeaverSystem() ? "120.55.36.234:8088" : "192.168.42.64:8080";
		return Util.null2String(getPropValue("doc_custom_for_weaver", "convert_client_path"));
	}

	public String getClientAddress(){
		return Util.null2String(getPropValue("doc_custom_for_weaver", "convert_ecology_path"));
	}

	public boolean convertForClient(){
		return "1".equals(getPropValue("doc_custom_for_weaver", "convert_client_open")) || canViewForWps();
	}

	public String getConvertType(){
		String convetType = Util.null2String(getPropValue("doc_custom_for_weaver", "convert_type"));
		return convetType;
	}

	public int getDaySet(){
		int day = Util.getIntValue(getPropValue("doc_custom_for_weaver", "delete_temp_day"),0);	//定时任务，清楚n天前的记录
		return day <= 2 ? 2 : day;	//默认2天(保留当前和前一天)
	}

	public boolean openWatermark(){
		return "1".equals(getPropValue("doc_custom_for_weaver", "open_watermark"));
	}

	public String getYozoClient(){
		return Util.null2String(getPropValue("doc_yozo_for_weaver", "yozo_client_path"));
	}


	public boolean isTooLarge(String extname,String filesize){
		extname = Util.null2String(extname).toLowerCase();

		String paramkey = "";
		if(extname.endsWith("xls") || extname.endsWith("xlsx")){
			paramkey = "excel";
		}else if(extname.endsWith("doc") || extname.endsWith("docx")){
			paramkey = "word";
		}else if(extname.endsWith("ppt") || extname.endsWith("pptx")){
			paramkey = "ppt";
		}else if(extname.endsWith("zip") || extname.endsWith("rar")){
			paramkey = "zip";
		}else if(extname.endsWith("wps")){
			paramkey = "wps";
		}else if(extname.endsWith("txt")){
			paramkey = "txt";
		}else{
			paramkey = extname;
		}

		double maxSize = Util.getDoubleValue(getPropValue("conver_filesize", paramkey),20);
		if(maxSize*1024*1024 < Util.getDoubleValue(filesize,0)){
			return true;
		}

		return false;
	}

	public Map<String,Object> toConvert(int fileid,User user,String clientAddress,String convertType,String from){
		//writeLog("^^^^^^^^^^^^^^^^^^^^" + 2);
		Map<String,Object> apidatas = new HashMap<String,Object>();
		apidatas.put("result",-10);
		RecordSet rs = new RecordSet();
		try{
			rs.executeUpdate("insert into IMG_IMAGEFILE(imagefileid,convertype,imgids,ecologyTable) values(?,?,?,?)",fileid,convertType,-1,from);

			 apidatas.put("result",-13);
			 //writeLog("^^^^^^^^^^^^^^^^^^^^" + obj);
			 apidatas.put("result",-12);
			 apidatas.put("clientAddress",clientAddress);
			 String convertByUrl = doConvert(fileid,user,clientAddress,convertType,false,null,from);
			 apidatas.put("result",-11);
			 apidatas.put("convertByUrl",convertByUrl);
			 //writeLog("^^^^^^^^^^^^^^^^^^^^" + convertByUrl);
			 JSONObject obj = JSONObject.parseObject(convertByUrl);
			 Integer result = (Integer)obj.get("result");
			 String message = (String)obj.get("message");
			 apidatas.put("result",result);
			 apidatas.put("message",message);
			 //writeLog("^^^^^^^^^^^^^^^^^^^^" + result);
			 if(result == 0){
				 Object data = obj.get("data");
				 List<Map<String,String>> dataList = new ArrayList<Map<String,String>>();
				 if(data != null){
					 //writeLog("^^^^^^^^^^1^^^^^^^^^^");
					 List<String> datas = (List<String>)data;
					 ImageFileManager ifm = new ImageFileManager();
					 int i = 1;
					 String date = TimeUtil.getCurrentDateString();
					 for(String link : datas){
						// writeLog("^^^^^^^^^^2^^^^^^^^^^");
						 InputStream is = downloadUrl(link);
						 byte []d = getbytes(is);
						 ifm.resetParameter();
						 ifm.setImagFileName(i+"." + ("jpg".equals(convertType) ? CONVERT_TYPE_JPG : CONVERT_TYPE_HTML));
						 ifm.setComefrom("jpg".equals(convertType) ? CONVERT_TYPE_JPG_FLAG : CONVERT_TYPE_HTML_FLAG);
						 ifm.setData(d);
						 int fid = ifm.saveImageFile();
						 int width = 0;
						 int height = 0;
						 //writeLog("^^^^^^^^^^3^^^^^^^^^^"+fid);
						 Map<String,String> d0 = new HashMap<String,String>();
						 d0.put("id",fid + "");
						 d0.put("wi",width + "");
						 d0.put("hi",height + "");
						 dataList.add(d0);
						 i++;
						 rs.executeUpdate("insert into IMG_IMAGEFILE(imagefileid,convertype,imgids,convertDate,ecologyTable,height,width) " +
						 		" values(?,?,?,?,?,?,?)",fileid,convertType,fid,date,from,height,width);
						 delTempFile(link);
					 }

				 }
				 apidatas.put("data",dataList);
			 }
			// writeLog("^^^^^^^^^^^^^^^^^^^^" + apidatas);
		}catch(Exception e){

		}finally{
			rs.executeUpdate("delete from IMG_IMAGEFILE where imagefileid=? and convertype=? and imgids=? and ecologyTable=?",fileid,convertType,-1,from);
		}
		 return apidatas;
	}

	public static String getYozoServer(String clientPath){
		if(clientPath != null && !clientPath.isEmpty()){

			clientPath = clientPath.endsWith("/") ? clientPath.substring(0,clientPath.length() - 1) : clientPath;

			int index = clientPath.indexOf("://");
			int index2 = clientPath.indexOf("/",index+3);
			if(index2 > 0){
				return clientPath;
			}
			return clientPath + "/ecology_dvs";
		}
		return clientPath;
	}

	public static Map<String,String> doConvertForFilePath(String filepath,String filename,String convertType){
		ImageConvertUtil icu = new ImageConvertUtil();
		String convertIp = icu.getConvertIp();
		Map<String,String> dataMap = new HashMap<String,String>();
		if(convertIp.isEmpty()){
			dataMap.put("result","-1");
			dataMap.put("message",""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10004434,weaver.general.ThreadVarLanguage.getLang())+"!");
			return dataMap;
		}

		File f = new File(filepath);
		if(!f.exists()){
			dataMap.put("result","-1");
			dataMap.put("message",""+weaver.systeminfo.SystemEnv.getHtmlLabelName(391241,weaver.general.ThreadVarLanguage.getLang())+"!");
			return dataMap;
		}

		int _convertType = -1;
		if("jpg".equals(convertType)){
			_convertType= 30;
		}else if("pdf".equals(convertType)){
			_convertType= 42;
		}else if("html".equals(convertType)){
			_convertType= 0;
		}
		BaseBean bb = new BaseBean();
        String requestJson = "";
        HttpClient httpclient = new DefaultHttpClient();
        try {
        	if(!convertIp.startsWith("http")){
    			convertIp = "http://"+convertIp;
    		}
        	convertIp = getYozoServer(convertIp);
            HttpPost httppost = new HttpPost(convertIp+"/upload");
            FileBody file = new FileBody(new File(filepath));
            MultipartEntity reqEntity = new MultipartEntity(
                    HttpMultipartMode.BROWSER_COMPATIBLE, null,
                    Charset.forName("UTF-8"));
            reqEntity.addPart("file", file);
            reqEntity.addPart("isDelSrc",
            		new StringBody("1", Charset.forName("UTF-8")));
			reqEntity.addPart("targetUrl",
                    new StringBody("1", Charset.forName("UTF-8")));
            reqEntity.addPart("convertType",
                    new StringBody(_convertType + "", Charset.forName("UTF-8")));
            httppost.setEntity(reqEntity);
            HttpResponse response = httpclient.execute(httppost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity resEntity = response.getEntity();
                //requestJson = EntityUtils.toString(resEntity);
				byte[] json=EntityUtils.toByteArray(resEntity);
                requestJson=new String(json,"UTF-8");
                EntityUtils.consume(resEntity);
                if(requestJson == null || requestJson.isEmpty()){
                	dataMap.put("result","-1");
        			dataMap.put("message",""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10004373,weaver.general.ThreadVarLanguage.getLang())+"!");
                }else{
                	JSONObject obj = JSONObject.parseObject(requestJson);
        			int result = (Integer)obj.get("result");
        			String message = (String)obj.get("message");
        			dataMap.put("result",result + "");
        			dataMap.put("message",message);
        			Object data = obj.get("data");
        			if(result == 0 && data != null){
	       				List<String> datas = (List<String>)data;
	       				 if(datas.size() > 0){
	       					 String convertPath = datas.get(0);

	       					 boolean flag = convertPath.contains("?");
	       					 dataMap.put("path",convertPath);
	       				 }
	       			}
                }
            }else{
            	dataMap.put("result","-1");
    			dataMap.put("message","http"+weaver.systeminfo.SystemEnv.getHtmlLabelName(10004435,weaver.general.ThreadVarLanguage.getLang())+"!");
            }
        } catch (ParseException e) {
        	 bb.writeLog("^^^^^^^err1^^^^^^" + e);
            e.printStackTrace();
        } catch (IOException e) {
        	bb.writeLog("^^^^^^^err2^^^^^^" + e);
            e.printStackTrace();
        } finally {
            try {
                httpclient.getConnectionManager().shutdown();
            } catch (Exception ignore) {
            }
        }
        return dataMap;
	}
	
	
	private static String getConvertType(String convertType,String extname,String ismobile){
		
		if(extname.equals("zip") || extname.equals("rar")){
			convertType = "html";
		}


		//是否只允许转pdf或者html
		if("1".equals(ismobile)){
			convertType = "html";
		}
		
		BaseBean bb = new BaseBean();
		
		String onlyConvert = bb.getPropValue("doc_custom_for_weaver", "onlyConvert");
		String suffixNotconvertToPDF = Util.null2s(Util.null2String(bb.getPropValue("doc_custom_for_weaver", "notConvertToPDF")),"rar,zip,7z,gz,tar").replaceAll("\\s","");
		boolean notConvertToPDF = ("," + suffixNotconvertToPDF + ",").contains("," + extname + ",");
		if("pdf".equals(onlyConvert) && !notConvertToPDF){
			convertType = "pdf";
		}else if("html".equals(onlyConvert) && !"pdf".equals(extname)){
			convertType = "html";
		}
		return convertType;
	}
	
	public static String doConvertForSocial(String filename,String downloadUrl,int isCopy){
		if(filename == null || downloadUrl == null){
			return "{\"result\":-1,\"message\":\"params is null!\"}";
		}
		String extname = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "";
		extname = extname.toLowerCase();
		
		String convertType = getConvertType("pdf",extname,"1");
		
		Map<String,String> params = new HashMap<String,String>();
		
		if(extname.equals("zip") || extname.equals("rar")){
			params.put("isRar5","1");
		}

		if("html".equals(convertType) && ("ppt".equals(extname) || "pptx".equals(extname))){
			params.put("isVertical","1");
		}
		
		params.put("extname",extname);
		params.put("filedownloadUrl",downloadUrl);
		params.put("isCopy",isCopy == 1 ? "1" : "0");
		
		return doConvert(0,null,"",convertType,true,params,SOCIAL_ACC_TABLE);
	}

	public static String doConvert(int fileid,User user,String clientAddress,String convertType,boolean online,Map<String,String> params,String from){
		//String filecode = fileid + "_" + user.getUID();
		//filecode = DownloadServlet.encode(filecode);
		ImageConvertUtil icu = new ImageConvertUtil();
		icu.dcsWriteLog("ImageConvertUtil-doConvert-335-fileid:["+fileid+"]"+"convertType:["+convertType+"]"+"online:["+online+"]"+"params:["+params+"]"+"from:["+from+"]");
		String convertIp = icu.getConvertIp();
		String _clientAddress = icu.getClientAddress();
		if(!_clientAddress.isEmpty()){
			clientAddress = _clientAddress;
		}

		int _convertType = -1;
		if("jpg".equals(convertType)){
			_convertType= 30;
		}else if("pdf".equals(convertType)){
			_convertType= online ? 42 : 3;
		}else if("html".equals(convertType)){
			_convertType= 0;
		}

		/*return  sendPost("http://"+convertIp+"/ecology_dvs/convertForUrl",
				 "downloadUrl="+clientAddress+"/weaver/weaver.file.ConvertFileDownlad&filecode="+filecode+"&convertType=" + _convertType);*/
			StaticObj staticObj=StaticObj.getInstance();
			staticObj.removeObject("SystemInfo");
	        SystemComInfo syscominfo = new SystemComInfo() ;
	        String createdir =syscominfo.getFilesystem() ;

	        if( createdir==null||createdir.equals("") ) createdir = GCONST.getSysFilePath() ;

			String filepath = createdir + "filesystem"+File.separatorChar+"sourceFilePath";

			String extname = "";
			if(EMAIL_ACC_TABLE.equals(from)){
				weaver.email.service.MailFilePreviewService mfps = new weaver.email.service.MailFilePreviewService();
				InputStream is = mfps.getInputStreamByMailFileId(fileid + "");
				//BaseBean bblog = new BaseBean();
				//bblog.writeLog("^^^^^^^^^^^param.extname=" + params.get("extname"));
				filepath = writeFile(is,filepath,"." + params.get("extname"),from);
				extname = params.get("extname");
			}else if(SOCIAL_ACC_TABLE.equals(from)){
				filepath = params.get("filedownloadUrl"); 
				extname = params.get("extname");
			}else{
				ImageFileManager ifm = new ImageFileManager();
				ifm.resetParameter();
				ifm.getImageFileInfoById(fileid);
				String filename = Util.null2String(ifm.getImageFileName());
				String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")+1) : "";
				extname = ext;
				filepath = writeFile(ifm.getInputStream(),filepath,"."+ext,from);
			}

			extname = Util.null2String(extname);
			if("pdf".equals(extname.toLowerCase())){
				_convertType = 20;
			}
			if("ofd".equals(extname.toLowerCase())){
				_convertType = 21;
			}
			if("uot".equals(extname.toLowerCase())){
				_convertType = 0;
			}
			icu.dcsWriteLog("ImageConvertUtil-doConvert-390-fileid:["+fileid+"]"+"filepath:["+filepath+"]"+"_convertType:["+_convertType+"]");
			if(filepath.isEmpty()){
				return "{\"result\":-1,\"message\":\""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10004436,weaver.general.ThreadVarLanguage.getLang())+"!\"}";
			}
			File f = new File(filepath);
			if(!f.exists()){
				return "{\"result\":-1,\"message\":\""+weaver.systeminfo.SystemEnv.getHtmlLabelName(391241,weaver.general.ThreadVarLanguage.getLang())+"!\"}";
			}
			Calendar c1 = Calendar.getInstance(); 
	        String requestJson = "";
	        HttpClient httpclient = new DefaultHttpClient();
	        try {
	        	if(!convertIp.startsWith("http")){
	    			convertIp = "http://"+convertIp;
	    		}
	        	convertIp = getYozoServer(convertIp);
	            HttpPost httppost = new HttpPost(convertIp+"/upload");
	            FileBody file = new FileBody(new File(filepath));
	            MultipartEntity reqEntity = new MultipartEntity(
	                    HttpMultipartMode.BROWSER_COMPATIBLE, null,
	                    Charset.forName("UTF-8"));
	            reqEntity.addPart("file", file);
	            reqEntity.addPart("isDelSrc",
	            		new StringBody("1", Charset.forName("UTF-8")));
				reqEntity.addPart("targetUrl",
	                    new StringBody("1", Charset.forName("UTF-8")));
	            reqEntity.addPart("convertType",
	                    new StringBody(_convertType + "", Charset.forName("UTF-8")));

	            
	            if("1".equals(params.get("isCopy"))){
	            	reqEntity.addPart("isCopy",
                            new StringBody("1", Charset.forName("UTF-8")));
	            }else if(DocDetailUtil.isopendoclock(fileid+"","file") && !DocDetailUtil.getCaneditByFileid(fileid+"",user,params,"0")){
					icu.dcsWriteLog("ImageConvertUtil-doConvert-420-fileid:["+fileid+"]"+"isCopy:["+1+"]");
                    reqEntity.addPart("isCopy",
                            new StringBody("1", Charset.forName("UTF-8")));
                }


	            if(params != null && "1".equals(params.get("isVertical"))){
	            	reqEntity.addPart("isVertical",
		                    new StringBody("1", Charset.forName("UTF-8")));
	            }
	            if(params != null && "1".equals(params.get("isRar5"))){
					icu.dcsWriteLog("ImageConvertUtil-doConvert-420-fileid:["+fileid+"]"+"isRar5:["+1+"]");
	            	reqEntity.addPart("isRar5",
	            			new StringBody("1", Charset.forName("UTF-8")));
	            }

	            httppost.setEntity(reqEntity);
	            HttpResponse response = httpclient.execute(httppost);
	            int statusCode = response.getStatusLine().getStatusCode();
	            if (statusCode == HttpStatus.SC_OK) {
	                HttpEntity resEntity = response.getEntity();
	                //requestJson = EntityUtils.toString(resEntity);
					byte[] json=EntityUtils.toByteArray(resEntity);
	                requestJson=new String(json,"UTF-8");
	                EntityUtils.consume(resEntity);
	                if(requestJson == null || requestJson.isEmpty()){
	                	requestJson = "{\"result\":-1,\"message\":\""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10004373,weaver.general.ThreadVarLanguage.getLang())+"!\"}";
	                }
	            }else{
	            	requestJson = "{\"result\":-1,\"statusCode\":"+statusCode+",\"URL\":\""+convertIp+"\",\"message\":\"http"+weaver.systeminfo.SystemEnv.getHtmlLabelName(10004435,weaver.general.ThreadVarLanguage.getLang())+"!\"}";
	            }
	        } catch (ParseException e) {
	        	new BaseBean().writeLog(e);
	            e.printStackTrace();
	        } catch (IOException e) {
				new BaseBean().writeLog(e);
	            e.printStackTrace();
	        } finally {
	            try {
	                httpclient.getConnectionManager().shutdown();
	            } catch (Exception ignore) {
	            }
	            //f.delete();
	           //FileSecurityUtil.deleteFile(f);
	            Calendar c2 = Calendar.getInstance();
	            
	            new BaseBean().writeLog("^^^^^^^^^^^^YoZhong use("+fileid+")("+(c2.getTimeInMillis()-c1.getTimeInMillis())+")^^^^^^^^^^");
	            new FileDeleteUtil().deleteFile(f);
	        }
	        return requestJson;
	}
	
	public static int doConvertForPath(String sourceFilePath,String targetFilePath,String convertType){
		ImageConvertUtil icu = new ImageConvertUtil();
		String convertIp = icu.getConvertIp();
		File f = new File(sourceFilePath);
		if(!f.exists()){
			return -2;
		}

		int _convertType = -1;
		if("jpg".equals(convertType)){
			_convertType= 30;
		}else if("pdf".equals(convertType)){
			_convertType= 3;
		}else if("html".equals(convertType)){
			_convertType= 0;
		}

		String requestJson = "";
        HttpClient httpclient = new DefaultHttpClient();
        try {
        	if(!convertIp.startsWith("http")){
    			convertIp = "http://"+convertIp;
    		}
        	convertIp = getYozoServer(convertIp);
            HttpPost httppost = new HttpPost(convertIp+"/upload");
            FileBody file = new FileBody(new File(sourceFilePath));
            MultipartEntity reqEntity = new MultipartEntity(
                    HttpMultipartMode.BROWSER_COMPATIBLE, null,
                    Charset.forName("UTF-8"));
            reqEntity.addPart("file", file);
            reqEntity.addPart("isDelSrc",
            		new StringBody("1", Charset.forName("UTF-8")));
			reqEntity.addPart("targetUrl",
                    new StringBody("1", Charset.forName("UTF-8")));
            reqEntity.addPart("convertType",
                    new StringBody(_convertType + "", Charset.forName("UTF-8")));
            httppost.setEntity(reqEntity);
            HttpResponse response = httpclient.execute(httppost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity resEntity = response.getEntity();
				byte[] json=EntityUtils.toByteArray(resEntity);
                requestJson=new String(json,"UTF-8");
                EntityUtils.consume(resEntity);
                if(requestJson == null || requestJson.isEmpty()){
                	requestJson = "{\"result\":-1,\"message\":\""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10004373,weaver.general.ThreadVarLanguage.getLang())+"!\"}";
                }
            }else{
            	requestJson = "{\"result\":-1,\"message\":\"http"+weaver.systeminfo.SystemEnv.getHtmlLabelName(10004435,weaver.general.ThreadVarLanguage.getLang())+"!\"}";
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpclient.getConnectionManager().shutdown();
            } catch (Exception ignore) {
            }
            FileSecurityUtil.deleteFile(f);
        }

        InputStream is = null;
        OutputStream os = null;
        try{
        	os = new FileOutputStream(targetFilePath);
        	JSONObject obj = JSONObject.parseObject(requestJson);
			Integer result = (Integer)obj.get("result");
			String message = (String)obj.get("message");

			if(result == 0){
				 Object data = obj.get("data");
				 if(data != null){
					 List<String> datas = (List<String>)data;
					 String convertPath="";
					 if(datas!=null && datas.size()>0){
						 convertPath=datas.get(0);
					 }
					 is = ImageConvertUtil.downloadUrl(convertPath);
					 if(is == null){
						 return -3;
					 }

					 byte b[] = new byte[2048];
					 int n = 0;
					 while((n=is.read(b)) != -1){
						 os.write(b,0,n);
					 }
				 }
			}else{
				BaseBean bb = new BaseBean();
				bb.writeLog("^^^^^^^^^^ImageConvertUtil^^^^^^^^^^doConvertForPath=" + message);
				return result;
			}

        }catch(Exception e){
        	e.printStackTrace();
        	return -4;
        }finally{
        	if(is != null){
        		try{
        			is.close();
        		}catch(Exception e){
        		}
        	}
        	if(os != null){
        		try{
        			os.close();
        		}catch(Exception e){
        		}
        	}
        }

        return 0;

	}


	public static void delTempFile(String filepath){
		//http://192.168.42.64:8080/ecology_dvs/deloutputfile?filepath=http://192.168.42.64:8080/2018/11/02/3b71ceada61d0a99ddb09352b083e01e/1.jpg
		ImageConvertUtil icu = new ImageConvertUtil();
		String convertIp = icu.getConvertIp();
		BaseBean bb = new BaseBean();
		String deleleFlag = Util.null2String(bb.getPropValue("doc_custom_for_weaver", "client_file_delete"));

		if(!convertIp.startsWith("http")){
			convertIp = "http://"+convertIp;
		}

		if(!"0".equals(deleleFlag)){
			convertIp = getYozoServer(convertIp);
			sendPost(convertIp+"/deloutputfile", "filepath="+filepath);
		}
	}

	public static String writeFile(InputStream is,String filepath,String ext,String from){
		if(is == null){
			return "";
		}

		File f = new File(filepath);
		if(!f.exists() || !f.isDirectory()){
			 f.mkdirs();
		}
		filepath +=	File.separatorChar + UUID.randomUUID().toString() + ext;

		if(!EMAIL_ACC_TABLE.equals(from)&&isOffice(ext.replace(".",""))&&isMsgObjToDocument()) {

            int byteread;
            byte data[] = new byte[1024];

          //正文的处理
            ByteArrayOutputStream bout = null;
            try {
                bout = new ByteArrayOutputStream() ;
                while((byteread = is.read(data)) != -1) {
                    bout.write(data, 0, byteread) ;
                    bout.flush() ;
                }
                byte[] fileBody = bout.toByteArray();
                iMsgServer2000 MsgObj = new iMsgServer2000();
                MsgObj.MsgFileBody(fileBody);            //将文件信息打包
                fileBody = MsgObj.ToDocument(MsgObj.MsgFileBody());    //通过iMsgServer200 将pgf文件流转化为普通Office文件流
                is = new ByteArrayInputStream(fileBody);
                bout.close();
            }catch(Exception e) {
            }finally{
            	 if(bout!=null) {
                 	try{
                 	bout.close();
                 	}catch(Exception ex){
                 	}
                 }
            }
        }

		OutputStream os = null;
		try{
			os = new FileOutputStream(filepath);
			byte []b = new byte[2048];
			int n =0;
			while((n=is.read(b)) != -1){
				os.write(b,0,n);
			}
		}catch(Exception e){
			filepath = "";
			BaseBean bb = new BaseBean();
			bb.writeLog("^^^^^^^^^^^^临时文件读写异常^^^^^^^^^^^^^" + e);
		}finally{
			if(os != null){
				try{
					os.close();
				}catch(Exception e){
				}
			}
			try{
				is.close();
			}catch(Exception e){
			}
		}
		return filepath;
	}

	public static byte[] getbytes(InputStream is){
		 ByteArrayOutputStream bos = new ByteArrayOutputStream();
		 try{
			 byte []b = new byte[2048];
			 int n = 0;
			 while((n=is.read(b)) != -1){
				 bos.write(b,0,n);

			 }
			 return bos.toByteArray();
		 }catch(Exception e){
		 }finally{
			 try{
				 if(is != null){
					 is.close();
				 }
			 }catch(Exception e){
			 }
			 try{
				 if(bos != null){
					 bos.close();
				 }
			 }catch(Exception e){
			 }
		 }
		 return null;
	}

	public static InputStream  downloadUrl(String urlStr){
		InputStream in = null;
		BaseBean bb = new BaseBean();
		bb.writeLog("th==ImageConvertUtil==downloadUrl==urlStr==695",urlStr);
		try {

			if(urlStr.startsWith("https://")){
				return getHttps(urlStr);
			}

			 URL url = new URL(urlStr);
		        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		                //设置超时间为3秒
		        conn.setConnectTimeout(3*1000);
		        //防止屏蔽程序抓取而返回403错误
		        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

		        //得到输入流
		        in = conn.getInputStream();

		} catch (Exception e) {
			bb.writeLog("th==ImageConvertUtil==downloadUrl==695",e);
			e.printStackTrace();
		}
		return in;

	}



    public static InputStream getHttps(String hsUrl) {
        try {
            URL url = new URL(hsUrl);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            X509TrustManager xtm = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }
            };

            TrustManager[] tm = {xtm};

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tm, null);

            con.setSSLSocketFactory(ctx.getSocketFactory());
            con.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
            InputStream inStream = con.getInputStream();
            return inStream;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

	@Deprecated
	public Map<String,String> uploadToYozo(int fileid,String filename){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return uploadToYozo(fileid, filename, null);
	}

	@Deprecated
	public Map<String,String> uploadToYozo(int fileid,String filename, User user){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.uploadToYozo(fileid, filename, user);
	}

	@Deprecated
	public Map<String,Object> loadFromYoZo(String filepath,String filename){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.loadFromYoZo(filepath, filename, "0", null);
	}

	@Deprecated
	public Map<String,Object> loadFromYoZo(String filepath,String filename, String loadFileId){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.loadFromYoZo(filepath, filename, loadFileId, null);
	}

	public static void main(String []args){


		String s = "402dafbfcc89c07c5598b7e889873f5886b55a5614aeb4720ba712affcbc28737f8858eb93c2373b6db5c315cee5ecd8";
		try{
			String decryptStr = weaver.file.AESCoder.decrypt(s, "myMw6qPt&3AD");
			net.sf.json.JSONObject jasonObject = net.sf.json.JSONObject.fromObject(decryptStr);
			Map paramMap = (Map)jasonObject;
			System.out.println(paramMap);
			System.out.println(Util.getIntValue(paramMap.get("fileId") + "",-1));
		}catch(Exception e){
			e.printStackTrace();
		}


		String mouldtext = "<p style=\"text-align: center;\">$DOC_Content</p>";
		int contentIndex = mouldtext.indexOf("$DOC_Content");
    	if(contentIndex > -1){
    		int contentPStartIndex = mouldtext.lastIndexOf("<p ", contentIndex);
    		if(contentPStartIndex < -1){
    			contentPStartIndex = mouldtext.lastIndexOf("<p>", contentIndex);
    		}
    		int contentPEndIndex = mouldtext.indexOf("</p>", contentIndex);
    		if(contentPStartIndex > -1 && contentPEndIndex > -1){
        		String preMouldtext = mouldtext.substring(0,contentPStartIndex);
        		String cenMouldtext = mouldtext.substring(contentPStartIndex,contentPEndIndex+4);
        		String endMouldtext = mouldtext.substring(contentPEndIndex+4);

        		cenMouldtext = "<div " + cenMouldtext.substring(2,cenMouldtext.length() - 4) + "</div>";
        		mouldtext = preMouldtext + cenMouldtext + endMouldtext;
    		}
    	}
    	System.out.println(mouldtext);

    	//
    	System.out.println("ss*".split("\\*"));




		/*String s = "{\"result\":0,\"data\":[\"http://192.168.42.64:8080/2018/10/17/631f08d92657fb862e99d19b3a852bf/1.jpg\"],\"pagecount\":1,\"message\":\"\",\"type\":6}";
		JSONObject obj = JSONObject.parseObject(s);
		System.out.println("^^^^^^^^^^^^^^^^^^^^" + obj);
		 Integer result = (Integer)obj.get("result");
		 String message = (String)obj.get("message");
		 System.out.println("^^^^^^^^^^^^^^^^^^^^" + result);
		 System.out.println("^^^^^^^^^^^^^^^^^^^^" + message);*/


		//System.out.println("^^^^^^^^^^^^^^^^^^^^" + obj.get("resData").get("data"));
		/* Calendar calendar = Calendar.getInstance();
		 System.out.println(TimeUtil.getDateString(calendar));
		 calendar.add(Calendar.DATE,-1*5);
		 System.out.println(TimeUtil.getDateString(calendar));*/

		/*String url = "http://192.168.42.64:8080/2018/10/17/631f08d92657fb862e99d19b3a852bf/1.jpg";
		InputStream is = downloadUrl(url);
		byte []data = getbytes(is);
		System.out.println(data.length);
		OutputStream os = new FileOutputStream(new File("F:\\pdf\\1.jpg"));
		os.write(data);*/
	}

	public static String sendPost(String url, String param) {
	    PrintWriter out = null;
	    BufferedReader in = null;
	    String result = "";
	    try {
	        URL realUrl = new URL(url);
	        // 打开和URL之间的连接
	        URLConnection conn = realUrl.openConnection();
	        conn.setRequestProperty("Accept-Charset", "UTF-8");
	        // 设置通用的请求属性
	        conn.setRequestProperty("accept", "*/*");
	        conn.setRequestProperty("connection", "Keep-Alive");
	        conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
	        // 发送POST请求必须设置如下两行
	        conn.setDoOutput(true);
	        conn.setDoInput(true);
	        // 获取URLConnection对象对应的输出流
	        out = new PrintWriter(conn.getOutputStream());
	        // 发送请求参数
	        out.print(param);
	        // flush输出流的缓冲
	        out.flush();
	        // 定义BufferedReader输入流来读取URL的响应
	        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String line;
	        while ((line = in.readLine()) != null) {
	            result += line;
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    // 使用finally块来关闭输出流、输入流
	    finally {
	        try {
	            if (out != null) {
	                out.close();
	            }
	            if (in != null) {
	                in.close();
	            }
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
	    return result;
	}



	public Map<String,Object> convertForPath(int fileid,User user,String clientAddress,Map<String,String> otherParams,String convertType,String from) throws Exception{
		otherParams = otherParams == null ? new HashMap<String,String>() : otherParams;
		Map<String,Object> apidatas = new HashMap<String,Object>();

		int isCopy = 0;//是否可以复制 0：可以复制  1：不可复制
		if(DocDetailUtil.isopendoclock(fileid+"","file") && !DocDetailUtil.getCaneditByFileid(fileid+"",user,otherParams,"0")){
			isCopy = 1;
		}
		boolean isMobile = "1".equalsIgnoreCase(otherParams.get("ismobile"));
		apidatas.put("result", -1);
		String extname = "";
		if(EMAIL_ACC_TABLE.equals(from)){
			if(otherParams.get("model") == null){
				otherParams.put("model","email");
			}
			//extname = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "";
			weaver.email.service.MailFilePreviewService mfps = new weaver.email.service.MailFilePreviewService();
			String filename = mfps.getFileNameOnly(fileid + "");
			//writeLog("^^^^^^^^^^^filename=" + filename);
			extname = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "";
			extname = extname.toLowerCase();
			//writeLog("^^^^^^^^^^^extname=" + extname);
		}else{
			from = DOC_ACC_TABLE;
			ImageFileManager ifm = new ImageFileManager();
			ifm.resetParameter();
			ifm.getImageFileInfoById(fileid);
			String filename = ifm.getImageFileName();
			extname = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "";
			extname = extname.toLowerCase();

		}


		BaseBean bb = new BaseBean();
		String pdfConvert = bb.getPropValue("doc_custom_for_weaver", "pdfConvert");
		String docid = takeDocByfileid(fileid+"");

		//==zj
		boolean isOpenWps = false;	//设置开关状态
		try{
			//查看开关状态
			RecordSet wps = new RecordSet();
			wps.executeQuery("select isopen from wps_config where type='wps'");
			if (wps.next()){
				int isOpen = wps.getInt("isopen");
				//如果开关是开启状态
				if (isOpen == 1){
					int uid = user.getUID();		//用户id
					String subcompanyName = "";		//分部id
					String subcompanylist = "";		//分部wps权限数组
					String hrmsql = "select id from hrmsubcompany where id = (select subcompanyid1 from Hrmresource where id =" +  uid + ")";
					new BaseBean().writeLog("==zj==(页面--根据用户id查询分部id)" + hrmsql);
					wps.executeQuery(hrmsql);	//查询用户id对应分部id
					if (wps.next()){
						subcompanyName = Util.null2String(wps.getString("id"));		//如果有赋值
						new BaseBean().writeLog("==zj==(页面--获取账号所在分部)" + subcompanyName);
					}
					//将分部id作为条件在建模表进行查询
					RecordSet sub = new RecordSet();
					String Docsql = "select subcompanyname from uf_wps";
					new BaseBean().writeLog("==zj==(页面--查询建模表分部信息)" + Docsql);
					sub.executeQuery(Docsql);
					if (sub.next()){
						subcompanylist = Util.null2String(sub.getString("subcompanyname"));
						new BaseBean().writeLog("==zj==（页面--获取到wps权限分部数组）" + JSON.toJSONString(subcompanylist));
						String[] result = subcompanylist.split(",");
						new BaseBean().writeLog("==zj==（页面--截取后的数据）" + JSON.toJSONString(result));
						for (int i = 0; i < result.length; i++) {
							if (subcompanyName.equals(result[i]) && !"".equals(subcompanyName)){
								isOpenWps = true;
								break;
							}
						}
					}
				}
			}
			new BaseBean().writeLog("==zj==(页面--是否开启wps)" + isOpenWps);
		}catch (Exception e){
			new BaseBean().writeLog("==zj==(exception)" + e);
		}
		//


			boolean isViewForWps = ImageConvertUtil.canViewForWps();
			String noWpsView=Util.null2String(otherParams.get("noWpsView"));
			String flowFile = otherParams.get("flowFile");		//判断是否来自流程文件   0、不是   1、是
			new BaseBean().writeLog("==zj==(是否来自流程文件--flowFile)" + flowFile);
			if ("1".equals(flowFile)){
				//==zj 如果来自流程文件
				if ("true".equals(noWpsView)){
					pdfConvert="0";
					isViewForWps=false;
				}
			}else {
				//==zj 如果来自知识文件
				if (!isOpenWps){
					pdfConvert="0";
				}
			}

			new BaseBean().writeLog("==zj==(打开方式)" + pdfConvert + " | " + isViewForWps);



		if("pdf".equals(extname) && "1".equals(pdfConvert) && ConvertPDFUtil.isUsePDFViewer() && (canViewForYOZO() || canViewForWps()) ){   //pdf也走永中转换
			convertType = "pdf";
		}else if(isPic(extname) || noNeedConvert(extname) || (isMobile && "ofd".equals(extname.toLowerCase())&&shukeLightReaderPrew() && !docid.isEmpty())){
			List<Map<String,String>> dataList = new ArrayList<Map<String,String>>();
			Map<String,String> data = new HashMap<String,String>();
			dataList.add(data);
			DocViewPermission dvp = new DocViewPermission();
			String mouldParam = dvp.getMoudleParams(otherParams);
			boolean canPrint=false;
			if(otherParams.containsKey("canPrint")&&"1".equals(otherParams.get("canPrint"))){
				canPrint = true;
			}
			if(extname.equals("pdf")){
				RecordSet rs = new RecordSet();
				rs.executeQuery("select 1 from docimagefile where imagefileid = "+fileid);
				if(rs.next()){
					if(isMobile){
						data.put("path", GCONST.getContextPath()+"/docs/pdfview/web/pdfViewer.jsp?canPrint=false&canDownload=false&nojump=1&pdfimagefileid=" + fileid + mouldParam);
					} else {
						data.put("path", GCONST.getContextPath()+"/docs/pdfview/web/pdfViewer.jsp?canPrint="+canPrint+"&canDownload=false&pdfimagefileid=" + fileid + mouldParam);
					}
				}else{
					String fileCode = "";
					if(EMAIL_ACC_TABLE.equals(from)){
						fileCode = fileid+"";
					}else{
						fileCode = DocDownloadCheckUtil.checkPermission(fileid+"",user);
					}
					if(isMobile){
						data.put("path", GCONST.getContextPath()+"/docs/pdfview/web/pdfViewer.jsp?canPrint=false&canDownload=false&nojump=1&pdfimagefileid=" + fileCode + mouldParam);
					} else {
						data.put("path", GCONST.getContextPath()+"/docs/pdfview/web/pdfViewer.jsp?canPrint=false&canDownload=false&pdfimagefileid=" + fileCode + mouldParam);
					}
				}
			}else if(noNeedConvert(extname)){
				String convertIp = getConvertIp();
				if(!convertIp.startsWith("http")){
					convertIp = "http://"+convertIp;
				}

				String ecologyPath = getClientAddress();
				if(!ecologyPath.isEmpty()){
					clientAddress = ecologyPath;
					if(!clientAddress.startsWith("http")){
						clientAddress = "http://"+clientAddress;
					}
				}
				convertIp = getYozoServer(convertIp);
				data.put("path",convertIp + GCONST.getContextPath()+"/pdfViewer/web/viewer.jsp?pdfLoadUrl="+clientAddress+"/weaver/weaver.file.FileDownload?fileid=" + fileid + mouldParam);
			}else if (isPic(extname)){
				data.put("path", GCONST.getContextPath()+"/spa/document/index2mobile.jsp?fileid=" + fileid + mouldParam);
			}else if("ofd".equals(extname)){
				data.put("path", GCONST.getContextPath()+"/docs/ofd/OfdShow.jsp?docid="+docid+"&imagefileId="+fileid+"&isEdit=false");
			}
			//writeLog("^^^^^^^^^^^data=" + data);
			apidatas.put("data",dataList);
			apidatas.put("result",0);
			return apidatas;
		}

//		boolean isViewForWps = ImageConvertUtil.canViewForWps();
//        String noWpsView=Util.null2String(otherParams.get("noWpsView"));
//        if ("true".equals(noWpsView)){
//			isViewForWps=false;
//		}
		//金山wps
		if(isViewForWps){
			otherParams.put("fileMould",EMAIL_ACC_TABLE.equals(from) ? "email" : "ecology");
			apidatas = WebOfficeUtil.convert(fileid,user,otherParams);
			apidatas.put("convert","client");
			return apidatas;
		}
		
		String ismobile = otherParams.get("ismobile");

		//wps文件可以转为pdf，去掉
		//if(extname.equals("zip") || extname.equals("rar") || extname.equals("wps")){
		convertType = getConvertType(convertType,extname,ismobile);
		
		if(extname.equals("zip") || extname.equals("rar")){
			otherParams.put("isRar5","1");
		}

		if("html".equals(convertType) && ("ppt".equals(extname) || "pptx".equals(extname))){
			otherParams.put("isVertical","1");
		}
		otherParams.put("extname",extname);
		RecordSet rs = new RecordSet();
		for(int i = 0 ;  i < 5 ; i ++){
			rs.executeQuery("select convertPath from CONVERT_IMAGEFILE where imagefileid=? and convertype=? and ecologyTable=? and isCopy=?",fileid,convertType,from,isCopy);
			if(rs.next()){
				String convertPath = Util.null2String(rs.getString("convertPath"));



				// 1

				if(convertPath.isEmpty()){
					Thread.sleep(2000);
				}else{
					
					ImageConvertUtil icu = new ImageConvertUtil();
					String convertIp = icu.getConvertIp();
					if(!"1".equals(ismobile) && !convertPath.startsWith("http")){
						if(!convertIp.startsWith("http")){
							convertIp = "http://"+convertIp;
						}
						convertIp = getYozoServer(convertIp);
						convertPath = convertIp + convertPath;
					}
					
					apidatas.put("result",0);
					Map<String,String> data = new HashMap<String,String>();

					//boolean isZip = convertPath.contains("zipview.html");

					boolean flag = convertPath.contains("?");

					convertPath = replaceClient(convertPath,otherParams);
					if("ofd".equals(extname.toLowerCase())){
						data.put("path",convertPath);
					}else {
						data.put("path",convertPath + getWatermark(user,otherParams,true,convertPath));
					}

					List<Map<String,String>> dataList = new ArrayList<Map<String,String>>();
					dataList.add(data);
					apidatas.put("data",dataList);
					break;
				}
			}else{
				apidatas = doConvertForPath(fileid,user,clientAddress,otherParams,convertType,from,isCopy);
				break;
			}

		}

		return apidatas;
	}

	public Map<String,Object> doConvertForPath(int fileid,User user,String clientAddress,Map<String,String> otherParams,String convertType,String from,int isCopy){
		Map<String,Object> apidatas = new HashMap<String,Object>();
		apidatas.put("result", -10);
		RecordSet rs = new RecordSet();
		String date = TimeUtil.getCurrentDateString();
		String time = TimeUtil.getOnlyCurrentTimeString();
		try{
			rs.executeUpdate("insert into CONVERT_IMAGEFILE(imagefileid,convertype,ecologyTable,convertDate,convertTime,convertPath,isCopy) values(?,?,?,?,?,?,?)",
					fileid,convertType,from,date,time,"",isCopy);
			apidatas.put("result", -11);

			String jsonMap = doConvert(fileid,user,clientAddress,convertType,true,otherParams,from);
			apidatas.put("result", -12);
			if(jsonMap == null || jsonMap.isEmpty()){
				apidatas.put("result", -13);

				ImageConvertUtil icu = new ImageConvertUtil();
				String convertIp = icu.getConvertIp();
				apidatas.put("convertIp",convertIp);
				apidatas.put("message","http请求超时!");
				return apidatas;
			}
			ImageFileManager ifm = new ImageFileManager();
			ifm.resetParameter();
			ifm.getImageFileInfoById(fileid);
			String filename = ifm.getImageFileName();
			String extname = filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "";
			extname = extname.toLowerCase();

			JSONObject obj = JSONObject.parseObject(jsonMap);
			apidatas.put("result", -14);
			int result = (Integer)obj.get("result");
			String message = (String)obj.get("message");
			apidatas.put("result",result);
			apidatas.put("message",message);
			Object data = obj.get("data");
			if(result == 0 && data != null){
				 List<String> datas = (List<String>)data;
				 if(datas.size() > 0){
					 String convertPath = datas.get(0);

					 rs.executeUpdate("update CONVERT_IMAGEFILE set convertPath=? where imagefileid=? and convertype=? and ecologyTable=? and convertDate=? and convertTime=? and isCopy=?",
							 convertPath,fileid,convertType,from,date,time,isCopy);

					 boolean flag = convertPath.contains("?");

					 String ismobile = otherParams.get("ismobile");
					 ImageConvertUtil icu = new ImageConvertUtil();
					 String convertIp = icu.getConvertIp();

					 if(!"1".equals(ismobile) && !convertPath.startsWith("http")){
						 if(!convertIp.startsWith("http")){
							 convertIp = "http://"+convertIp;
						 }
						 convertIp = getYozoServer(convertIp);
						 convertPath = convertIp + convertPath;
					 }
					 // 2
					 convertPath = replaceClient(convertPath,otherParams);

					 Map<String,String> dataMap = new HashMap<String,String>();
					 if("ofd".equals(extname.toLowerCase())){
						 dataMap.put("path",convertPath);
					 }else{
						 dataMap.put("path",convertPath  + getWatermark(user,otherParams,true,convertPath));
					 }

					 List<Map<String,String>> dataList = new ArrayList<Map<String,String>>();
					 dataList.add(dataMap);
					 apidatas.put("data",dataList);
				 }
			}else{
				apidatas.put("fileid",fileid);
				apidatas.put("jsonMap",jsonMap);
				ImageConvertUtil icu = new ImageConvertUtil();
				String convertIp = icu.getConvertIp();
				apidatas.put("convertIp",convertIp);
			}

		}catch(Exception e){

		}finally{
			if(!"0".equals(apidatas.get("result").toString())){
				rs.executeUpdate("delete from CONVERT_IMAGEFILE where imagefileid=? and convertype=? and ecologyTable=? and convertDate=? and convertTime=?",
					fileid,convertType,from,date,time);
			}
		}
		return apidatas;
	}

	public String getToken(String convertPath){
		if(convertPath == null){
			return "";
		}
		String filename = "";
		if(convertPath.contains("/")){
			filename = convertPath.substring(convertPath.lastIndexOf("/") + 1);
		}else if(convertPath.contains("\\")){
			filename = convertPath.substring(convertPath.lastIndexOf("\\") + 1);
		}

		if(filename.contains(".")){
			filename = filename.substring(0,filename.lastIndexOf("."));
		}

		ImageConvertUtil icu = new ImageConvertUtil();
		String convertIp = icu.getConvertIp();

		filename = URLEncoder.encode(filename);

		if(!convertIp.startsWith("http")){
			convertIp = "http://"+convertIp;
		}

		convertIp = getYozoServer(convertIp);
		String jsonMap = sendPost(convertIp+"/getTokenKey", "viewurl="+filename);

		if(jsonMap != null && !jsonMap.isEmpty()){
			try{
				JSONObject obj = JSONObject.parseObject(jsonMap);
				int result = (Integer)obj.get("result");
				if(result == 0){
					return (String)obj.get("tokenKey");
				}
			}catch(Exception e){
				writeLog("^^^^^^^^获取token异常("+filename+")^^^^^^^^^" + jsonMap);
			}
		}
		writeLog("^^^^^^^^获取token为空("+filename+")^^^^^^^^^" + jsonMap);
		return "";
	}

	public String getWatermark(User user,Map<String,String> otherParams,boolean encode){
		return getWatermark(user,otherParams,encode,"");
	}
	/**
	 * 获取水印内容
	 * */
	public String getWatermark(User user,Map<String,String> otherParams,boolean encode,String Url){

		//if(otherParams == null || otherParams.get("converSelf") == null || otherParams.get("converSelf").isEmpty()){
		//	return "";
		//}

		if(!openWatermark()){
			return "";
		}

		String waterContent = "";
		boolean hasParams = Url.indexOf("?")>0;

		String userinfo = getPropValue("doc_custom_for_weaver", "watercontent");
		if(!userinfo.isEmpty()){
			RecordSet rs = new RecordSet();
			boolean flag = rs.executeQuery("select " + userinfo + " from hrmresource where id=?",user.getUID());
			if(flag && rs.next()){
				String []columns = rs.getColumnName();
				for(String column : columns){
					String value = rs.getString(column);
					if(encode){
						try{
						value = URLEncoder.encode(value,"UTF-8");
						}catch(Exception e){}
					}
					if(waterContent.isEmpty()){
						waterContent = (hasParams? "&watermark_txt=" :"?watermark_txt=") + value;
					}else{
						waterContent += " " + value;
					}
				}
				waterContent += " " + TimeUtil.getCurrentDateString();
				waterContent += " " + TimeUtil.getOnlyCurrentTimeString();
				return waterContent;
			}
		}

		String username = user.getLastname();

		username = Util.formatMultiLang(username,user.getLanguage() + "");

		if(encode){
			try{
				username = URLEncoder.encode(username,"UTF-8");
			}catch(Exception e){
			}
		}

		RecordSet rs = new RecordSet();
		rs.executeQuery("select workcode from hrmresource where id=?",user.getUID());
		String workcode = "";
		if(rs.next()){
			workcode = rs.getString("workcode");
		}

		waterContent = (hasParams? "&watermark_txt=" :"?watermark_txt=") + username +
			"" + workcode +
			" " + TimeUtil.getCurrentDateString() +
			" " + TimeUtil.getOnlyCurrentTimeString();


		return waterContent;
	}

	/**
	 * 判断移动端是否支持预览（邮件）
	 * */
	public static boolean readOnlineForMobile(String extname,Map<String,String> paramsMap){
		extname = Util.null2String(extname);
		extname = extname.toLowerCase();
		String checkPic = Util.null2s(paramsMap.get("checkPic"),"0");
		String checkSize = Util.null2s(paramsMap.get("checkSize"),"0");
		int docid = Util.getIntValue(paramsMap.get("docid"));
		if("pdf".equals(extname)){
			return true;
		}
		if("1".equals(checkPic) && isPic(extname)){
			return true;
		}
		if("ofd".equals(extname.toLowerCase()) && shukeLightReaderPrew() && docid>0){
			return true;
		}
		if(!canConvertType(extname)){
			return false;
		}
		boolean isUsePDFViewer = ConvertPDFUtil.isUsePDFViewer();

		if(isUsePDFViewer){
			return true;
		}else if(isUsePoiForMobile() && ("doc".equals(extname.toLowerCase()) ||
                "docx".equals(extname.toLowerCase()) ||
                "xls".equals(extname.toLowerCase()) ||
                "xlsx".equals(extname.toLowerCase()))
                ){
            return true;
         }

		return false;
	}

	/**
	 * 移动端支持poi
	 * */
	public static boolean isUsePoiForMobile(){
        BaseBean bb = new BaseBean();
        String isusepoi = Util.null2s(bb.getPropValue("doc_mobile_isusepoi","isUsePoi"),"0");
        String IsUseDocPreview = Util.null2s(bb.getPropValue("docpreview","IsUseDocPreview"),"0");
        String isUsePDFViewer = Util.null2s(bb.getPropValue("docpreview","isUsePDFViewer"),"0");
        if("1".equals(isusepoi) && "1".equals(IsUseDocPreview) && "0".equals(isUsePDFViewer)){
            return true;
        }else{
            return false;
        }
    }
	/**
	 * pc是否开启poi转html
	 * */
	public static boolean isUsePoiForPC(){
		BaseBean bb = new BaseBean();
		return "1".equals(bb.getPropValue("docpreview","poiTohtml"));
	}

	/**
	 * 支持转换的格式
	 * */
	public static boolean canConvertType(String extname){
		extname = Util.null2String(extname);
		extname = extname.toLowerCase();
		if(isOffice(extname) ||
        		extname.equals("txt")
			){
			return true;
		}else if(noNeedConvert(extname)){
			return true;
		}else if(previewZip(extname)){
			return true;
		}else if(isUotOfd(extname)){
			return true;
		}else if(canConvertFromProp(extname)){
			return true;
		}
		return false;
	}

	public static boolean isUotOfd(String extname){
		extname = Util.null2String(extname);
		extname = extname.toLowerCase();
		if((extname.equals("uot")||extname.equals("ofd"))&& !canViewForWps() && canViewForYOZO()){
			return true;
		}
		return false;
	}
	/**
	 * 不转换，只调转换服务器预览
	 * */
	public static boolean noNeedConvert(String extname){
		extname = Util.null2String(extname);
		extname = extname.toLowerCase();
		if(extname.equals("pdf")){
			return true;
		}
		return false;
	}

	/**
	 * 是否支持压缩包预览
	 * */
	public static boolean previewZip(String extname){
		ImageConvertUtil icu = new ImageConvertUtil();
		if(!icu.convertForClient()){
			return false;
		}
		extname = Util.null2String(extname);
		extname = extname.toLowerCase();
		if(extname.equals("zip") ||
        		extname.equals("rar")
			){
			return true;
		}

		// tif、tiff格式支持
		if(extname.equals("tif") || extname.equals("tiff")){
			return true;
		}

		return false;
	}

	/**
	 * 是否是office
	 * */
	public static boolean isOffice(String extname){
		extname = Util.null2String(extname);
		extname = extname.toLowerCase();

		if(extname.equals("doc") ||
        		extname.equals("docx") ||
        		extname.equals("xls") ||
        		extname.equals("xlsx") ||
        		extname.equals("ppt") ||
        		extname.equals("pptx") ||
        		extname.equals("wps")
			){
			return true;
		}
		return false;
	}

	/**
	 * 判断文件类型是否包含在配置文件中指定的可预览类型，指定预览服务可转换的文件类型
	 * @param extname
	 * @return
	 */
	public static boolean canConvertFromProp(String extname){
		if(null == extname || "".equals(extname)){
			return false;
		}

		return getExtendConvertTypeFromProp().contains(extname);
	}

	/**
	 * 从配置文件中读取额外的参数，指定预览服务可转换的文件类型
	 * @return 返回所有支持的类型
	 */
	public static List<String> getExtendConvertTypeFromProp(){
		List<String> exts = new ArrayList<String>();
		BaseBean toolBean = new BaseBean();
		String suffix = Util.null2String(toolBean.getPropValue("docpreview","canPreviewFileType"));
		if("".equals(suffix)){
			return exts;
		}
		String[] extFromProp = suffix.replaceAll("[\\s]+","").split(",");
		for(String ext : extFromProp) {
			if("".equals(ext)){
				continue;
			}
			exts.add(ext);
		}
		return exts;
	}

	@Deprecated
	public static boolean canEditForYozo(String extname,User user){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return canEditForYozo(extname, user, false);
	}

	/**
	 * 支持永中在线编辑
	 * */
	@Deprecated
	public static boolean canEditForYozo(String extname,User user, boolean ismobile){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.canEditForYozo(extname, user, ismobile);
	}

	public static boolean canViewForWps(){
		BaseBean bb = new BaseBean();
		return "1".equals(bb.getPropValue("doc_wps_for_weaver", "wps_view_online")) || canViewForWpsForLinux()|| canViewForWpsDocCenter() ;
	}
	public static boolean canViewForWps(User user){
		BaseBean bb = new BaseBean();
		return "1".equals(bb.getPropValue("doc_wps_for_weaver", "wps_view_online")) || canViewForWpsDocCenter(user);
	}
	public static boolean canUseForWpsDocCenter(){
		return weaver.wps.doccenter.utils.Config.isWpsDocCenterOpen(Config.OPEN_ALL);
	}

	public static boolean canUseForWpsDocCenter(String type){
		return weaver.wps.doccenter.utils.Config.isWpsDocCenterOpen(type);
	}

	public static boolean canViewForWpsDocCenter(){
		return weaver.wps.doccenter.utils.Config.isWpsDocCenterOpen(Config.OPEN_PREVIEW);
	}

	public static boolean canViewForWpsDocCenter(User user){
		return weaver.wps.doccenter.utils.Config.alwaysUseDocCenter(user, Config.OPEN_PREVIEW);
	}

	public static boolean canFormatForWpsDocCenter(){
		return weaver.wps.doccenter.utils.Config.isWpsDocCenterOpen(Config.OPEN_FORMAT);
	}

	public static boolean canEditForWpsDocCenter(){
		return weaver.wps.doccenter.utils.Config.isWpsDocCenterOpen(Config.OPEN_EDIT);
	}

	public static boolean canEditForWpsDocCenter(User user){
		return weaver.wps.doccenter.utils.Config.alwaysUseDocCenter(user, Config.OPEN_EDIT);
	}
	public static boolean canViewForWpsForLinux(){
		PropUtil propUtil = new PropUtil(null);
		return propUtil.isLinuxPreviewOpen();
	}

	public static boolean isOfficedV2(){
		BaseBean bb = new BaseBean();
		String officedInterfaceVersion = bb.getPropValue("doc_wps_for_weaver","officedInterfaceVersion");
		return "v2".equals(officedInterfaceVersion);
	}

	public static boolean shukeLightReaderPrew(){
		BaseBean bb = new BaseBean();
		String readerType = bb.getPropValue("weaver_OFDReader","readerType");
		return ("2".equals(readerType) || "3".equals(readerType));
	}
	public static String takeDocByfileid(String fileid){
		RecordSet rs = new RecordSet();
		rs.executeQuery("select docid from docimagefile where imagefileid = "+fileid);
		if(rs.next()){
			return  Util.null2String(rs.getString("docid"));
		}
		return "";
	}

	public static boolean canViewForYOZO(){
		BaseBean bb = new BaseBean();
		return "1".equals(bb.getPropValue("doc_custom_for_weaver", "convert_client_open"));
	}

	public static boolean canEditForWps(String extname,User user){
		/*if(user == null){
			return false;
		}*/

		BaseBean bb = new BaseBean();
		boolean wpsOnline = "1".equals(bb.getPropValue("doc_wps_for_weaver", "wps_edit_online"));
		if(!wpsOnline && !canEditForWpsDocCenter(user)){
			return false;
		}

		/*int deptid = user.getUserDepartment();
		weaver.hrm.company.DepartmentComInfo dci = new weaver.hrm.company.DepartmentComInfo();
		String pid = dci.getDepartmentsupdepid(deptid + "");
		if(deptid != 350 && deptid != 905 && user.getUID() != 1 && user.getUID() != 8 && !"25".equals(pid)){
			return false;
		}*/

		if(!isOffice(extname) || "wps".equals(extname)){
			return false;
		}
		return true;
	}

	/**
	 * 替换服务器ip
	 * path : 预览路径
	 * params : 转换参数集(可以判断移动、pc，浏览器类型等)
	 * */
	public String replaceClient(String path,Map<String,String> params){
		String agent = Util.null2s(params.get("userAgent"),"");
		String isOpenPathReplace = Util.null2String(getPropValue("doc_custom_for_weaver", "pathReplaceType"));

		boolean ismobile = params != null && "1".equals(params.get("ismobile"));
		if(!(agent.toLowerCase().contains("emobile") || agent.toLowerCase().contains("android") || agent.toLowerCase().contains("iphone") || agent.toLowerCase().contains("ipad"))) {
			ismobile = false;
		}
		boolean _writeLog ="1".equals(Util.null2String(getPropValue("doc_wps_for_weaver", "writeLog")));

		if(_writeLog)
			writeLog("^^^^^^^^(isOpenPathReplace="+isOpenPathReplace+")(ismobile="+ismobile+")^^^^^^^^^params=" + params+"^^^^^^^^agent:"+agent);

		//1、只替换pc;2、只替换移动;3、都替换;其他值或不配置、不替换
		if(("1".equals(isOpenPathReplace) || "3".equals(isOpenPathReplace)) && !ismobile){

			String sourceClientPath = Util.null2String(getPropValue("doc_custom_for_weaver", "sourceClientPath"));
			String targetClientPath = Util.null2String(getPropValue("doc_custom_for_weaver", "targetClientPath"));

			return path.replace(sourceClientPath,targetClientPath);
		}else if(("2".equals(isOpenPathReplace) || "3".equals(isOpenPathReplace)) && ismobile){
			String sourceClientPath = Util.null2String(getPropValue("doc_custom_for_weaver", "sourceClientPath"));
			String targetClientPathForMobile = Util.null2String(getPropValue("doc_custom_for_weaver", "targetClientPathForMobile"));
			return path.replace(sourceClientPath,targetClientPathForMobile);
		}

		return path;

	}

	/**
	 * 是否是图片
	 * */
	public static boolean isPic(String extname){
		extname = Util.null2String(extname);
		extname = extname.toLowerCase();
		if(extname.equals("jpg") ||
				extname.equals("jpeg") ||
				extname.equals("gif") ||
				extname.equals("png") ||
				extname.equals("bmp")
				){
			return true;
		}
		return false;
	}
	
	/**
	 * 是否启用新的附件预览方式
	 * */
	public static boolean useNewAccView(){
		boolean isUsePDFViewer = ConvertPDFUtil.isUsePDFViewer();
		if(!isUsePDFViewer){
			return false;
		}
		boolean useNewAccView = "1".equals(new BaseBean().getPropValue("docpreview","useNewAccView"));
		return useNewAccView;
	}

	public static boolean isMsgObjToDocument(){
        boolean isMsgObjToDocument=true;
        BaseBean basebean = new BaseBean();
        String mClientName=Util.null2String(basebean.getPropValue("weaver_obj","iWebOfficeClientName"));
        boolean isIWebOffice2003 = (mClientName.indexOf("iWebOffice2003")>-1)?true:false;
        String isHandWriteForIWebOffice2009=Util.null2String(basebean.getPropValue("weaver_obj","isHandWriteForIWebOffice2009"));
        if(isIWebOffice2003||isHandWriteForIWebOffice2009.equals("0")){
            isMsgObjToDocument=false;
        }

        return isMsgObjToDocument;
    }

	public void deleteConvert(int fileid,User user){
		RecordSet rs = new RecordSet();
		rs.executeUpdate("delete from CONVERT_IMAGEFILE where imagefileid=?",fileid);
	}

	public void deleteFileForPath(){


		int day = getDaySet();
		Calendar calendar = Calendar.getInstance();
		if(day > 1){
			calendar.add(Calendar.DATE,-day);
		}
		String currentDate = TimeUtil.getDateString(calendar);

		writeLog("^^^^^^^^^^^^^^ 定时删除("+day+")天前转换数据 start ^^^^^^^^^^^^^^" + currentDate);

		RecordSet rs = new RecordSet();
		rs.executeUpdate("delete from CONVERT_IMAGEFILE where convertDate<=?",currentDate);
		
		rs.executeUpdate("delete from doc_yzconvert_fileviewurl where convertDate<=?",currentDate);

		writeLog("^^^^^^^^^^^^^^ 删除数据库数据 over ^^^^^^^^^^^^^^");

		ImageConvertUtil icu = new ImageConvertUtil();
		String convertIp = icu.getConvertIp();

		if(!convertIp.startsWith("http")){
			convertIp = "http://"+convertIp;
		}

		writeLog("^^^^^^^^^^^^^^ 调远程服务接口 before ^^^^^^^^^^^^^^" + convertIp);

		convertIp = getYozoServer(convertIp);
		sendPost(convertIp+"/deloutputfilefordate","date=" + currentDate);

		writeLog("^^^^^^^^^^^^^^ 调远程服务接口 end ^^^^^^^^^^^^^^");

		writeLog("^^^^^^^^^^^^^^ 定时删除("+day+")天前转换数据 end ^^^^^^^^^^^^^^");

	}
	public void dcsWriteLog(String log){
		Boolean open =  "1".equals(Util.null2s(getPropValue("doc_custom_for_weaver","yozowriteLog"),"0"));
		if(open){
			writeLog(log);
		}
	}

	/**
	 * 判断当前后缀的文档是否支持在线预览
	 * 1、不部署在线转换服务
	 * 		可在线预览的 图片、pdf doc、docx、xls、xlsx
	 *
	 * 2、部署在线转换服务
	 * 		ImageConvertUtil.canConvertType(extName);
	 * @param extname
	 * @return
	 */
	public static boolean canViewOnline(String extname){
	    return canViewOnline(extname,false);
    }
	public static boolean canViewOnline(String extname,Boolean isIE){
		extname = extname.toLowerCase();
		ImageConvertUtil imageConvertUtil = new ImageConvertUtil();
		boolean isViewOpen = imageConvertUtil.convertForClient();
		if(extname.equals("doc") ||
				extname.equals("docx") ||
				extname.equals("xls") ||
				extname.equals("xlsx") ||
				isPic(extname) ||
				extname.equals("pdf")
				){
			return true;
		}
		if("ofd".equals(extname.toLowerCase()) && shukeLightReaderPrew()){
			return true;
		}
//		if(isIE &&
//				(extname.equals("ppt")
//						|| extname.equals("pptx")
//						|| extname.equals("wps"))){
//			return true;
//		}
//		if(isViewOpen){
//			return ImageConvertUtil.canConvertType(extname);
//		}
		return false;
	}

	public static boolean canViewOnlineByFileid(String fileid){
		String extname = "";
		String sql = "select imagefilename from imagefile where imagefileid = ?";
		RecordSet rs = new RecordSet();
		rs.executeQuery(sql,fileid);
		if(rs.next()){
			extname = Util.null2s(rs.getString("imagefilename"),"");
		}
		extname = DocDetailUtil.getFileSuffixName(extname,false);
		return canViewOnline(extname);
	}

	@Deprecated
	public Map<String, String> openFile2Yozo(Map<String, String> params, User user){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.openFile2Yozo(params, user);
	}

	@Deprecated
	public Map<String, String> saveFile2Yozo(Map<String, String> params, User user){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.saveFile2Yozo(params, user);
	}

	@Deprecated
	public Map<String, String> closeFile2Yozo(Map<String, String> params, User user){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.closeFile2Yozo(params, user);
	}

	@Deprecated
	public Map<String, String> isOpen2Yozo(Map<String, String> params, User user){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.isOpen2Yozo(params, user);
	}

	@Deprecated
	private Map<String, String> operat2Yozo(int method, String fileId, User user, JSONObject otherParam){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.operat2Yozo(method, fileId, user, otherParam);
	}

	@Deprecated
	private Map<String, String> operat2Yozo(int method, String fileId, User user, JSONObject otherParam, String clientPath){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.operat2Yozo(method, fileId, user, otherParam, clientPath);
	}

	@Deprecated
	private Map<String,String> httpDataPostForYozo(String url, Object param, User user){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.httpDataPostForYozo(url, param, user);
	}

	@Deprecated
	private Map<String,String> httpDataPost(String url, HttpEntity reqEntity){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.httpDataPost(url, reqEntity, null);
	}

	/**
	 * 是否通过 fileManagement.do创建文档
	 */
	@Deprecated
	public boolean isCreateDocByFM(){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.isCreateDocByFM();
	}

	/**
	 * 新建文档时的一级目录
	 */
	@Deprecated
	public String getYozoDocFilePath(){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.getYozoDocFilePath();
	}

	/**
	 * 获取永中服务文件上传接口
	 */
	@Deprecated
	public String getYozoUploadApi(){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.getYozoUploadApi();
	}

	/**
	 * 是否通过 fileManagement.do创建文档
	 */
	@Deprecated
	public boolean isDelAfterUpload2Yozo(){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.isDelAfterUpload2Yozo();
	}

	/**
	 * 获取永中配置文件中临时目录
	 */
	@Deprecated
	public String getYozoFileTempDir(){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.getYozoFileTempDir();
	}

	/**
	 * 查询永中WebOffice的配置文件信息
	 */
	@Deprecated
	public static String getYozoProperties(String propName){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.getYozoProperties(propName);
	}

	/**
	 * 用于输出永中WebOffice服务相关日志输出日志
	 */
	@Deprecated
	public void writeLogsForYozo(Object msg){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		YozoWebOfficeUtil.writeLogsForYozo(msg);
	}

	/**
	 * 查询永中各个方法的参数
	 */
	@Deprecated
	public static Map<String, Integer> getYoaoApiMethods(){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.getYoaoApiMethods();
	}

	/**
	 * 通过接口创建文档
	 */
	@Deprecated
	private Map<String,String> createDocByFM(String filename, User user){
		// 代码分离，保留方法名适配已集成的客户，方法体移到独立代码中
		return YozoWebOfficeUtil.createDocByFM(filename, user);
	}
}
