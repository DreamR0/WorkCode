/**
 * Title:        基本环境常量类
 * Company:      泛微软件
 * @author:      刘煜
 * @version:     1.0
 * create date : 2001-10-23
 * modify log: 
 *
 *
 * Description:  GCONST 设置基本的环境常量，包括系统访问目录和系统服务名（也是默认的数据库链接池名称）
 *               GCONST 的常量由 InitServer 设置
 *
 *
 *
 *
 */

package weaver.general;


import com.engine.fna.cmd.documentCompareNew.CompareUtil;
import com.engine.portal.biz.nonstandardfunction.SysModuleInfoBiz;
import weaver.conn.RecordSet;
import weaver.conn.WeaverThreadPool;

import java.io.*;
import java.util.*;


public class GCONST {
    private static String CONFIG_FILE = "weaver";
    private static String ROOT_PATH;
    private static String CONTEXT_PATH = "/";
    private static String SERVER_NAME;
    private static WeaverThreadPool wThreadPool;
    public static ArrayList WFProcessing=new ArrayList();
    private static Properties othersprop=null;
    private static Properties mutillanguageprop=null;
    private static Properties Wfmonitorprop=null;
    private static String WORKFLOWWAYOUT = "WORKFLOWWAYOUT"; //流程出口提示信息开关：1：打开  其它：关闭
    private static String WFERRORDATACLEARTIME = "WFERRORDATACLEARTIME"; //定时清除流程垃圾信息的周期，同时也做为消息提醒定时删除周期和保留天数。 默认7天
    private static String MOREACCOUNTLANDING = "MOREACCOUNTLANDING"; //多账号启用开关： Y：启用 其他：关闭
    private static String WORKFLOWINTERVENORBYMONITOR = "WORKFLOWINTERVENORBYMONITOR"; //流程监控中流程干预开关： Y：启用 其他：关闭
    private static String FREEFLOW = "FREEFLOW"; //自由流程开关： Y：启用 其他：关闭
    private static String CHATSERVER = "CHATSERVER"; //即时通讯服务器名称
    private static String CHATREESOURCE = "CHATRESOURCE"; //即时通讯resource
    private static String CHATHOST = "CHATHOST"; //即时通讯服务器地址
    private static String WorkFlowOverTimeTmp = "WORKFLOWOVERTIMETEMP"; //是否启用过滤非工作时间计算超时流程
    private static String MailReminderSet = "MailReminderSet"; //邮件提醒地址配置： Y：启用 其他：关闭
    private static String RTXReminderSet = "RTXReminderSet"; //RTX提醒地址配置： Y：启用 其他：关闭
    private static Properties freeflowProp=null;
    private static Properties dactylogramProp=null;
    private static Properties wfFieldTriggerProp=null;
    private static Properties hrmchatProp=null;
    private static Properties wfovertime=null;
    private static Properties reminderProp=null;
    private static Properties workflowsayoutProp = null;
    private static Properties issigninorsignoutProp = null;
    private static Properties moreaccountlandingProp = null;
    private static Properties workflowimpProp = null;
    private static Properties dmlactionProp = null;
    private static String ZH_TW_LANGUAGE = "ZH_TW_LANGUAGE";
	private static String EN_LANGUAGE = "EN_LANGUAGE";
	private static Properties workflowstoporcancelProp = null; //流程暂停或取消
	private static Properties workflowimportdetailProp = null; //流程明细导入
	private static Properties workflowspecialApprovalProp = null;//流程特批件
	private static Properties workflowreturnnodeProp = null;//流程特批件
	private static Properties systemSettingMenuProp = null;//流程特批件
	private static Properties systemThemeTemplateProp = null;//系统主题软件模板、网站模板启用配置
	private static Properties wsactionProp = null;
    private static Properties esbactionProp = null;
	private static Properties sapactionProp = null;
	private static Properties coremailProp = null;
	public static String PROP_UTF8 = "UTF-8";//PROP文件设置UTF-8编码
	public static String XML_UTF8 = "UTF-8";//XML文件设置UTF-8编码
	public static String NOMRAL_UTF8 = "UTF-8";//普通文件UTF-8编码
	//多语言过滤器用	
	public static final String inputRegP = "(<input( [^>]+ | )name {0,3}= {0,3}['\"]?)([^>]{0,500}?)(['\"]?( [^>]+ | )value {0,3}= {0,3}['|\"]?)(~`~`([\\d ]{1,2}[^`]{0,2000}?`~`)+[\\d ]{1,2}[^`]{0,2000}?`~`~)(['\"]?[^>]*/?>)|(<input( [^>]+ | )value {0,3}= {0,3}['\"]?)(~`~`([\\d ]{1,2}[^`]{0,2000}?`~`)+[\\d ]{1,2}[^`]{0,2000}?`~`~)(['\"]?( [^>]+ | )name {0,3}= {0,3}['\"]?)([^>]{0,500}?)(((['\" ][^>]*/?)|[/?])(>))";	
    //public static final String inputRegP = "(<input( [^>]+ | )name {0,3}= {0,3}['\"]?)([^>]*?)(['\"]?( [^>]+ | )value {0,3}= {0,3}['\"]?)(~`~`([\\d ]{1,2}[^`]*?`~`)+[\\d ]{1,2}[^`]*?`~`~)(['\"]?[^>]*/?>)|(<input( [^>]+ | )value {0,3}= {0,3}['\"]?)(~`~`([\\d ]{1,2}[^`]*?`~`)+[\\d ]{1,2}[^`]*?`~`~)(['\"]?( [^>]+ | )name {0,3}= {0,3}['\"]?)([^>]*?)(((['\" ][^>]*/?)|[/?])(>))";
	public static final String langRegP = "(?<!value {0,3}= {0,3}('|\"))(~`~`(([\\d ]{1,2}[^`]{0,2000}?`~`)+[\\d ]{1,2}[^`]{0,2000}?)`~`~).{0,2000}?";
	public static final String jsonRegP = "((\"\\w{0,50}\"):( {0,3}\"(~`~`(([\\d ]{1,2}[^`]{0,2000}?`~`)+[\\d ]{1,2}[^`]{0,2000}?)`~`~)\")([,]?))";
	public static final String LANG_CONTENT_PREFIX = "~`~`";
	public static final String LANG_CONTENT_SPLITTER1 = "`~`";
	public static final String LANG_CONTENT_SUFFIX = "`~`~";
	public static final String LANG_INPUT_PREFIX = "__multilangpre_";
	public static final String valueRegP = "~`~`(([\\d ]{1,2}[^`]{0,2000}?`~`)+[\\d ]{1,2}[^`]{0,2000}?)`~`~";
	public static final String IS_MULTILANG_SET = "is_multilang_set";
    private static String host = "";
	public static String propertyPath="";

    public static String propFileBakName = "weaver_bak";

    public GCONST() {
    }
    
    /**
     * 设置服务器地址
     *
     * @param value 服务器地址
     */
    public static void setHost(String value) {
        host = value;
    }

    /**
     * 获取服务器地址
     *
     */
    public static String getHost() {
        return host;
    }

    /**
     * 设置系统访问根目录 （为 ServletConfig config.getRealPath("/") + File.separatorChar）
     * 由 InitServer 获取后设置
     *
     * @param value 系统访问根目录
     */
    public static void setRootPath(String value) {
        ROOT_PATH = value;
    }

    /**
     * 系统应用上下文路径
     */
    public static void setContextPath(String value) {
        if (Objects.isNull(value)) return;
        if ("/".equals(value)) {
            CONTEXT_PATH = value;
            return;
        }
        if (!value.startsWith("/")) value = "/" + value;
        if (value.endsWith("/")) value = value.substring(0,value.length()-1);
        CONTEXT_PATH = value;
    }
    /**
     * 系统应用上下文路径(为了解决路径拼接问题,如果为“/”,将返回"")
     */
    public static String getContextPath() {
       if (Objects.isNull(getContextRealPath())) return "";
       if ("/".equals(getContextRealPath())) return "";
       return getContextRealPath();
    }

    /**
     * 系统应用上下文真实路径(默认为"/")
     */
    public static String getContextRealPath() {
        return CONTEXT_PATH;
    }

    /**
     * 设置系统服务名称 ，（也是默认的数据库链接池名称）
     * 由 InitServer 从APPLICATION SERVER 中设置的serverName值获得后设置
     *
     * @param value 系统服务名称
     */
    public static void setServerName(String value) {
        SERVER_NAME = value;
    }

    /**
     * 获取系统访问根目录
     *
     * @return String 系统访问根目录
     */
    public static String getRootPath() {
        return ROOT_PATH;
    }

    /**
     * 获取系统服务名称 ，（也是默认的数据库链接池名称）
     *
     * @return String 系统服务名称
     */
    public static String getServerName() {
        return SERVER_NAME;
    }

    /**
     * 获取默认配置文件名称 ，（常量 weaver）
     *
     * @return String 默认配置文件名称
     */
    public static String getConfigFile() {
        return CONFIG_FILE;
    }

    /**
     * 获取系统属性文档根目录 ，为系统访问根目录下的 prop 目录 pathtoroot/prop/
     *
     * @return String 系统属性文档根目录
     */
    public static String getPropertyPath() {
		if(propertyPath==null||"".equals(propertyPath)||propertyPath.contains("null")){
		 File f = new File(ROOT_PATH + "WEB-INF" + File.separatorChar + "prop");
			if (f.exists())
				propertyPath = ROOT_PATH + "WEB-INF" + File.separatorChar + "prop" + File.separatorChar;
			else
				propertyPath = ROOT_PATH  + "prop" + File.separatorChar;
		}
		return  propertyPath;    
    }

    /**
     * 获取系统日志文档根目录 ，为系统访问根目录下的 log 目录 pathtoroot/log/
     *
     * @return String 系统日志文档根目录
     */
    public static String getLogPath() {
        return ROOT_PATH + "log" + File.separatorChar;
    }


    /**
     * 获取系统打印模板文档根目录 ，为系统访问根目录下的 printmoudlefile 目录 pathtoroot/printmoudlefile/
     *
     * @return String 系统打印模板文档根目录
     */
    public static String getPrintMoudlePath() {
        return ROOT_PATH + "printmoudlefile" + File.separatorChar;
    }

    /**
     * 获取系统SQL文档根目录 ，为系统访问根目录下的 sql 目录 pathtoroot/sql/
     *
     * @return String 系统SQL文档根目录
     */
    public static String getSqlPath() {
        return ROOT_PATH + "sql" + File.separatorChar;
    }

    /**
     * 获取系统上传文件的存放默认根目录
     *
     * @return String 系统上传文件的存放默认根目录
     */
    public static String getSysFilePath() {
        return ROOT_PATH + "filesystem" + File.separatorChar;
    }

    /**
     * 获取系统上传文件的默认备份根目录
     *
     * @return String 系统上传文件的默认备份根目录
     */
    public static String getSysFileBackupPath() {
        return ROOT_PATH + "filesystembackup" + File.separatorChar;
    }

    /**
     * 获取系统上传文件的默认备份时间间隔(分钟)
     *
     * @return String 系统上传文件的默认备份时间间隔(分钟)
     */
    public static String getSysFileBackupTime() {
        return "60";
    }

    /**
     * 设置系统线程池
     * 由 InitServer 获取后设置
     *
     * @param ThreadPool 系统线程池
     */
    public static void setWeaverThreadPool(WeaverThreadPool ThreadPool) {
        wThreadPool = ThreadPool;
    }

    /**
     * 获取系统线程池
     *
     * @return WeaverThreadPool 系统线程池
     */
    public static WeaverThreadPool getWeaverThreadPool() {
        return wThreadPool;
    }
    //设置webservice action配置文件
    public static void setWsActionProp(Properties oprop){
    	GCONST.wsactionProp = oprop;
    }

    //设置webservice action配置文件
    public static void setESBActionProp(Properties oprop) {
        GCONST.esbactionProp = oprop;
    }
    //设置Sap action配置文件
    public static void setSapActionProp(Properties oprop){
    	GCONST.sapactionProp = oprop;
    }
    /**
     * 获取系统coremail.properties
     */
    public static void setCoremailProp(Properties oprop) {
		GCONST.coremailProp = oprop;
	}
    /**
     * 获取系统Others.properties
     *
     * @return Properties
     */
    public static void setOthersProperties(Properties oprop) {
        othersprop=oprop;
    }
    /**
     * 获取系统mutillanguageprop.properties
     *
     * @return Properties
     */
    public static void setMutilLanguageProperties(Properties oprop) {
    	mutillanguageprop=oprop;
    }
    /**
     * 获取系统dactylogram.properties
     *
     * @return Properties
     */
    public static void setDactylogramProperties(Properties oprop) {
        dactylogramProp=oprop;
    }

    /**
     * 获取系统dactylogram.properties
     *
     * @return Properties
     */
    public static void setWfFieldTriggerProperties(Properties oprop) {
        wfFieldTriggerProp=oprop;
    }


    /**
     * 获取系统workflowmonitor.properties
     *
     * @return Properties
     */
    public static void setWFMonitorProperties(Properties oprop) {
        Wfmonitorprop=oprop;
    }

    /**
     * 获取系统FreeFlow.properties
     *
     * @return Properties
     */
    public static void setFreeFlowProperties(Properties oprop) {
        freeflowProp=oprop;
    }
    
    /**
     * 获取即时通讯hrmchat.properties
     */
    public static void setHrmChatProperties(Properties oprop) {
    	hrmchatProp = oprop;
    }
    
    /**
     * 获取是否启用过滤非工作时间计算超时流程workflowovertime.properties
     */
    public static void setWfOverTimeProperties(Properties oprop) {
    	wfovertime = oprop;
    }
    
    /**
     * 获取是否启用流程出口消息提醒workflowwayout.properties
     */
    public static void setWfwayoutProperties(Properties oprop) {
    	workflowsayoutProp = oprop;
    }
    
    /**
     * 获取是否启用签到签退功能issigninorsignout.properties 已改为从数据库读取
     */
    public static void setSigninProperties(Properties oprop) {
    	RecordSet signRs = new RecordSet();
    	signRs.executeSql("select needsign, needsignhasinit from hrmkqsystemSet ");
    	int needsign = 0; 
    	int needsignhasinit =0;
    	if(signRs.next()){
    		needsign = signRs.getInt("needsign"); 
      	needsignhasinit = signRs.getInt("needsignhasinit"); 
    	}
    	if(needsignhasinit==1){
    		oprop.setProperty("ISSIGNINORSIGNOUT", ""+needsign);
    		GCONST.issigninorsignoutProp = oprop;
    	}else{
    		GCONST.issigninorsignoutProp = oprop;
    	}
    }
    
    /**
     * 获取系统是否使用多账号登陆MoreAccountLanding.properties
     */
    public static void setMoreAccountProperties(Properties oprop) {
    	GCONST.moreaccountlandingProp = oprop;
    }

    public static void setWorkflowImpProp(Properties oprop)
    {
    	GCONST.workflowimpProp = oprop;
    }
    /**
     * 设置dmlaction配置文件
     * @param oprop
     */
    public static void setDMLActionProp(Properties oprop)
    {
    	GCONST.dmlactionProp = oprop;
    }
    /**
     * 获取属性值
     * @param name
     * @return
     */
    private static String getProp(String name,Properties prop){
        if(prop!=null){
            return Util.null2String(prop.getProperty(name));
        }
        return "";
    }

    /**
     * 获得流程出口提示信息开关值 1：打开  其它：关闭
     * @return boolean
     */
    public static boolean getWorkflowWayOut() {
    	String workflowsayoutsettmp = Util.null2String(getProp(WORKFLOWWAYOUT,othersprop));
    	String workflowsayoutsettmp_1 = Util.null2String(getProp("WORKFLOWWAYOUT",workflowsayoutProp));  
    	if(!"".equals(workflowsayoutsettmp_1)) {
    		if(workflowsayoutsettmp_1.equals("1")){
                return true;
            }
    	} else {
    		if(workflowsayoutsettmp.toLowerCase().equals("t")) {
    			return true;
    		}
    	}      
        return false;
    }
    
    /**
     * 获得是否启用签到签退功能   1：打开  其它：关闭
     * @return boolean
     */
    public static String getIsSignInOrSignOut() {
    	String issignistmp = getProp("ISSIGNINORSIGNOUT",issigninorsignoutProp);
    	if("".equals(issignistmp)){
    		issignistmp = getProp("ISSIGNINORSIGNOUT",issigninorsignoutProp);
    	} else {
    		if("true".equals(issignistmp)) issignistmp = "1";
    	}
    	return issignistmp;
    }

    /**
     * 获得定时清除流程垃圾信息的周期，同时也做为消息提醒定时删除周期和保留天数。
     * @return 默认7天
     */
    public static int getWorkflowErrorDataClearTime(){
        return Util.getIntValue(getProp(WFERRORDATACLEARTIME,othersprop),7);
    }
    /**
     * 获得系统配置文件是否使用多账号登陆
     * @return 默认7天
     */
    public static boolean getMOREACCOUNTLANDING(){
    	String moreaccountlandingsettmp = getProp(MOREACCOUNTLANDING,moreaccountlandingProp);
    	if(moreaccountlandingsettmp.toLowerCase().equals("y") || moreaccountlandingsettmp.equals("1")){
            return true;
        }
    	return false;
    }
    /**
     * 获得系统配置文件是否使用字段联动
     * @return boolean
     */
    public static boolean getFIELDLINKAGE(){
    	/*String wffieldtriggersettmp = getProp("FIELDLINKAGE",wfFieldTriggerProp);
    	if(wffieldtriggersettmp.toLowerCase().equals("y") || wffieldtriggersettmp.equals("1")){
            return true;
        }*/
        return SysModuleInfoBiz.checkNonstandardStatus("020");
    }

    /**
     * 获得系统配置文件是否启用监控人可以干预流程流转
     * @return boolean
     */
    public static boolean getWorkflowIntervenorByMonitor(){
    	String wfmonitorsettmp = getProp(WORKFLOWINTERVENORBYMONITOR,Wfmonitorprop);
    	if(wfmonitorsettmp.toLowerCase().equals("y") || wfmonitorsettmp.equals("1")){
            return true;
        }
    	return false;
    }

    /**
     * 获得系统配置文件是否启用指纹登录
     * @return boolean
     */
    public static boolean getONDACTYLOGRAM(){
    	String ondactylogramsettmp = getProp("ONDACTYLOGRAM",dactylogramProp);
    	if(ondactylogramsettmp.toLowerCase().equals("y") || ondactylogramsettmp.equals("1")){
            return true;
        }
    	return false;
    }

    /**
     * 获得系统配置文件是否启用自由流程
     * @return boolean
     */
    public static boolean getFreeFlow(){
    	String freeflowsettmp = getProp("FREEFLOW",freeflowProp);
    	if(freeflowsettmp.toLowerCase().equals("y") || freeflowsettmp.equals("1")){
            return true;
        }
    	return false;
    }
    
    /**
     * 获取openfire服务器域名
     * @return String
     */
    public static String getCHATSERVER(){
    	return getProp(CHATSERVER,hrmchatProp);
    }
    
    /**
     * 获取即时通讯resource
     * @return String
     */
    public static String getCHATREESOURCE(){
    	return getProp(CHATREESOURCE,hrmchatProp);
    }
    
    /**
     * 获取是否启用过滤非工作时间计算超时流程(0是不启用,1是启用)
     * @return String
     */
    public static String getWorkFlowOverTimeTmp(){
    	return getProp(WorkFlowOverTimeTmp,wfovertime);
    }
    
    /**
     * 获取openfire服务器地址一般为防火墙地址
     * @return String
     */
    public static String getCHATHOST(){
    	return getProp(CHATHOST,hrmchatProp);
    }
    public static int getZHTWLANGUAGE(){
    	//System.out.println("ZH_TW_LANGUAGE : "+Util.getIntValue(getProp(ZH_TW_LANGUAGE,othersprop)));
        return Util.getIntValue(getProp(ZH_TW_LANGUAGE,mutillanguageprop),0);
    }
    
    /**
     * 英语语言配置属性读取 返回值 1 开启 0 关闭
     * @return
     */ 
    public static int getENLANGUAGE(){
    	//System.out.println("EN_LANGUAGE : "+Util.getIntValue(getProp(EN_LANGUAGE,mutillanguageprop)));
        return Util.getIntValue(getProp(EN_LANGUAGE,mutillanguageprop),0);
    }

    /**
     * 获取邮件和RTX提醒配置文件Reminder.properties
     *
     * @return Properties
     */
	public static void setReminderProp(Properties reminderProp) {
		GCONST.reminderProp = reminderProp;
	}
	
	/**
     * 获得系统配置文件是否启用邮件提醒地址配置
     * @return boolean
     */
    public static boolean getMailReminderSet(){
    	String mailremindsettmp = getProp(MailReminderSet,reminderProp);
    	if(mailremindsettmp.toLowerCase().equals("y") || mailremindsettmp.equals("1")){
            return true;
        }
    	return false;
    }
    
    /**
     * 获得系统配置文件是否启用RTX提醒地址配置
     * @return boolean
     */
    public static boolean getRTXReminderSet(){
    	String rtxremindsettmp = getProp(RTXReminderSet,reminderProp);
    	if(rtxremindsettmp.toLowerCase().equals("y") || rtxremindsettmp.equals("1")){
            return true;
        }
    	return false;
    }
    
    /**
     * 获得系统配置文件邮件提醒LoginPage地址
     * @return boolean
     */
    public static String getMailLoginPage(){
    	return getProp("MailLoginPage", reminderProp);
    }
    
    /**
     * 获得系统配置文件邮件提醒GotoPage地址
     * @return boolean
     */
    public static String getMailGotoPage(){
    	return getProp("MailGotoPage", reminderProp);
    }
    
    /**
     * 获得系统配置文件RTX提醒LoginPage地址
     * @return boolean
     */
    public static String getVerifyRTXLoginPage(){
    	return getProp("VerifyRTXLoginPage", reminderProp);
    }
    
    /**
     * 获得系统配置文件RTX提醒GotoPage地址
     * @return boolean
     */
    public static String getVerifyRTXGotoPage(){
    	return getProp("VerifyRTXGotoPage", reminderProp);
    }
    /**
     * 获取是否开启流程导入导出开关
     * @return
     */
    public static boolean isWorkflowIsOpenIOrE()
    {
    	return Util.null2String(getProp("wf_e_i_isopen", workflowimpProp)).equals("1");
    }
    /**
     * 获取是否启用dmlaction配置，1为启用
     * @return
     */
    public static boolean isDMLAction()
    {
    	return Util.null2String(getProp("isdmlaction", dmlactionProp)).equals("1");
    }
    
    /**
     * 初始化流程暂停撤消配置文件
     * @param oprop
     */
    public static void setWorkflowStopOrCancel(Properties oprop) {
    	workflowstoporcancelProp=oprop;
    }
    
    /**
     * 获得是否启用流程暂停撤销功能   1：打开  其它：关闭
     * @return boolean
     */
    public static String getWorkflowStopOrCancel() {
    	return SysModuleInfoBiz.checkNonstandardStatus("045") ? "1" : "0";
    }


    /**
     * 获得是否启用文档比对功能   1：打开  其它：关闭
     * @return boolean
     */
    public static String getDocCompare() {
//        return SysModuleInfoBiz.checkNonstandardStatus("") ? "0" : "1";
        CompareUtil compareUtil = new CompareUtil();
        boolean isopen  = compareUtil.isOpen();
        if(isopen){
            return "1";
        }
        return "0";
    }

    /**
     * 初始化流程明细表通过EXCEL导入配置文件
     * @param oprop
     */
    public static void setWorkflowImportDetail(Properties oprop) {
    	workflowimportdetailProp=oprop;
    }
    
    /**
     * 获得是否启用流程明细表通过EXCEL导入销功能   1：打开  其它：关闭
     * @return boolean
     */
    public static String getWorkflowImportDetail() {
    	return getProp("WORKFLOWIMPORTDETAIL",workflowimportdetailProp);
    }
    
    /**
     * 初始化是否启用流程特批件设置配置文件
     * @param oprop
     */
    public static void setWorkflowSpecialApproval(Properties oprop) {
    	workflowspecialApprovalProp=oprop;
    }
    
    /**
     * 获得是否启用流程特批件设置功能   1：打开  其它：关闭
     * @return boolean
     */
    public static String getWorkflowSpecialApproval() {
    	return getProp("WORKFLOWSPECIALAPPROVAL",workflowspecialApprovalProp);
    }
    
    /**
     * 初始化是否启用流程特批件设置配置文件
     * @param oprop
     */
    public static void setWorkflowReturnNode(Properties oprop) {
    	workflowreturnnodeProp=oprop;
    }
    
    /**
     * 获得是否启用流程特批件设置功能   1：打开  其它：关闭
     * @return boolean
     */
    public static String getWorkflowReturnNode() {
    	return getProp("WORKFLOWRETURNNODE",workflowreturnnodeProp);
    }
    
    /**
     * 初始化是否启用非IE浏览器下系统设置配置文件
     * @param oprop
     */
    public static void setSystemSettingMenu(Properties oprop) {
    	systemSettingMenuProp=oprop;
    }
    
    /**
     * 获得是否启用非IE浏览器下系统设置功能   1：打开  其它：关闭
     * @return boolean
     */
    public static String getSystemSettingMenu() {
    	return getProp("SYSTEMSETTINGMENU",systemSettingMenuProp);
    }
    
    /**
     * 初始化是否启用软件模板、网站模板配置文件
     * @param oprop
     */
    public static void setSystemThemeTemplate(Properties oprop) {
    	systemThemeTemplateProp=oprop;
    }
    
    /**
     * 获得是否启用软件模板、网站模板配置文件   1：打开  其它：关闭
     * @return boolean
     */
    public static String getsystemThemeTemplate() {
    	return getProp("isOpenSoftAndSiteTempate",systemThemeTemplateProp);
    }
    /**
     * 获取是否启用WebService action配置，1为启用
     * @return
     */
    public static boolean isWsAction(){
    	return Util.null2String(getProp("iswsaction", wsactionProp)).equals("1");
    }

    /**
     * 获取是否启用WebService action配置，1为启用
     * @return
     */
    public static boolean isESBAction() {
        return Util.null2String(getProp("isesbaction", esbactionProp)).equals("1");
    }
    /**
     * 获取是否启用Sap action配置，1为启用
     * @return
     */
    public static boolean isSapAction(){
    	return Util.null2String(getProp("issapaction", sapactionProp)).equals("1");
    }
	/**
     * 获取CoreMail邮件系统新建账户默认密码
     * @return String
     */
    public static String getDefaultPassword() {
    	return getProp("DEFAULTPASSWORD", coremailProp);
    }
}