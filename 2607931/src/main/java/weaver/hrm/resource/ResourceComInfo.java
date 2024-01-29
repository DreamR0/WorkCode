package weaver.hrm.resource;

import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2607931.util.CheckUtil;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.encrypt.biz.EncryptFieldViewScopeConfigComInfo;
import com.engine.kq.biz.KQGroupMemberComInfo;
import org.apache.commons.lang3.StringUtils;

import net.sf.json.JSONArray;
import com.alibaba.fastjson.JSONObject;
import weaver.cache.*;
import weaver.common.StringUtil;
import weaver.conn.*;
import weaver.file.*;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.cachecenter.bean.KVResourceComInfo;
import weaver.hrm.cachecenter.bean.RolemembersComInfo;
import weaver.hrm.common.Tools;
import weaver.hrm.privacy.PrivacyComInfo;
import weaver.hrm.privacy.UserPrivacyComInfo;
import weaver.hrm.tools.Time;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import com.engine.encrypt.biz.DecryptResourceComInfo;
import weaver.hrm.companyvirtual.ResourceVirtualComInfo;
import weaver.systeminfo.SystemEnv;
import weaver.general.ThreadVarManager;
import com.api.system.language.biz.TransLatorComInfo;
/**
 * Title:        cobusiness
 * Description:
 * Copyright:    Copyright (c) 2002
 * Company:      weaver
 *
 * @author liuyu
 * @version 1.0
 */
import weaver.systeminfo.setting.HrmUserIconSettingComInfo;
import weaver.systeminfo.setting.HrmUserSettingComInfo;
import weaver.systeminfo.systemright.CheckUserRight;

/**
 * 人力资源基本信息缓存类
 */
public class ResourceComInfo extends CacheBase {
    String sql = "";
    LogMan lm = LogMan.getInstance();

    // 需要定义的信息 begin
    protected static String TABLE_NAME = "HrmResource";
    /**
     * sql中的where信息，不要以where开始
     */
    protected static String TABLE_WHERE = null;
    /**
     * sql中的order by信息，不要以order by开始
     */
    protected static String TABLE_ORDER = "dsporder asc,id asc";

  	@PKColumn(type = CacheColumnType.NUMBER)
  	protected static String PK_NAME = "id";

  	@CacheColumn(name = "loginid")
  	protected static int loginid;//账号
  	@CacheColumn(name = "workcode")
  	protected static int workcode;//编号
    @CacheColumn(name = "lastname")
    protected static int lastname;//名字
    @CacheColumn(name = "pinyinlastname")
    protected static int pinyinlastname;//拼音名字
    @CacheColumn(name = "sex")
    protected static int sex;//性别
  	@CacheColumn(name = "email")
  	protected static int email;//email
  	@CacheColumn(name = "resourcetype")
  	protected static int resourcetype;//人员类别
  	@CacheColumn(name = "locationid")
  	protected static int locationid;// 工作地点
  	@CacheColumn(name = "departmentid")
  	protected static int departmentid;//部门
  	@CacheColumn(name = "subcompanyid1")
  	protected static int subcompanyid;//分部
  	@CacheColumn(name = "costcenterid")
  	protected static int costcenterid;//成本中心（未使用）
  	@CacheColumn(name = "jobtitle")
  	protected static int jobtitleid;//岗位
  	@CacheColumn(name = "managerid")
  	protected static int managerid;//上级
  	@CacheColumn(name = "assistantid")
  	protected static int assistantid;//助理
  	@CacheColumn(name = "joblevel")
  	protected static int joblevel;//职级
  	@CacheColumn(name = "seclevel")
  	protected static int seclevel;//安全级别
  	@CacheColumn(name = "status")
  	protected static int status;//状态
  	@CacheColumn(name = "account")
  	protected static int account;//账号（用于ldap）
  	@CacheColumn(name = "mobile")
  	protected static int mobile;//手 机
  	@CacheColumn(name = "mobileshowtype")
  	protected static int mobileshowtype;//手 机显示类型
  	@CacheColumn(name = "password")
  	protected static int pwd;//口令
  	@CacheColumn(name = "systemLanguage")
  	protected static int systemLanguage;//语言
  	@CacheColumn(name = "telephone")
  	protected static int telephone;//电话
  	@CacheColumn(name = "managerstr")
  	protected static int managerstr;//所有上级
    @CacheColumn(name = "messagerurl")
    protected static int messagerurl;//小照片路径
    //pluginResourceCominfo中移到这里
    @CacheColumn(name = "accounttype")
    protected static int accounttype;
    @CacheColumn(name = "belongto")
    protected static int belongto;
    @CacheColumn(name = "createdate")
    protected static int createdate;
    @CacheColumn(name = "resourceimageid")
    protected static int resourceimageid;
    @CacheColumn(name = "classification")
    protected static int classification;
    @CacheColumn(name = "encKey")
    protected static int encKey;
    @CacheColumn(name = "crc")
    protected static int crc;
    @CacheColumn(name = "companyStartDate")
    protected static int companyStartDate;
    @CacheColumn(name = "workStartDate")
    protected static int workStartDate;
    @CacheColumn(name = "enddate")
    protected static int enddate;
    @CacheColumn(name = "jobcall")
    protected static int jobCall;
    @CacheColumn(name = "mobilecall")
    protected static int mobileCall;
    @CacheColumn(name = "fax")
    protected static int fax;
    @CacheColumn(name = "jobactivitydesc")
    protected static int jobactivitydesc;
    @CacheColumn(name = "workroom")
    protected static int workroom;
    @CacheColumn(name = "dsporder")
    protected static int dsporder;


    @CacheColumn(name = "workyear")//工龄
    protected static int workyear;

    @CacheColumn(name = "companyworkyear")//司龄
    protected static int companyworkyear;

    @CacheColumn(name = "birthday")//生日
    protected static int birthday;

    @CacheColumn(name = "educationlevel")//学历
    protected static int educationlevel;

    public String getWorkYear(){
        return ((String) (getRowValue(workyear))).trim();
    }

    public String getWorkYear(String key) {
        return ((String) getValue(workyear, key)).trim();
    }

    public String getCompanyWorkYear(){
        return ((String) (getRowValue(companyworkyear))).trim();
    }

    public String getCompanyWorkYear(String key) {
        return ((String) getValue(companyworkyear, key)).trim();
    }

    public String getBirthday(){
        return ((String) (getRowValue(birthday))).trim();
    }

    public String getBirthday(String key) {
        return ((String) getValue(birthday, key)).trim();
    }

    public String getEducationlevel(){
        return ((String) (getRowValue(educationlevel))).trim();
    }

    public String getEducationlevel(String key) {
        return ((String) getValue(educationlevel, key)).trim();
    }



    @CacheColumn(name = "isAdmin")
    protected static int isAdmin;

  	    // 需要定义的信息 end
	public ResourceComInfo() throws Exception {
    	ThreadVarManager.setHasMultiFilter(true);
    }

	public ResourceComInfo(boolean tryCatched){
		try{
			ThreadVarManager.setHasMultiFilter(true);
		}catch (Exception e) {
			throw new RuntimeException(e) ;
		}
	}
    public CacheMap initCache() throws Exception {
        CacheMap localData = createCacheMap();

        RecordSet rs = new RecordSet();
        String sql = " select a.*, 0 as isAdmin from hrmresource a order by a.dsporder asc, a.id asc";
        rs.executeSql(sql);
        while (rs.next()) {
            String id = Util.null2String(rs.getString(PK_NAME));
            CacheItem row = createCacheItem();
            parseResultSetToCacheItem(rs,  row,"hrmresource");
            // 这里需要记得调用这个 转化方法， 否则 这些自己加载的数据就不能正确转化，当然你也可以直接把转化逻辑写在这里，不过这样先让代码重复了
            modifyCacheItem(id, row);
            localData.put(id, row);
        }
        return localData;
    }

    public CacheItem initCache(String key) {
        if (Util.getIntValue(key) <= 0) {
            return null;
        }

        boolean isHrmResource = false;
        CacheItem cacheItem = null;
        RecordSet rs = new RecordSet();
        String sql = " select a.*, 0 as isAdmin from hrmresource a where id=" + key + " order by a.dsporder asc, a.id asc";
        rs.executeSql(sql);
        if (rs.next()) {
            cacheItem = createCacheItem();
            parseResultSetToCacheItem(rs, cacheItem,"hrmresource");
            modifyCacheItem(key, cacheItem);
            isHrmResource = true;
        }

        if (!isHrmResource) {
            sql = "SELECT id,loginid,'' AS workcode,lastname,'' as pinyinlastname ,'' AS sex, '' AS email,'' AS resourcetype,0 AS locationid,0 AS departmentid,0 AS subcompanyid1, 0 AS costcenterid,0 AS jobtitle,0 AS managerid,0 AS assistantid,0 AS joblevel,seclevel,status,'' AS account,mobile,password,systemLanguage, '' AS telephone,'' AS managerstr,'' AS messagerurl , id AS dsporder,0 AS accounttype, 0 AS belongto, 0 as mobileshowtype,creater,created,modified,modifier,tokenKey, '' AS createdate ,0 AS resourceimageid,9999 AS classification,'' AS encKey,'' AS crc,'' as enddate,1 as isAdmin FROM HrmResourceManager where id=" + key;
            rs.executeSql(sql);
            if (rs.next()) {
                cacheItem = createCacheItem();
                parseResultSetToCacheItem(rs, cacheItem,"HrmResourceManager");
                modifyCacheItem(key, cacheItem);
            }
        }
        return cacheItem;
    }

    /**
     * 获取缓存纪录数目
     *
     * @return
     */
    public int getResourceNum() {
        return size();
    }

    /**
     * 判断缓存游标是否到达终点
     *
     * @return
     */
  	public boolean next() {
  		while (super.next()) {
  			if ("sysadmin".equals(getRowValue(loginid))) {
  				continue;
  			}
  			return true;
  		}
  		return false;
  	}


    /**
     * 获取人员id
     *
     * @return
     */
    public String getResourceid() {
        return (String) getRowValue(PK_INDEX);
    }

    /**
     * 获取人员名
     *
     * @return
     */
    public String getResourcename() {
        return ((String) (getRowValue(lastname))).trim();
    }

    /**
     * 获取姓
     *
     * @return
     */
    public String getFirstname() {
        return "";
    }

    /**
     * 获取账号
     *
     * @return
     */
    public String getLoginID() {
    	return ((String) (getRowValue(loginid))).trim();
    }
    
    /**
     * 获取编号
     *
     * @return
     */
    public String getWorkcode() {
    	return ((String) (getRowValue(workcode))).trim();
    }
    
    /**
     * 获取密码
     *
     * @return
     */
    public String getPWD() {
    	return ((String) (getRowValue(pwd))).trim();
    }  

    /**
     * 获取人员名
     *
     * @return
     */
    public String getLastname() {
    	return ((String) (getRowValue(lastname))).trim();
    }

    /**
     * 获取人员姓名拼音
     *
     * @return
     */
    public String getPinyinlastname() {
        return ((String) (getRowValue(pinyinlastname))).trim();
    }


    /**
     * 获取性别
     *
     * @return
     */
    public String getSex() {
    	return ((String) (getRowValue(sex))).trim();
    }

    /**
     * 获取邮件
     *
     * @return
     */
    public String getEmail() {
    	return ((String) (getRowValue(email))).trim();
    }

    /**
     * 获取人员类型
     *
     * @return
     */
    public String getResourcetype() {
    	return ((String) (getRowValue(resourcetype))).trim();
    }

    /**
     * 获取工作地点
     *
     * @return
     */
    public String getLocation() {
    	return ((String) (getRowValue(locationid))).trim();
    }

/*  public String getWorkroom(){
    return ((String)(workrooms.get(current_index))).trim() ;
  }  */

    /**
     * 获取部门id
     *
     * @return
     */
    public String getDepartmentID() {
    	return (""+Util.getIntValue((String) (getRowValue(departmentid)),0)).trim();
    }

    /**
     * 获取分部id
     *
     * @return
     */
    public String getSubCompanyID() {
    	return (""+Util.getIntValue((String) (getRowValue(subcompanyid)),0)).trim();
    }

    /**
     * 获取成本中心id
     *
     * @return
     */
    public String getCostcenterID() {
    	return (""+Util.getIntValue((String) (getRowValue(costcenterid)),0)).trim();
    }

    /**
     * 获取岗位
     *
     * @return
     */
    public String getJobTitle() {
    	return ((String) (getRowValue(jobtitleid))).trim();
    }

    /**
     * 获取上级id
     *
     * @return
     */
    public String getManagerID() {
    	return ((String) (getRowValue(managerid))).trim();
    }

	/**
     * 获取所有上级id
     *
     * @return
     */
    public String getManagersIDs() {
    	return ((String) (getRowValue(managerstr))).trim();
    }

    public String getMessagerUrls() {
        String str = ((String) (getRowValue(messagerurl))).trim();
        ;
        if ("".equals(str)) {
            if (((String) (getRowValue(sex))).trim().equals("1"))
                str = "/messager/images/icon_w_wev8.jpg";
            else
                str = "/messager/images/icon_m_wev8.jpg";
        }
        return str;
    }

		/**
     *  由关键字获取所有上级
     *
     * @param key  关键字
     * @return
     */
    public String getManagersIDs(String key) {
    	return ((String) (getValue(managerstr,key))).trim();
    }

    public String getMessagerUrls(String key) {
        String str=((String) (getValue(messagerurl,key))).trim();
        if("".equals(str)){
            int resourceimageid = Util.getIntValue(this.getResourceimageid(key));
            if (resourceimageid > 0) {
                str = "/weaver/weaver.file.FileDownload?fileid=" + resourceimageid;
            } else {
                if(((String) (getValue(sex,key))).trim().equals("1"))
                    str="/messager/images/icon_w_wev8.jpg";
                else
                    str="/messager/images/icon_m_wev8.jpg";
            }
        }
        return str;

    }

    public String getUserIconInfoStr(String userId,User user){
        return JSONObject.toJSONString(getUserIconInfo(userId,user));
    }

    public Map<String, Object> getUserIconInfo(String userId,User user){
        Map<String, Object> retmap = new HashMap<String, Object>();
        try{
            ResourceComInfo rci = new ResourceComInfo();
            HrmUserIconSettingComInfo hic = new HrmUserIconSettingComInfo();
            boolean isDefault = false;
            String headformat = hic.getHeadformat();  //头像显示方式。 1：显示姓名首字  2：显示固定图片  3：显示姓名后两字
            String background = "";  //头像背景颜色
            String fontcolor = "";  //头像显示字体颜色
            String messagerurl = "";
            String gender = "";
            String lastname = "";
            String shortname = "";
            String messagerurltemp = "";
            String defaultmessagerurl = "";  //用于头像加载失败的情况下取默认设置
            messagerurl=rci.getMessagerUrls(userId);
            gender = StringUtil.vString(rci.getSexs(userId).trim(),"0");
            lastname = Util.null2String(Util.formatMultiLang(rci.getLastname(userId), user.getLanguage()+""));
            shortname = User.getLastname(lastname);
            /**
             * 组件逻辑：
             * 有头像展示头像，头像加载失败，根据默认设置进行展示
             * 无头像，直接取默认设置进行展示
             */
            if (messagerurl.indexOf("icon_w_wev8.jpg") > -1 || messagerurl.indexOf("icon_m_wev8.jpg") > -1 || messagerurl.indexOf("dummyContact.png") > -1) {
                messagerurl = "";
            }
            if (!userId.equals("-99")) {
                isDefault = true;
                if (gender.equals("1")) {
                    background = hic.getFheadbackcolor();
                    fontcolor = hic.getFheadfontcolor();
                    messagerurltemp = hic.getFmessagerurl();
                } else {
                    background = hic.getMheadbackcolor();
                    fontcolor = hic.getMheadfontcolor();
                    messagerurltemp = hic.getMmessagerurl();
                }
            } else {
                //协作添加匿名头像逻辑
                isDefault = true;
                gender = "0";  //匿名头像默认取男性得配置
                background = hic.getMheadbackcolor();
                fontcolor = hic.getMheadfontcolor();
                lastname = Util.null2String(SystemEnv.getHtmlLabelName(18611, user.getLanguage()));   //匿名
                shortname = User.getLastname(lastname, Util.getIntValue(headformat));
                messagerurltemp = "/cloudstore/resource/pc/com/images/anomous.png";   //匿名头像路径
            }
            if("2".equals(headformat)){
                headformat = "0";  //展示默认图片
                if(messagerurl.length()==0){
                    messagerurl = messagerurltemp;
                }
                if (gender.equals("1")) {
                    defaultmessagerurl = hic.getFmessagerurl();
                } else {
                    defaultmessagerurl = hic.getMmessagerurl();
                }
            }else {
                if ("3".equals(headformat)){
                    headformat = "2";  //展示姓名后两字
                }
            }
            retmap.put("isDefault",isDefault);
            retmap.put("headformat",headformat);
            retmap.put("gender",gender);
            retmap.put("background",background);
            retmap.put("fontcolor",fontcolor);
            retmap.put("lastname",lastname);
            retmap.put("shortname",shortname);
            retmap.put("messagerurl",messagerurl);
            retmap.put("defaultmessagerurl",defaultmessagerurl);
        }catch (Exception e){
            writeLog(e);
        }
        return retmap;
    }

    /**
     * 获取助理id
     *
     * @return
     */
    public String getAssistantID() {
    	return ((String) (getRowValue(assistantid))).trim();
    }

    /**
     * 获取职级
     *
     * @return
     */
    public String getJoblevel() {
    	return ((String) (getRowValue(joblevel))).trim();
    }

    /**
     * 获取安全级别
     *
     * @return
     */
    public String getSeclevel() {

        //return ((String) seclevels.get(current_index)).trim();
    	String tempseclevel=((String) (getRowValue(seclevel))).trim();
    	if(tempseclevel==null||tempseclevel.trim().equals("")){
    		tempseclevel="0";
    	}
    	return tempseclevel;

    }

    /**
     * 获取安全级别
     *
     * @return
     */
    public String getSeclevelTwo() {
        return ((String) (getRowValue(seclevel))).trim();

    }

    /**
     * 获取状态
     *
     * @return
     */
    public String getStatus() {

    	return ((String) (getRowValue(status))).trim();

    }

    /**
     * 获取账号
     *
     * @return
     */
    public String getAccount() {

    	return ((String) (getRowValue(account))).trim();

    }

    /**
     * 获取手机号
     *
     * @return
     */
    public String getMobile() {

    	return ((String) (getRowValue(mobile))).trim();

    }
    
    /**
     * 获取手机显示类型
     *
     * @return
     */
    @Deprecated
    public String getMobileshowtype() {

    	return ((String) (getRowValue(mobileshowtype))).trim();

    }
    
    /**
     * 得到用户语言种类
     *
     * @return
     */
    public String getSystemLanguage() {
        //return ((String) (getRowValue(systemLanguage))).trim();
        String  systemLanguage = User.getUserLang(Util.getIntValue(getResourceid()))+"";
        return systemLanguage;
    }

	/**
     * 得到用户电话
     *
     * @return
     */
    public String getTelephone() {
    	return ((String) (getRowValue(telephone))).trim();
    }

    /**
     * 由关键字获取人员名
     *
     * @param key  关键字
     * @return
     */
    public String getResourcename(String key) {
        return getResourcename(key, "");
    }
    public String getResourcename(String key,String type) {
        if (type.contains("1024")){
            //qczj
            CheckUtil checkUtil = new CheckUtil();
            String reportName = "";//考勤报表名称
            String resourceType = "";//人力资源类型
            String reportId = "";//id
            String safeLevel = "";
            if (type.indexOf("+")>-1){
                String[] split = type.split("\\+");
                resourceType = Util.null2s(split[0].trim(), "");
                reportName = Util.null2s(split[1].trim(), "");
                reportId = Util.null2s(split[2].trim(), "");
                safeLevel = checkUtil.getSaveLevel(reportName,resourceType,reportId);
                return "安全级别("+safeLevel+")";
            }
            return "安全级别";
        }else {
            return StringUtil.replace(((String) getValue(lastname, key)).trim(), ",", "，");
        }

    }

    /**
     * 由关键字获取人员名
     *
     * @param key 关键字
     * @return
     */
    public String filterResourcename(String key, String keyfield) {
        if ("".equals(keyfield) || "3".equals(keyfield))
            return StringUtil.replace(((String) getValue(lastname, key)).trim(), ",", "，");
        int colName = 0;
        switch (keyfield) {
            case "1":
                colName = workcode;
                break;
            case "2":
                colName = loginid;
                break;
        }
        return StringUtil.replace(((String) getValue(colName, key)).trim(), ",", "，");
    }

    /**
     * 由关键字获取人员姓
     *
     * @param key   关键字
     * @return
     */
    public String getFirstname(String key) {
        return "";
    }

    /**
     *  由关键字获取账号
     *
     * @param key  关键字
     * @return
     */
    public String getLoginID(String key) {
    	return ((String) getValue(loginid, key)).trim();
    }
    
    /**
     *  由关键字获取编号
     *
     * @param key  关键字
     * @return
     */
    public String getWorkcode(String key) {
    	return ((String) getValue(workcode, key)).trim();
    }

    /**
     * 由关键字获取人员名
     *
     * @param key   关键字
     * @return
     */
    public String getLastname(String key) {
    	return ((String) getValue(lastname, key)).trim();
    }


    /**
     * 获取人员姓名拼音
     *
     * @param key 关键字
     * @return
     */
    public String getPinyinlastname(String key) {
        return ((String) getValue(pinyinlastname, key)).trim();
    }

    /**
     * 由关键字获取口令
     *
     * @param key   关键字
     * @return
     */
    public String getPWD(String key) {
    	return ((String) getValue(pwd, key)).trim();
    }

    /**
     *  由关键字获取性别
     *
     * @param key   关键字
     * @return
     */
    public String getSexs(String key) {
    	return ((String) getValue(sex, key)).trim();
    }

    /**
     *  由关键字获取email
     *
     * @param key  关键字
     * @return
     */
    public String getEmail(String key) {
    	return ((String) getValue(email, key)).trim();
    }
    
    /**
     * added by cyril on 2008-11-05
     *  根据email获得lastname
     *
     * @param key  关键字
     * @return
     */
    public String getLastnameByEmail(String key) {
    	RecordSet rs = new RecordSet();
    	String lastname = "";
    	rs.executeSql("select lastname from hrmresource where email='"+key+"'");
        	if(rs.next()){
        		lastname = Util.null2String(rs.getString("lastname"));
        	}
    	return lastname;
    }

    /**
     *  由关键字获取人员类型
     *
     * @param key   关键字
     * @return
     */
    public String getResourcetype(String key) {
    	return ((String) getValue(resourcetype, key)).trim();
    }

    /**
     * 由关键字获取工作地点
     *
     * @param key  关键字
     * @return
     */
    public String getLocationid(String key) {
    	return ((String) getValue(locationid, key)).trim();
    }

/*  public String getWorkroom(String key)
  {
    int index=ids.indexOf(key);
    if(index!=-1)
      return ((String)workrooms.get(index)).trim() ;
    else
      return "";
  }   */

    /**
     *  由关键字获取部门id
     *
     * @param key   关键字
     * @return
     */
    public String getDepartmentID(String key) {
    	return (""+Util.getIntValue((String) getValue(departmentid, key),0)).trim();
    }

    /**
     *  由关键字获取分部id
     *
     * @param key   关键字
     * @return
     */
    public String getSubCompanyID(String key) {
    	return (""+Util.getIntValue((String) getValue(subcompanyid, key),0)).trim();
    }

    /**
     *  由关键字获取成本中心
     *
     * @param key   关键字
     * @return
     */
    public String getCostcenterID(String key) {
    	return (""+Util.getIntValue((String) getValue(costcenterid, key),0)).trim();
    }

    /**
     *  由关键字获取岗位
     *
     * @param key  关键字
     * @return
     */
    public String getJobTitle(String key) {
    	return ((String) getValue(jobtitleid, key)).trim();
    }

    /**
     *  由关键字获取上级
     *
     * @param key  关键字
     * @return
     */
    public String getManagerID(String key) {
    	return ((String) getValue(managerid, key)).trim();
    }

    /**
     * 由关键字获取助理
     *
     * @param key  关键字
     * @return
     */
    public String getAssistantID(String key) {
    	return ((String) getValue(assistantid, key)).trim();
    }

    /**
     * 由关键字获取职级
     *
     * @param key  关键字
     * @return
     */
    public String getJoblevel(String key) {
    	return ((String) getValue(joblevel, key)).trim();
    }

    /**
     * 由关键字获取安全级别
     *
     * @param key  关键字
     * @return
     */
    public String getSeclevel(String key) {
    	String tempseclevel=((String) getValue(seclevel, key)).trim();
    	if(tempseclevel==null||tempseclevel.trim().equals("")){
    		tempseclevel="0";
    	}
    	return tempseclevel;   
    }

    /**
     * 由关键字获取安全级别
     *
     * @param key  关键字
     * @return
     */
    public String getSeclevelTwo(String key) {
        return ((String) getValue(seclevel, key)).trim();
    }

    /**
     * 由关键字获取状态
     *
     * @param key 关键字
     * @return
     */
    public String getStatus(String key) {
    	return ((String) getValue(status, key)).trim();
    }

    /**
     * 由关键字获取账号（ldap）
     *
     * @param key 关键字
     * @return
     */
    public String getAccount(String key) {
    	return ((String) getValue(account, key)).trim();
    }

    /**
     *   由关键字获取手机号
     *
     * @param key    关键字
     * @return
     */
    public String getMobile(String key) {
    	return ((String) getValue(mobile, key)).trim();
    }
    
    /**
     *   由关键字获取手机显示类型
     *
     * E9此方法已经屏蔽，不要使用
     * @param key    关键字
     * @return
     */
    @Deprecated
    public String getMobileshowtype(String key) {
    	return ((String) getValue(mobileshowtype, key)).trim();
    }
    
    /**
     * 由关键字得到语言种类
     *
     * @param key  关键字
     * @return
     */
    public String getSystemLanguage(String key) {
    	String  systemLanguage = User.getUserLang(Util.getIntValue(key))+"";
        return systemLanguage;
    }


	/**
     * 由关键字得到电话号码
     *
     * @param key  关键字
     * @return
     */
    public String getTelephone(String key) {
    	return ((String) getValue(telephone, key)).trim();
    }

    public String getAccountType(String key) {
    	return ((String) getValue(accounttype, key)).trim();
    }

	public String getBelongTo(String key) {
		return ((String) getValue(belongto, key)).trim();
	}
  
    /**
		 * 由多个人员id(逗号分隔)得到人员姓名字符串
		 * 
		 * @param ids
		 * @return
		 */
    public String getMulResourcename(String ids) {
        String names = "";
        String temp = "";
        ids += ",";
        for (int i = 0; i < ids.length(); i++) {
            if (ids.charAt(i) != ',') {
                temp += ids.charAt(i);
            } else {
                names += " " + getResourcename(temp);
                temp = "";
            }
        }
        return names;
    }

    /**
     * 由多个人员id(逗号分隔)得到人员姓名字符串(html格式 )
     *
     * @param ids
     * @return
     */
    public String getMulResourcename1(String ids) {
    	return getMulResourcename1(ids,"&nbsp;&nbsp;");
    }
    
    public String getMulResourcename1(String ids,String splitFlag) {
        String names = "";
        String temp = "";
        ids += ",";
        for (int i = 0; i < ids.length(); i++) {
            if (ids.charAt(i) != ',') {
                temp += ids.charAt(i);
            } else {
            	if("".equals(temp)) continue;
            	if(names.equals("")){
            		names = "<a href=\"javascript:openhrm('"+temp+"');\" onclick='pointerXY(event);'>"+getResourcename(temp)+"</a>";
            	}else{
            		names += splitFlag + "<a href=\"javascript:openhrm('"+temp+"');\" onclick='pointerXY(event);'>"+getResourcename(temp)+"</a>";
            	}
                temp = "";
            }
        }
        return names;
    }

    /**
     * 由多个人员id（逗号分隔）得到人员姓名字符串（thml格式）
     *
     * @param ids
     * @return
     */
    public String getMulResourcename2(String ids) { 
        String names = "";
        String temp = "";
        ids += ",";
        for (int i = 0; i < ids.length(); i++) {
            if (ids.charAt(i) != ',') {
                temp += ids.charAt(i);
            } else {
            	if("".equals(temp)) continue;
                names += "<a href='javascript:void(0)' onclick=openFullWindowForXtable('/hrm/resource/HrmResource.jsp?id="+temp+"')>"+getResourcename(temp)+"</a>&nbsp;";
                temp = "";
            }
        }
        return names;
    }
    
    /**
     * 判断user1是否是user2的经理
     *
     * @param userid   user1
     * @param id     user2
     * @return
     */
    public boolean isManager(int userid, String id) {
        RecordSet rs = new RecordSet();
        sql = "select managerstr from HrmResource where id = '" + id + "'";
        rs.executeSql(sql);
        while (rs.next()) {
            String managerstr = Util.null2String(rs.getString("managerstr"));
            String temp = "";
            managerstr += ",";
            for (int i = 0; i < managerstr.length(); i++) {
                if (managerstr.charAt(i) != ',') {
                    temp += managerstr.charAt(i);
                } else {
                    if (temp.equals("" + userid)) {
                        return true;
                    }
                    temp = "";
                }
            }
        }
        return false;
    }

    /**
     * 判断user1是否能查看user2的系统信息
     *
     * @param userid   user1
     * @param id    user2
     * @return
     */
    public boolean isSysInfoView(int userid, String id) {
        RecordSet rs = new RecordSet();
        sql = "select hrmid from HrmInfoMaintenance where id = 1";
        rs.executeSql(sql);
        int hrmid = 0;
        while (rs.next()) {
            hrmid = Util.getIntValue(rs.getString("hrmid"));
        }
        if (userid == hrmid) {
            return true;
        }
        if (isNewResource(userid) && isSuperviser(userid, id)) {
            return true;
        }
        return false;
    }

    /**
     * 判断user1是否能查看user2的系统信息
     *
     * @param userid user1
     * @param id     user2
     * @return
     */
    public boolean isSysInfoView2(int userid, String id) {
        RecordSet rs = new RecordSet();
        sql = "select hrmid,hrmids from HrmInfoMaintenance where id = 1";
        rs.executeSql(sql);
        int hrmid = 0;
        String hrmids = "";
        while (rs.next()) {
            hrmid = Util.getIntValue(rs.getString("hrmid"));
            hrmids = Util.null2String(rs.getString("hrmids"));
        }
        if (userid == hrmid) {
            return true;
        }
        String[] strings = hrmids.split(",");
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].equals(userid + "")) {
                return true;
            }
        }
        if (isNewResource(userid) && isSuperviser(userid, id)) {
            return true;
        }
        return false;
    }


    /**
     * 判断user1是否能查看user2的财务信息
     *
     * @param userid  user1
     * @param id       user2
     * @return
     */
    public boolean isFinInfoView(int userid, String id) {
        RecordSet rs = new RecordSet();
        sql = "select hrmid from HrmInfoMaintenance where id = 2";
        rs.executeSql(sql);
        int hrmid = 0;
        while (rs.next()) {
            hrmid = Util.getIntValue(rs.getString("hrmid"));
        }
        if (userid == hrmid) {
            return true;
        }
        if (isNewResource(userid) && isSuperviser(userid, id)) {
            return true;
        }
        return false;
    }


    /**
     * 判断user1是否能查看user2的财务信息
     *
     * @param userid user1
     * @param id     user2
     * @return
     */
    @SuppressWarnings("deprecation")
    public boolean isFinInfoView2(int userid, String id) {
        RecordSet rs = new RecordSet();
        sql = "select hrmid,hrmids from HrmInfoMaintenance where id = 2";
        rs.executeSql(sql);
        int hrmid = 0;
        String hrmids = "";
        while (rs.next()) {
            hrmid = Util.getIntValue(rs.getString("hrmid"));
            hrmids = Util.null2String(rs.getString("hrmids"));
        }
        if (userid == hrmid) {
            return true;
        }
        String[] strings = hrmids.split(",");
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].equals(userid + "")) {
                return true;
            }
        }
        if (isNewResource(userid) && isSuperviser(userid, id)) {
            return true;
        }
        return false;
    }


    /**
     *  判断user1是否能查看user2的资产信息
     *
     * @param userid   user1
     * @param id     user2
     * @return
     */
    public boolean isCapInfoView(int userid, String id) {
        RecordSet rs = new RecordSet();
        sql = "select hrmid from HrmInfoMaintenance where id = 3";
        rs.executeSql(sql);
        int hrmid = 0;
        while (rs.next()) {
            hrmid = Util.getIntValue(rs.getString("hrmid"));
        }
        if (userid == hrmid) {
            return true;
        }
        if (isNewResource(userid) && isSuperviser(userid, id)) {
            return true;
        }
        return false;
    }


    /**
     * 判断user1是否能查看user2的资产信息
     *
     * @param userid user1
     * @param id     user2
     * @return
     */
    @SuppressWarnings("deprecation")
    public boolean isCapInfoView2(int userid, String id) {
        RecordSet rs = new RecordSet();
        sql = "select hrmid,hrmids from HrmInfoMaintenance where id = 3";
        rs.executeSql(sql);
        int hrmid = 0;
        String hrmids = "";
        while (rs.next()) {
            hrmid = Util.getIntValue(rs.getString("hrmid"));
            hrmids = Util.null2String(rs.getString("hrmids"));
        }
        if (userid == hrmid) {
            return true;
        }
        String[] strings = hrmids.split(",");
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].equals(userid + "")) {
                return true;
            }
        }
        if (isNewResource(userid) && isSuperviser(userid, id)) {
            return true;
        }
        return false;
    }


    /**
     * 判断user1是否能维护user2的人员信息
     *
     * @param userid    user1
     * @param resourceid      user2
     * @return
     */
    public boolean isSuperviser(int userid, String resourceid) {
        RecordSet rs = new RecordSet();
        sql = "select hrmid from HrmInfoMaintenance where id = 10";
        rs.executeSql(sql);
        int hrmid = 0;
        while (rs.next()) {
            hrmid = Util.getIntValue(rs.getString("hrmid"));
        }
        if (userid == hrmid) {
            return true;
        }
        if (isCreaterOfResource(userid, resourceid)) return true;
        return false;
    }

    /**
     * 判断user1是否是user2的创建人
     *
     * @param userid   user1
     * @param resourceid  user2
     * @return
     */
    public boolean isCreaterOfResource(int userid, String resourceid) {
        RecordSet rs = new RecordSet();
        sql = "select createrid from HrmResource where id = '" + resourceid + "'";
        rs.executeSql(sql);
        int hrmid = 0;
        while (rs.next()) {
            hrmid = Util.getIntValue(rs.getString("createrid"));
        }
        if (userid == hrmid) {
            return true;
        }
        return false;
    }

    public boolean isFinish(String id) {
        RecordSet rs = new RecordSet();
        sql = "select * from HrmInfoStatus where itemid < 4 and hrmid = '" + id + "'";
        rs.executeSql(sql);
        while (rs.next()) {
            String status = rs.getString("status");
            if (status.equals("0")) {
                return false;
            }
        }
        return true;
    }

    public boolean isNewResource(int id) {
        RecordSet rs = new RecordSet();
        sql = "select status from HrmInfoStatus where hrmid='" + id + "'";
        rs.executeSql(sql);
        while (rs.next()) {
            String status = rs.getString("status");
            if (status.equals("0")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取缺勤天数
     *
     * @param resourceid
     * @param hrmschedulediffid
     * @return
     */
    public int getAbsenceDayTotal(String resourceid, String hrmschedulediffid) {
        String diffid = hrmschedulediffid;
        int day = 0;
        try {
            RecordSet rs = new RecordSet();
            sql = "select totalday from HrmScheduleMaintance where resourceid ='" + resourceid + "' and diffid = " + diffid;
            rs.executeSql(sql);
            while (rs.next()) {
                day += rs.getInt(1);
            }
        } catch (Exception e) {
            lm.writeLog(e);
        }
        return day;
    }

    /**
     * 获取缺勤天数
     *
     * @param resourceid
     * @return
     */
    public int getAbsenceDayTotal(String resourceid) {
        int day = 0;
        try {
            RecordSet rs = new RecordSet();
            sql = "select totalday from HrmScheduleMaintance where resourceid ='" + resourceid + "'";
            rs.executeSql(sql);
            while (rs.next()) {
                day += rs.getInt(1);
            }
        } catch (Exception e) {
            lm.writeLog(e);
        }
        return day;
    }

    /**
     * endtime小时值减starttime小时值
     *
     * @param starttime
     * @param endtime
     * @return
     */
    private int subHour(String starttime, String endtime) {
        if (starttime.equals(endtime)) {
            return 0;
        }
        ArrayList timearray1 = Util.TokenizerString(starttime, ":");
        ArrayList timearray2 = Util.TokenizerString(endtime, ":");
        int hour1 = Util.getIntValue((String) timearray1.get(0));
        int hour2 = Util.getIntValue((String) timearray2.get(0));
        return hour2 - hour1;
    }

    /**
     * endtime分钟值减starttime分钟值
     *
     * @param starttime
     * @param endtime
     * @return
     */
    private int subMinute(String starttime, String endtime) {
        if (starttime.equals(endtime)) {
            return 0;
        }
        ArrayList timearray1 = Util.TokenizerString(starttime, ":");
        ArrayList timearray2 = Util.TokenizerString(endtime, ":");
        int min1 = Util.getIntValue((String) timearray1.get(1));
        int min2 = Util.getIntValue((String) timearray2.get(1));
        return min2 - min1;
    }

    /**
     * 判断是否为工作日
     *
     * @param date
     * @param userid
     * @return
     */
    public boolean isWorkDay(String date, String userid) {
        Time time = new Time();
        return time.isWorkDay(date, Util.getIntValue(userid));
    }

    public void addResourceInfoCache(String key){
    	addCache(key);
      new DecryptResourceComInfo().addCache(key);
      new EncryptFieldViewScopeConfigComInfo().removeCache();
        new KVResourceComInfo().removeCache();
		new TransLatorComInfo().removeCache();
    }
    
    /**
     * 删除缓存信息
     */
    public void removeResourceCache() {
    	super.removeCache();
     	try{
        new DecryptResourceComInfo().removeCache();
    		new ResourceBelongtoComInfo().removeResourceBelongtoCache();
    		new HrmUserSettingComInfo().removeHrmUserSettingComInfoCache();
		 	new ResourceVirtualComInfo().removeResourceVirtualCache();
    		new User().removeUserCache();
            new KQGroupMemberComInfo().removeCache();
            new EncryptFieldViewScopeConfigComInfo().removeCache();
			new KVResourceComInfo().removeCache();
			new TransLatorComInfo().removeCache();
    	}catch (Exception e) {
				writeLog(e);
			}
        try{
            CheckUserRight checkUserRight = new CheckUserRight();
            checkUserRight.removeMemberRoleCache();
            new RolemembersComInfo().removeCache() ;
        }catch(Exception e ){
            writeLog(e);
        }
    }

    /**
     * 更新指定缓存信息
     *
     * @param key  指定缓存
     */
    public void updateResourceInfoCache(String key) {
    	 boolean flag = false;
 				String tmpkey = "";
    	 if(Util.getIntValue(this.getAccountType(key))>0&&Util.getIntValue(this.getBelongTo(key))>0){
    		 flag = true; 
    		 tmpkey = this.getBelongTo(key);
    	 }
    	 updateCache(key);
    	 if(!flag&&Util.getIntValue(this.getAccountType(key))>0&&Util.getIntValue(this.getBelongTo(key))>0){
    		 flag = true; 
    		 tmpkey = this.getBelongTo(key);
    	 }
    	 if(flag){//更新主账号信息
    		 try{
     		 	new ResourceBelongtoComInfo().updateCache(tmpkey);
     		 	HrmUserSettingComInfo hrmUserSettingComInfo = new HrmUserSettingComInfo();
     		 	String id = hrmUserSettingComInfo.getId(tmpkey);
     		 	hrmUserSettingComInfo.updateCache(id);
          new KQGroupMemberComInfo().updateCache(id);
       	}catch (Exception e) {
   				writeLog(e);
   			}
    	 }
    	 try{
         new DecryptResourceComInfo().updateCache(key);
    		 	new ResourceBelongtoComInfo().updateCache(key);
    		 	HrmUserSettingComInfo hrmUserSettingComInfo = new HrmUserSettingComInfo();
    		 	String id = hrmUserSettingComInfo.getId(""+key);
    		 	hrmUserSettingComInfo.updateCache(id);
    		 	new ResourceVirtualComInfo().removeResourceVirtualCache();
    		 	new User().updateCache(key);
            new KQGroupMemberComInfo().removeCache();
         new EncryptFieldViewScopeConfigComInfo().removeCache();
			new TransLatorComInfo().removeCache();
      	}catch (Exception e) {
  				writeLog(e);
  			}

        new KVResourceComInfo().removeCache();
        try{
            CheckUserRight checkUserRight = new CheckUserRight();
            checkUserRight.removeMemberRoleCache();
            new RolemembersComInfo().removeCache() ;
        }catch(Exception e ){
            writeLog(e);
        }
    }

    /**
     * 删除指定缓存信息
     *
     * @param key 指定缓存
     */
    public void deleteResourceInfoCache(String key) {
    	boolean flag = false;
    	String tmpkey = "";
    	if(Util.getIntValue(this.getAccountType(key))>0&&Util.getIntValue(this.getBelongTo(key))>0){
    		flag = true; 
    		tmpkey = this.getBelongTo(key);
    	}
    	deleteCache(key);
      new DecryptResourceComInfo().deleteCache(key);
    	if(!flag&&Util.getIntValue(this.getAccountType(key))>0&&Util.getIntValue(this.getBelongTo(key))>0){
    		flag = true; 
    		tmpkey = this.getBelongTo(key);
    	}
    	if(flag){//更新主账号信息
    		try{
   		 		new ResourceBelongtoComInfo().deleteCache(tmpkey);
   		 		HrmUserSettingComInfo hrmUserSettingComInfo = new HrmUserSettingComInfo();
   		 		String id = hrmUserSettingComInfo.getId(tmpkey);
   		 		hrmUserSettingComInfo.deleteCache(id);
     	}catch (Exception e) {
 					writeLog(e);
 			}
  	 }
   	 try{
 		 	new ResourceBelongtoComInfo().deleteCache(key);
 		 	HrmUserSettingComInfo hrmUserSettingComInfo = new HrmUserSettingComInfo();
 		 	String id = hrmUserSettingComInfo.getId(""+key);
 		 	hrmUserSettingComInfo.deleteCache(id);
   	}catch (Exception e) {
				writeLog(e);
			}
    }

//该方法与实际情况不符，故注释  update by  fanggsh 2007-03-22 for TD6164    
//    /*
//    * @param  submitType  1:员工入口修改合同 2:客户入口修改合同
//    * @param  modifierID  系统管理员和员工分存两张表, 系统管理员表只有一条记录, id暂定为1
//    */
//    public String getClientDetailModifier(
//            String modifierID,
//            String submitType,
//            String loginType) {
//        RecordSet rs = new RecordSet();
//        String sql = "";
//        if ("1".equals(submitType)) {
//            if (!"1".equals(modifierID)) {
//                sql =
//                        "select lastname from HrmResource where id ='"
//                                + modifierID
//                                + "'";
//            } else if ("1".equals(modifierID)) {
//                sql =
//                        "select lastname from HrmResourceManager where id ='"
//                                + modifierID
//                                + "'";
//            } else {
//            }
//
//
//        } else if ("2".equals(submitType)) {
//            sql =
//                    "select name from CRM_CustomerInfo where id ='"
//                            + modifierID
//                            + "'";
//        } else {
//        }
//        //System.out.println("loginType == " + loginType);
//        //System.out.println(sql);
//        rs.executeSql(sql);
//        if (rs.next()) {
//            if ("1".equals(submitType)) {
//                return Util.null2String(rs.getString("lastname"));
//            } else if ("2".equals(submitType)) {
//                return Util.null2String(rs.getString("name"));
//            } else {
//                return "";
//            }
//
//        }
//
//
//        return "";
//
//
//    }
    
  /*
  * @param  submitType  1:员工入口修改合同 2:客户入口修改合同
  * @param  modifierID  系统管理员和员工分存两张表, 系统管理员表只有一条记录, id暂定为1
  */
  public String getClientDetailModifier(
          String modifierID,
          String submitType,
          String loginType) {
	  
	  //安全检查
	  if(modifierID==null||modifierID.trim().equals("")
		||(!"1".equals(submitType)&&!"2".equals(submitType))	  
	  ){
		  return "";
	  }
	  
      RecordSet rs = new RecordSet();
      String sql = "";
      if ("1".equals(submitType)) {
    	  return getResourcename(modifierID);
      } else if ("2".equals(submitType)) {
          sql =
                  "select name from CRM_CustomerInfo where id ='"
                          + modifierID
                          + "'";
          rs.executeSql(sql);
          if(rs.next()){
        	  return Util.null2String(rs.getString("name"));
          }          
      } 

      return "";


  }
    
    /**
     * A是否是B的下级
     *
     * @param personIdA
     * @param PersonIdB
     * @return
     */
    public boolean isLowLevel(int personIdA,int PersonIdB){
    	boolean lowLevel = false ;
    	RecordSet rs = new RecordSet();
    	rs.executeSql("select  count(id) from HrmResource where managerid="+PersonIdB+" and id="+personIdA);
    	if (rs.next()) {
    		int countId = rs.getInt(1);
    		lowLevel = countId==1?true:false;
    	}
    	return lowLevel;
    }

    /**
     * 获取指定人员所有上级
     *
     * @param userId   指定人员
     * @return
     */
	 public String getManagers(String userId){
	    	String managers = "";
	    	RecordSet rs = new RecordSet();
	    	rs.executeSql("select managerstr from HrmResource where id="+userId);
	    	if (rs.next()) {
	    		managers = Util.null2String(rs.getString(1));
	    	}
	    	return managers;
	    }

     /**
      * Description:是否在线
     *
      * @param userId  用户id
      * @return 在线图标地址
     */
    public String isOnline(String userId){
       if(HrmUserVarify.isUserOnline(userId))
            return "<img src='"+weaver.general.GCONST.getContextPath()+"/images/State_LoggedOn_wev8.gif'/>";
        else
            return "";
    }
    
  /**
     * Description:是否主账号
     *
     * @param accounttype
     * @return 主账号标识
    */
   public String accounttype(String accounttype) {
		if (Util.null2String(accounttype).equals("")||Util.null2String(accounttype).equals("0")) {
			return "<img src='/hrm/images/accounttype0_wev8.png' style='margin-left: -5px;'/>";
		} else {
			return "<img src='/hrm/images/accounttype1_wev8.png' style='margin-left: -5px;'/>";
		}
	}


 	/**
 	 * Description: 所有下属
 	 */
  public HashMap getUnderlining(){
 		if(underlinings == null) {
 			underlinings = getUnderliningCache();
 		}
 		return underlinings;
 	}
 	
 	public static HashMap underlinings = null;
 	
 	public HashMap getUnderliningCache() {
 		HashMap h = new HashMap();
 		try{
 			setTofirstRow();
 			while(next()){
 				String id = getResourceid();
 				String managerstr = getManagersIDs();
 				String[] a = Util.TokenizerString2(managerstr, ",");
 				for(int i=0;i<a.length;i++){
 					if(a[i].equals("")) continue;
 					if(h.containsKey(a[i])){
 						String tempValue = (String)h.get(a[i]);
 						if((","+tempValue+",").indexOf(","+id+",")!=-1) continue;

 						h.put(a[i], tempValue + "," + id);
 					}else{
 						h.put(a[i], "," + id);
 					}
 				}
 			}
 		}catch(Exception e){
 			 lm.writeLog(e);
 		}
 		return h;
 	}

 	/**
 	 * 根据userid获取所有上级
     *
 	 * @param userId
 	 * @return 如：1,2,3
 	 */
 	public String getAllManagerByUserId(String userId){
 		String allManager = Util.null2String(getManagersIDs(userId));
 		if(allManager.length()==0)return "";
 		if(allManager.startsWith(","))allManager=allManager.substring(1,allManager.length());
 		if(allManager.endsWith(","))allManager=allManager.substring(0,allManager.length()-1);
 		return allManager;
 	}
 	
 	/**
 	 * 根据userid获取所有下级
     *
 	 * @param userId
 	 * @return如：1,2,3
 	 */
	public String getUnderliningByUserId(String userId){
		String underlining = "";
  	RecordSet rs = new RecordSet();
  	rs.executeSql("select id from HrmResource where managerstr like '%,"+userId+",%'");
  	while (rs.next()) {
  		if(underlining.length()>0)underlining+=",";
  		underlining += Util.null2String(rs.getString("id"));
  	}
		return underlining;
	}

	
	public String getUserIdByLoginId(String loginId) {
  	String userid = "";
  	RecordSet rs = new RecordSet();
        rs.executeSql("select id from HrmResource where loginid='" + loginId + "'");
  	if (rs.next()) {
  		userid = Util.null2String(rs.getString(1));
  	  	}
  	return userid;
	}
	
	public String getMutiResourceLink(String ids) throws Exception {
	  String names = "";
	  String temp = "";
	  ResourceComInfo comInfo = new ResourceComInfo();
	  ArrayList a_ids = Util.TokenizerString(ids,",");
	  for(int i=0;i<a_ids.size();i++){
			temp = (String)a_ids.get(i);
			names += "<a href=javascript:openFullWindowForXtable(\'/hrm/resource/HrmResource.jsp?id="+temp+"\')> "+comInfo.getResourcename(temp)+"</a> ";
	  }
	  return names;
 }
	
	public String getLastnameAllStatus(String userids){
		String returnStr="";
		String[] arr=Util.TokenizerString2(userids, ",");
		for(int i=0;i<arr.length;i++){
			returnStr+=getLastname(arr[i])+",";
		}
		if(!"".equals(returnStr)) returnStr=returnStr.substring(0,returnStr.length()-1);
		return returnStr;
	}
	
	public String getLastnames(String userids){
		String returnStr="";
		String[] arr=Util.TokenizerString2(userids, ",");
		for(int i=0;i<arr.length;i++){
			String status=getStatus(arr[i]);
			if(status.equals("0")||status.equals("1")||status.equals("2")||status.equals("3")||status.equals("5"))
			  returnStr+=getLastname(arr[i])+",";
		}
		if(!"".equals(returnStr)) returnStr=returnStr.substring(0,returnStr.length()-1);
		return returnStr;
	}
	
  //用于可编辑列表
  public String getLastnamesForEdit(String userIds)throws Exception{
  	JSONArray jsa = new JSONArray();
  	String userNames = "";
	  ResourceComInfo comInfo = new ResourceComInfo();
    String[] userIdArrays = Util.TokenizerString2(userIds,",");
      for (int i=0 ;userIdArrays!=null&&i<userIdArrays.length;i++){
          String tempUserId = userIdArrays[i];
          userNames="<a href=\"javaScript:openFullWindowHaveBar(\'/hrm/resource/HrmResource.jsp?id=" + tempUserId
                  + "\')\">"+comInfo.getResourcename(tempUserId)+"</a> &nbsp;";
          JSONObject json = new JSONObject();
          json.put("browserValue", tempUserId);
          json.put("browserSpanValue", userNames);
          jsa.add(json);
      }
      return jsa.toString();
  }
  
//用于显示小卡片
  public String getLastnamesForSimpleHrm(String userIds)throws Exception{
  	JSONArray jsa = new JSONArray();
  	String userNames = "";
	  ResourceComInfo comInfo = new ResourceComInfo();
    String[] userIdArrays = Util.TokenizerString2(userIds,",");
      for (int i=0 ;userIdArrays!=null&&i<userIdArrays.length;i++){
          String tempUserId = Util.null2String(userIdArrays[i]);
          if(tempUserId.equals("0"))tempUserId="";
          userNames="<a onclick=\"pointerXY(event)\" href='javaScript:openhrm(" + tempUserId
                  + ");'>"+comInfo.getResourcename(tempUserId)+"</a> &nbsp;";
          JSONObject json = new JSONObject();
          json.put("browserValue", tempUserId);
          json.put("browserSpanValue", userNames);
          jsa.add(json);
      }
      return jsa.toString();
  }
  
	
	public String getSexName(String sex){
		return getSexName(sex, "7");
	}
  
	
	public String getSexName(String sex, String otherpara){
		int language = StringUtil.parseToInt(otherpara, 7);
		if(Util.null2String(sex).equals("0")){
			return SystemEnv.getHtmlLabelName(28473, language);
        } else if (Util.null2String(sex).equals("1")) {
			return SystemEnv.getHtmlLabelName(28474, language);
		}
		return "";
	}

	public String getMobileShow1(String mobile, String otherpara){
		User user = new User();
		String userId = otherpara.split("\\+")[0];
		String currentUserId = otherpara.split("\\+")[1];
		user.setUid(Integer.parseInt(currentUserId));
		if(currentUserId.equals("1"))user.setLoginid("sysadmin");
		return getMobileShow(String.valueOf(userId), user);
	}
	
	public String getMobileShow(String userId, String currentUserId){
		User user = new User();
		user.setUid(Integer.parseInt(currentUserId));
		if(currentUserId.equals("1"))user.setLoginid("sysadmin");
		return getMobileShow(String.valueOf(userId), user);
	}
	
	private static final Pattern p = Pattern.compile("^(\\d{3})(\\d{4})(\\d{4})$") ;
	
	/**
	 * 手机号码格式化3-4-4
     *
	 * @param mobile
	 * @return
	 */
	public static String formatMobile(String mobile){
		if(StringUtils.isBlank(mobile)) return StringUtils.EMPTY ;
		Matcher m = p.matcher(mobile) ;
		if(m.find()){
			//11位数字 -- 大陆电话 3-4-4格式
			return m.group(1).concat("-")
			                 .concat(m.group(2))
			                 .concat("-")
			                 .concat(m.group(3)) ;
		}
		
		//非大陆电话
		return mobile ;
	}

  /**
   * 获取通用的隐私设置情况
   * @param mobileShowType 隐私设置类型 1:公开   2：对所有人保密  3：对低于自身安全级别者保密
   * @param resourceId 被查看人员的ID
   * @param userId 当前用户ID
   * @return
   */
  public boolean showCommonPrivacy(String mobileShowType,String resourceId,String userId) throws Exception{
    boolean canShow = true;

    ResourceComInfo rc = new ResourceComInfo();
    int secLevel = 0, currentUserSeclevel = 0;
    int mobileShowTypeNew = Util.getIntValue(mobileShowType,0);

    secLevel = Util.getIntValue(rc.getSeclevel(resourceId),0);

    //自己登录可以查看自己手机号码
    if(resourceId.equalsIgnoreCase(userId)){
      return canShow;
    }

    if(mobileShowTypeNew==2){
      //对所有人保密
      canShow = false;
    }else if(mobileShowTypeNew==3){
      //对低于自身安全级别者保密
      currentUserSeclevel = Util.getIntValue(rc.getSeclevel(userId),0);
      if(secLevel>currentUserSeclevel)canShow=false;
    }else{
      //公开或者未设置，则统一为公开
    }

    return canShow;
  }

	/**
	 * 根据设置取mobile显示
     *
	 * @param resourceId 被查看人员的ID
	 * @param user 当前用户
	 * @return
	 */
	public String getMobileShow(String resourceId, User user){
    int userId = user.getUID();
    String mobileShow  = Util.null2String(this.getMobile(resourceId));
    boolean canShow = true;
    //默认这里是手机号的获取
    String fieldname = "mobile";
    UserPrivacyComInfo upc = new UserPrivacyComInfo();
    PrivacyComInfo pc = new PrivacyComInfo();
    Map<String, String> mapShowSets = pc.getMapShowSets();
    Map<String, String> mapShowTypeDefaults = pc.getMapShowTypeDefaults();

    try {
      //判断是否有手机号码的隐藏设置
      if(mapShowSets != null && mapShowSets.get(fieldname) != null ){
        String mobileShowSet = Util.null2String(mapShowSets.get(fieldname));
        String mobileShowTypeDefault = Util.null2String(mapShowTypeDefaults.get(fieldname));
        //判断是否开启了手机号码允许个人设置
        if(mobileShowSet.equals("1")){
          String comPk = resourceId+"__"+fieldname;
          String fieldValue = Util.null2String(upc.getPvalue(comPk));
          //判断此用户是否真的做了自定义的设置
          if(fieldValue.length() > 0){
            canShow = showCommonPrivacy(fieldValue, ""+resourceId, ""+userId);
          }else{
            canShow = showCommonPrivacy(mobileShowTypeDefault, ""+resourceId, ""+userId);
          }
        }else{
          canShow = showCommonPrivacy(mobileShowTypeDefault, ""+resourceId, ""+userId);
        }
      }
    }catch (Exception e){
      e.printStackTrace();
      writeLog(e);
    }

		if(!canShow&&mobileShow.length()>0){
      if(mobileShow.startsWith("desensitization__")){
        mobileShow = EncryptConfigBiz.getDecryptData(mobileShow);
      }
			//不能直接显示的用*号代替
			if(mobileShow.length()<=4){
				mobileShow="****";
			}
			mobileShow=mobileShow.substring(0, mobileShow.length()-4)+"****";
		}
		return formatMobile(mobileShow);
	}
	
	public ArrayList<String> getResourceOperate(String id, String canEdit, String otherPara){
		ArrayList<String> resultList = new ArrayList<String>();
		String log = otherPara.split(":")[0];
		
		if(canEdit.equals("true")){
			resultList.add("true");
		}else{
			resultList.add("false");
		}

		if(log.equals("true")){
			resultList.add("true");
		}else{
			resultList.add("false");
		}
		return resultList;
	}
	
	public static String getStatusName(String strstatus, String strlanguage){
		String statusname = "";
		int status = Util.getIntValue(strstatus);
		int language = Util.getIntValue(strlanguage);
		switch(status){
			case 0:
				statusname=SystemEnv.getHtmlLabelName(15710,language);
				break;
			case 1:
				statusname=SystemEnv.getHtmlLabelName(15711,language);
				break;
			case 2:
				statusname=SystemEnv.getHtmlLabelName(480,language);
				break;
			case 3:
				statusname=SystemEnv.getHtmlLabelName(15844,language);
				break;
			case 4:
				statusname=SystemEnv.getHtmlLabelName(6094,language);
				break;
			case 5:
				statusname=SystemEnv.getHtmlLabelName(6091,language);
				break;
			case 6:
				statusname=SystemEnv.getHtmlLabelName(6092,language);
				break;
			case 7:
				statusname=SystemEnv.getHtmlLabelName(2245,language);
				break;
			case 9:
				statusname=SystemEnv.getHtmlLabelName(332,language);
				break;
			default:
				statusname=SystemEnv.getHtmlLabelName(1831,language);
				break;
		}
		return statusname;
	}

	public static String getStatusName(int status, User user){
		String statusname = getStatusName(""+status, ""+user.getLanguage());
		
		return statusname;
	}
	
    public String getCreatedate(String key) {
        return ((String) getValue(createdate, key)).trim();
}

    public String getResourceimageid(String key) {
        return ((String) getValue(resourceimageid, key)).trim();
    }

    public String getClassification() {
        return (String) getRowValue(classification);
    }

    public String getClassification(String key) {
        return (String) getValue(classification, key);
    }

    public String getEncKey() {
      return (String) getRowValue(encKey);
    }

    public String getEncKey(String key) {
      return (String) getValue(encKey, key);
    }

    public String getCrc() {
      return (String) getRowValue(crc);
    }

    public String getCrc(String key) {
      return (String) getValue(crc, key);
    }

    public String getCompanyStartDate() {
        return (String) getRowValue(companyStartDate);
    }

    public String getCompanyStartDate(String key) {
        return (String) getValue(companyStartDate, key);
    }

    public List<Map> getResource(String key, List fields){
        List<Map> datas = new ArrayList<>();
        Map rowData = new HashMap();

        //List sqlParams = new ArrayList();
        //Object[] objects= DBUtil.transListIn(key,sqlParams);
        //String sql = "select id, " + String.join(",", fields) + ", dsporder from HrmResource where id in (" + objects[0] + ")";
        String sql = "select id, " + String.join(",", fields) + ", dsporder from HrmResource where "+Tools.getOracleSQLIn(key,"id");
        try{
            RecordSet rs = new RecordSet();
            rs.executeQuery(sql);
            String[] columnNames = rs.getColumnName();
            while (rs.next()) {
                rowData = new HashMap();
                for(String column : columnNames){
                    rowData.put(column.toLowerCase(), rs.getString(column));
                }
                datas.add(rowData);
            }
        }catch (Exception e){

        }
        return datas;
    }

    public String getWorkStartDate() {
        return ((String) (getRowValue(workStartDate))).trim();
    }

    public String getWorkStartDate(String key) {
        return ((String) getValue(workStartDate, key)).trim();
    }

    public String getEndDate() {
        return ((String) (getRowValue(enddate))).trim();
    }

    public String getEndDate(String key) {
        return ((String) getValue(enddate, key)).trim();
    }

    public String getJobCall(){
	    return ((String) (getRowValue(jobCall))).trim();
    }

    public String getJobCall(String key) {
        return ((String) getValue(jobCall, key)).trim();
    }

    public String getMobileCall(){
        return ((String) (getRowValue(mobileCall))).trim();
    }

    public String getMobileCall(String key) {
        return ((String) getValue(mobileCall, key)).trim();
    }

    public String getFax(){
        return ((String) (getRowValue(fax))).trim();
    }

    public String getFax(String key) {
        return ((String) getValue(fax, key)).trim();
    }

    public String getJobActivityDesc(){
        return ((String) (getRowValue(jobactivitydesc))).trim();
    }

    public String getJobActivityDesc(String key) {
        return ((String) getValue(jobactivitydesc, key)).trim();
    }

    public String getWorkroom(){
        return ((String) (getRowValue(workroom))).trim();
    }

    public String getWorkroom(String key) {
        return ((String) getValue(workroom, key)).trim();
    }

    public String getDsporder(){
        return ((String) (getRowValue(dsporder))).trim();
    }

    public String getDsporder(String key) {
        return ((String) getValue(dsporder, key)).trim();
    }

    public boolean isAdmin(){
	    String tempIsAdmin = ((String) (getRowValue(isAdmin))).trim();
	    if("1".equals(tempIsAdmin)){
            return true;
        }else{
	        return false;
        }
    }

    public boolean isAdmin(String key) {
        String tempIsAdmin = ((String) getValue(isAdmin, key)).trim();
        if("1".equals(tempIsAdmin)){
            return true;
        }else{
            return false;
        }
    }

}

