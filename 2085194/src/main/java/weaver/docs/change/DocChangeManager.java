package weaver.docs.change;

import DBstep.iMsgServer2000;
import com.api.odoc.bean.*;
import com.api.odoc.constant.ExchangeWebserviceConstant;
import com.api.odoc.util.ExchangeClientXmlUtil;
import com.api.odoc.util.ExchangeWebserviceUtil;
import com.api.odoc.util.RequestIdUtil;
import com.engine.hrm.biz.HrmClassifiedProtectionBiz;
import com.engine.odocExchange.constant.ExchangeFieldType;
import com.engine.odocExchange.entity.ExchangeField;
import com.engine.odocExchange.enums.ExchangeStatusEnum;
import com.engine.odocExchange.util.DocIdentifierGenerateUtil;
import com.engine.odocExchange.util.ExchangeCommonMethodUtil;
import com.engine.workflow.biz.requestForm.RequestSecLevelBiz;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.xfire.client.Client;
import org.docx4j.dml.chart.CTTimeUnit;
import org.dom4j.Node;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import sun.misc.BASE64Encoder;
import weaver.common.StringUtil;
import weaver.conn.DBUtil;
import weaver.conn.RecordSet;
import weaver.conn.RecordSetTrans;
import weaver.docs.senddoc.DocReceiveUnitComInfo;
import weaver.docs.senddoc.DocReceiveUnitManager;
import weaver.docs.util.SslUtils;
import weaver.file.ImageFileManager;
import weaver.general.*;
import weaver.hrm.User;
import weaver.hrm.tools.Time;
import weaver.soa.workflow.request.MainTableInfo;
import weaver.soa.workflow.request.Property;
import weaver.soa.workflow.request.RequestInfo;
import weaver.soa.workflow.request.RequestService;
import weaver.systeminfo.SystemEnv;
import weaver.workflow.field.FieldComInfo;
import weaver.workflow.field.FieldValue;
import weaver.workflow.request.RequestDoc;
import weaver.workflow.request.RequestManager;
import weaver.workflow.request.SubWorkflowTriggerService;
import weaver.workflow.workflow.WFModeNodeFieldManager;
import weaver.workflow.workflow.WorkflowComInfo;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

/**
 * 待办列表
 */
public class DocChangeManager extends BaseBean {
	private static final Log LOG = LogFactory.getLog(DocChangeManager.class);
	private StaticObj staticobj = null;
	private static String Encoding = GCONST.XML_UTF8;

	private Element flowDetail;
	private List formsList = new ArrayList();
	private List attachmentsList = new ArrayList();
	private String formName = "";
	private String currentTime = "";
    private String currentDate = "";
    private Map sendMap = new HashMap();
    private DocReceiveUnitComInfo docReceiveUnitComInfo;
    public String serverURL = "";//交换服务器地址
    public boolean isChangeField = false;//是否交换流程字段
    private Map versionMap = new HashMap();//版本

    public static final String MAIN_FLAG = "S";//主送方标识
    public static final String OUTSIDE_FLAG = "R";//接收方标识
    public static final String RECEIVE_FLAG = "R_";//接收文件标识
    public static final String SEND_FLAG = "S_";//发送文件标识
    private static String requesturl = Util.null2String(new BaseBean().getPropValue("DocChange", "requesturl"));

    public int autoSendTime = 0;
    public int autoReceiveTime = 0;
    public boolean autoSend = false;
    public boolean autoReceive = false;
    public boolean isFtp = false;      //是否FTP方式
    public static String cversion = "";//系统版本

	String ftpurl = "";
	String ftpuser = "";
	String ftppwd = "";
	String ftpport = "";

    public DocChangeManager() {
    	staticobj = StaticObj.getInstance();
    	try {
    		setSettingCache();
    		docReceiveUnitComInfo = new DocReceiveUnitComInfo();
    		//获取执行时间
        	currentTime = TimeUtil.getOnlyCurrentTimeString();
            currentDate = TimeUtil.getCurrentDateString();
    	}catch(Exception e) {
    	}
    }

    /**
     * 公文交换设置缓存
     */
    public void setSettingCache() {
    	if(null!=staticobj&&staticobj.getObject("DocChangeSetting.autoSend")==null) {
			RecordSet rs = new RecordSet();
			rs.executeSql("SELECT * FROM DocChangeSetting");
			if(rs.next()) {
				staticobj.putObject("DocChangeSetting.autoSend", rs.getString("autoSend"));
				staticobj.putObject("DocChangeSetting.autoSendTime", rs.getString("autoSendTime"));
				staticobj.putObject("DocChangeSetting.autoReceive", rs.getString("autoReceive"));
				staticobj.putObject("DocChangeSetting.autoReceiveTime", rs.getString("autoReceiveTime"));
				staticobj.putObject("DocChangeSetting.serverURL", rs.getString("serverURL"));
				staticobj.putObject("DocChangeSetting.serverUser", rs.getString("serverUser"));
				staticobj.putObject("DocChangeSetting.serverPwd", rs.getString("serverPwd"));
				staticobj.putObject("DocChangeSetting.changeMode", rs.getString("changeMode"));
				staticobj.putObject("DocChangeSetting.maincategory", rs.getString("maincategory"));
				staticobj.putObject("DocChangeSetting.subcategory", rs.getString("subcategory"));
				staticobj.putObject("DocChangeSetting.seccategory", rs.getString("seccategory"));
				staticobj.putObject("DocChangeSetting.pathcategory", rs.getString("pathcategory"));
				staticobj.putObject("DocChangeSetting.serverPort", rs.getString("serverPort"));
			}
			//系统版本
			rs.executeSql("select cversion from license");
			if(rs.next()) {
				cversion = Util.null2String(rs.getString("cversion")).substring(0,1);
			}
		}

    	ftpurl = (String) staticobj.getObject("DocChangeSetting.serverURL");
		ftpuser = (String) staticobj.getObject("DocChangeSetting.serverUser");
		ftppwd = (String) staticobj.getObject("DocChangeSetting.serverPwd");
		ftpport = (String) staticobj.getObject("DocChangeSetting.serverPort");
		try {
			serverURL = "ftp://"+ URLEncoder.encode(ftpuser, "UTF-8")+":"+URLEncoder.encode(ftppwd, "UTF-8")+"@"+ftpurl+"/";//目前只支持FTP，所以写死了
		} catch (UnsupportedEncodingException e) {
			writeLog("serverURL URLEncoder.encode exception:",e);
		}

		autoSendTime = Util.getIntValue((String) staticobj.getObject("DocChangeSetting.autoSendTime"), 60);
		autoReceiveTime = Util.getIntValue((String) staticobj.getObject("DocChangeSetting.autoReceiveTime"), 60);

		isChangeField = ("ftp").equals((String) staticobj.getObject("DocChangeSetting.changeMode"));
		autoSend = ("1").equals((String) staticobj.getObject("DocChangeSetting.autoSend"));
		autoReceive = ("1").equals((String) staticobj.getObject("DocChangeSetting.autoReceive"));
		isFtp = ("ftp").equals((String) staticobj.getObject("DocChangeSetting.changeMode"));
    }

    /**
     * 手动发送公文
     * @param userid	当前用户
     * @param requestList	发送公文的流程ID列表
     * @return -1:手动发送失败,请求ID为空
     */
    public String SendDocManual(int userid, String requestList) {
    	if(Util.null2String(requestList).equals("")) {
    		return "-1";
    	}
    	return SendDoc(userid, requestList);
    }

    public String SendDoc(int userid, String requestsStr) {
    	return doSendDoc(userid, userid, requestsStr, null);
    }

    public String doSendDoc(int sender, int userid, String requestsStr) {
    	return doSendDoc(userid, userid, requestsStr, null);
    }

    /**
     * 发送公文
     * @param sender	发送者
     * @param userid	当前用户
     * @param requestsStr	发送公文的流程ID列表 如果为空则发送全部
     * @return	0:成功； 1:失败.系统出错
     */
    public String doSendDoc(int sender, int userid, String requestsStr, String otherSender) {
    	LOG.info("doSendDoc() sender="+sender+",userid="+userid+",requestsStr="+requestsStr+",otherSender="+otherSender);
    	String sql = "";
    	RecordSet rs = new RecordSet();
    	//获取执行时间
    	currentTime = TimeUtil.getOnlyCurrentTimeString();
        currentDate = TimeUtil.getCurrentDateString();
        LOG.info("doSendDoc() currentDateTime="+currentDate+" "+currentTime);
		try {
			StringTokenizer st = new StringTokenizer(requestsStr, ",");
            int sendCountNum = st.countTokens();
			int sendSuccessCountNum = 0;
			String errorMsg = "";
			while (st.hasMoreTokens()) {
				String requestid = st.nextToken();

				Map oldSenderMap = new HashMap();
				boolean isResend = !Util.null2String(otherSender).equals("");
				if(isResend) {
					//检查已发送过的收文单位列表
					rs.executeSql("SELECT receiver FROM DocChangeSendDetail WHERE requestid="+requestid+" AND status IN(0,1)");
					while(rs.next()) {
						oldSenderMap.put(rs.getString("receiver"), "X");
					}
				}

				LOG.info("doSendDoc() requestid="+requestid+",isResend="+isResend+",oldSenderMap="+ JSONObject.fromObject(oldSenderMap).toString());
				Document responseXml = new Document();
				responseXml = GetXml(userid, requestid, isResend);

				List sendList = (List) sendMap.get(requestid);
				LOG.info("doSendDoc() sendMap="+ JSONObject.fromObject(sendMap).toString());
				if(sendList==null) {
					sendList = new ArrayList();
				}
				if(isResend) {
					//如果是重发的单位,放进发送列表中
					StringTokenizer tz = new StringTokenizer(otherSender, ",");
					while (tz.hasMoreTokens()) {
						String cpid = tz.nextToken();
						if(!Util.null2String(cpid).equals("")) {
							String cdir = docReceiveUnitComInfo.getChangeDir(cpid);
							sendList.add(cpid+","+cdir);
						}
					}
				}
				LOG.info("doSendDoc() sendList="+ StringUtils.join(sendList, "<-->"));
				Map checkMap = new HashMap();
				boolean isSend = false;
				if(sendList!=null&&sendList.size()>0) {
					for(int i=0; i<sendList.size(); i++) {
						String tempstr = (String) sendList.get(i);
						if(checkMap.get(tempstr)!=null) continue;//重复的不上传
						else checkMap.put(tempstr, tempstr);
						String dir = "";
						String receiverid = "";
						StringTokenizer tempst = new StringTokenizer(tempstr, ",");
						int cnt = 0;
						while (tempst.hasMoreTokens()) {
							if(cnt==0) receiverid = tempst.nextToken();
							else dir = tempst.nextToken();
							cnt++;
						}
						LOG.info("doSendDoc() tempstr="+tempstr+",dir="+dir+",receiverid="+receiverid);
						String urlpath = "";
						if(StringUtil.isNotNull(dir,docReceiveUnitComInfo.getIsMain(receiverid))){
						    if(isResend) {
                                //重发情况下对已经发过的不重发
                                if(oldSenderMap.get(receiverid)!=null) continue;
                            }
                            String xmlfilename = SEND_FLAG+requestid+".xml";
                            String addstr = "";
                            if(docReceiveUnitComInfo.getIsMain(receiverid).equals("0")) {
                                //接收单位是非主送方的,标记置为主送方
                                addstr = MAIN_FLAG;
                            } else {
                                //接收单位是主送方的,标记置为非主送方
                                addstr = OUTSIDE_FLAG;
                            }
                            xmlfilename = addstr+xmlfilename;
                            //创建目录
                            try{
                                new FtpClientUtil(ftpurl, Util.getIntValue(ftpport,21), ftpuser, ftppwd).CreateDir(dir);
                            }catch(Exception e){
                                e.printStackTrace();
                                writeLog(e);
                            }
                            urlpath = dir+"/"+xmlfilename;
                            isSend = fileToServer(urlpath, responseXml);//上传文件至服务器
                            LOG.info("doSendDoc() isSend="+isSend+",urlpath="+urlpath);
						}else{
						    errorMsg = SystemEnv.getHtmlLabelName(525855,new User(userid).getLanguage());
						    writeLog("公文交换上传文件失败.未设置是否主送方或公文交换目录！");
						}
						if(!isSend) {
						    if(StringUtil.isNull(errorMsg))errorMsg = SystemEnv.getHtmlLabelName(526650,new User(userid).getLanguage());
							writeLog("公文交换上传文件失败.路径="+urlpath);
							continue;
						} else {
							String sendType = "0";
							if(isResend) sendType = "1";
							sql = "INSERT INTO DocChangeSendDetail(id,type,receiver,requestid,status) VALUES ("+getNextChangeId()+",'"+sendType+"','"+receiverid+"','"+requestid+"','0')";
							LOG.info("doSendDoc(0) sql="+sql);
							rs.executeSql(sql);
						}
					}
				}else{
				    errorMsg =  SystemEnv.getHtmlLabelName(526602,new User(userid).getLanguage());;
				}
				if(isSend) {
                    sendSuccessCountNum = sendSuccessCountNum + 1;
					//成功将文件传至各个服务器目录
					sql = "INSERT INTO DocChangeSend(id,senddate,sendtime,requestid,sender) VALUES ("+getNextChangeId()+",'"+currentDate+"','"+currentTime+"','"+requestid+"','"+sender+"')";
					LOG.info("doSendDoc(1) sql="+sql);
					rs.executeSql(sql);
				}
			}
			if(sendCountNum==1){
			    if(sendSuccessCountNum==0){
			        return "-1_"+errorMsg;
			    }else{
			        return "0";
			    }
			}else{
			    return "0_"+sendSuccessCountNum+SystemEnv.getHtmlLabelName(521943,new User(userid).getLanguage())+","+(sendCountNum-sendSuccessCountNum)+SystemEnv.getHtmlLabelName(521944,new User(userid).getLanguage());
			}

		} catch (Exception e) {
			writeLog(e.getMessage());
			return "1";
		}
    }
    /**
	 * @param requestid 流程id
	 * @param userid 用户id
	 */
	public String getWorkflowContent(String requestid, String userid) {
		String workflowcontent = "";
		String url[] = getUrl(requestid, userid);
		if(url!=null && url.length==5){//避免url为空时，出现异常
			boolean hasNull = false;//检查5个值是不是有空，如果有，就不导为文档了
			for(int cx=0; cx<url.length; cx++){
				if(url[cx]==null || "".equals(url[cx])){
					hasNull = true;
				}
			}
			//System.out.println("dddddddddddddddddddddddddddd : hasNull : "+hasNull);
			if(hasNull == false){
				workflowcontent = getWorkflowHtml(url, requestid,userid);
			}
		}
		return workflowcontent;
	}

	/**
	 * 获取流程的url
	 *
	 * @param requestid
	 *            流程id
	 * @param userid
	 *            当前用户
	 * @return
	 */
	public String[] getUrl(String requestid, String userid) {
		String sql = "";
		String tempurl = "";
		String loginid = "";
		String password = "";
		String para = "";
		String oaaddress = "";
		String params[] = new String[5];

		RecordSet rs = new RecordSet();
		sql = "select * from SystemSet";
		rs.executeSql(sql);
		rs.next();
		oaaddress = Util.null2String(rs.getString("oaaddress"));
		if (oaaddress.equals("")) {
			this.writeLog("流程保存为文档失败，因为系统未设置OA访问地址");
			return params;
		}

		sql = "select * from hrmresource where id = " + userid;
		rs.executeSql(sql);
		while (rs.next()) {
			loginid = rs.getString("loginid");
			password = rs.getString("password");
		}

		sql = "select * from HrmResourceManager where id = " + userid;
		rs.executeSql(sql);
		while (rs.next()) {
			loginid = rs.getString("loginid");
			password = rs.getString("password");
		}
		if("".equals(requesturl))
		{
			requesturl = "/workflow/request/PrintRequest.jsp?isprint=1&fromFlowDoc=1&urger=0&ismonitor=0&requestid=";
		}
		if (!loginid.equals("") && !password.equals("")) {
			para = requesturl + requestid
					+ "&para2=" + loginid + "&para3=" + password;
		} else {
			this.writeLog("流程保存为文档失败，因为用户名和密码为空");
			return params;
		}
		tempurl = oaaddress
				+ "/login/VerifyRtxLogin.jsp?urlfrom=workflowtodoc&para1="
				+ para;

		params[0] = oaaddress+"/login/VerifyRtxLogin.jsp";
		params[1] = "workflowtodoc";
		params[2] = requesturl + requestid;
		try {
			params[3] = new String(loginid.getBytes(), "8859_1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		params[4] = password;


		return params;
	}

	/**
	 * 根据url读取html文件，并生成文档，放到指定的目录下
	 *
	 * @param url 流程页面的url
	 *
	 * @param requestid 流程id
	 */
	public String getWorkflowHtml(String url[], String requestid,String userid)
	{
		StringBuffer filesb = new StringBuffer();
		//HttpClient client = new HttpClient();
		HttpClient client = FWHttpConnectionManager.getHttpClient();

		PostMethod method = new PostMethod(url[0]);//oa地址
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,new DefaultHttpMethodRetryHandler(3, false));
		try {
			NameValuePair[] params = {
					new NameValuePair("urlfrom", url[1]),//urlfrom
					new NameValuePair("para1",url[2]),//requesturl
					new NameValuePair("para2",url[3] ),//loginid
					new NameValuePair("para3",url[4])};//url4密码
			method.setRequestBody(params);
			int statusCode = client.executeMethod(method);

			if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY
					|| statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
				Header locationHeader = method.getResponseHeader("location");
				if (locationHeader != null) {
					String tempurl = locationHeader.getValue();
					tempurl = getFinallyUrl(client, tempurl);
					//if(index==1)
					//	tempurl = tempurl.replaceFirst(".jsp", "Iframe.jsp");
					this.writeLog(tempurl);
					GetMethod g = new GetMethod(tempurl);
					client.executeMethod(g);


					BufferedReader in = new BufferedReader(new InputStreamReader(g
							.getResponseBodyAsStream(), "gbk"));
					String line = in.readLine();

					while (line != null) {
						line = line.trim();
//						log.error(line);
						if(line.indexOf("</a>")>=0&&line.indexOf("openSignPrint()")>=0&&line.indexOf("onclick")>=0){
							//log.error(line);
						}else if(line.indexOf("var")>=0&&line.indexOf("bar")>=0&&line.indexOf("eval")>=0&&line.indexOf("handler")>=0&&line.indexOf("text")>=0){
							filesb.append("var bar=eval(\"[]\");\n");
						}else{
							filesb.append(line + "\n");
						}
						line = in.readLine();
					}
					//去掉ext的button,<script type="text/javascript" src="/js/wf_wev8.js">var bar=eval("[]");</script>
					filesb.append("<script type=\"text/javascript\">\n");
					filesb.append("function drm4request2doc(){\n");
					filesb.append("\tbar=eval(\"[]\");\n");
					filesb.append("\tdocument.getElementById(\"rightMenu\").style.display=\"none\";\n");
					filesb.append("}\n");
					filesb.append("window.attachEvent(\"onload\", drm4request2doc);\n");
					filesb.append("</script>");

					in.close();

					if(g!=null){
						g.releaseConnection();
					}
					if(method!=null){
						method.releaseConnection();
					}
				}
			}

		} catch (HttpException e) {
			this.writeLog("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			this.writeLog("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			method.releaseConnection();
		}
		return filesb.toString();
	}
	/**
	 * 获得最后的url，因为response之后的值，post方法不能直接获取
	 * @param client
	 * @param url
	 * @return
	 */
	public String getFinallyUrl(HttpClient client, String url) {
		PostMethod g = new PostMethod(url);
		try {
			client.executeMethod(g);
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return url;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return url;
		}
		Header locationHeader = g.getResponseHeader("location");
		if (locationHeader != null) {
			url = locationHeader.getValue();
			url = getFinallyUrl(client, url);
		}
		if (g != null) {
			g.releaseConnection();
		}
		return url;
	}
    /**
     * 上传文件至服务器
     * @param urlpath
     * @param xmldoc
     * @return
     */
    public boolean fileToServer(String urlpath, Document xmldoc) {
    	try {
    		Format format = Format.getCompactFormat();
    		format.setEncoding(Encoding); // 设置xml文件的字符为Encoding
    		format.setIndent("  "); // 设置xml文件的缩进为4个空格
    		XMLOutputter xmlout = new XMLOutputter(format);// 在元素后换行，每一层元素缩排四格
			writeLog("1----------------file to server:"+serverURL+urlpath);
    		URL interUrl = new URL(serverURL+urlpath);// 创建URL
			URLConnection con = interUrl.openConnection();// 建立连接
			con.setDoOutput(true);
            BufferedOutputStream out = new BufferedOutputStream(con.getOutputStream());
			xmlout.output(xmldoc, out);
			if(out!=null) out.close();
			writeLog("file to server:"+urlpath);
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    		return false;
    	}
    	return true;
    }

    /**
     * 测试ftp服务器配置是否正常
     * @return
     */
    public boolean testFtpServer(String ftpurl,String ftpport,String ftpuser,String ftppwd){
    	try{
    		if("".equals(ftpurl)||"".equals(ftpuser)||"".equals(ftppwd)){
    			return false;
    		}
			FtpClientUtil fcu = new FtpClientUtil(ftpurl, Util.getIntValue(ftpport,21), ftpuser, ftppwd);
			boolean success = fcu.open();
			if(success){
				fcu.close();
			}
			return success;
    	}catch(Exception e){
    		writeLog(e);
    		return false;
    	}
    }

    /**
     * 测试webservice服务器配置是否正确
     * @param wsPlatformUrl webservice地址
     * @return true:连通 false:非连通
     */
    public boolean testWebserviceServer(String wsPlatformUrl) {
        try {
            if(StringUtil.isNull(wsPlatformUrl)){
                return false;
            }else{
                URL urlObj = new URL(wsPlatformUrl+ ExchangeWebserviceConstant.WEBSERVICE_ADDR+ ExchangeWebserviceConstant.PROTOCOL_SUFFIX);
//                URL urlObj = new URL("http://localhost:8080/services/OdocExchangeWebService?wsdl");
                SslUtils.ignoreSsl();
                HttpURLConnection oc = (HttpURLConnection) urlObj.openConnection();
                oc.setUseCaches(false);//post 请求不使用缓存模式
                oc.setConnectTimeout(3000); //设置超时时间
                int status = oc.getResponseCode();//请求状态
                if(200 == status){
                    return true;
                }else{
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new BaseBean().writeLog("testWebserviceServer exception:",e);
            return false;
        }
    }

    /**
     * 取得流程信息的XML
     * @param userid
     * @param reqid
     * @param isResend
     * @return
     * @throws Exception
     */
    public Document GetXml(int userid, String reqid, boolean isResend) throws Exception {
    	LOG.info("GetXml() userid="+userid+",reqid="+reqid+",isResend="+isResend);
    	Map outidMap = new HashMap();
        //生成xml文件
        Document responseXml = new Document();
        //根
        Element root = new Element("Results");
        //msc.setAttribute("type", xmlreqheader.getMsctype());

        List requestids = new ArrayList();//待办ID
        List readyids = new ArrayList();//已办ID

        RecordSet rs = new RecordSet();
        RecordSet rs1 = new RecordSet();
        RecordSet rs2 = new RecordSet();
        String workflowid = RequestIdUtil.getWorkflowIdByRequestId(reqid);

        String sql = "";

        StringBuffer sb = new StringBuffer();

        sql = "select t1.*,t2.receivedate,t2.receivetime,t2.userid,t1.currentnodeid nodeid,t3.formid from workflow_requestbase t1, workFlow_CurrentOperator t2, workflow_base t3 ";
        sql += " where t1.requestid=t2.requestid and t1.currentnodeid=t2.nodeid ";
        sql += " and t2.userid="+userid;
        sql += " and t1.workflowid=t3.id ";
        //lz add 20181227 or t1.workflowid in(select workflowids from DocChangeWorkflow)
        sql += " and t1.requestid > 0 and t1.currentnodetype='3' and ( t1.workflowid in(select workflowid from DocChangeWorkflow) or " +
				"  ";
        if("mysql".equals(rs.getDBType())){
			sql += "  exists (select 1 from DocChangeWorkflow where  concat(',',workflowids,',') like concat('%,',"+workflowid+",',%')))";
		}else if("sqlserver".equals(rs.getDBType())){
			sql +=  " exists ( select 1 from DocChangeWorkflow where ','+workflowids+',' like '%,"+workflowid+",%'))";
		}else{
			sql += "  exists ( select 1 from DocChangeWorkflow where ','||workflowids||',' like '%,"+workflowid+",%'))";
		}
        if(!isResend) sql += " and t1.requestid not in (select requestid from DocChangeSend)";
        sql += " and t1.requestid = "+reqid;
        rs.executeSql(sql);
        LOG.info("GetXml(0) sql="+sql+" --> "+rs.getCounts());

        //data
        Element data = new Element("data");

        String eqUserid = ""+userid;//判断用户为当前的

        Element userNode = new Element("user");
        Element id = new Element("id");
        Element processids = new Element("processids");
        int i = 0;
        int count = rs.getCounts();
        StringBuffer s = new StringBuffer();
        while (rs.next()) {
        	i++;
        	//取得用户
            User user = new User();
        	user.setLanguage(rs.getInt("SYSTEMLANGUAGE"));
        	user.setUid(rs.getInt("userid"));

            //String typename=rs.getString(1);
            String receivedate=rs.getString("receivedate");
            String receivetime=rs.getString("receivetime");
            String rid = rs.getString("requestid");//请求ID
			String secLevelAndValidate = ""; //密级
            //int isBill = rs.getInt("isbill");
            LOG.info("GetXml() userid="+user.getUID()+",requestid="+rid+",receivedate="+receivedate+",receivetime="+receivetime);
            String[] rids = new String[4];
            rids[0] = rid;
            rids[1] = String.valueOf(user.getUID());
            rids[2] = receivedate;
            rids[3] = receivetime;
            requestids.add(rids);
            String requestname=rs.getString("requestname");
            int formid = rs.getInt("formid");

            WFModeNodeFieldManager wFModeNodeFieldManager=new WFModeNodeFieldManager();
            boolean isMode=wFModeNodeFieldManager.getIsModeByWorkflowIdAndNodeId(Util.getIntValue(workflowid,0), Util.getIntValue(rs.getString("nodeid"),0));
            LOG.info("GetXml() requestname="+requestname+",workflowid="+workflowid+",formid="+formid+",isMode="+isMode);

            Element requestid = new Element("requestid");
            requestid.setText(rs.getString("requestid"));

            //respheader
            Element sn = new Element("Sn");
            sn.setText(rid);

            Element Eresptime = new Element("Time");//响应时间
            String currentTime = TimeUtil.getOnlyCurrentTimeString();
            String currentDate = TimeUtil.getCurrentDateString();
            Eresptime.setText(currentDate+" "+currentTime);

            Element Fcaption = new Element("Title");

            String adname = "";
            String adfname = "";
            rs1.executeSql("select fieldId from workflow_TitleSet where flowid="+ Util.getIntValue(workflowid,0));
            if(rs1.next()) {
        		rs2.executeSql(this.requestSql(eqUserid, rid, isMode));
        		while(rs2.next()) {
        			if(rs2.getInt("fieldid")==rs1.getInt("fieldId")) {
        				adname = rs2.getString("fieldname");
        				adfname = rs2.getString("fieldlable");
        			}
        		}
            }
            LOG.info("GetXml() adname="+adname+",adfname="+adfname);
            Fcaption.setText(requestname);//+adtext工作流名称改成请求标题。
            //field
            this.formsList = new ArrayList();
            this.attachmentsList = new ArrayList();
            String[] flowStrArr = new String[5];
            flowStrArr[0] = "Fields";
            flowStrArr[1] = "Requestid";
            flowStrArr[2] = "Field";
            flowStrArr[3] = "Fieldname";
            flowStrArr[4] = "Fieldvalue";
            versionMap = new HashMap();
            this.getRequestField(user, workflowid, rid, flowStrArr, isMode, false);//非关联流程
            try {
            	LOG.info("GetXml() versionMap="+ JSONObject.fromObject(versionMap).toString());
			} catch (Exception e) {}

            //添加版本号
            Element version = new Element("Version");
            version.setText(versionMap.get(workflowid)+"");
            root.addContent(sn);//添加流水号
            root.addContent(version);//添加版本号
            root.addContent(Eresptime);//添加发送时间
            root.addContent(Fcaption);//添加公文标题

            Element elcgFlag = new Element("Flag");
    		elcgFlag.setText(workflowid);
    		root.addContent(elcgFlag);//交换标识

    		Element elcgFlagTitle = new Element("FlagTitle");
    		elcgFlagTitle.setText(new WorkflowComInfo().getWorkflowname(workflowid));
    		root.addContent(elcgFlagTitle);//标识名称
			if(HrmClassifiedProtectionBiz.isOpenClassification()){
				//获取流程密级和保密期限
				Map<String, Object> secInfo = new RequestSecLevelBiz().getSecInfoByRequestId(Util.getIntValue(reqid), user);
				String secLevel = Util.null2String(secInfo.get("secLevel"));
				String secValidity = Util.null2String(secInfo.get("secValidity"));
				if("".equals(secValidity)){
					secLevelAndValidate = secLevel;
				}else{
					secLevelAndValidate = secLevel + "#"  + secValidity;
				}
				//增加密级元素
				Element secEle = getSecLevel(secLevelAndValidate);
				formsList.add(secEle);
			}
            //form
            for(int k=0; k<formsList.size(); k++) {
            	Element form = (Element) formsList.get(k);
            	flowDetail.addContent(form);
            }
            //context
            RequestDoc requestDoc = new RequestDoc();
            boolean docFlag = requestDoc.haveDocFiled(workflowid,rs.getString("nodeid"));
            if(docFlag) {
	            ArrayList flowDoc = requestDoc.getDocFiled(rs.getString("workflowid"));
	    		if(flowDoc!=null && flowDoc.size()>0){
		    		int flowDocField= Util.getIntValue((String)flowDoc.get(1),-1);//创建文档字段
		    		int flowDocCatField= Util.getIntValue((String)flowDoc.get(3),-1);// 确定文件存放目录的字段
		    		//3、由创建文档字段、确定文件存放目录的字段获取文档id，确定文件存放目录的字段值。
		    		String flowDocFieldName = "";
		    		String flowDocCatFieldName = "";
		    		String tablename = "workflow_form";
		    		if(formid<0) {
		    			//新表单
		    			rs1.executeSql("select tablename from workflow_bill where id="+formid);
		    			if(rs1.next()) tablename = rs1.getString("tablename");
		    			rs1.executeSql("select fieldname from workflow_billfield where id="+flowDocField);
		    			if(rs1.next()) {
		    				flowDocFieldName = rs1.getString("fieldname");
		    			}
		    			rs1.executeSql("select fieldname from workflow_billfield where id="+flowDocCatField);
		    			if(rs1.next()) {
		    				flowDocCatFieldName = rs1.getString("fieldname");
		    			}
		    		} else {
		    			FieldComInfo fieldComInfo=new FieldComInfo();
		    			flowDocFieldName=fieldComInfo.getFieldname(""+flowDocField);
		    			flowDocCatFieldName=fieldComInfo.getFieldname(""+flowDocCatField);
		    		}
		    		LOG.info("GetXml() flowDocFieldName="+flowDocFieldName+",flowDocCatFieldName="+flowDocCatFieldName
		    				+",tablename="+tablename+",flowDocField="+flowDocField+",flowDocCatField="+flowDocCatField);
		    		if(flowDocCatFieldName!=null&&!flowDocCatFieldName.trim().equals("")){
		    			sql = "select "+flowDocFieldName+","+flowDocCatFieldName+" from "+tablename+" where requestId="+rid;
		    		}else{
		    			sql = "select "+flowDocFieldName+",-1 from "+tablename+" where requestId="+rid;
		    		}
		    		rs1.executeSql(sql);
		    		LOG.info("GetXml(0) sql="+sql+" --> "+rs1.getCounts());
		    		if(rs1.next()){
		    			int docId = Util.getIntValue(rs1.getString(1),-1);
		    			if(docId!=-1) {
		    				Element context = new Element("Context");
			    			Element name = new Element("Filename");
			    			Element link = new Element("FileContent");
			    			Element filemerge = new Element("FileMerge");//是否合并到正文的附件
			    			String linktext = "";
			    			String exe = "";
			    			rs2.executeSql("select imagefileid,imagefilename,docfiletype from docimagefile where docid="+docId+" and (isextfile <> '1' or isextfile is null) order by id asc,versionId desc");
							if(rs2.next()) {
								int docfiletype = rs2.getInt("docfiletype");
								if(docfiletype<=2)
									exe = this.getExe(rs2.getString("imagefilename"));
								else if(docfiletype==3)
									exe = ".doc";
								else if(docfiletype==4)
									exe = ".xls";
								else if(docfiletype==5)
									exe = ".ppt";
								else if(docfiletype==6)
									exe = ".wps";
								else if(docfiletype==7)
									exe = ".docx";
							}
							String imagefileid = rs2.getString("imagefileid");//取得文件ID

							linktext = getFileToBase64(imagefileid, true);//获取正文的BASE64码

			    			link.setText(linktext);//"/docs/docs/DocDspExt.jsp?id="+docId+"&requestid="+rid);
			    			String nametext = "";
			    			rs2.executeSql(requestSql(eqUserid, rid, isMode));
			    			while(rs2.next()) {
			    				if(rs2.getString("fieldname").equals(flowDocFieldName)) {
			    					String filename = new FieldValue().getFieldValue(user, rs2.getInt("fieldid"), rs2.getInt("fieldhtmltype"), rs2.getInt("type"), ""+docId, rs2.getInt("isBill"))+exe;
			    					filename = filename.replaceAll("/", "-").replaceAll("\\\\", "-").replaceAll(":", "-").replaceAll("\\*", "-").replaceAll("\\|", "-").replaceAll("\\?", "-");
			    					nametext = filename;
			    				}
			    			}
			    			name.setText(nametext);
			    			if(linktext.equals("") || nametext.equals("")) {
			    				//do nothing
			    			} else {
				    			context.addContent(name);
				    			context.addContent(link);
				    			root.addContent(context);
			    			}
			    			//LOG.info("GetXml() docId="+docId+",imagefileid="+imagefileid+",linktext="+linktext+",nametext="+nametext);
			    			int docImageFileId=0;
			    			int hisDocImageFileId=0;
							rs2.executeSql("select id,imagefileid,imagefilename from docimagefile where docid="+docId+" and isextfile = '1' order by id asc,versionId desc");
							while(rs2.next()) {
								docImageFileId= Util.getIntValue(rs2.getString("id"),0);
								if(docImageFileId==hisDocImageFileId){
									continue;
								}
								hisDocImageFileId=docImageFileId;

								String alinktext = this.getFileToBase64(rs2.getString("imagefileid"), false);//获取附件BASE64码
//								String anametext = "["+rs2.getString("imagefileid")+"]"+rs2.getString("imagefilename");
								String anametext = rs2.getString("imagefilename");
								//LOG.info("GetXml() docImageFileId="+docImageFileId+",linktext="+linktext+",nametext="+nametext);
								Element attachment = new Element("File");
								Element aname = new Element("Filename");
								Element afilemerge = new Element("FileMerge");//是否合并到正文的附件

								Element alink = new Element("FileContent");
								aname.setText(anametext);
								alink.setText(alinktext);
								afilemerge.setText("1");
								if(nametext.equals("") || linktext.equals("")) {
									//do nothing
								} else {
									LOG.info("======12====attachmentsList:"+anametext);
									attachment.addContent(aname);
									attachment.addContent(alink);
									attachment.addContent(afilemerge);
									attachmentsList.add(attachment);
								}
							}
		    			}
		    		}
	    		}
            }

            //attachments
            Element attachments = new Element("Files");
            for(int k=0; k<attachmentsList.size(); k++) {
            	Element attachment = (Element) attachmentsList.get(k);
				LOG.info("==========attachment:"+attachment.getChildText("Filename"));
            	attachments.addContent(attachment);
				//attachment.setContent(attachment.getChildren());
            }

            if(attachmentsList.size()>0) {
				LOG.info("==========attachmentsList:root.addContent(attachments)"+attachments.getChild("File").getChildText("Filename"));
            	root.addContent(attachments);
            }
        }
        rs.writeLog("====0====this.isChangeField:"+this.isChangeField+"===="+this.flowDetail != null);
        if(this.isChangeField && this.flowDetail != null) {
			rs.writeLog("====1====this.isChangeField:"+this.isChangeField+"===="+this.flowDetail != null);
        	root.addContent(this.flowDetail);//交换字段
        }

        Element Status = new Element("Status");
        Status.setText("0");
        root.addContent(Status);//添加交换状态

        responseXml.addContent(root);
        return responseXml;
    }

    /**
     * 取得流程字段或相关流程字段
     * @param user	当前用户
     * @param requestid	请求ID
     * @param elements	数组定义Node名称
     * @param isAss	是否相关流程
     * @throws Exception
     */
	private void getRequestField(User user, String workflowid, String requestid, String[] elements, boolean isMode, boolean isAss) throws Exception {
		List dirList = new ArrayList();//交换目录列表
		RecordSet rs0 = new RecordSet();
		RecordSet rs1 = new RecordSet();

		//System.out.println(elements[0]+"__"+elements[1]+"__"+elements[2]+"__"+elements[3]+"__"+elements[4]);
		boolean isForm = elements[0].equals("form");
		Element e = new Element(elements[0]);
		Element e1 = new Element(elements[1]);
		if(!isForm) {
			e1.setText(requestid);
			//e.addContent(e1);
		}
		else {
			e1.setText(this.formName);
			e.addContent(e1);//FROM 名称
		}
		//退回判断
		String isreject = "false";
        StringBuffer s = new StringBuffer();
        s.append("select c.isreject,d.status, a.isremark ");
        s.append("from workflow_currentoperator a,workflow_flownode b,workflow_nodebase c, workflow_requestbase d ");
        s.append("where a.nodeid=b.nodeid and a.workflowid=b.workflowid and a.isremark in('0','1','5','8','9','7') ");
        s.append("and a.nodeid=c.id and a.requestid=d.requestid ");
        s.append("and a.usertype=0 and a.requestid=" + requestid + " and a.userid=" + user.getUID() + " order by a.id desc");
        //System.out.println(s.toString());
        rs0.executeSql(s.toString());
        if(rs0.next()) {
        	if(rs0.getString("isreject").equals("1"))
        		isreject = "true";
        }

		//lz add 2019/1/7 11:23  ------ start ------
		String changeid = "";
		String changeIdSql = "select id from DocChangeWorkflow d where d.workflowid = "+workflowid;
		String dbtype = rs0.getDBType();
		if (dbtype.indexOf("sqlserver") >= 0) {
			changeIdSql += " or ','+d.workflowids+',' like '%,"+workflowid+",%'";
		}
		else if (dbtype.indexOf("mysql") >= 0) {
			changeIdSql += " or concat(',',d.workflowids,',') like '%,"+workflowid+",%'";
		}
		else
		{
			changeIdSql += " or ','||d.workflowids||',' like '%,"+workflowid+",%'";
		}
		rs0.writeLog("=========changeIdSql:"+changeIdSql);
		rs0.executeSql(changeIdSql);
		while(rs0.next())
		{
			changeid += rs0.getString("id")+",";
		}
		if(changeid.indexOf(",")>-1)
		{
			changeid = changeid.substring(0, changeid.length() - 1);
		}
		//lz add 2019/1/7 11:23  ------  end  ------
        //获取指定的收文单位
		Map fieldsMap = new HashMap();
		//lz add changeid 兼容老客户，新webservice方式
	    rs1.executeSql("SELECT * FROM DocChangeWfField WHERE (changeid in ("+changeid+") or workflowid = "+workflowid+") AND isCompany='1' AND version=(SELECT max(version) FROM DocChangeWfField WHERE changeid in ("+changeid+") or workflowid = "+workflowid+")");
	    while(rs1.next()){
	    	fieldsMap.put(rs1.getInt("fieldid")+"","X");
	    	versionMap.put(workflowid, rs1.getString("version"));
	    }

        Element reject = new Element("Reject");
        reject.setText(isreject);
        //请求状态,true正常的,false退回的
        Element ereject = new Element("Status");
        ereject.setText(rs0.getString("status"));
        Element remark = new Element("isremark");
        remark.setText(rs0.getString("isremark"));

        //e.addContent(reject);
        //e.addContent(ereject);
        //e.addContent(remark);

		rs0.executeSql(this.requestSql(String.valueOf(user.getUID()), requestid, isMode));
		String[] fields = new String[6];
		//Map fieldsMap = new HashMap();
		List fieldsMapList = new ArrayList();
		while(rs0.next()) {
			fields = new String[6];
			fields[0] = rs0.getString("fieldid");
			fields[1] = rs0.getString("fieldlable");
			fields[2] = rs0.getString("fieldname");
			fields[3] = rs0.getString("fieldhtmltype");
			fields[4] = rs0.getString("type");
			fields[5] = rs0.getString("isBill");
			//fieldsMap.put(rs0.getString("fieldname"), fields);
			fieldsMapList.add(fields);
		}
		//交换字段查询
		Map changeFiledMap = new HashMap();
		//lz add changeid 兼容老客户，新webservice方式
		rs0.executeSql("SELECT * FROM DocChangeWfField WHERE changeid in ("+changeid+") AND version=(SELECT max(version) FROM DocChangeWfField WHERE changeid in ("+changeid+") or workflowid = "+workflowid+")");
	    while(rs0.next()){
	    	LOG.info("===1==fieldid:"+ rs0.getInt("fieldid")+"===isChangeZ:"+rs0.getInt("isChange"));
	    	changeFiledMap.put(rs0.getInt("fieldid")+"",""+rs0.getInt("isChange"));
	    }
	    //end 交换字段查询
		String tablename = "workflow_form";
		rs0.executeSql("select t4.tablename from workflow_base t1, workflow_requestbase t2, workflow_form t3, workflow_bill t4 where t1.id=t2.workflowid and t2.requestid=t3.requestid and t4.id=t3.billformid and t3.billid>0 and t2.requestid="+requestid);
		if(rs0.next()) {
			tablename = rs0.getString("tablename");
		}
		rs0.executeSql(this.requestValueSql(requestid, fieldsMapList, tablename));
		if(rs0.next()) {
			int currsize = fieldsMapList.size();
			//Iterator it = fieldsMap.entrySet().iterator();
			//while(it.hasNext()) {
			for(int i=0; i<fieldsMapList.size(); i++) {
				Element field = new Element(elements[2]);
				//Map.Entry entry = (Map.Entry) it.next();
				if(!tablename.equals("workflow_form"))
					currsize--;
				else currsize = i;
				String[] strarr = (String[]) fieldsMapList.get(currsize);//(String[]) entry.getValue();
				LOG.info("===1con==fieldid:"+strarr[0]+"===isconpany:"+changeFiledMap.get(""+Integer.parseInt(strarr[0]))+"====fieldname:"+strarr[2]+"====isChangeField:"+isChangeField+"===fieldhtmltype:"+strarr[3]);
				if(isChangeField) {
					//不在交换字段中
					if(!Util.null2String(""+changeFiledMap.get(""+Integer.parseInt(strarr[0]))).equals("1")) {
						continue;
					}
				}
				//若交换字段中不含有当前字段，则不发送该字段
				if(strarr[0] != null && changeFiledMap.get(strarr[0]) == null){
					continue;
				}
				//公文交换不需要交换相关流程内容
				/*
				if(strarr[3].equals("3") && strarr[4].equals("16") && !isForm) {//相关流程
					if(!isChangeField) continue;//公文交换暂不需要普通字段
					this.formName = Util.toHtml(new FieldValue().getFieldValue(user, Integer.parseInt(strarr[0]), Integer.parseInt(strarr[3]), Integer.parseInt(strarr[4]), rs0.getString(strarr[2]), Integer.parseInt(strarr[5])));
					String[] elementsForm = new String[5];
					elementsForm[0] = "form";
					elementsForm[1] = "formname";
					elementsForm[2] = "formfield";
					elementsForm[3] = "formfieldname";
					elementsForm[4] = "formfieldvalue";
					this.getRequestField(user, workflowid, requestid, elementsForm, isMode, true);
				}
				else
				*/
				if(strarr[3].equals("6") && !isForm) {//附件处理
					LOG.info("=======field.toString():"+field.toString());
					String docids = rs0.getString(strarr[2]);
					if(!Util.null2String(docids).equals("")) {
						String linktext = "";
						String nametext = "";
		    			int docImageFileId=0;
		    			int hisDocImageFileId=0;
						rs1.executeSql("select id,imagefileid,imagefilename from docimagefile where docid in("+docids+") order by id asc,versionId desc");
						while(rs1.next()) {
							docImageFileId= Util.getIntValue(rs1.getString("id"),0);
							if(docImageFileId==hisDocImageFileId){
								continue;
							}
							hisDocImageFileId=docImageFileId;

							linktext = this.getFileToBase64(rs1.getString("imagefileid"), false);//获取附件BASE64码
							//linktext = "/weaver/weaver.file.FileDownload?userid="+user.getUID()+"&fileid="+rs1.getString("imagefileid")+"&coworkid=0&requestid="+requestid;
//							nametext = "["+rs1.getString("imagefileid")+"]"+rs1.getString("imagefilename");
							nametext = rs1.getString("imagefilename");
//                            nametext = "["+rs1.getString("imagefileid")+"]"+rs1.getString("imagefilename");

							Element attachment = new Element("File");
							Element name = new Element("Filename");
							//name.setText(Util.StringReplace(Util.null2String(new FieldValue().getFieldValue(user, Integer.parseInt(strarr[0]), Integer.parseInt(strarr[3]), Integer.parseInt(strarr[4]), rs0.getString(entry.getKey().toString()), Integer.parseInt(strarr[5]))),",",""));
							Element link = new Element("FileContent");
							Element filemerge = new Element("FileMerge");//是否合并到正文的附件
							name.setText(nametext);
							link.setText(linktext);
							filemerge.setText("0");
							if(nametext.equals("") || linktext.equals("")) {
								LOG.info("======附件no====");
								//do nothing
							}
							else {
								attachment.addContent(name);
								attachment.addContent(link);
								attachment.addContent(filemerge);
								LOG.info("======附件yes====attachmentsList:"+nametext+"==="+linktext.length());
								attachmentsList.add(attachment);
							}
						}
					}
				}
				else {
					if(!isAss && fieldsMap.get(strarr[0])!=null) {
					    //查看该字段是否在指定的收文单位中//只有主流程的接收单位才做处理
						//公文接收字段
						if(Integer.parseInt(strarr[4])==142) {
							String sendIds = Util.null2String(rs0.getString(strarr[2]));
							StringTokenizer st = new StringTokenizer(sendIds, ",");
							while (st.hasMoreTokens()) {
								String cid = st.nextToken();
								String companyType = docReceiveUnitComInfo.getCompanyType(cid);
								if(Util.null2String(companyType).equals("1")) {
									dirList.add(cid+","+docReceiveUnitComInfo.getChangeDir(cid));
								}
							}
						}
					}
					if(!isChangeField){
						continue;//公文交换暂不需要交换字段
					}
		        	Element fieldname = new Element(elements[3]);
		        	fieldname.setText(strarr[1]);
		        	Element fieldvalue = new Element(elements[4]);
		        	fieldvalue.setText(Util.toHtml(new FieldValue().getFieldValue(user, Integer.parseInt(strarr[0]), Integer.parseInt(strarr[3]), Integer.parseInt(strarr[4]), rs0.getString(strarr[2]), Integer.parseInt(strarr[5]))));
		        	Element fieldid = new Element("Fieldid");
		        	fieldid.setText(strarr[2]);//字段名称
		        	field.addContent(fieldid);
		        	field.addContent(fieldname);
		        	field.addContent(fieldvalue);
					LOG.info("=======fieldid:"+strarr[2]+"========fieldname:"+strarr[1]+"=====fieldvalue:"+Util.toHtml(new FieldValue().getFieldValue(user, Integer.parseInt(strarr[0]), Integer.parseInt(strarr[3]), Integer.parseInt(strarr[4]), rs0.getString(strarr[2]), Integer.parseInt(strarr[5]))));
					LOG.info("=======field.toString():"+field.toString());
		        	e.addContent(field);
				}
			}
		}

		LOG.info("============isForm:"+isForm);
		if(isForm) {
			this.formsList.add(e);
		}
		else {
			this.flowDetail = e;
		}
		if(dirList.size()>0) sendMap.put(requestid, dirList);//将接收单位字段放至MAP中
	}

    /**
     * 获取请求字段名称
     * @param requestid		请求ID
     * @return
     */
    private String requestSql(String userid, String requestid, boolean isMode) {
    	StringBuffer s = new StringBuffer();
    	s.append("SELECT * FROM (");
        s.append("select t5.fieldorder, t2.fieldid, t3.fieldlable, t4.fieldname,t4.fieldhtmltype,t4.type, 0 isBill from ");
        if(isMode) s.append("workflow_modeview ");
        else s.append("workflow_nodeform ");
        s.append("t2, workflow_fieldlable t3, workflow_formdict t4, workflow_formfield t5 ");
        s.append("where t2.isView='1' and t2.fieldid = t3.fieldid and t2.fieldid = t4.id ");
        if(isMode) s.append("and t2.formid=t3.formid ");
        s.append("and t5.formid=t3.formid and t5.fieldid=t2.fieldid ");
        s.append("and t2.nodeid in (select t6.nodeid from workflow_currentoperator t6 where t6.requestid="+requestid+" and t6.userid="+userid+" and t6.isremark=4  union select nodeid   from workflow_currentoperator t6 where t6.requestid="+requestid+" and agenttype=2 and agentorbyagentid="+userid+" and isremark='4') ");
        s.append("and t3.formid in (select billformid from workflow_form t1 where t1.requestid="+requestid+") ");
        s.append("UNION ALL ");
        s.append("select t2.fieldid fieldorder,t2.fieldid, (select INDEXDESC from HtmlLabelIndex where id=t4.fieldlabel) fieldname, t4.fieldname,t4.fieldhtmltype,t4.type, 1 ");
        s.append("from ");
        if(isMode) s.append("workflow_modeview ");
        else s.append("workflow_nodeform ");
        s.append("t2, workflow_billfield t4 ");
        s.append("where t2.isView='1' and t2.fieldid = t4.id ");
        s.append("and t2.nodeid in (select t6.nodeid from workflow_currentoperator t6 where t6.requestid="+requestid+" and t6.userid="+userid+" and t6.isremark=4  union select nodeid   from workflow_currentoperator t6 where t6.requestid="+requestid+" and agenttype=2 and agentorbyagentid="+userid+" and isremark='4') ");
        s.append("and t4.billid in (select billformid from workflow_form t1 where t1.requestid="+requestid+") ");
        if(cversion.equals("5")) s.append("and t4.viewtype='0' ");//只取主字段
        s.append(") A order by fieldorder");
        //if(requestid.equals("2624"))
        //System.out.println(s.toString());
    	return s.toString();
    }

    /**
     * 获取请求字段值
     * @param requestid		请求ID
     * @param fdlist		获取的请求字段名称
     * @param tablename		获取的请求字段名称
     * @return
     */
    private String requestValueSql(String requestid, List fdlist, String tablename) {
    	StringBuffer s = new StringBuffer();
    	String billfield = "";
        s.append("select ");
        for(int i=0; i<fdlist.size(); i++) {
        	String[] fds = new String[6];
        	fds = (String[]) fdlist.get(i);
        	billfield = fds[2]+billfield;
			if(tablename.equals("workflow_form")) s.append(fds[2]);
        	if(fdlist.size()>1 && i<(fdlist.size()-1)) {
        		billfield = ","+billfield;
        		if(tablename.equals("workflow_form")) s.append(",");
        	}
        }
        /*
        Iterator it = fieldsMap.entrySet().iterator();
        int j = 0;
		while(it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			billfield = entry.getKey().toString()+billfield;
			if(tablename.equals("workflow_form")) s.append(entry.getKey().toString());
        	if(fieldsMap.size()>1 && j<(fieldsMap.size()-1)) {
        		billfield = ","+billfield;
        		if(tablename.equals("workflow_form")) s.append(",");
        	}
        	j++;
        }
        */
		if(!tablename.equals("workflow_form")) s.append(billfield);
        s.append(" from "+tablename);
        s.append(" where requestid="+requestid);
        //if(requestid.equals("2623"))
        //System.out.println(s.toString());
		writeLog("======s:"+s.toString());
        return s.toString();
    }

    /**
     * 获取扩展名
     * @param str
     * @return
     */
    private String getExe(String str) {
    	str = Util.null2String(str);
    	if(str.indexOf(".")==-1) {
    		return "";
    	}
    	else {
	    	str = str.substring(str.indexOf("."), str.length());
	    	if(str.indexOf(".")>0)
	    		str = this.getExe(str);
			return str;
    	}
    }

    /**
     * 将文件转化成BASE64码形式
     * @param imagefileid
     * @param iscontext
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
	private String getFileToBase64(String imagefileid, boolean iscontext) throws FileNotFoundException, IOException {
		String str = "";
		if(imagefileid.equals("")){
			return str;
		}
		RecordSet rs = new RecordSet();
		//****************Start 正文转化成BASE64
		rs.executeSql("SELECT * FROM imagefile WHERE imagefileid="+imagefileid);
		rs.next();
		String iszip = rs.getString("iszip");
		String filerealpath = rs.getString("filerealpath");
		 String isaesencrypt = rs.getString("isaesencrypt");
        String aescode = rs.getString("aescode");

		int byteread;
		byte bydata[] = new byte[1024];
		InputStream imagefile = null;
		if (filerealpath.equals("")) {         // 旧的文件放在数据库中的方式
			//do nothing
		} else {
			ImageFileManager imageFileManager=new ImageFileManager();
			imageFileManager.getImageFileInfoById(Util.getIntValue(imagefileid));
			imagefile=imageFileManager.getInputStream();

			//正文的处理
			ByteArrayOutputStream bout = null;
			try {
				bout = new ByteArrayOutputStream() ;
		        while((byteread = imagefile.read(bydata)) != -1) {
		            bout.write(bydata, 0, byteread) ;
		            bout.flush() ;
		        }
		        BASE64Encoder encoder = new BASE64Encoder();
		        if(iscontext) {
			        byte[] fileBody = bout.toByteArray();
			        iMsgServer2000 MsgObj = new iMsgServer2000();
					MsgObj.MsgFileBody(fileBody);			//将文件信息打包
					fileBody = MsgObj.ToDocument(MsgObj.MsgFileBody());    //通过iMsgServer200 将pgf文件流转化为普通Office文件流
					//BASE64加密
					str = encoder.encode(fileBody);
		        }
		        else {
		        	//BASE64加密
					str = encoder.encode(bout.toByteArray());
		        }
		        if(bout!=null) bout.close();
		        str = str.replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
			}
			catch(Exception e) {
				if(bout!=null) bout.close();
			}
		}
		if(imagefile!=null) imagefile.close();
		//****************End 正文转化成BASE64
		return str;
	}

	/**
	 * 公文交换字段设置
	 * @param req	请求
	 * @param userid	用户ID
	 * @return
	 */
	public boolean saveChangeFields(HttpServletRequest req, int userid) {
		int version = 1;
		RecordSet rs = new RecordSet();
		String sql = "";

		Map showMap = new HashMap();
		int workflowid = Util.getIntValue(req.getParameter("wfid"),-1);

		rs.executeSql("SELECT max(version) FROM DocChangeWfField WHERE workflowid="+workflowid);
		if(rs.next()) {
			version = Util.getIntValue(rs.getString(1),0)+1;
		}

		String[] fieldid_node = req.getParameterValues("fieldid_node");
		if(fieldid_node!=null) {
			for(int i=0; i<fieldid_node.length; i++) {
				showMap.put(fieldid_node[i], "X");
				sql = "INSERT INTO DocChangeWfField(workflowid,version,fieldid,isChange,isCompany,creator) VALUES";
				sql += "('"+workflowid+"',"+version+",'"+fieldid_node[i]+"','1','0',"+userid+")";
				rs.executeSql(sql);
			}
		}
		String[] fieldid_change = req.getParameterValues("fieldid_change");
		if(fieldid_change!=null) {
			for(int i=0; i<fieldid_change.length; i++) {
				if(showMap.get(fieldid_change[i])==null) {
					sql = "INSERT INTO DocChangeWfField(workflowid,version,fieldid,isChange,isCompany,creator) VALUES";
					sql += "('"+workflowid+"',"+version+",'"+fieldid_change[i]+"','0','1',"+userid+")";
					rs.executeSql(sql);
				}
				else {
					sql = "UPDATE DocChangeWfField set isCompany='1' WHERE workflowid='"+workflowid+"' AND fieldid='"+fieldid_change[i]+"' AND version='"+version+"'";
					rs.executeSql(sql);
				}
			}
		}
		return true;
	}

	/**
     * 得到最新交换的ID+1
     * @throws Exception
     */
    public synchronized static int getNextChangeId() throws Exception {
    	RecordSet rs = new RecordSet();
        int docindex = -1;
        rs.executeProc("SequenceIndex_SelectNextID", "docchangeid");
        if (rs.next())
            docindex = rs.getInt(1);
        return docindex;
    }

    public List getPopedom(String id,String params){
    	List list = new ArrayList();
    	list.add("true");
    	String sn = params.split("\\+")[0];
    	String version = params.split("\\+")[1];
    	RecordSet rs = new RecordSet();
    	rs.executeSql("select * from DocChangeFieldConfig where sn = '"+sn+"' and version = " + version);
    	if(rs.next()){
    		list.add("false");
    		list.add("true");
    	}else{
    		list.add("true");
    		list.add("false");
    	}
    	return list;
    }

    private static boolean updateInnerDataToSended(List<String> innerList,User user){
        boolean result = false;
        if(null!=innerList&&innerList.size()>0){
            List<List<Object>> paramLists = new ArrayList();
            List<List<Object>> paramReceiveLists = new ArrayList();
            List<List<Object>> paramList4SendMsgs = new ArrayList();
            List<List<Object>> paramList4ReceiveMsgs = new ArrayList();
            List param = new ArrayList();
            String requestids = "";
            String currentDate = TimeUtil.getCurrentDateString();
            String currentTime = TimeUtil.getOnlyCurrentTimeString();
            RequestIdUtil requestIdUtil = new RequestIdUtil();
            String documentIdentifier = "";
            RecordSet recordSet = new RecordSet();
            for (String requestid:innerList) {
                String tmpSql = "";
                if(StringUtil.isNotNull(requestid)){
                    if(requestid.contains("_")){
                        String[] tmpArr = requestid.split("_");
                        if(null==tmpArr)continue;
                        if(StringUtil.isNull(tmpArr[0])) continue;
                        if(tmpArr.length>1){
                            if(StringUtil.isNotNull(tmpArr[1])){
                                recordSet.executeQuery( "select * from odoc_requestdoc_inner where requestid=? and subWorkflowSettingId=?",tmpArr[0],tmpArr[1]);
                            }else{
                                recordSet.executeQuery( "select * from odoc_requestdoc_inner where requestid=?",tmpArr[0]);
                            }
                        }else{
                            recordSet.executeQuery( "select * from odoc_requestdoc_inner where requestid=?",tmpArr[0]);
                        }
                    }else{
                        recordSet.executeQuery( "select * from odoc_requestdoc_inner where requestid=?",requestid);
                    }
                    if(recordSet.next()){
                        List paramList = new ArrayList();
                        List paramReceiveList = new ArrayList();
                        paramList.add(Util.null2String(recordSet.getString("requestid")));
                        paramList.add(Util.null2String(recordSet.getString("requestname")));
                        paramList.add(Util.null2String(recordSet.getString("document_identifier")));
                        paramList.add(Util.null2String(recordSet.getString("sendunit")));
                        paramList.add(currentDate);
                        paramList.add(currentTime);
                        paramList.add(Util.null2String(recordSet.getString("issued_num")));
                        paramList.add(user.getUID());
                        paramList.add(recordSet.getString("subworkflowsettingid"));
                        paramLists.add(paramList);
                        paramReceiveList.add(Util.null2String(recordSet.getString("document_identifier")));
                        paramReceiveList.add(Util.null2String(recordSet.getString("requestname")));
                        paramReceiveList.add(Util.null2String(recordSet.getString("sendunit")));
                        paramReceiveList.add("0");//待签收
                        paramReceiveList.add(Util.null2String(recordSet.getString("issued_num")));
                        String docid = requestIdUtil.getDocumentTextIdByRequestId(Util.null2String(recordSet.getString("requestid")),new User(1));
                        if(StringUtil.isNotNull(docid)){
                            paramReceiveList.add(docid);
                        }else{
                            paramReceiveList.add("-1");
                        }
                        paramReceiveList.add("");
                        paramReceiveList.add(currentDate);
                        paramReceiveList.add(currentTime);
                        paramReceiveLists.add(paramReceiveList);
                        String receiveunit = Util.null2String(recordSet.getString("receiveunit"));
                        String[] receiveunitArr = receiveunit.split(",");
                        for (String s:receiveunitArr) {
                            List paramList4SendMsg = new ArrayList();
                            List paramList4ReceiveMsg = new ArrayList();
                            paramList4SendMsg.add(Util.null2String(recordSet.getString("document_identifier")));
                            paramList4SendMsg.add(s);
                            paramList4SendMsgs.add(paramList4SendMsg);
                            paramList4ReceiveMsg.add(user.getUID());
                            paramList4ReceiveMsg.add(currentDate);
                            paramList4ReceiveMsg.add(currentTime);
                            paramList4ReceiveMsg.add(Util.null2String(recordSet.getString("document_identifier")));
                            paramList4ReceiveMsg.add(s);
                            paramList4ReceiveMsg.add(recordSet.getString("subworkflowsettingid"));
                            paramList4ReceiveMsgs.add(paramList4ReceiveMsg);
                        }
                        documentIdentifier = documentIdentifier + Util.null2String(recordSet.getString("document_identifier"))+",";
                    }
                }
            }
            RecordSetTrans recordSetTrans = new RecordSetTrans();
            try {
                recordSetTrans.setAutoCommit(false);
                if(paramLists.size()>0){
                    recordSetTrans.executeBatchSql(" insert into exchange_sendDocInfo_inner (request_id,document_title,document_identifier,send_company_id, create_date, create_time, issued_number_of_document,send_status,userid,subworkflowsettingid) " +
                            " values (?,?,?,?,?,?,?,'0',?,?) ",paramLists);
                }
                if(paramList4SendMsgs.size()>0){
                    String sql4SendMsg = "insert into exchange_receiveunitlist_inner (document_identifier, receive_company_id, doc_status,type) " +
                            " values (?, ?, '0', '0')";
                    recordSetTrans.executeBatchSql(sql4SendMsg,paramList4SendMsgs);
                }
                if(paramReceiveLists.size()>0){
                    String sql4Receive="insert into exchange_receive_doc_inner (document_identifier, document_title, sending_department, receive_status, issued_number_of_document, document_text,json, receive_date, receive_time) " +
                            " values (?,?,?,?,?,?,?,?,?)";
                    recordSetTrans.executeBatchSql(sql4Receive,paramReceiveLists);
                }
                if(paramList4ReceiveMsgs.size()>0){
                    String sql4ReceiveMsg = "insert into exchange_receivestatus_inner (operator, operate_date, operate_time, operate_status, document_identifier, receiver_department,subworkflowsettingid) " +
                            " values (?,?,?,'0',?,?,?)";
                    recordSetTrans.executeBatchSql(sql4ReceiveMsg,paramList4ReceiveMsgs);
                    if(StringUtil.isNotNull(documentIdentifier)){
                        documentIdentifier = documentIdentifier.substring(0,documentIdentifier.length()-1);
                        List param1 = new ArrayList();
                        Object[] objects1 = DBUtil.transListIn(documentIdentifier,param1);
                        String sql = "select * from exchange_receive_doc_inner where document_identifier in ("+objects1[0]+")";
                        recordSetTrans.executeQuery(sql,param1);
                        List<List<Object>> lists = new ArrayList<>();
                        while(recordSetTrans.next()){
                            List<Object> list = new ArrayList<>();
                            String receiveDocInfoOaId = recordSetTrans.getString("id");
                            String tmpDocumentIdentifier = recordSetTrans.getString("document_identifier");
                            list.add(receiveDocInfoOaId);
                            list.add(tmpDocumentIdentifier);
                            lists.add(list);
                        }
                        if(lists.size()>0){
                            recordSetTrans.executeBatchSql("update exchange_receivestatus_inner set receive_doc_info_oa_id=? where document_identifier=?",lists);
                        }
                    }
                }
                recordSetTrans.commit();
                recordSetTrans.setAutoCommit(true);
            } catch (Exception e) {
                recordSet.writeLog("exception:",e);
                recordSetTrans.setAutoCommit(true);
                recordSetTrans.rollback();
            }
        }
        return result;
    }


    public static String SendDocByWebservice(User user, String requestids, String receiverDepartment) {
        return SendDocByWebservice(user,requestids,receiverDepartment,"0");
    }






	/**
	 * webservice方式发送公文
	 * @param user
	 * @param requestids
	 * @param receiverDepartment
     * @return
	 */
	public static String SendDocByWebservice(User user, String requestids, String receiverDepartment,String isInnerChange) {
		String rtResult = "-1";
        if(StringUtil.isNull(requestids)){
            LOG.info("SendDocByWebservice 参数ids为空");
        }else{
            try {
                String[] requestidArr = requestids.split(",");
                List<String> requestidList = new ArrayList();
                for (String s:requestidArr) {
                    if(StringUtil.isNotNull(s)&&!requestidList.contains(s)){
                        requestidList.add(s);
                    }
                }
                if(null!=requestidList&&requestidList.size()>0){
                    if(StringUtil.isNotNull(isInnerChange)&&"1".equals(isInnerChange)){
                        updateInnerDataToSended(requestidList,user);
                        rtResult = "0";
                    }else{
                        String sessionCode = null;
                        try {
                            sessionCode = ExchangeWebserviceUtil.getSessionKey();
                        } catch (Exception e) {
                            LOG.info("通过Webservice获取sessionCode异常，exception：",e);
                        }
                        if(StringUtil.isNull(sessionCode)){
                            LOG.info("通过Webservice获取sessionCode为空或为null");
                            return rtResult + "_"+SystemEnv.getHtmlLabelName(	517452,user.getLanguage());
                        }
                        Object [] paraObjArr = new Object[2];
                        paraObjArr[1] =  sessionCode;
                        int sendCountNum = requestidList.size();
                        int sendSuccessCountNum = 0;
                        for(String requestid:requestidList){
                            RecordSetTrans rst = new RecordSetTrans();
                            try{
                                Map<String, Property> requestFormData = RequestIdUtil.getFormValueByRequestId(requestid,user);
                                ArrayList<ExchangeField> exchangeFieldList = getExchangeFieldWithValueList(requestFormData,requestid,user);
                                if(exchangeFieldList.size()>0){
                                    String sendData = "";
                                    if(StringUtil.isNull(receiverDepartment)){
                                        sendData = ExchangeClientXmlUtil.createSendDataXml(exchangeFieldList,"",user);
                                    }else{
                                        sendData = ExchangeClientXmlUtil.createSendDataXml(exchangeFieldList,receiverDepartment,user);//发送指定单位
                                    }
                                    if(StringUtil.isNotNull(sendData)){
                                        if(sendData.startsWith("-1")){//发文单位为空直接返回
                                            return sendData;
                                        }
                                    }
//                                    LOG.info("发送公文数据为："+sendData);
                                    ExchangeSendDocInfoOa exchangeSendDocInfoOa = getExchangeSendDocInfoOa(exchangeFieldList);
                                    String[] receiveIds = getReceiveIds(exchangeFieldList);
                                    if(!(receiveIds.length>0&&StringUtil.isNotNull(receiveIds[0]))){
                                        LOG.info("接收单位不允许为空！");
                                        return rtResult + "_"+SystemEnv.getHtmlLabelName(	502943,user.getLanguage());
                                    }
                                    exchangeSendDocInfoOa.setRequest_Id(Integer.valueOf(requestid));
                                    String documentIdentifier = DocIdentifierGenerateUtil.generateIdentifier();
                                    String getDocumentIdentifierByRequestIdSql = "select b.document_identifier from exchange_sendDocInfo_oa b where b.request_id=?";
                                    boolean isGetDocumentIdentifierByRequestIdSql = rst.executeQuery(getDocumentIdentifierByRequestIdSql,requestid);
                                    if(isGetDocumentIdentifierByRequestIdSql&&rst.next()){
                                        documentIdentifier = rst.getString("document_identifier");
                                    }
                                    exchangeSendDocInfoOa.setCreate_Date(TimeUtil.getCurrentDateString());
                                    exchangeSendDocInfoOa.setCreate_Time(TimeUtil.getOnlyCurrentTimeString());
                                    exchangeSendDocInfoOa.setDocument_Identifier(documentIdentifier);
                                    exchangeSendDocInfoOa.setDocument_Title(ExchangeCommonMethodUtil.getRequestNameByRequestId(requestid));
                                    paraObjArr[0] = sendData;
                                    rst.setAutoCommit(false);
                                    String sendDataResultContent = ExchangeWebserviceUtil.getCallWebserviceMethodResult("sendData",paraObjArr);
                                    String sqlDetail = "";
                                    String sql = "";
                                    if(!ExchangeWebserviceUtil.isEmptyStr(sendDataResultContent)){
                                        if(ExchangeClientXmlUtil.getResultContent(sendDataResultContent, ExchangeWebserviceConstant.WEBSERVICE_RESULT_NODE).equals(ExchangeWebserviceConstant.WEBSERVICE_RESULT_SUCCESS)){
                                            if(StringUtil.isNull(receiverDepartment)){
                                                sql = "insert into exchange_sendDocInfo_oa (DOCUMENT_IDENTIFIER, DOCUMENT_TITLE, SEND_COMPANY_ID, REQUEST_ID, DOCUMENT_TYPE, CREATE_DATE, CREATE_TIME, ISSUED_NUMBER_OF_DOCUMENT)" +
                                                        "values (?, ?, ?, ?, ?, ?, ?, ?)";
                                                boolean isInsertSuccess = rst.executeUpdate(sql,documentIdentifier,exchangeSendDocInfoOa.getDocument_Title(),exchangeSendDocInfoOa.getSending_Department(),
                                                        exchangeSendDocInfoOa.getRequest_Id(),exchangeSendDocInfoOa.getDocument_Type(),exchangeSendDocInfoOa.getCreate_Date(),exchangeSendDocInfoOa.getCreate_Time(),exchangeSendDocInfoOa.getIssued_Number_of_Document());
                                                if(isInsertSuccess){
                                                    String isInsertReceiveUnitSql = "insert into exchange_receiveUnitList_oa (DOCUMENT_IDENTIFIER, RECEIVE_COMPANY_ID, DOC_STATUS) values (?, ?, ?)";
                                                    //插入明细数据
                                                    for (String receiveId:receiveIds) {
                                                        rst.executeUpdate(isInsertReceiveUnitSql,documentIdentifier,receiveId,0);
                                                    }
                                                }
                                                sql = "update exchange_sendDocInfo_oa set send_status = 0,Document_Identifier = ? where Document_Identifier = ?";
                                                sqlDetail = "update exchange_receiveUnitList_oa set doc_status = 0,Document_Identifier = ?  where Document_Identifier = ?";
                                                rst.executeUpdate(sqlDetail,ExchangeClientXmlUtil.getResultContent(sendDataResultContent, ExchangeWebserviceConstant.WEBSERVICE_DOCUMENT_IDENTIFIERS_NODE),documentIdentifier);
                                                rst.executeUpdate(sql,ExchangeClientXmlUtil.getResultContent(sendDataResultContent, ExchangeWebserviceConstant.WEBSERVICE_DOCUMENT_IDENTIFIERS_NODE),documentIdentifier);
                                            }else{
                                                sqlDetail = "update exchange_receiveUnitList_oa set doc_status = 0,response_msg='' where  document_identifier=? and receive_company_id=?";
                                                rst.executeUpdate(sqlDetail,documentIdentifier,receiverDepartment);
                                            }
                                            sendSuccessCountNum = sendSuccessCountNum + 1;
                                            rtResult = "0";
                                        }else{
                                            if(sendCountNum>1){
                                                continue;
                                            }else {
                                                String errorMsg = ExchangeClientXmlUtil.getResultContent(sendDataResultContent, ExchangeWebserviceConstant.WEBSERVICE_RESPONSE_MSG_NODE);
                                                LOG.info("发送失败，错误信息为："+errorMsg);
                                                rtResult += "_"+errorMsg;
                                                return rtResult;
                                            }
                                        }
                                    }else{
                                        LOG.info("交换平台反馈信息为null或为空");
                                    }
                                }else {
                                    LOG.info("获取标准字段失败，请检查webservice系统设置是否正确！");
                                }
                                rst.commit();
                                rst.setAutoCommit(true);
                            }catch (Exception e){
                                LOG.info("SendDocByWebservice11 ",e);
                                rst.rollback();
                                rst.setAutoCommit(true);
                            }
                        }
                        if(sendCountNum==sendSuccessCountNum){
                            rtResult = "0";
                        }else{
                            return "0_"+sendSuccessCountNum+SystemEnv.getHtmlLabelName(521943,user.getLanguage())+","+(sendCountNum-sendSuccessCountNum)+SystemEnv.getHtmlLabelName(521944,user.getLanguage());
                        }
                    }
                }
            }catch (Exception e){
                LOG.info("SendDocByWebservice ",e);
            }
        }
		return rtResult;
	}

    /**
     * 获取发文实体类
     * @param exchangeFieldList
     * @return
     */
    private static ExchangeSendDocInfoOa getExchangeSendDocInfoOa(ArrayList<ExchangeField> exchangeFieldList) {
        ExchangeSendDocInfoOa exchangeSendDocInfoOa = new ExchangeSendDocInfoOa();
        if(null!=exchangeFieldList&&exchangeFieldList.size()>0){
            for (ExchangeField exchangeField:exchangeFieldList) {
                if(null!=exchangeField&&null!=exchangeField.getField_type()){
                    if(exchangeField.getField_type().equals(ExchangeFieldType.DOCUMENT_TITLE)){
                        if(StringUtil.isNotNull(exchangeField.getXmlValue())){
                            exchangeSendDocInfoOa.setDocument_Title(exchangeField.getXmlValue());
                        }else{
                            exchangeSendDocInfoOa.setDocument_Title("");
                        }
                    }else if(exchangeField.getField_type().equals(ExchangeFieldType.SENDING_DEPARTMENT)){
                        if(StringUtil.isNotNull(exchangeField.getXmlValue())){

                            try {
                                exchangeSendDocInfoOa.setSending_Department(Integer.valueOf(exchangeField.getXmlValue()));
                            } catch (NumberFormatException e) {
                                exchangeSendDocInfoOa.setSending_Department(-1);
                                new BaseBean().writeLog("set sendDepartment exception:",e);
                            }
                        }else{
                            exchangeSendDocInfoOa.setSending_Department(ExchangeWebserviceConstant.NOT_RIGHT_ID);
                        }
                    }else if(exchangeField.getField_type().equals(ExchangeFieldType.ISSUED_NUMBER_OF_DOCUMENT)){
                        if(StringUtil.isNotNull(exchangeField.getXmlValue())){
                            exchangeSendDocInfoOa.setIssued_Number_of_Document(exchangeField.getXmlValue());
                        }else{
                            exchangeSendDocInfoOa.setIssued_Number_of_Document("");
                        }
                    }else if(exchangeField.getField_type().equals(ExchangeFieldType.DOCUMENT_TYPR)){
                        if(StringUtil.isNotNull(exchangeField.getXmlValue())){
                            exchangeSendDocInfoOa.setDocument_Type(exchangeField.getXmlValue());
                        }else{
                            exchangeSendDocInfoOa.setDocument_Type("");
                        }
                    }
                }else{
                    LOG.info("DocChangeManager.getExchangeSendDocInfoOa exchangeField为null或者exchangeField.getField_type()为null");
                }
            }
        }else{
            LOG.info("DocChangeManager.getExchangeSendDocInfoOa 参数：exchangeFieldList为null或者集合为空");
            return exchangeSendDocInfoOa;
        }
        return exchangeSendDocInfoOa;
    }

    /**
     * 获取receiveIds
     * @param exchangeFieldList
     * @return
     */
    private static String[] getReceiveIds(ArrayList<ExchangeField> exchangeFieldList) {
        String mainReceiverDepartmentValue = "";
        String copyToDepartmentValue = "";
        for (ExchangeField exchangeField:exchangeFieldList) {
            if(exchangeField.getExchange_xml_name().toUpperCase().equals(ExchangeWebserviceConstant.DOCUMENT_NODE_MAIN_RECEIVER_DEPARTMENT.toUpperCase())){
                mainReceiverDepartmentValue = exchangeField.getXmlValue();
            }
            if(exchangeField.getExchange_xml_name().toUpperCase().equals(ExchangeWebserviceConstant.DOCUMENT_NODE_COPY_TO_DEPARTMENT.toUpperCase())){
                copyToDepartmentValue = exchangeField.getXmlValue();
            }
        }
        String receivedIds = "";
        if(StringUtil.isNotNull(mainReceiverDepartmentValue)){
            if(StringUtil.isNotNull(copyToDepartmentValue)){
                receivedIds = mainReceiverDepartmentValue+","+copyToDepartmentValue;
            }else{
                receivedIds = mainReceiverDepartmentValue;
            }
        }else{
            LOG.info("接收单位为空");
//            throw new RuntimeException("接收单位为空");
        }
        receivedIds = removeRepeat(receivedIds);
        return receivedIds.split(",");
    }

    /**
     * 字符串去重
     * @param str
     * @return
     */
    private static String removeRepeat(String str) {
        String result = "";
        if(StringUtil.isNotNull(str)){
            String[] arr= str.split(",");
            Set<String> set= new HashSet<>(Arrays.asList(arr));
            Object[] tmpArr = set.toArray();
            String reStr = "";
            for(Object s:tmpArr){
                reStr = reStr+s+",";
            }
            if(reStr.endsWith(",")){
                reStr = reStr.substring(0,reStr.length()-1);
            }
            result = reStr;
        }
        return result;
    }

	/**
	 * 通过请求ID生成xml报文
	 * @param requestFormData
     * @param requestId
	 * @return
	 */
	private static ArrayList<ExchangeField> getExchangeFieldWithValueList(Map<String, Property> requestFormData, String requestId,User user) {
        //获取该单位在平台配置的与标准字段对应的节点信息
        ArrayList<ExchangeField> exchangeFieldList = ExchangeWebserviceUtil.getFieldSetFromOdocExchange();
        //获取发文流程中表单字段与交换字段的对应关系集合
        ArrayList<DocChangeWfField> docChangeWfFieldList = getDocChangeWfFieldList(requestId);
        //获取表单字段名称与编号的map
        Map<String,String> fieldIdAndFieldNameMap = getFieldNameListByRequesetId(requestId);
        if(null!=docChangeWfFieldList&&docChangeWfFieldList.size()>0&&null!=exchangeFieldList&&exchangeFieldList.size()>0){
            for (DocChangeWfField docChangeWfField:docChangeWfFieldList) {
                for(int i=0;i<exchangeFieldList.size();i++){
                    if(null==exchangeFieldList.get(i)||null==exchangeFieldList.get(i).getId()||null==docChangeWfField){
                        continue;
                    }else{
                        if(exchangeFieldList.get(i).getId().equals(docChangeWfField.getExchangefieldid())){
                            if(docChangeWfField.getFieldid()==ExchangeWebserviceConstant.WORKFLOW_REQUEST_NAME_ID){
                                exchangeFieldList.get(i).setXmlValue(RequestIdUtil.getWorkflowRequsetNameByRequestId(requestId));
                            }else if(docChangeWfField.getFieldid()==ExchangeWebserviceConstant.WORKFLOW_REQUEST_SECRET_LEVEL_ID){
								if(HrmClassifiedProtectionBiz.isOpenClassification()){
									//获取流程密级和保密期限
									String secLevelAndValidate = "";
									Map<String, Object> secInfo = new RequestSecLevelBiz().getSecInfoByRequestId(Util.getIntValue(requestId), user);
									String secLevel = Util.null2String(secInfo.get("secLevel"));
									String secValidity = Util.null2String(secInfo.get("secValidity"));
									if("".equals(secValidity)){
										secLevelAndValidate = secLevel;
									}else{
										secLevelAndValidate = secLevel + "#"  + secValidity;
									}
									exchangeFieldList.get(i).setXmlValue(secLevelAndValidate);
								}
                            }else{
                                if(null==fieldIdAndFieldNameMap.get(docChangeWfField.getFieldid()+"")||null==requestFormData){
                                    continue;
                                }else{
                                    if(null==requestFormData.get(fieldIdAndFieldNameMap.get(docChangeWfField.getFieldid()+""))){
                                        continue;
                                    }else{
                                        exchangeFieldList.get(i).setXmlValue(requestFormData.get(fieldIdAndFieldNameMap.get(docChangeWfField.getFieldid()+"")).getValue());
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
        //设置公文标识
        RecordSet rs = new RecordSet();
        String getDocumentIdentifierByRequestIdSql = "select b.document_identifier from exchange_sendDocInfo_oa b where b.request_id=?";
        boolean isGetDocumentIdentifierByRequestIdSql = rs.executeQuery(getDocumentIdentifierByRequestIdSql,requestId);
        String documentIdentifier = "";
        if(isGetDocumentIdentifierByRequestIdSql&&rs.next()){
            documentIdentifier = rs.getString("document_identifier");
        }
        for (ExchangeField exchangeField:exchangeFieldList) {
            if(null!=exchangeField){
                if(null!=exchangeField.getExchange_xml_name()&&exchangeField.getExchange_xml_name().toUpperCase().equals(ExchangeWebserviceConstant.DOCUMENT_NODE_DOCUMENT_IDENTIFIER.toUpperCase())){
                    exchangeField.setXmlValue(documentIdentifier);
                }
                /**过滤单位编码为空的单位*/
                if(null!=exchangeField.getField_type()&&exchangeField.getField_type()==ExchangeFieldType.MAIN_RECEIVER_DEPARTMENT){
                    exchangeField = filterEmptyUnitCode(exchangeField,exchangeField.getXmlValue());
                }else if(null!=exchangeField.getField_type()&&exchangeField.getField_type()==ExchangeFieldType.COPY_TO_DEPARTMENT){
                    exchangeField = filterEmptyUnitCode(exchangeField,exchangeField.getXmlValue());
                }
            }
        }

        return exchangeFieldList;
	}


    /**
     * 过滤单位编码为空的单位
     * @param exchangeField  交换字段实体
     * @param unitCodes      单位编码集合 id1,id2,id3
     * @return 更新属性xmlValue后的交换字段实体
     */
	private static ExchangeField filterEmptyUnitCode(ExchangeField exchangeField,String unitCodes){
	    if(null==exchangeField||StringUtil.isNull(unitCodes)){
	        LOG.info("exchangeField为null或者unitCodes="+unitCodes);
	        return exchangeField;
        }
        String rtUnitCodes = "";
        String[] arrMainReceiverDepartment = unitCodes.split(",");
        for (String unitId:arrMainReceiverDepartment ) {
            if(StringUtil.isNotNull(ExchangeCommonMethodUtil.getUnitCodesByUnitId(unitId))){
                rtUnitCodes = rtUnitCodes + unitId + ",";
            }else{
                LOG.info("DocChangeManager.filterEmptyUnitCode.unitId="+unitId+"获取的系统编码为空或为null！");
            }
        }
        if(StringUtil.isNotNull(rtUnitCodes)&&rtUnitCodes.endsWith(",")){
            exchangeField.setXmlValue(rtUnitCodes.substring(0,rtUnitCodes.length()-1));
        }else{
            LOG.info("DocChangeManager.filterEmptyUnitCode.rtUnitCodes="+rtUnitCodes+"返回的系统编码为空或为null");
        }
        return exchangeField;
    }
    /**
     * 获取表单字段的名称
     * @param requestid
     * @returnqueryBillType
     */
    private static Map<String,String> getFieldNameListByRequesetId(String requestid) {
        Map<String,String> fieldIdAndFieldNameMap = new HashMap<>();
        if(StringUtil.isNull(requestid)){
            LOG.info("DocChangeManager.getFieldNameListByRequesetId 参数：requestid为空或者为null");
            return fieldIdAndFieldNameMap;
        }
        //判断表单类型 1：新表单 非1：老表单
        RecordSet rs = new RecordSet();
        boolean isNewBill = false;
        String queryBillType = "select isbill from workflow_base where id = (select workflowid from workflow_requestbase where requestid=?)";
        boolean isQueryBillTypeSuccess = rs.executeQuery(queryBillType,requestid);
        if(isQueryBillTypeSuccess){
            while(rs.next()){
                if(rs.getInt("isbill")==1){
                    isNewBill = true;
                }
            }
        }else{
            LOG.info("DocChangeManager.getFieldNameListByRequesetId queryBillType="+queryBillType+"参数：requestid="+requestid+"查询失败！");
            return fieldIdAndFieldNameMap;
        }
        if(isNewBill){
            String newBillSql = "select d.id as fieldid,d.fieldname from workflow_billfield d where d.billid =(select c.formid from workflow_base c where c.id = (select t.workflowid from workflow_requestbase t where t.requestid=?))";
            rs.executeQuery(newBillSql,requestid);
        }else{
            String oldBillSql = "select f.fieldid,g.fieldname from workflow_formfield f left join workflow_formdict g on f.fieldid=g.id where f.formid = (select e.formid from workflow_base e where e.id =(select t.workflowid from workflow_requestbase t where t.requestid=?))";
            rs.executeQuery(oldBillSql,requestid);
        }
        while(rs.next()){
            fieldIdAndFieldNameMap.put(rs.getString("fieldid"),rs.getString("fieldname"));
        }
        fieldIdAndFieldNameMap.put("-3",RequestIdUtil.getWorkflowRequsetNameByRequestId(requestid));
        return  fieldIdAndFieldNameMap;
    }

    /**
     * 获取发文流程所对应的字段
     */
    public static ArrayList<DocChangeWfField> getDocChangeWfFieldList(String requestId){
        ArrayList<DocChangeWfField> docChangeWfFieldList = new ArrayList<>();
        if(StringUtil.isNull(requestId)){
            LOG.info("DocChangeManager.getDocChangeWfFieldList 参数requestId为空或为null");
            return docChangeWfFieldList;
        }
        RecordSet rs = new RecordSet();
        String workflowId = "";
        String queryWorkflowId = "select d1.workflowid from workflow_requestbase d1 where d1.requestid=?";
        boolean isQueryWorkflowId = rs.executeQuery(queryWorkflowId,requestId);
        if(isQueryWorkflowId&&rs.next()){
            workflowId = rs.getString("workflowid");
        }
        if(StringUtil.isNull(workflowId)){
            LOG.info("DocChangeManager.getDocChangeWfFieldList sql="+queryWorkflowId+"参数requestId="+requestId+"获取的workflowId为空或为null");
            return docChangeWfFieldList;
        }
        String sqlForDocChangeWfChangeId = "";
        String dbType = rs.getDBType();
        if(dbType.equals(ExchangeWebserviceConstant.DB_TYPE_ORACLE)){
            sqlForDocChangeWfChangeId = "select a.id from DocChangeWorkflow a where a.exchangetype='1' and (a.workflowid =? or ',' || a.workflowids || ',' like '%,' || ? || ',%')";
        }else if(dbType.equals(ExchangeWebserviceConstant.DB_TYPE_MYSQL)){
            sqlForDocChangeWfChangeId = "select a.id from DocChangeWorkflow a where a.exchangetype='1' and (a.workflowid =? or CONCAT(',',a.workflowids,',') like CONCAT('%,',?,',%'))";
        }else if(dbType.equals(ExchangeWebserviceConstant.DB_TYPE_SQLSERVER)){
            sqlForDocChangeWfChangeId = "select a.id from DocChangeWorkflow a where a.exchangetype='1' and (a.workflowid =? or ',' + a.workflowids + ',' LIKE '%,' + ? + ',%')";
        }else{//达梦
            sqlForDocChangeWfChangeId = "select a.id from DocChangeWorkflow a where a.exchangetype='1' and (a.workflowid =? or ',' || a.workflowids || ',' like '%,' || ? || ',%')";
        }
        String changeId = "";
        boolean isSqlForDocChangeWfChangeId = rs.executeQuery(sqlForDocChangeWfChangeId,workflowId,workflowId);
        if(!isSqlForDocChangeWfChangeId){
            LOG.info("DocChangeManager.getDocChangeWfFieldList sqlForDocChangeWfChangeId="+sqlForDocChangeWfChangeId+"参数workflowid="+workflowId+"查询失败");
            return docChangeWfFieldList;
        }else{
            LOG.info("通过workflowid获取到的id为"+rs.getCounts()+"条");
            while (rs.next()){
                changeId = rs.getString("id");
            }
        }
        if(StringUtil.isNull(changeId)){
            LOG.info("DocChangeManager.getDocChangeWfFieldList sqlForDocChangeWfChangeId="+sqlForDocChangeWfChangeId+"参数workflowid="+workflowId+"未获取到交换字段的配置");
            return docChangeWfFieldList;
        }
        String sqlForDocChangeWfField = "select a.* from DocChangeWfField a where a.changeid =? and a.version = (select max(b.version) from DocChangeWfField b where b.changeid=?)";
        boolean isQuerySuccess = rs.executeQuery(sqlForDocChangeWfField,changeId,changeId);
        if(isQuerySuccess){
            while(rs.next()){
                DocChangeWfField docChangeWfField = new DocChangeWfField();
                docChangeWfField.setChangeid(rs.getInt("changeid"));
                docChangeWfField.setCreator(rs.getInt("creator"));
                docChangeWfField.setExchangefieldid(rs.getInt("exchangefieldid"));
                docChangeWfField.setId(rs.getInt("id"));
                docChangeWfField.setFieldid(rs.getInt("fieldid"));
                docChangeWfField.setIschange(rs.getString("ischange"));
                docChangeWfField.setIscompany(rs.getString("iscompany"));
                docChangeWfField.setVersion(rs.getInt("version"));
                docChangeWfField.setWorkflowid(rs.getInt("workflowid"));
                docChangeWfFieldList.add(docChangeWfField);
            }
        }else{
            LOG.info("DocChangeManager.getDocChangeWfFieldList sqlForDocChangeWfField="+sqlForDocChangeWfField+"参数changeid="+changeId+"未获取到交换字段的配置");
            return docChangeWfFieldList;
        }
        return docChangeWfFieldList;
    }


    /**
     * 发送撤销和作废的处理逻辑
     * @param ids     id的集合
     * @param status  状态0：重发 4：撤销 5：作废
     * @param detail  撤销或作废的原因
     */
    public boolean replyActionByWebservice(String ids,String receiverDepartments, String status, String detail,User user,String isInnerChange) {
        boolean rtResult = false;
        if(StringUtil.isNull(status)){
            LOG.info("动作参数status为null或为空！");
            return rtResult;
        }
        if(status.equals("4")){
            if(StringUtil.isNull(ids,receiverDepartments)){
                LOG.info("撤销动作参数ids或者receiverDepartments为null或为空！");
                return rtResult;
            }
        }else if(status.equals("5")){
            if(StringUtil.isNull(ids)){
                LOG.info("作废动作参数ids为null或为空！");
                return rtResult;
            }
        }else if(status.equals("0")){
            if(StringUtil.isNull(ids,receiverDepartments)){
                LOG.info("重发动作参数ids、receiverDepartments为null或为空！");
                return rtResult;
            }
        }else{
            LOG.info("status="+status+"不是重发、撤销或作废对应的状态，0：重发 4：撤销 5：作废");
            return rtResult;
        }
        if(status.equals("4")){
            rtResult = doRevokedDoc(ids,receiverDepartments,status,detail,user,isInnerChange);
        }else if(status.equals("5")){
            rtResult = doCancelDoc(ids,status,detail,user,isInnerChange);
        }else {
            rtResult = doResendDoc(ids,receiverDepartments,status,user,isInnerChange);
        }
        return rtResult;
    }

    /**
     * 重发逻辑
     * @param ids 请求编号
     * @param receiverDepartments 接收单位编号
     * @param status  状态
     * @param user   用户
     * @return false：重发失败 true：重发成功
     */
    private boolean doResendDoc(String ids, String receiverDepartments, String status, User user,String isInnerChange) {
        boolean rtResult = false;
        if(StringUtil.isNull(ids,receiverDepartments,status)){
            LOG.info("重发参数ids="+ids+" receiverDepartments="+receiverDepartments+" status="+status);
            return false;
        }
        try {
//            String updateResendDocSendDocOa = "update exchange_sendDocInfo_oa  c set c.send_status=? where c.request_id =?";
            String updateResendDocReceiveUnitListOa = "UPDATE exchange_receiveUnitList_oa SET doc_status =?, response_msg =?, oper_date =?, oper_time =? WHERE document_identifier =? AND receive_company_id =?";
            String getAreadySendDocSql = "SELECT b.id FROM exchange_sendDocInfo_oa a inner join exchange_receiveUnitList_oa b on a.document_identifier=b.document_identifier where a.request_id =? and b.receive_company_id = ? and b.document_identifier=?";
            String insertResendDocReceiveUnitListOa="insert into exchange_receiveUnitList_oa (DOCUMENT_IDENTIFIER, RECEIVE_COMPANY_ID, DOC_STATUS,OPER_DATE,OPER_TIME,TYPE) values (?,?,?,?,?,?)";
            String[] receiverDepartmentArr = receiverDepartments.split(",");
            String requestIdsStr = "";
            RecordSet tmpRecordSet = new RecordSet();
            List params = new ArrayList();
			Object[] obj = DBUtil.transListIn(ids, params);
			String sql = "select request_id from exchange_sendDocInfo_oa where id in ("+obj[0]+")";
			tmpRecordSet.executeQuery(sql,params);
			while (tmpRecordSet.next()){
				requestIdsStr += tmpRecordSet.getString("request_id")+",";
			}
			if(StringUtil.isNotNull(requestIdsStr)&&requestIdsStr.endsWith(",")){
				requestIdsStr = requestIdsStr.substring(0,requestIdsStr.length()-1);
			}
			String[] requestIds = requestIdsStr.split(",");

            String[] idArr = ids.split(",");
            //requestid去重
            Set<String> set = new HashSet<>();
            for(int i=0;i<idArr.length;i++){
                set.add(idArr[i]);
            }
            String[] arrayResult = (String[]) set.toArray(new String[set.size()]);
            List<String> baseIds = new ArrayList();
            String currentDate = TimeUtil.getCurrentDateString();
            String currentTime = TimeUtil.getOnlyCurrentTimeString();
            if(null!=arrayResult&&arrayResult.length>0){
                for (String s:arrayResult) {
                    if(StringUtil.isNotNull(s)&&!baseIds.contains(s)){
						baseIds.add(s);
                    }
                }
            }


            if(StringUtil.isNotNull(isInnerChange)&&"1".equals(isInnerChange)){
                List<List<Object>> updateDatas = new ArrayList<>();
                List<List<Object>> insertDatas = new ArrayList<>();

                 List<List<Object>> updateDatasInner = new ArrayList<>();
                List<List<Object>> insertDatasInner = new ArrayList<>();
                RecordSet recordSet3 = new RecordSet();

				/**过滤开启了禁止触发子流程的收发文单位*/
				RecordSet recordSet1 = new RecordSet();
				recordSet1.executeQuery("select id from docreceiveunit where id in("+receiverDepartments+") and canstartchildrequest=1");
				String receiveUnitValueOnlyCanStartChildRequest = "";
				while(recordSet1.next()){
					receiveUnitValueOnlyCanStartChildRequest += Util.null2String(recordSet1.getString("id"))+",";
				}
				if(StringUtil.isNotNull(receiveUnitValueOnlyCanStartChildRequest)){
					receiveUnitValueOnlyCanStartChildRequest = receiveUnitValueOnlyCanStartChildRequest.substring(0,receiveUnitValueOnlyCanStartChildRequest.length()-1);
				}
				receiverDepartments = receiveUnitValueOnlyCanStartChildRequest;
				if(StringUtil.isNull(receiverDepartments))return true;
				receiverDepartmentArr = receiverDepartments.split(",");
                for (String baseId:baseIds) {
                    RecordSet recordSet = new RecordSet();
                    String tmpDocumentIdentifier = "";
					String subWorkflowSettingId = "";
					String requestid = "";
					recordSet.executeQuery("select document_identifier,subWorkflowSettingId,request_id from exchange_senddocinfo_inner where id=?",baseId);
					if(recordSet.next()){
						tmpDocumentIdentifier = recordSet.getString("document_identifier");
						subWorkflowSettingId = recordSet.getString("subWorkflowSettingId");
						requestid = recordSet.getString("request_id");
					}
					boolean innerChange = false;
					boolean autoSend = false;
					boolean autoReceive = false;
					String responseMsg = "";
					if(StringUtil.isNotNull(subWorkflowSettingId)){
						String workflowIdStr = RequestIdUtil.getWorkflowIdByRequestId(""+requestid);
						boolean isTriggerDifference = "1".equals(isTriggerDifference(Integer.valueOf(workflowIdStr)));
						String tmpSql="";
						if(isTriggerDifference){
							tmpSql = "select innerChange,autoSend,autoReceive from Workflow_TriDiffWfDiffField where id=?";
						}else{
							tmpSql = "select innerChange,autoSend,autoReceive from Workflow_SubwfSet where id=?";
						}
						recordSet.executeQuery(tmpSql,subWorkflowSettingId);
						if(recordSet.next()){
							innerChange = "1".equals(recordSet.getString("innerChange"));
							autoSend = "1".equals(recordSet.getString("autoSend"));
							autoReceive = "1".equals(recordSet.getString("autoReceive"));
						}
					}
					if(autoReceive){
						status = "1";
						responseMsg = SystemEnv.getHtmlLabelName(514997,7);
					}
					try {
						for (String receiverDepartment:receiverDepartmentArr) {
							RecordSetTrans recordSetTrans = new RecordSetTrans();
							recordSetTrans.setAutoCommit(false);
							try {
								recordSetTrans.executeQuery(" select id from exchange_receiveunitlist_inner where document_identifier=? and receive_company_id=?",tmpDocumentIdentifier,receiverDepartment);
								List<Object> insertDataInner = new ArrayList<>();
								if(recordSetTrans.next()){
									recordSetTrans.executeUpdate("update exchange_receiveunitlist_inner set doc_status =?, response_msg =?, oper_date =?, oper_time =? where document_identifier =? and receive_company_id =?",status,
											responseMsg,currentDate,currentTime,tmpDocumentIdentifier,receiverDepartment);
								}else{
									recordSetTrans.executeUpdate("insert into exchange_receiveunitlist_inner (document_identifier, receive_company_id, doc_status,oper_date,oper_time,type,response_msg) values (?,?,?,?,?,?,?)",tmpDocumentIdentifier,receiverDepartment,
											status,currentDate,currentTime,"1",responseMsg);
								}
								recordSetTrans.executeQuery("SELECT a.*,b.* FROM exchange_receive_doc_inner a LEFT JOIN exchange_receivestatus_inner b ON a.DOCUMENT_IDENTIFIER = b.DOCUMENT_IDENTIFIER WHERE a.document_identifier = ? and b.Receiver_Department=? ",tmpDocumentIdentifier,receiverDepartment);
								if(recordSetTrans.next()){
									recordSetTrans.executeUpdate("update exchange_receivestatus_inner set operate_status =?, note =?, operate_date =?, operate_time =?,subrequestid=null,creater=null,subWorkflowSettingId=? where document_identifier =? and receiver_department =?",status,responseMsg,currentDate,currentTime,subWorkflowSettingId,tmpDocumentIdentifier,receiverDepartment);
								}else{
									recordSetTrans.executeUpdate("insert into exchange_receivestatus_inner (document_identifier, receiver_department, operate_status,operate_date,operate_time,operator,subWorkflowSettingId) values (?,?,?,?,?,?,?)",tmpDocumentIdentifier,receiverDepartment,status,currentDate,currentTime,user.getUID(),subWorkflowSettingId);
								}

								recordSetTrans.commit();
								recordSetTrans.setAutoCommit(true);
								if(autoReceive){
									if(StringUtil.isNotNull(receiverDepartment)) {
										String subWorkflowCreatorId = new DocReceiveUnitManager().getUserIdsByDocReceiveUnit(receiverDepartment);
										String[] creatorIds = subWorkflowCreatorId.split(",");
										//收发文创建人取第一个人
										if (creatorIds.length > 0) {
											triggerSubWorkflow(Integer.valueOf(requestid),Integer.valueOf(subWorkflowSettingId),new User(Integer.valueOf(creatorIds[0])),receiverDepartment);
										}
									}

								}
							} catch (Exception e) {
								writeLog("docchangemanager exception:",e);
								recordSetTrans.rollback();
								rtResult = false;
							}
						}
					} catch (Exception e) {
						writeLog("docchangemanager exception:",e);
					}
					rtResult = true;


					/*for (String receiverDepartment:receiverDepartmentArr) {
                        recordSet.executeQuery(" select id from exchange_receiveunitlist_inner where document_identifier=? and receive_company_id=?",tmpDocumentIdentifier,receiverDepartment);
                        if(recordSet.next()){
                            List<Object> updateData = new ArrayList<>();
                            updateData.add(status);
                            updateData.add("");
                            updateData.add(currentDate);
                            updateData.add(currentTime);
                            updateData.add(tmpDocumentIdentifier);
                            updateData.add(receiverDepartment);
                            updateDatas.add(updateData);
                        }else{
                            List<Object> insertData = new ArrayList<>();
                            insertData.add(tmpDocumentIdentifier);
                            insertData.add(receiverDepartment);
                            insertData.add(status);
                            insertData.add(currentDate);
                            insertData.add(currentTime);
                            insertData.add("1");
                            insertDatas.add(insertData);
                        }
                        recordSet.executeQuery("SELECT a.*,b.* FROM exchange_receive_doc_inner a LEFT JOIN exchange_receivestatus_inner b ON a.DOCUMENT_IDENTIFIER = b.DOCUMENT_IDENTIFIER WHERE a.document_identifier = ? and b.Receiver_Department=? ",tmpDocumentIdentifier,receiverDepartment);
                        if(recordSet.next()){
                            List<Object> updateDataInner = new ArrayList<>();
                            updateDataInner.add(status);
                            updateDataInner.add("");
                            updateDataInner.add(currentDate);
                            updateDataInner.add(currentTime);
							updateDataInner.add(subWorkflowSettingId);
                            updateDataInner.add(tmpDocumentIdentifier);
                            updateDataInner.add(receiverDepartment);
                            updateDatasInner.add(updateDataInner);
                        }else{
                            List<Object> insertDataInner = new ArrayList<>();
                            insertDataInner.add(tmpDocumentIdentifier);
                            insertDataInner.add(receiverDepartment);
                            insertDataInner.add(status);
                            insertDataInner.add(currentDate);
                            insertDataInner.add(currentTime);
                            insertDataInner.add(user.getUID());
							insertDataInner.add(subWorkflowSettingId);
                            insertDatasInner.add(insertDataInner);
                        }


                    }*/

                    /*int nodeId = -1;
                    if(StringUtil.isNotNull(requestid)){
                        String sql4NodeId = "select nodeid from odoc_requestdoc_inner where requestid=?";
                        recordSet3.executeQuery(sql4NodeId,requestid);
                        if(recordSet3.next()){
                            nodeId = Util.getIntValue(recordSet3.getString("nodeid"),-1);
                        }
                    }
                    if(StringUtil.isNotNull(requestid)&&nodeId>0){
                        triggerSubWorkflow(Integer.valueOf(requestid),nodeId,user,receiverDepartments);
                    }*/


                }
                /*RecordSetTrans recordSetTrans = new RecordSetTrans();
                recordSetTrans.setAutoCommit(false);
                try {
                    if(updateDatas.size()>0){
                        recordSetTrans.executeBatchSql("update exchange_receiveunitlist_inner set doc_status =?, response_msg =?, oper_date =?, oper_time =? where document_identifier =? and receive_company_id =?",updateDatas);
                    }
                    if(insertDatas.size()>0){
                        recordSetTrans.executeBatchSql("insert into exchange_receiveunitlist_inner (document_identifier, receive_company_id, doc_status,oper_date,oper_time,type) values (?,?,?,?,?,?)",insertDatas);
                    }

                    if(updateDatasInner.size()>0){
                        recordSetTrans.executeBatchSql("update exchange_receivestatus_inner set operate_status =?, note =?, operate_date =?, operate_time =?,subrequestid='',subWorkflowSettingId=? where document_identifier =? and receiver_department =?",updateDatasInner);
                    }
                    if(insertDatasInner.size()>0){
                        recordSetTrans.executeBatchSql("insert into exchange_receivestatus_inner (document_identifier, receiver_department, operate_status,operate_date,operate_time,operator,subWorkflowSettingId) values (?,?,?,?,?,?,?)",insertDatasInner);
                    }

                    recordSetTrans.commit();
                } catch (Exception e) {
                    recordSetTrans.rollback();
                    LOG.info("Exception:",e);
                }
                recordSetTrans.setAutoCommit(true);*/



            }else{
                for (String requestid:requestIds) {
                    try {
                        RecordSet rs = new RecordSet();
                        String tmpDocumentIdentifier = "";
                        for (String receiverDepartment:receiverDepartmentArr) {
                            rs.executeQuery("select b.* from exchange_sendDocInfo_oa b where b.request_id=?",requestid);
                            if(rs.next()){
                                tmpDocumentIdentifier = rs.getString("document_identifier");
                            }
                            boolean isGetAreadySendDocSql = rs.executeQuery(getAreadySendDocSql,requestid,receiverDepartment,tmpDocumentIdentifier);
                            if(isGetAreadySendDocSql){
                                SendDocByWebservice(user,requestid,receiverDepartment);
                                if(rs.getCounts()>0){
                                    rs.executeUpdate(updateResendDocReceiveUnitListOa,status,"",TimeUtil.getCurrentDateString(),TimeUtil.getOnlyCurrentTimeString(),tmpDocumentIdentifier,receiverDepartment);
                                }else{
                                    rs.executeQuery("select b.* from exchange_sendDocInfo_oa b where b.request_id=?",requestid);
                                    if(rs.next()){
                                        rs.executeUpdate(insertResendDocReceiveUnitListOa,tmpDocumentIdentifier,receiverDepartment,status,TimeUtil.getCurrentDateString(),TimeUtil.getOnlyCurrentTimeString(),"1");
                                    }
                                }
                            }else{
                                LOG.info("getAreadySendDocSql="+getAreadySendDocSql+" 参数：id="+requestid+" receiverDepartment="+receiverDepartment+"执行失败！");
                            }
                        }
                        rtResult = true;
                    } catch (Exception e) {
                        LOG.info("exception:",e);
                    }
                }
            }
        } catch (Exception e) {
            LOG.info("exception:",e);
        }
        return rtResult;
    }




    /**
     * 作废逻辑
     * @param ids
     * @param detail
     */
    private boolean doCancelDoc(String ids,String status, String detail,User user,String isInnerChange) {
        boolean rtResult = false;
        RecordSet rs = new RecordSet();
        String currentDate = TimeUtil.getCurrentDateString();
        String currentTime = TimeUtil.getOnlyCurrentTimeString();
        if(StringUtil.isNotNull(ids)){
            if(StringUtil.isNotNull(isInnerChange)&&"1".equals(isInnerChange)){
                List paramInner = new ArrayList();
                Object[] objects = DBUtil.transListIn(ids,paramInner);
                String updateCancelDocSendDocInner = "update exchange_senddocinfo_inner  set send_status=? where id in ("+objects[0]+")";
                String updateCancelReceiveUnitListInner = "update exchange_receiveunitlist_inner set doc_status=?,response_msg=?,OPER_DATE=?,OPER_TIME=? where document_identifier in(SELECT document_identifier FROM exchange_senddocinfo_inner  where id in("+objects[0]+"))";
                rs.executeUpdate(updateCancelDocSendDocInner,status,paramInner);
                rs.executeUpdate(updateCancelReceiveUnitListInner,status,detail,currentDate,currentTime,paramInner);
                String documentIdentifier = "";
                rs.executeQuery("select document_identifier from exchange_senddocinfo_inner where id in ("+objects[0]+")",paramInner);
                while(rs.next()){
                    documentIdentifier = documentIdentifier + rs.getString("document_identifier")+",";
                }
                documentIdentifier = documentIdentifier.substring(0,documentIdentifier.length()-1);
                List paramInner1 = new ArrayList();
                Object[] objects1 = DBUtil.transListIn(documentIdentifier,paramInner1);
                String tmpSql ="update exchange_receive_doc_inner set receive_status=? where document_identifier in ("+objects1[0]+")";
                String tmpSql1 ="update exchange_receivestatus_inner set operate_status=?,operate_date=?,operate_time=?,note=? where document_identifier in ("+objects1[0]+")";
                rs.executeUpdate(tmpSql,status,paramInner1);
                rs.executeUpdate(tmpSql1,status,currentDate,currentTime,detail,paramInner1);
                rtResult = true;

            }else{
                List paramOut = new ArrayList();
                Object[] objectsOut = DBUtil.transListIn(ids,paramOut);
                boolean isQueryDcoumentIdentifier = rs.executeQuery("SELECT * FROM exchange_sendDocInfo_oa WHERE id in ("+objectsOut[0]+")",objectsOut[1]);
                String documentIdentifier = "";
                if(isQueryDcoumentIdentifier){
                    while (rs.next()){
                        String tmpDocumentIdentifier = rs.getString("DOCUMENT_IDENTIFIER");
                        documentIdentifier = documentIdentifier + tmpDocumentIdentifier + ",";
                    }
                    if(documentIdentifier.endsWith(",")){
                        documentIdentifier = documentIdentifier.substring(0,documentIdentifier.length()-1);
                    }
                }
                ExchangeResponse exchangeResponse = new ExchangeResponse();
                Issuer issuer = new Issuer();
                PlaintextMessage plaintextMessage = new PlaintextMessage();
                exchangeResponse.setDocument_Identifier(documentIdentifier);
                boolean isGetIssuerUnitCodeAndName = rs.executeQuery("select ws_access_syscode,ws_access_sysname from docchangesetting ");
                String wsAccessSysCode = "";
                String wsAccessSysName = "";
                if(isGetIssuerUnitCodeAndName&& rs.next()){
                    wsAccessSysCode = rs.getString("ws_access_syscode");
                    wsAccessSysName = rs.getString("ws_access_sysname");
                }
                issuer.setIdentification_of_Document_Issuer(wsAccessSysCode);
                issuer.setName_of_Document_Issuer(wsAccessSysName);
                plaintextMessage.setType("RET");
                plaintextMessage.setBusiness_Type(ExchangeStatusEnum.STATUS_CANCELLED.getStatusValue());
                plaintextMessage.setOperate_Date(TimeUtil.getCurrentDateString());
                plaintextMessage.setOperate_Time(TimeUtil.getOnlyCurrentTimeString());
                plaintextMessage.setOperator(user.getUID()+"");
                plaintextMessage.setNote(detail);
                exchangeResponse.setIssuer(issuer);
                exchangeResponse.setPlaintext_Message(plaintextMessage);
                String operateData = ExchangeWebserviceUtil.getReponseXml(exchangeResponse);
//                LOG.info("作废 operateData="+operateData);
                String sessionCode = ExchangeWebserviceUtil.getSessionKey();
                Object [] paraObjArr = new Object[]{operateData,sessionCode};
                String result = ExchangeWebserviceUtil.getCallWebserviceMethodResult("postDocOperateData",paraObjArr);
                RecordSetTrans rst = new RecordSetTrans();
                if(!ExchangeWebserviceUtil.isEmptyStr(result)){
                    String successFlag = ExchangeClientXmlUtil.getResultContent(result, ExchangeWebserviceConstant.WEBSERVICE_RESULT_NODE);
                    if(StringUtil.isNotNull(successFlag)&&successFlag.equals(ExchangeWebserviceConstant.WEBSERVICE_RESULT_SUCCESS)){
                        String updateCancelDocSendDocOa = "update exchange_sendDocInfo_oa  set send_status=? where id in ("+objectsOut[0]+")";
                        String updateCancelReceiveUnitListOa = "update exchange_receiveUnitList_oa set doc_status=?,response_msg=?,OPER_DATE=?,OPER_TIME=? where document_identifier in(SELECT document_identifier FROM exchange_sendDocInfo_oa  where id in("+objectsOut[0]+"))";
                        rst.setAutoCommit(false);
                        try {
                            rst.executeUpdate(updateCancelDocSendDocOa,status,objectsOut[1]);
                            rst.executeUpdate(updateCancelReceiveUnitListOa,status,detail,currentDate,currentTime,objectsOut[1]);
                            rst.commit();
                            rtResult = true;
                        } catch (Exception e) {
                            rst.rollback();
                            e.printStackTrace();
                            LOG.info("参数为ids="+ids+"status="+status+"detail="+detail+"更新失败");
                        }finally {
                            rst.setAutoCommit(true);
                        }
                    }else{
                        LOG.info("发送失败，错误信息为："+ExchangeClientXmlUtil.getResultContent(result, ExchangeWebserviceConstant.WEBSERVICE_RESPONSE_MSG_NODE));
                    }
                }else{
                    LOG.info("发送失败，平台反馈信息为空或为null");
                }
            }
        }
        return rtResult;
    }

    /**
     * 撤销逻辑
     * @param ids
     * @param receiverDepartments
     * @param detail
     */
    private boolean doRevokedDoc(String ids, String receiverDepartments,String status,String detail,User user,String isInnerChange) {
        RecordSetTrans rst = new RecordSetTrans();
        String currentDate = TimeUtil.getCurrentDateString();
        String currentTime = TimeUtil.getOnlyCurrentTimeString();
        boolean rtResult = false;
        try {
            RecordSet recordSet = new RecordSet();
            if(StringUtil.isNotNull(isInnerChange)&&"1".equals(isInnerChange)){
                recordSet.executeQuery(" select * from exchange_senddocinfo_inner where id=?",ids);
                if(recordSet.next()){
                    String tmpDocumentIdentifier = Util.null2String(recordSet.getString("document_identifier"));
                    if(StringUtil.isNotNull(tmpDocumentIdentifier,receiverDepartments)){
                        List params = new ArrayList();
                        Object[] obj = DBUtil.transListIn(receiverDepartments, params);
                        String updateRevokeDocSql = "UPDATE exchange_receiveunitlist_inner SET doc_status =?, response_msg =?, oper_date =?, oper_time =? WHERE document_identifier =? AND receive_company_id IN ("+obj[0]+")";
                        recordSet.executeUpdate(updateRevokeDocSql,status,detail,currentDate,currentTime,tmpDocumentIdentifier,params);
                        String updateRevokeDoc4ReceiveSql = "update exchange_receivestatus_inner set operate_status =?, note =?, operate_date =?, operate_time =? where document_identifier =? and receiver_department in ("+obj[0]+")";
                        recordSet.executeUpdate(updateRevokeDoc4ReceiveSql,status,detail,currentDate,currentTime,tmpDocumentIdentifier,params);
                        rtResult = true;
                    }
                }
            }else{
                boolean isQueryDcoumentIdentifier = rst.executeQuery("SELECT * FROM exchange_sendDocInfo_oa WHERE id =?",ids);
                String documentIdentifier = "";
                if(isQueryDcoumentIdentifier&&rst.next()){
                    documentIdentifier = rst.getString("DOCUMENT_IDENTIFIER");
                }
                ExchangeResponse exchangeResponse = new ExchangeResponse();
                Issuer issuer = new Issuer();
                Receiver receiver = new Receiver();
                PlaintextMessage plaintextMessage = new PlaintextMessage();
                exchangeResponse.setDocument_Identifier(documentIdentifier);
                String unitCodes = "";
                String receiveUnitNames = "";
                boolean isGetReceiverUnitCodeAndName = rst.executeQuery("select systemcode,unitcode,receiveunitname from docreceiveunit where id in ("+receiverDepartments+")");
                if(isGetReceiverUnitCodeAndName){
                    while(rst.next()){
                        String unitCode = rst.getString("unitcode");
                        String systemcode = rst.getString("systemcode");
                        String receiveUnitName = rst.getString("receiveunitname");
                        if(StringUtil.isNotNull(unitCode)){
                            unitCodes = unitCodes+systemcode+"_"+unitCode+",";
                        }else{
                            unitCodes = unitCodes+systemcode+",";
                        }
                        receiveUnitNames = receiveUnitNames+receiveUnitName+",";
                    }
                }
                if(unitCodes.endsWith(",")){
                    unitCodes = unitCodes.substring(0,unitCodes.length()-1);
                }
                if(receiveUnitNames.endsWith(",")){
                    receiveUnitNames = receiveUnitNames.substring(0,receiveUnitNames.length()-1);
                }
                receiver.setIdentification_of_Document_Receiver(unitCodes);
                receiver.setName_of_Document_Receiver(receiveUnitNames);
                boolean isGetIssuerUnitCodeAndName = rst.executeQuery("select ws_access_syscode,ws_access_sysname as ws_access_sysname from docchangesetting");
                String wsAccessSysCode = "";
                String wsAccessSysName = "";
                if(isGetIssuerUnitCodeAndName&& rst.next()){
                    wsAccessSysCode = rst.getString("ws_access_syscode");
                    wsAccessSysName = rst.getString("ws_access_sysname");
                }
                issuer.setIdentification_of_Document_Issuer(wsAccessSysCode);
                issuer.setName_of_Document_Issuer(wsAccessSysName);
                plaintextMessage.setType("RET");
                plaintextMessage.setBusiness_Type(ExchangeStatusEnum.STATUS_REVOKED.getStatusValue());
                plaintextMessage.setOperate_Date(TimeUtil.getCurrentDateString());
                plaintextMessage.setOperate_Time(TimeUtil.getOnlyCurrentTimeString());
                plaintextMessage.setOperator(user.getUID()+"");
                plaintextMessage.setNote(detail);
                exchangeResponse.setIssuer(issuer);
                exchangeResponse.setReceiver(receiver);
                exchangeResponse.setPlaintext_Message(plaintextMessage);
                String operateData = ExchangeWebserviceUtil.getReponseXml(exchangeResponse);
//                LOG.info("撤销 operateData="+operateData);
                String sessionCode = ExchangeWebserviceUtil.getSessionKey();
                Object [] paraObjArr = new Object[]{operateData,sessionCode};
                String result = ExchangeWebserviceUtil.getCallWebserviceMethodResult("postDocOperateData",paraObjArr);
                if(!"".equals(result)) {
                    //反馈信息成功
                    rst.setAutoCommit(false);
                    String updateRevokeDocSql = "UPDATE exchange_receiveUnitList_oa SET doc_status =?, response_msg =?, oper_date =?, oper_time =? WHERE document_identifier =? AND receive_company_id IN ("+receiverDepartments+")";
                    rst.executeQuery("select * from exchange_sendDocInfo_oa where id=?",ids);
                    String tmpDocumentIdentifier = "";
                    if(rst.next()){
                        tmpDocumentIdentifier = rst.getString("document_identifier");
                    }
                    boolean isUpdateRevokeDocSql = rst.executeUpdate(updateRevokeDocSql,status,detail,TimeUtil.getCurrentDateString(),TimeUtil.getOnlyCurrentTimeString(),tmpDocumentIdentifier);
                    if(isUpdateRevokeDocSql){
                        rst.commit();
                        rst.setAutoCommit(true);
                        rtResult = true;
                    }else{
                        rst.rollback();
                        rst.setAutoCommit(true);
                        rtResult = false;
                    }
                }else {
                    rtResult = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            rst.rollback();
            rst.setAutoCommit(true);
            rtResult = false;
        }
        return rtResult;
    }

    /**
     * 获取表单内容数据
     */
    public static Map<String, Property> getRequestFormData(String requestId){
        //获取流程信息
        RequestInfo ri = new RequestService().getRequest(Integer.valueOf(requestId));
        MainTableInfo mi = ri.getMainTableInfo();
        Map<String, Property> formPropMap = new HashMap<>();
        Property prop;
        for (int i = 0; i < mi.getPropertyCount(); i++) {
            prop = mi.getProperty(i);
            String fieldName = prop.getName();
//            LOG.info("字段名称：prop.getName()"+fieldName+"字段值：prop.getValue()"+prop.getValue()+"字段类型：prop.getType()="+prop.getType());
            formPropMap.put(fieldName,prop);
        }
        return formPropMap;
    }


	/**
	 * 取得流程字段或相关流程字段-取得单位名称
	 * @param user	当前用户
	 * @param requestid	请求ID
	 * @param elements	数组定义Node名称
	 * @param isAss	是否相关流程
	 * @throws Exception
	 */
	public Map<String, List> getRequestWebReceiveName(User user, String workflowid, String requestid, String[] elements, boolean isMode, boolean isAss) throws Exception {
		List unitNameList = new ArrayList();//收发文单位名称列表
		List unitIdList = new ArrayList();//收发文单位ID列表
		RecordSet rs0 = new RecordSet();
		RecordSet rs1 = new RecordSet();

		//System.out.println(elements[0]+"__"+elements[1]+"__"+elements[2]+"__"+elements[3]+"__"+elements[4]);
		boolean isForm = elements[0].equals("form");
		Element e = new Element(elements[0]);
		Element e1 = new Element(elements[1]);
		if(!isForm) {
			e1.setText(requestid);
			//e.addContent(e1);
		}
		else {
			e1.setText(this.formName);
			e.addContent(e1);//FROM 名称
		}
		//退回判断
		String isreject = "false";
		StringBuffer s = new StringBuffer();
		s.append("select c.isreject,d.status, a.isremark ");
		s.append("from workflow_currentoperator a,workflow_flownode b,workflow_nodebase c, workflow_requestbase d ");
		s.append("where a.nodeid=b.nodeid and a.workflowid=b.workflowid and a.isremark in('0','1','5','4','8','9','7') ");
		s.append("and a.nodeid=c.id and a.requestid=d.requestid ");
		s.append("and a.usertype=0 and a.requestid=" + requestid + " and a.userid=" + user.getUID() + " order by a.id desc");
		//System.out.println(s.toString());
		rs0.executeSql(s.toString());
		if(rs0.next()) {
			if(rs0.getString("isreject").equals("1"))
				isreject = "true";
		}

		//获取指定的收文单位
		Map fieldsMap = new HashMap();
		rs1.executeSql("SELECT * FROM DocChangeWfField WHERE workflowid="+workflowid+" AND isCompany='1' AND version=(SELECT max(version) FROM DocChangeWfField WHERE workflowid="+workflowid+" and isCompany='1')");
		while(rs1.next()){
			fieldsMap.put(rs1.getInt("fieldid")+"","X");
			versionMap.put(workflowid, rs1.getString("version"));
		}

		Element reject = new Element("Reject");
		reject.setText(isreject);
		//请求状态,true正常的,false退回的
		Element ereject = new Element("Status");
		ereject.setText(rs0.getString("status"));
		Element remark = new Element("isremark");
		remark.setText(rs0.getString("isremark"));

		//e.addContent(reject);
		//e.addContent(ereject);
		//e.addContent(remark);

		rs0.executeSql(this.requestSql(String.valueOf(user.getUID()), requestid, isMode));
		String[] fields = new String[6];
		//Map fieldsMap = new HashMap();
		List fieldsMapList = new ArrayList();
		while(rs0.next()) {
			fields = new String[6];
			fields[0] = rs0.getString("fieldid");
			fields[1] = rs0.getString("fieldlable");
			fields[2] = rs0.getString("fieldname");
			fields[3] = rs0.getString("fieldhtmltype");
			fields[4] = rs0.getString("type");
			fields[5] = rs0.getString("isBill");
			//fieldsMap.put(rs0.getString("fieldname"), fields);
			fieldsMapList.add(fields);
		}
		//交换字段查询
		Map changeFiledMap = new HashMap();
		rs0.executeSql("SELECT * FROM DocChangeWfField WHERE workflowid="+workflowid+" AND version=(SELECT max(version) FROM DocChangeWfField WHERE workflowid="+workflowid+")");
		while(rs0.next()){
			changeFiledMap.put(rs0.getInt("fieldid")+"",""+rs0.getInt("isChange"));
		}
		//end 交换字段查询
		String tablename = "workflow_form";
		rs0.executeSql("select t4.tablename from workflow_base t1, workflow_requestbase t2, workflow_form t3, workflow_bill t4 where t1.id=t2.workflowid and t2.requestid=t3.requestid and t4.id=t3.billformid and t3.billid>0 and t2.requestid="+requestid);
		if(rs0.next()) {
			tablename = rs0.getString("tablename");
		}
		rs0.executeSql(this.requestValueSql(requestid, fieldsMapList, tablename));
		if(rs0.next()) {
			int currsize = fieldsMapList.size();
			//Iterator it = fieldsMap.entrySet().iterator();
			//while(it.hasNext()) {
			for(int i=0; i<fieldsMapList.size(); i++) {
				Element field = new Element(elements[2]);
				//Map.Entry entry = (Map.Entry) it.next();
				if(!tablename.equals("workflow_form"))
					currsize--;
				else currsize = i;
				String[] strarr = (String[]) fieldsMapList.get(currsize);//(String[]) entry.getValue();
				if(isChangeField) {
					//不在交换字段中
					if(!Util.null2String(""+changeFiledMap.get(""+Integer.parseInt(strarr[0]))).equals("1")) {
						continue;
					}
				}
				//若交换字段中不含有当前字段，则不发送该字段
				//if(strarr[0] != null && changeFiledMap.get(strarr[0]) == null){
					//continue;
				//}
				//公文交换不需要交换相关流程内容
				/*
				if(strarr[3].equals("3") && strarr[4].equals("16") && !isForm) {//相关流程
					if(!isChangeField) continue;//公文交换暂不需要普通字段
					this.formName = Util.toHtml(new FieldValue().getFieldValue(user, Integer.parseInt(strarr[0]), Integer.parseInt(strarr[3]), Integer.parseInt(strarr[4]), rs0.getString(strarr[2]), Integer.parseInt(strarr[5])));
					String[] elementsForm = new String[5];
					elementsForm[0] = "form";
					elementsForm[1] = "formname";
					elementsForm[2] = "formfield";
					elementsForm[3] = "formfieldname";
					elementsForm[4] = "formfieldvalue";
					this.getRequestField(user, workflowid, requestid, elementsForm, isMode, true);
				}
				else
				*/
				if(strarr[3].equals("6") && !isForm) {//附件处理
					String docids = rs0.getString(strarr[2]);
					if(!Util.null2String(docids).equals("")) {
						String linktext = "";
						String nametext = "";
						int docImageFileId=0;
						int hisDocImageFileId=0;
						rs1.executeSql("select id,imagefileid,imagefilename from docimagefile where docid in("+docids+") order by id asc,versionId desc");
						while(rs1.next()) {
							docImageFileId=Util.getIntValue(rs1.getString("id"),0);
							if(docImageFileId==hisDocImageFileId){
								continue;
							}
							hisDocImageFileId=docImageFileId;

							linktext = this.getFileToBase64(rs1.getString("imagefileid"), false);//获取附件BASE64码
							//linktext = "/weaver/weaver.file.FileDownload?userid="+user.getUID()+"&fileid="+rs1.getString("imagefileid")+"&coworkid=0&requestid="+requestid;
//							nametext = "["+rs1.getString("imagefileid")+"]"+rs1.getString("imagefilename");
                            nametext = rs1.getString("imagefilename");

							Element attachment = new Element("File");
							Element name = new Element("Filename");
							//name.setText(Util.StringReplace(Util.null2String(new FieldValue().getFieldValue(user, Integer.parseInt(strarr[0]), Integer.parseInt(strarr[3]), Integer.parseInt(strarr[4]), rs0.getString(entry.getKey().toString()), Integer.parseInt(strarr[5]))),",",""));
							Element link = new Element("FileContent");
							Element filemerge = new Element("FileMerge");//是否合并到正文的附件
							name.setText(nametext);
							link.setText(linktext);
							filemerge.setText("0");
							if(nametext.equals("") || linktext.equals("")) {
								//do nothing
							}
							else {
								attachment.addContent(name);
								attachment.addContent(link);
								attachment.addContent(filemerge);
								attachmentsList.add(attachment);
							}
						}
					}
				}
				else {
					//if(!isAss && fieldsMap.get(strarr[0])!=null) {//查看该字段是否在指定的收文单位中//只有主流程的接收单位才做处理
						//公文接收字段
						if(Integer.parseInt(strarr[4])==142) {
						    if(isMainReceiveDepartmentOrCopyDepartment(strarr[0],workflowid)){
                                String sendIds = Util.null2String(rs0.getString(strarr[2]));
                                StringTokenizer st = new StringTokenizer(sendIds, ",");
                                String companyType = "";
                                while (st.hasMoreTokens()) {
                                    String cid = st.nextToken();
                                    companyType = docReceiveUnitComInfo.getReceiveUnitName(cid);
                                    unitIdList.add(cid);
                                    unitNameList.add(companyType);
								/*if(Util.null2String(companyType).equals("1")) {
									dirList.add(cid+","+docReceiveUnitComInfo.getChangeDir(cid));
								}*/
                                }
                            }
						}
					//}
					if(!isChangeField){
						continue;//公文交换暂不需要交换字段
					}
					Element fieldname = new Element(elements[3]);
					fieldname.setText(strarr[1]);
					Element fieldvalue = new Element(elements[4]);
					fieldvalue.setText(Util.toHtml(new FieldValue().getFieldValue(user, Integer.parseInt(strarr[0]), Integer.parseInt(strarr[3]), Integer.parseInt(strarr[4]), rs0.getString(strarr[2]), Integer.parseInt(strarr[5]))));
					Element fieldid = new Element("Fieldid");
					fieldid.setText(strarr[2]);//字段名称
					field.addContent(fieldid);
					field.addContent(fieldname);
					field.addContent(fieldvalue);
					e.addContent(field);
				}
			}
		}
		if(isForm) {
			this.formsList.add(e);
		}
		else {
			this.flowDetail = e;
		}
		if(unitIdList.size()>0){
			sendMap.put("unitNameList", unitNameList);//将接收单位字段放至MAP中\
			sendMap.put("unitIdList", unitIdList);//将接收单位字段放至MAP中\
		}

		return sendMap;
	}


    /**
     * 通过字段编号和工作流编号判断此字段编号是否对应平台的主送单位或抄送单位
     * @param fieldId            字段编号
     * @param workflowId         工作流编号
     * @return  true：对应  false：不对应
     */
	public boolean isMainReceiveDepartmentOrCopyDepartment(String fieldId,String workflowId){
	    boolean isInclude = false;
	    if(StringUtil.isNull(fieldId,workflowId)){
	        LOG.info("DocChangeManager.isMainReceiveDepartmentOrCopyDepartment 参数：fieldId="+fieldId+" workflowId="+workflowId);
	        return isInclude;
        }
        /**获取该单位在平台配置的与标准字段对应的节点信息*/
        ArrayList<ExchangeField> exchangeFieldList = ExchangeWebserviceUtil.getFieldSetFromOdocExchange();
	    /**通过字段编号和工作流编号获取平台对应的编号*/
	    RecordSet rs = new RecordSet();
        String dbType = rs.getDBType();
        String docChangeWfChangeId = "";
        String sqlForDocChangeWfChangeId = "";
        if(dbType.equals(ExchangeWebserviceConstant.DB_TYPE_ORACLE)){
            sqlForDocChangeWfChangeId = "select a.id from DocChangeWorkflow a where a.workflowid =? or ',' || a.workflowids || ',' like '%,' || ? || ',%'";
        }else if(dbType.equals(ExchangeWebserviceConstant.DB_TYPE_MYSQL)){
            sqlForDocChangeWfChangeId = "select a.id from DocChangeWorkflow a where a.workflowid =? or CONCAT(',',a.workflowids,',') like CONCAT('%,',?,',%')";
        }else if(dbType.equals(ExchangeWebserviceConstant.DB_TYPE_SQLSERVER)){
            sqlForDocChangeWfChangeId = "select a.id from DocChangeWorkflow a where a.workflowid =? or ',' + a.workflowids + ',' LIKE '%,' + ? + ',%'";
        }else{//达梦
            sqlForDocChangeWfChangeId = "select a.id from DocChangeWorkflow a where a.workflowid =? or ',' || a.workflowids || ',' like '%,' || ? || ',%'";
        }
        boolean isSqlForDocChangeWfChangeId = rs.executeQuery(sqlForDocChangeWfChangeId,workflowId,workflowId);
        if(isSqlForDocChangeWfChangeId&&rs.getCounts()>0){
            if(rs.next()){
                docChangeWfChangeId = rs.getString("id");
                String sqlForDocChangeWfField = "select exchangefieldid from DocChangeWfField a where a.changeid =? and a.version = (select max(b.version) from DocChangeWfField b where b.changeid=?) and fieldid=? ";
                boolean isSqlForDocChangeWfField = rs.executeQuery(sqlForDocChangeWfField,docChangeWfChangeId,docChangeWfChangeId,fieldId);
                if(isSqlForDocChangeWfField&&rs.next()){
                    int exchangeFieldId = rs.getInt("exchangefieldid");
                    for (ExchangeField exchangeField:exchangeFieldList) {
                        if(exchangeField.getId()==exchangeFieldId&&(exchangeField.getField_type()==ExchangeFieldType.MAIN_RECEIVER_DEPARTMENT||exchangeField.getField_type()==ExchangeFieldType.COPY_TO_DEPARTMENT)){
                            isInclude = true;
                        }else{
                            LOG.info("DocChangeManager.isMainReceiveDepartmentOrCopyDepartment exchangeFieldId="+exchangeField.getId()+" fieldType="+exchangeField.getField_type());
                        }
                    }
                }else{
                    LOG.info("DocChangeManager.isMainReceiveDepartmentOrCopyDepartment sql：sqlForDocChangeWfField="+sqlForDocChangeWfField+" changeid="+docChangeWfChangeId+" exchangefieldid="+fieldId);
                }
            }else{
                LOG.info("DocChangeManager.isMainReceiveDepartmentOrCopyDepartment sql：sqlForDocChangeWfChangeId="+sqlForDocChangeWfChangeId+" workflowId="+workflowId);
            }
        }else{
            LOG.info("DocChangeManager.isMainReceiveDepartmentOrCopyDepartment sql：sqlForDocChangeWfChangeId="+sqlForDocChangeWfChangeId+" workflowId="+workflowId);
        }
        return isInclude;
    }


    private Element getSecLevel(String secValue){
		Element fieldid = new Element("Fieldid");
		fieldid.setText("secLevel");
		Element fieldname = new Element("Fieldname");
		fieldname.setText(SystemEnv.getHtmlLabelName(500520,7));
		Element fieldvalue = new Element("Fieldvalue");
		fieldvalue.setText(secValue);
		Element field = new Element("Field");
		field.addContent(fieldid);
		field.addContent(fieldname);
		field.addContent(fieldvalue);
		LOG.info("=================seclevel:"+field.toString());
		return field;
	}

    /**
     *
     * @param ids      记录id
     * @param status   5:作废 4:撤销
     * @param user     用户对象
     * @return         true:存在不在创建节点的子流程 false：未创建子流程或者子流程都在创建节点
     */
    public boolean checkHasSign(String ids,String receiverDepartments, String status, User user) {
        boolean rtResult = false;
        RecordSet recordSet = new RecordSet();
        RecordSet recordSet1 = new RecordSet();
        try {
            if("5".equals(status)){
                List params = new ArrayList();
                Object[] obj = DBUtil.transListIn(ids, params);
                String sql = "select * from exchange_receivestatus_inner where DOCUMENT_IDENTIFIER in (select DOCUMENT_IDENTIFIER from " +
                             " exchange_senddocinfo_inner where id in ("+obj[0]+")) and subrequestid is not null";
                recordSet.executeQuery(sql, params);
                String tmpSubrequestid = "";
                while(recordSet.next()){
                    tmpSubrequestid = tmpSubrequestid + recordSet.getString("subrequestid")+",";
                }
                if(StringUtil.isNotNull(tmpSubrequestid)){
                    if(tmpSubrequestid.endsWith(",")){
                        tmpSubrequestid = tmpSubrequestid.substring(0,tmpSubrequestid.length()-1);
                    }
                    List tmpParams = new ArrayList();
                    Object[] tmpObj = DBUtil.transListIn(tmpSubrequestid, tmpParams);
                    recordSet.executeQuery("select currentnodetype from workflow_requestbase where requestid in("+tmpObj[0]+") and currentnodetype!=0",tmpParams);
                    if(recordSet.next()){
                        rtResult = true;
                    }
                }
            }else if("4".equals(status)){
                List params = new ArrayList();
                List paramsReceiverDepartments = new ArrayList();
                Object[] obj = DBUtil.transListIn(ids, params);
                Object[] objreceiverDepartments = DBUtil.transListIn(receiverDepartments, paramsReceiverDepartments);
                String sql = "select * from exchange_receivestatus_inner where DOCUMENT_IDENTIFIER in (select DOCUMENT_IDENTIFIER from " +
                             " exchange_senddocinfo_inner where id in ("+obj[0]+")) and Receiver_Department in ("+objreceiverDepartments[0]+") and subrequestid is not null";
                recordSet.executeQuery(sql, params,paramsReceiverDepartments);
                if(recordSet.next()){
                    String subrequestid = recordSet.getString("subrequestid");
                    if(StringUtil.isNotNull(subrequestid)){
                        recordSet1.executeQuery("select currentnodetype from workflow_requestbase where requestid=? and currentnodetype!=0",subrequestid);
                        if(recordSet1.next()){
                            rtResult = true;
                        }
                    }
                }
            }

        } catch (Exception e) {
            recordSet.writeLog("checkHasSign exception:",e);
        }
        return rtResult;


    }


     private void triggerSubWorkflow(int requestId,int subworkflowSettingId,User user,String receiveDepartment){
		String hasTriggeredSubwf = "";
		String triggerType = "1";//自动触发
		String triggerTime = "";//触发时间
		int triggerNodeId = -1;//触发节点
		try {
			RequestManager mainRequest = new RequestManager();
			RecordSet rs = new RecordSet();
			String workflowIdStr = RequestIdUtil.getWorkflowIdByRequestId(""+requestId);
			boolean isTriggerDifference = "1".equals(isTriggerDifference(Integer.valueOf(workflowIdStr)));
			String sql="";
			if(isTriggerDifference){
				sql = "select triggerNodeId,triggerTime,triggerType from Workflow_TriDiffWfDiffField where id=?";
			}else{
				sql = "select triggerNodeId,triggerTime,triggerType from Workflow_SubwfSet where id=?";
			}


			rs.executeQuery(sql,subworkflowSettingId);
			if(rs.next()){
				triggerType = rs.getString("triggerType");
				triggerTime = rs.getString("triggerTime");
				triggerNodeId = rs.getInt("triggerNodeId");
			}
			rs.executeQuery("select a.*,b.isbill,b.formid from workflow_requestbase a inner join workflow_base b on a.workflowid=b.id where a.requestid=" + requestId);
			if (rs.next()) {
				mainRequest.setWorkflowid(Util.getIntValue(rs.getString("workflowid"), 0));
				mainRequest.setCreater(Util.getIntValue(rs.getString("creater"), 0));
				mainRequest.setCreatertype(Util.getIntValue(rs.getString("createrType"), 0));
				mainRequest.setRequestid(requestId);
				mainRequest.setRequestname(rs.getString("requestname"));
				mainRequest.setRequestlevel(rs.getString("requestlevel"));
				mainRequest.setMessageType(rs.getString("messagetype"));
				mainRequest.setSrc("submit");
				mainRequest.setIsbill(Util.getIntValue(rs.getString("isbill"), 0));
				mainRequest.setFormid(Util.getIntValue(rs.getString("formid"), 0));
			}
			SubWorkflowTriggerService triggerService = new SubWorkflowTriggerService(mainRequest, triggerNodeId, hasTriggeredSubwf, user);
			triggerService.triggerSubWorkflow(triggerType, triggerTime, ""+subworkflowSettingId,true,receiveDepartment);
		} catch (Exception e) {
		    new BaseBean().writeLog("triggerSubWorkflow exception:",e);
		}

    }

	private static String isTriggerDifference(int workflowId){
		RecordSet rs = new RecordSet();
		//判断是否触发不同流程
		rs.executeSql("select isTriDiffWorkflow from workflow_base where id="+workflowId);
		if(rs.next()){
			//如果没有设置，则返回“0”，代表默认触发相同流程
			String isDiff = Util.null2String(rs.getString("isTriDiffWorkflow"));
			return isDiff.equals("") ? "0" : isDiff;
		}else{
			return "";
		}
	}
}