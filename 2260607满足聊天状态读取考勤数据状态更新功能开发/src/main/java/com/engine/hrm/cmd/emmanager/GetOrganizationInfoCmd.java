package com.engine.hrm.cmd.emmanager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.hrm.bean.RightMenu;
import com.api.hrm.bean.RightMenuType;
import com.api.hrm.cmd.usericon.GetUserIconCmd;
import com.api.hrm.service.HrmUserIconService;
import com.api.hrm.service.impl.HrmUserIconServiceImpl;
import com.api.hrm.util.ViewSharingUtil;
import com.api.system.language.util.ParseLangDataUtil;
import com.customization.qc2260607.AccountStateUtil;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.biz.EncryptConfigBiz;
import com.engine.common.entity.BizLogContext;
import com.engine.common.util.ServiceUtil;
import com.engine.core.interceptor.CommandContext;
import com.engine.hrm.util.HrmUtil;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.docs.docs.CustomFieldManager;
import org.apache.commons.lang3.StringUtils;
import weaver.general.BaseBean;
import weaver.general.GCONST;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.appdetach.AppDetachComInfo;
import weaver.hrm.cachecenter.util.SqlUtil;
import weaver.hrm.common.database.dialect.DialectUtil;
import weaver.hrm.company.CompanyComInfo;
import weaver.hrm.company.DepartmentComInfo;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.companyvirtual.CompanyVirtualComInfo;
import weaver.hrm.companyvirtual.DepartmentVirtualComInfo;
import weaver.hrm.companyvirtual.ResourceVirtualComInfo;
import weaver.hrm.companyvirtual.SubCompanyVirtualComInfo;
import weaver.hrm.definedfield.HrmFieldManager;
import weaver.hrm.job.JobActivitiesComInfo;
import weaver.hrm.job.JobGroupsComInfo;
import weaver.hrm.job.JobTitlesComInfo;
import weaver.hrm.privacy.PrivacyComInfo;
import weaver.hrm.privacy.PrivacyUtil;
import weaver.hrm.resource.HrmListValidate;
import weaver.hrm.resource.ResourceComInfo;
import weaver.systeminfo.SystemEnv;
import weaver.hrm.cachecenter.bean.KVResourceComInfo;
import weaver.hrm.cachecenter.bean.LoadComInfo;
import weaver.hrm.cachecenter.bean.SuperDepartmentComInfo;
import weaver.hrm.cachecenter.bean.SuperSubCompanyComInfo;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取em通讯录
 *
 * @author lvyi
 */

public class GetOrganizationInfoCmd extends AbstractCommonCommand<Map<String, Object>> {
  private AppDetachComInfo adci = null;
  private static ConcurrentHashMap<String, Object> id2count = new ConcurrentHashMap<String, Object>();
  private boolean showChild = true;
  public GetOrganizationInfoCmd(Map<String, Object> params) {
    this.params = params;
    int userid = Util.getIntValue(Util.null2String(params.get("userid")));
    this.user = new User(userid);
    String lang_tag = Util.null2String(params.get("lang_tag"));//多语言
    this.user.setLanguage(ParseLangDataUtil.convertLang(lang_tag));
    adci = new AppDetachComInfo(user);
    if("0".equals(new BaseBean().getPropValue("hrmFieldSync","showChild"))){
      this.showChild = false;
    }else if("0".equals(Util.null2String(params.get("showChild")))){
      this.showChild = false;
    }
    this.needInit();
  }

  @Override
  public Map<String, Object> execute(CommandContext commandContext) {
    Map<String, Object> retmap = new HashMap<String, Object>();
    try {
      long startTime=System.currentTimeMillis();
      //writeLog("进入getOrganizationInfo方法时间===="+startTime);
      String cmd = Util.null2String(params.get("cmd"));
      if(cmd.equals("getOrganizationInfo")){
        retmap = this.getOrganizationInfo();
      }else if(cmd.equals("getSubordinate")){
        retmap = this.getSubordinate();
      }else if(cmd.equals("searchBaseResourceList")){
        retmap = this.searchBaseResourceList();
      }else if(cmd.equals("searchResourceList")){
        retmap = this.searchResourceList();
      }else if(cmd.equals("getResourceSimpleInfo")){
        retmap = this.getResourceSimpleInfo();
      }else if(cmd.equals("getResourceDetailInfo")){
        retmap = this.getResourceDetailInfo();
      }
    } catch (Exception e) {
      retmap.put("errcode", -5);
      retmap.put("errmsg", ""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10003158,weaver.general.ThreadVarLanguage.getLang())+"EM"+weaver.systeminfo.SystemEnv.getHtmlLabelName(10005236,weaver.general.ThreadVarLanguage.getLang())+"" + e.getMessage());
      writeLog(e);
    }finally {
      long endTime=System.currentTimeMillis();
      //writeLog("结束getOrganizationInfo方法时间===="+endTime);
    }
    return retmap;
  }

  /***
   * 我的下属
   * @return
   */
  private Map<String, Object> getSubordinate(){
    Map<String, Object> retmap = new HashMap<String, Object>();
    try{
      List<Object> resourceList = new ArrayList<>();
      Map<String,Object> resourceInfo = null;
      RecordSet rs = new RecordSet();
      String sql = "";
      DepartmentComInfo departmentComInfo = new DepartmentComInfo();
      ResourceComInfo resourceComInfo = new ResourceComInfo();
      JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
      boolean enable_main_account = "1".equals(Util.null2String(params.get("enable_main_account")).trim());
      if(adci==null) adci = new AppDetachComInfo(user);
      sql = " select hr.* "
              + " from hrmresource hr "
              + " where hr.managerid= ? ";
      String sqlwhere =" and hr.status in (0,1,2,3)";
      if(adci.isUseAppDetach()){
        String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
        String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
        sqlwhere+=tempstr;
      }
      if(sqlwhere.length()>0)sql+=sqlwhere;
      sql += " order by hr.dsporder ";
      rs.executeQuery(sql,user.getUID());
      while (rs.next()) {
        String userid = rs.getString("id");
        String lastname = Util.formatMultiLang(rs.getString("lastname"),""+user.getLanguage());
        String accounttype = rs.getString("accounttype");
        String belongto = "1".equals(accounttype)?Util.null2String(rs.getString("belongto")):"";
        String belongto_name = Util.formatMultiLang(resourceComInfo.getLastname(belongto),""+user.getLanguage());
        String sex = Util.null2String(rs.getString("sex"),"0");
        String managerid = rs.getString("managerid");
        String managername = Util.formatMultiLang(managerid.length() > 0 ? resourceComInfo.getResourcename(managerid) : "",""+user.getLanguage());
        String jobtitle = rs.getString("jobtitle");
        String jobtitlename = Util.formatMultiLang(jobTitlesComInfo.getJobTitlesname(jobtitle),""+user.getLanguage());
        String status = rs.getString("status");
        String showorder = Util.toDecimalDigits(rs.getString("dsporder"),2);
        String messagerurl = rs.getString("messagerurl");
        String mobile = resourceComInfo.getMobileShow(userid, user);
        String telephone = rs.getString("telephone");
        String email = rs.getString("email");
        //隐私设置部分
        PrivacyComInfo pc = new PrivacyComInfo();
        Map<String, String> mapShowSets = pc.getMapShowSets();
        Map<String, String> mapShowTypeDefaults = pc.getMapShowTypeDefaults();
        email = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "email", Util.null2String(rs.getString("hrmresource","email",true,true)), mapShowSets, mapShowTypeDefaults, pc);
        telephone = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "telephone", Util.null2String(rs.getString("hrmresource","telephone",true,true)), mapShowSets, mapShowTypeDefaults, pc);

        String subcount = "0";
        String totalcount = "0";
        resourceInfo = new HashMap<>();

        resourceInfo.put("id",userid);
        resourceInfo.put("userid",userid);
        resourceInfo.put("base_user_id",userid);
        resourceInfo.put("base_user_name",lastname);
        if(enable_main_account){
          resourceInfo.put("main_base_user_id",belongto );
          resourceInfo.put("main_base_user_name",belongto_name);
        }
        resourceInfo.put("name",lastname);
        resourceInfo.put("manager_id",managerid);
        resourceInfo.put("manager_name",managername);
        resourceInfo.put("position",jobtitlename);
        //EM端只有3种状态 1-正常状态 -2-禁用 3=删除 应EM要求在职人员默认返回1
        resourceInfo.put("status","1");
        resourceInfo.put("showorder",showorder);
        resourceInfo.put("gender",Util.getIntValue(sex));
        resourceInfo.put("mobile", EncryptConfigBiz.getDecryptData(mobile));
        resourceInfo.put("telephone",EncryptConfigBiz.getDecryptData(telephone));
//        resourceInfo.put("subcount",subcount);
//        resourceInfo.put("totalcount",totalcount);
        resourceInfo.put("email",EncryptConfigBiz.getDecryptData(email));
        resourceInfo.put("mobile_prefix","+86");
        ArrayList<Object> ls = null;
        ls = new ArrayList<>();
        ls.add(rs.getString("departmentid"));
        resourceInfo.put("department",ls);
        ls = new ArrayList<>();
        ls.add(departmentComInfo.getDepartmentName(rs.getString("departmentname")));
        resourceInfo.put("deptlist",ls);
        ls = new ArrayList<>();
        ls.add(rs.getString("showorder"));
        resourceInfo.put("order",ls);
        Map<String,Object> avatar = new HashMap<>();//头像信息
        params.put("userId",userid);
        Map<String,Object> userIconInfo = ((HrmUserIconService) ServiceUtil.getService(HrmUserIconServiceImpl.class, user)).getUserIcon(params,user);
        String media_id = userIconInfo.get("messagerurl").toString();
        //这种格式得手机端显示不出来,取默认设置图片
        if(media_id.startsWith("/weaver/weaver.file.FileDownload")){
          media_id = userIconInfo.get("defaultmessagerurl").toString();
        }else if(messagerurl.length()>0){
          String filePath = GCONST.getRootPath() + media_id;
          File file = new File(filePath);
          if(!file.exists()){
            media_id = userIconInfo.get("defaultmessagerurl").toString();
          }
        }
        resourceInfo.put("avatar",avatar);
        avatar.put("default","");
        avatar.put("show_name", userIconInfo.get("shortname").toString());
        avatar.put("show_color",userIconInfo.get("background").toString());
        avatar.put("media_id",media_id);
        resourceList.add(resourceInfo);
      }
      retmap.put("userlist",resourceList);
      retmap.put("totalcount",resourceList.size());
    }catch (Exception e){
      writeLog(e);
    }
    return retmap;
  }

  private Map<String, Object> searchBaseResourceList(){
    Map<String, Object> retmap = new HashMap<String, Object>();
    try{
      List<Object> resourceList = new ArrayList<>();
      List<String> sqlParams = new ArrayList<String>() ;
      Map<String,Object> resourceInfo = null;
      RecordSet rs = new RecordSet();
      String sql = "";
      SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo();
      DepartmentComInfo departmentComInfo = new DepartmentComInfo();
      ResourceComInfo resourceComInfo = new ResourceComInfo();
      JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
      if(adci==null) adci = new AppDetachComInfo(user);

      String keyword = Util.null2String(params.get("keyword")).trim();
      String deptid = Util.null2String(params.get("deptid")).trim();
      String fetch_child = Util.null2String(params.get("fetch_child")).trim();
      String fetch_mine = Util.null2String(params.get("fetch_mine")).trim();
      int user_count = Util.getIntValue(Util.null2String(params.get("user_count")));
      boolean enable_main_account = "1".equals(Util.null2String(params.get("enable_main_account")).trim());
      String type = "3";//节点类型 1 com，2 subcom,  3 dept
      if(fetch_mine.equals("1")){
        deptid = "3"+user.getUserDepartment();
      }else{
        String tmpStr = deptid;
        if(deptid.length()>0) {
          if (deptid.startsWith("-")) {
            tmpStr = deptid.replace("-", "");
            type = tmpStr.substring(0, 1);
            deptid = "-" + tmpStr.substring(1, tmpStr.length());
          } else {
            type = tmpStr.substring(0, 1);
            deptid = tmpStr.substring(1, tmpStr.length());
          }
        }
      }
      if(deptid.length()>0){
        if(fetch_child.equals("1")){
          String childids = "";
          if(type.equals("2")) {
            childids = subCompanyComInfo.getAllChildSubcompanyId(deptid, childids);
          }else if(type.equals("3")){
            childids = departmentComInfo.getAllChildDepartId(deptid, childids);
          }
          if(childids.length()>0){
            if(childids.startsWith(",")) {
              deptid = deptid + childids;
            }else{
              deptid = deptid + ","+childids;
            }
          }
        }
      }


      sql = " select hr.* "
              + " from hrmresource hr ";
      String sqlwhere =" where hr.status in (0,1,2,3)";
      String mysqlEscope = "/";
      if(keyword.length()>0) {
        String keywordtemp = "";
        if (DialectUtil.isMySql()) {
          keywordtemp = "%" + Util.StringReplace(keyword.toLowerCase(), "_", mysqlEscope + "_") + "%";
          sqlwhere += " and (lastname like ? escape '" + mysqlEscope
                  + "' or pinyinlastname like ? escape '" + mysqlEscope
                  + "' or mobile like ? escape '" + mysqlEscope
                  + "' or telephone like ? escape '" + mysqlEscope
                  + "' or EXISTS (SELECT 1 FROM hrmjobtitles WHERE hr.jobtitle=hrmjobtitles.id AND jobtitlename LIKE ? escape '" + mysqlEscope + "') "
                  + " or lastname like ? or lower(pinyinlastname) like ? or workcode like ? or mobile like ? or telephone like ? "
                  + " or EXISTS (SELECT 1 FROM hrmjobtitles WHERE hr.jobtitle=hrmjobtitles.id AND jobtitlename LIKE ? ))";
        } else {
          keywordtemp = "%" + Util.StringReplace(keyword.toLowerCase(), "_", "\\_")+"%";
          sqlwhere += " and (lastname like ? escape '\\' " +
                  " or pinyinlastname like ? escape '\\' " +
                  " or mobile like ? escape '\\' " +
                  " or telephone like ? escape '\\' " +
                  " or EXISTS (SELECT 1 FROM hrmjobtitles WHERE hr.jobtitle=hrmjobtitles.id AND jobtitlename LIKE ? escape '\\') " +
                  " or lastname like ? or lower(pinyinlastname) like ? or workcode like ? or mobile like ? or telephone like ? " +
                  " or EXISTS (SELECT 1 FROM hrmjobtitles WHERE hr.jobtitle=hrmjobtitles.id AND jobtitlename LIKE ? ))";
        }
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
      }
      if(deptid.length()>0){
        Object[] descs = SqlUtil.toPlaceHolderAndParamsListForIds(deptid) ;
        String hodler = (String) descs[0] ;
        List<String> hodlerParams = (List<String>) descs[1] ;
        sqlParams.addAll(hodlerParams) ;
        if(type.equals("1")){//总部不用控制

        }else if(type.equals("2")){
          sqlwhere += " and hr.subcompanyid1 in ("+hodler+")";
        }else if(type.equals("3")){
          sqlwhere += " and hr.departmentid in ("+hodler+")";
        }
      }

      if(adci.isUseAppDetach()){
        String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
        String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
        sqlwhere+=tempstr;
      }
      if(sqlwhere.length()>0)sql+=sqlwhere;
      sql += " order by hr.dsporder ";
      rs.executeQuery(sql,sqlParams);
      int idx = 0;
      while (rs.next()) {
        if(user_count>0) {
          idx++;
          if (idx > user_count) break;
        }
        String userid = rs.getString("id");
        String lastname = Util.formatMultiLang(rs.getString("lastname"),""+user.getLanguage());
        String accounttype = rs.getString("accounttype");
        String belongto = "1".equals(accounttype)?Util.null2String(rs.getString("belongto")):"";
        String belongto_name = Util.formatMultiLang(resourceComInfo.getLastname(belongto),""+user.getLanguage());
        String sex = Util.null2String(rs.getString("sex"),"0");
        String managerid = rs.getString("managerid");
        String managername = Util.formatMultiLang(managerid.length() > 0 ? resourceComInfo.getResourcename(managerid) : "",""+user.getLanguage());
        String jobtitle = rs.getString("jobtitle");
        String jobtitlename = Util.formatMultiLang(jobTitlesComInfo.getJobTitlesname(jobtitle),""+user.getLanguage());
        String status = rs.getString("status");
        String showorder = Util.toDecimalDigits(rs.getString("dsporder"),2);
        String messagerurl = rs.getString("messagerurl");
        String mobile = resourceComInfo.getMobileShow(userid, user);
        String telephone = rs.getString("telephone");
        String email = rs.getString("email");
        //隐私设置部分
        PrivacyComInfo pc = new PrivacyComInfo();
        Map<String, String> mapShowSets = pc.getMapShowSets();
        Map<String, String> mapShowTypeDefaults = pc.getMapShowTypeDefaults();
        email = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "email", Util.null2String(rs.getString("email")), mapShowSets, mapShowTypeDefaults, pc);
        telephone = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "telephone", Util.null2String(rs.getString("telephone")), mapShowSets, mapShowTypeDefaults, pc);

        String subcount = "0";
        String totalcount = "0";
        resourceInfo = new HashMap<>();

        resourceInfo.put("id",userid);
        resourceInfo.put("name",lastname);
        resourceInfo.put("userid",userid);
        resourceInfo.put("base_user_id",userid);
        resourceInfo.put("base_user_name",lastname);
        if(enable_main_account){
          resourceInfo.put("main_base_user_id",belongto );
          resourceInfo.put("main_base_user_name",belongto_name);
        }
        resourceInfo.put("manager_id",managerid);
        resourceInfo.put("manager_name",managername);
        resourceInfo.put("position",jobtitlename);
        //EM端只有3种状态 1-正常状态 -2-禁用 3=删除 应EM要求在职人员默认返回1
        resourceInfo.put("status","1");
        resourceInfo.put("showorder",showorder);
        resourceInfo.put("gender",Util.getIntValue(sex));
        resourceInfo.put("mobile",EncryptConfigBiz.getDecryptData(mobile));
        resourceInfo.put("telephone",EncryptConfigBiz.getDecryptData(telephone));
//        resourceInfo.put("subcount",subcount);
//        resourceInfo.put("totalcount",totalcount);
        resourceInfo.put("email",EncryptConfigBiz.getDecryptData(email));
        resourceInfo.put("mobile_prefix","+86");
        ArrayList<Object> ls = null;
        ls = new ArrayList<>();
        ls.add(rs.getString("departmentid"));
        resourceInfo.put("department",ls);
        ls = new ArrayList<>();
        ls.add(departmentComInfo.getDepartmentName(rs.getString("departmentid")));
        resourceInfo.put("deptlist",ls);
        ls = new ArrayList<>();
        ls.add(rs.getString("showorder"));
        resourceInfo.put("order",ls);
        Map<String,Object> avatar = new HashMap<>();//头像信息
        params.put("userId",userid);
        Map<String,Object> userIconInfo = ((HrmUserIconService) ServiceUtil.getService(HrmUserIconServiceImpl.class, user)).getUserIcon(params,user);
        String media_id = userIconInfo.get("messagerurl").toString();
        //这种格式得手机端显示不出来,取默认设置图片
        if(media_id.startsWith("/weaver/weaver.file.FileDownload")){
          media_id = userIconInfo.get("defaultmessagerurl").toString();
        }else if(messagerurl.length()>0){
          String filePath = GCONST.getRootPath() + media_id;
          File file = new File(filePath);
          if(!file.exists()){
            media_id = userIconInfo.get("defaultmessagerurl").toString();
          }
        }
        resourceInfo.put("avatar",avatar);
        avatar.put("default","");
        avatar.put("show_name", userIconInfo.get("shortname").toString());
        avatar.put("show_color",userIconInfo.get("background").toString());
        avatar.put("media_id",media_id);
        resourceList.add(resourceInfo);
      }
      retmap.put("userlist",resourceList);
      retmap.put("totalcount",resourceList.size());
    }catch (Exception e){
      writeLog(e);
    }
    return retmap;
  }

  private Map<String, Object> searchResourceList(){
    Map<String, Object> retmap = new HashMap<String, Object>();
    try{
      List<Object> resourceList = new ArrayList<>();
      List<String> sqlParams = new ArrayList<String>() ;
      Map<String,Object> resourceInfo = null;
      RecordSet rs = new RecordSet();
      String sql = "";
      DepartmentComInfo departmentComInfo = new DepartmentComInfo();
      ResourceComInfo resourceComInfo = new ResourceComInfo();
      JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
      departmentComInfo.setTofirstRow();
      if(adci==null) adci = new AppDetachComInfo(user);

      String keyword = Util.null2String(params.get("keyword")).trim();
      int user_count = Util.getIntValue(Util.null2String(params.get("user_count")));
      boolean enable_main_account = "1".equals(Util.null2String(params.get("enable_main_account")).trim());

      sql = " select hr.*,t2.departmentname,t2.showorder "
              + " from hrmresource hr, hrmdepartment t2 "
              + " where hr.departmentid=t2.id ";
      String sqlwhere =" and hr.status in (0,1,2,3)";
      String mysqlEscope = "/";
      if(keyword.length()>0) {
        String keywordtemp = "";
        if (DialectUtil.isMySql()) {
          keywordtemp = "%" + Util.StringReplace(keyword.toLowerCase(), "_", mysqlEscope + "_") + "%";
          sqlwhere += " and (lastname like ? escape '" + mysqlEscope
                  + "' or pinyinlastname like ? escape '" + mysqlEscope
                  + "' or mobile like ? escape '" + mysqlEscope
                  + "' or telephone like ? escape '" + mysqlEscope
                  + "' or EXISTS (SELECT 1 FROM hrmjobtitles WHERE hr.jobtitle=hrmjobtitles.id AND jobtitlename LIKE ? escape '" + mysqlEscope + "') "
                  + " or lastname like ? or lower(pinyinlastname) like ? or workcode like ? or mobile like ? or telephone like ? "
                  + " or EXISTS (SELECT 1 FROM hrmjobtitles WHERE hr.jobtitle=hrmjobtitles.id AND jobtitlename LIKE ? ))";
        } else {
          keywordtemp = "%" + Util.StringReplace(keyword.toLowerCase(), "_", "\\_")+"%";
          sqlwhere += " and (lastname like ? escape '\\' " +
                  " or pinyinlastname like ? escape '\\' " +
                  " or mobile like ? escape '\\' " +
                  " or telephone like ? escape '\\' " +
                  " or EXISTS (SELECT 1 FROM hrmjobtitles WHERE hr.jobtitle=hrmjobtitles.id AND jobtitlename LIKE ? escape '\\') " +
                  " or lastname like ? or lower(pinyinlastname) like ? or workcode like ? or mobile like ? or telephone like ? " +
                  " or EXISTS (SELECT 1 FROM hrmjobtitles WHERE hr.jobtitle=hrmjobtitles.id AND jobtitlename LIKE ? ))";
        }
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
        sqlParams.add(keywordtemp);
      }

      if(adci.isUseAppDetach()){
        String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
        String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
        sqlwhere+=tempstr;
      }
      if(sqlwhere.length()>0)sql+=sqlwhere;
      sql += " order by hr.dsporder ";
      rs.executeQuery(sql,sqlParams);
      int idx = 0;
      while (rs.next()) {
        if(user_count>0) {
          idx++;
          if (idx > user_count) break;
        }
        String userid = rs.getString("id");
        String accounttype = rs.getString("accounttype");
        String belongto = "1".equals(accounttype)?Util.null2String(rs.getString("belongto")):"";
        String belongto_name = resourceComInfo.getLastname(belongto);
        String lastname = Util.formatMultiLang(rs.getString("lastname"),""+user.getLanguage());
        String sex = Util.null2String(rs.getString("sex"),"0");
        String managerid = rs.getString("managerid");
        String managername = Util.formatMultiLang(managerid.length() > 0 ? resourceComInfo.getResourcename(managerid) : "",""+user.getLanguage());
        String jobtitle = rs.getString("jobtitle");
        String jobtitlename = Util.formatMultiLang(jobTitlesComInfo.getJobTitlesname(jobtitle),""+user.getLanguage());
        String status = rs.getString("status");
        String showorder = Util.toDecimalDigits(rs.getString("dsporder"),2);
        String messagerurl = rs.getString("messagerurl");
        String mobile = resourceComInfo.getMobileShow(userid, user);
        String telephone = rs.getString("telephone");
        String email = rs.getString("email");
        //隐私设置部分
        PrivacyComInfo pc = new PrivacyComInfo();
        Map<String, String> mapShowSets = pc.getMapShowSets();
        Map<String, String> mapShowTypeDefaults = pc.getMapShowTypeDefaults();
        email = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "email", Util.null2String(rs.getString("hrmresource","email",true,true)), mapShowSets, mapShowTypeDefaults, pc);
        telephone = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "telephone", Util.null2String(rs.getString("hrmresource","telephone",true,true)), mapShowSets, mapShowTypeDefaults, pc);

        String subcount = "0";
        String totalcount = "0";
        resourceInfo = new HashMap<>();

        resourceInfo.put("id",userid);
        resourceInfo.put("name",lastname);
        resourceInfo.put("userid",userid);
        if(enable_main_account){
          resourceInfo.put("main_base_user_id",belongto );
          resourceInfo.put("main_base_user_name",belongto_name);
        }
        resourceInfo.put("base_user_id",userid);
        resourceInfo.put("base_user_name",lastname);
        resourceInfo.put("manager_id",managerid);
        resourceInfo.put("manager_name",managername);
        resourceInfo.put("position",jobtitlename);
        //EM端只有3种状态 1-正常状态 -2-禁用 3=删除 应EM要求在职人员默认返回1
        resourceInfo.put("status","1");
        resourceInfo.put("showorder",showorder);
        resourceInfo.put("gender",Util.getIntValue(sex));
        resourceInfo.put("mobile",EncryptConfigBiz.getDecryptData(mobile));
        resourceInfo.put("telephone",EncryptConfigBiz.getDecryptData(telephone));
//        resourceInfo.put("subcount",subcount);
//        resourceInfo.put("totalcount",totalcount);
        resourceInfo.put("email",EncryptConfigBiz.getDecryptData(email));
        resourceInfo.put("mobile_prefix","+86");
        ArrayList<Object> ls = null;
        ls = new ArrayList<>();
        ls.add(rs.getString("departmentid"));
        resourceInfo.put("department",ls);
        ls = new ArrayList<>();
        ls.add(rs.getString("departmentname"));
        resourceInfo.put("deptlist",ls);
        ls = new ArrayList<>();
        ls.add(rs.getString("showorder"));
        resourceInfo.put("order",ls);
        Map<String,Object> avatar = new HashMap<>();//头像信息
        params.put("userId",userid);
        Map<String,Object> userIconInfo = ((HrmUserIconService) ServiceUtil.getService(HrmUserIconServiceImpl.class, user)).getUserIcon(params,user);
        String media_id = userIconInfo.get("messagerurl").toString();
        //这种格式得手机端显示不出来,取默认设置图片
        if(media_id.startsWith("/weaver/weaver.file.FileDownload")){
          media_id = userIconInfo.get("defaultmessagerurl").toString();
        }else if(messagerurl.length()>0){
          String filePath = GCONST.getRootPath() + media_id;
          File file = new File(filePath);
          if(!file.exists()){
            media_id = userIconInfo.get("defaultmessagerurl").toString();
          }
        }
        resourceInfo.put("avatar",avatar);
        avatar.put("default","");
        avatar.put("show_name", userIconInfo.get("shortname").toString());
        avatar.put("show_color",userIconInfo.get("background").toString());
        avatar.put("media_id",media_id);
        resourceList.add(resourceInfo);
      }
      retmap.put("userlist",resourceList);
      retmap.put("totalcount",resourceList.size());
    }catch (Exception e){
      writeLog(e);
    }
    return retmap;
  }

  private Map<String, Object> getResourceSimpleInfo(){
    Map<String, Object> retmap = new HashMap<String, Object>();
    try{
      List<Object> resourceList = new ArrayList<>();
      List<String> sqlParams = new ArrayList<String>() ;
      Map<String,Object> resourceInfo = null;
      RecordSet rs = new RecordSet();
      String sql = "";
      DepartmentComInfo departmentComInfo = new DepartmentComInfo();
      ResourceComInfo resourceComInfo = new ResourceComInfo();
      JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
      departmentComInfo.setTofirstRow();
      if(adci==null) adci = new AppDetachComInfo(user);

      String userlist = Util.null2String(params.get("userlist")).trim();
      boolean enable_main_account = "1".equals(Util.null2String(params.get("enable_main_account")).trim());
      String userliststr = "";
      //处理管理员信息
      String [] userListIds = Util.TokenizerString2(userlist,",");
      for(String userListId: userListIds){
        if(resourceComInfo.isAdmin(userListId)){
          resourceInfo = new HashMap<>();
          resourceInfo.put("id",userListId);
          resourceInfo.put("name",resourceComInfo.getLastname(userListId));
          resourceInfo.put("base_user_id",userListId);
          resourceInfo.put("base_user_name",resourceComInfo.getLastname(userListId));
          if(enable_main_account){
            resourceInfo.put("main_base_user_id","" );
            resourceInfo.put("main_base_user_name","");
          }
          resourceInfo.put("name_simple_pingyin","");
          resourceInfo.put("name_full_pingyin","");
          resourceInfo.put("base_name_simple_pingyin","");
          resourceInfo.put("base_name_full_pingyin","");
          resourceInfo.put("user_type",1);
          resourceInfo.put("work_cond_icon","");
          resourceInfo.put("work_cond_txt","");
          resourceInfo.put("gender",Util.getIntValue(resourceComInfo.getSex(userListId)));
          resourceInfo.put("position","");
          resourceInfo.put("status","1");
          resourceInfo.put("mobile_prefix","");
          Map<String,Object> avatar = new HashMap<>();//头像信息
          params.put("userId",userListId);
          Map<String,Object> userIconInfo = ((HrmUserIconService) ServiceUtil.getService(HrmUserIconServiceImpl.class, user)).getUserIcon(params,user);
          String media_id = userIconInfo.get("messagerurl").toString();
          //这种格式得手机端显示不出来,取默认设置图片
          if(media_id.startsWith("/weaver/weaver.file.FileDownload")){
            media_id = userIconInfo.get("defaultmessagerurl").toString();
          }
          resourceInfo.put("avatar",avatar);
          avatar.put("default","");
          avatar.put("show_name", userIconInfo.get("shortname").toString());
          avatar.put("show_color",userIconInfo.get("background").toString());
          avatar.put("media_id",media_id);
          resourceList.add(resourceInfo);
        }else{
          if(userliststr.length()>0) userliststr+=",";
          userliststr+=userListId;
        }
      }
      sql = " select hr.*,t2.departmentname,t2.showorder "
              + " from hrmresource hr, hrmdepartment t2 "
              + " where hr.departmentid=t2.id ";
      String sqlwhere ="";
      //根据id查询人员信息，如果传了id就查询所有状态的人员，没有传id默认查询在职人员
      if(userliststr.length()==0){
        sqlwhere+=" and hr.status in (0,1,2,3)";
      }
      if(adci.isUseAppDetach()){
        String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
        String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
        sqlwhere+=tempstr;
      }
      if(sqlwhere.length()>0)sql+=sqlwhere;
      if(userliststr.length()>0){
        Object[] descs = SqlUtil.toPlaceHolderAndParamsListForIds(userliststr) ;
        String hodler = (String) descs[0] ;
        List<String> hodlerParams = (List<String>) descs[1] ;
        sqlParams.addAll(hodlerParams) ;
        sql+=" and hr.id in( "+hodler+" ) ";
      }
      sql += " order by hr.dsporder ";
      rs.executeQuery(sql,sqlParams);

      while (rs.next()) {
        String userid = rs.getString("id");
        String accounttype = rs.getString("accounttype");
        String belongto = "1".equals(accounttype)?Util.null2String(rs.getString("belongto")):"";
        String belongto_name = Util.formatMultiLang(resourceComInfo.getLastname(belongto),""+user.getLanguage());
        String lastname = Util.formatMultiLang(rs.getString("lastname"),""+user.getLanguage());
        String status = rs.getString("status");
        //EM端只有3种状态 1-正常状态 -2-禁用 3=删除  如果人员为非在职状态人员返回3，在职返回1
        if(status.equals("0")||status.equals("1")||status.equals("2")||status.equals("3")){
          status = "1";
        }else{
          status = "3";
        }
        String sex = Util.null2String(rs.getString("sex"),"0");
        //手机端性别和OA不同  1表示男性，2表示女性，0表示未知
        sex = sex.equals("0")?"1":(sex.equals("1")?"2":"0");
        String jobtitle = rs.getString("jobtitle");
        String jobtitlename = Util.formatMultiLang(jobTitlesComInfo.getJobTitlesname(jobtitle),""+user.getLanguage());
        String messagerurl = rs.getString("messagerurl");
        String []pinyin = Util.TokenizerString2(rs.getString("pinyinlastname"),"^");
        String name_simple_pingyin =pinyin.length>0 ? pinyin[0]:"";
        String name_full_pingyin = pinyin.length>0 ? pinyin[pinyin.length-1]:"";
        //==zj 获取当前时间人员的考勤流程状态
        AccountStateUtil asu = new AccountStateUtil();
        String work_cond_txt = "";
        List<Map<String, String>> resultList = asu.kqFlowGet(userid);
        new BaseBean().writeLog("==zj==(获取简易人员信息)" + JSON.toJSONString(resultList));
        if (resultList != null){
          for (int i = 0; i < resultList.size(); i++) {
            Map<String,String> result = resultList.get(i);
            work_cond_txt += result.get("考勤类型")+ " ";
          }
        }else {
          work_cond_txt="";
        }
        new BaseBean().writeLog("==zj==(work_cond_txt)" + work_cond_txt);
        resourceInfo = new HashMap<>();

        resourceInfo.put("id",userid);
        resourceInfo.put("name",lastname);
        resourceInfo.put("base_user_id",userid);
        resourceInfo.put("base_user_name",lastname);
        if(enable_main_account){
          resourceInfo.put("main_base_user_id",belongto );
          resourceInfo.put("main_base_user_name",belongto_name);
        }
        resourceInfo.put("name_simple_pingyin",name_simple_pingyin);
        resourceInfo.put("name_full_pingyin",name_full_pingyin);
        resourceInfo.put("base_name_simple_pingyin",name_simple_pingyin);
        resourceInfo.put("base_name_full_pingyin",name_full_pingyin);
        resourceInfo.put("user_type",1);
        resourceInfo.put("work_cond_icon","");
        resourceInfo.put("work_cond_txt",work_cond_txt);
        resourceInfo.put("gender",Util.getIntValue(sex));
        resourceInfo.put("position",jobtitlename);
        resourceInfo.put("status",status);
        resourceInfo.put("mobile_prefix","");
        Map<String,Object> avatar = new HashMap<>();//头像信息
        params.put("userId",userid);
        Map<String,Object> userIconInfo = ((HrmUserIconService) ServiceUtil.getService(HrmUserIconServiceImpl.class, user)).getUserIcon(params,user);
        String media_id = userIconInfo.get("messagerurl").toString();
        //这种格式得手机端显示不出来,取默认设置图片
        if(media_id.startsWith("/weaver/weaver.file.FileDownload")){
          media_id = userIconInfo.get("defaultmessagerurl").toString();
        }
        resourceInfo.put("avatar",avatar);
        avatar.put("default","");
        avatar.put("show_name", userIconInfo.get("shortname").toString());
        avatar.put("show_color",userIconInfo.get("background").toString());
        avatar.put("media_id",media_id);
        resourceList.add(resourceInfo);
      }
      retmap.put("userlist",resourceList);
      retmap.put("errcode",0);
      retmap.put("errmsg","");
      retmap.put("operationLogId","");
    }catch (Exception e){
      retmap.put("errcode",-1);
      retmap.put("errmsg","catch exception : " + e.getMessage());
      writeLog(e);
    }
    return retmap;
  }

  private Map<String, Object> getResourceDetailInfo(){
    Map<String, Object> retmap = new HashMap<String, Object>();
    try{
      List<Object> resourceList = new ArrayList<>();
      List<String> sqlParams = new ArrayList<String>() ;
      Map<String,Object> resourceInfo = null;
      RecordSet rs = new RecordSet();
      String sql = "";
      DepartmentComInfo departmentComInfo = new DepartmentComInfo();
      SubCompanyComInfo sci = new SubCompanyComInfo();
      ResourceComInfo resourceComInfo = new ResourceComInfo();
      JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
      JobActivitiesComInfo jobActivitiesComInfo = new JobActivitiesComInfo();
      JobGroupsComInfo jobGroupsComInfo = new JobGroupsComInfo();
      departmentComInfo.setTofirstRow();
      if(adci==null) adci = new AppDetachComInfo(user);

      String id = Util.null2String(params.get("id")).trim();
      boolean isSelf = id.equals(user.getUID()+"");
      boolean enable_main_account = "1".equals(Util.null2String(params.get("enable_main_account")).trim());

      sql = " select hr.*,t2.departmentname,t2.showorder "
              + " from hrmresource hr, hrmdepartment t2 "
              + " where hr.departmentid=t2.id ";
      String sqlwhere =" and hr.id =? ";
      sqlParams.add(id) ;
      if(adci.isUseAppDetach()){
        String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
        String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
        sqlwhere+=tempstr;
      }
      if(sqlwhere.length()>0)sql+=sqlwhere;
      sql += " order by hr.dsporder ";
      rs.executeQuery(sql,sqlParams);
      if (rs.next()) {
        String userid = rs.getString("id");
        String accounttype = rs.getString("accounttype");
        String belongto = "1".equals(accounttype)?Util.null2String(rs.getString("belongto")):"";
        String belongto_name = resourceComInfo.getLastname(belongto);
        String workcode = rs.getString("workcode");
        String lastname = rs.getString("lastname");
        String managerid = rs.getString("managerid");
        String managername = Util.formatMultiLang(managerid.length() > 0 ? resourceComInfo.getResourcename(managerid) : "",""+user.getLanguage());
        String messagerurl = rs.getString("messagerurl");
        String status = rs.getString("status");
        //EM端只有3种状态 1-正常状态 -2-禁用 3=删除  如果人员为非在职状态人员返回3，在职返回1
        if(status.equals("0")||status.equals("1")||status.equals("2")||status.equals("3")){
          status = "1";
        }else{
          status = "3";
        }
        String sex = Util.null2String(rs.getString("sex"),"0");
        String jobtitle = rs.getString("jobtitle");
        String jobtitlename = Util.formatMultiLang(jobTitlesComInfo.getJobTitlesname(jobtitle),""+user.getLanguage());
        String []pinyin = Util.TokenizerString2(rs.getString("pinyinlastname"),"^");
        String name_simple_pingyin =pinyin.length>0 ? pinyin[0]:"";
        String name_full_pingyin = pinyin.length>0 ? pinyin[pinyin.length-1]:"";
        resourceInfo = new HashMap<>();
        resourceInfo.put("workcode",workcode);
        resourceInfo.put("gender",Util.getIntValue(sex));
        resourceInfo.put("position",jobtitlename);
        resourceInfo.put("user_type",1);
        resourceInfo.put("status",status);
        resourceInfo.put("id",userid);
        resourceInfo.put("name",Util.formatMultiLang(lastname,""+user.getLanguage()));
        resourceInfo.put("manager_id",managerid);
        resourceInfo.put("manager_name",managername);
        //隐私设置部分
        PrivacyComInfo pc = new PrivacyComInfo();
        Map<String, String> mapShowSets = pc.getMapShowSets();
        Map<String, String> mapShowTypeDefaults = pc.getMapShowTypeDefaults();

        String mobile = resourceComInfo.getMobileShow(userid, user);
        String email = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "email", Util.null2String(rs.getString("hrmresource","email",true,true)), mapShowSets, mapShowTypeDefaults, pc);
        String telephone = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "telephone", Util.null2String(rs.getString("hrmresource","telephone",true,true)), mapShowSets, mapShowTypeDefaults, pc);
        String otherphone = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "mobilecall", Util.null2String(rs.getString("hrmresource","mobilecall",true,true)), mapShowSets, mapShowTypeDefaults, pc);
        //==zj 获取当前时间人员的考勤流程状态
        AccountStateUtil asu = new AccountStateUtil();
        String work_cond_txt = "";
        List<Map<String, String>> resultList = asu.kqFlowGet(userid);
        new BaseBean().writeLog("==zj==(获取详细人员信息)" + JSON.toJSONString(resultList));
        if (resultList != null){
          for (int i = 0; i < resultList.size(); i++) {
            Map<String,String> result = resultList.get(i);
            work_cond_txt += result.get("考勤类型")+ result.get("考勤状态")+" ";
          }
        }else {
          work_cond_txt="";
        }
        new BaseBean().writeLog("==zj==(getResourceDetailInfo的状态信息)" + work_cond_txt);

        resourceInfo.put("mobile",EncryptConfigBiz.getDecryptData(mobile));
        resourceInfo.put("telephone",EncryptConfigBiz.getDecryptData(telephone));
        resourceInfo.put("email",EncryptConfigBiz.getDecryptData(email));
        resourceInfo.put("otherphone",otherphone);
        if(enable_main_account){
          resourceInfo.put("main_base_user_id",belongto);
          resourceInfo.put("main_base_user_name",belongto_name);
        }

        resourceInfo.put("nick_name","");
        resourceInfo.put("mobile_prefix","+86");

        resourceInfo.put("name_simple_pingyin",name_simple_pingyin);
        resourceInfo.put("name_full_pingyin",name_full_pingyin);
        resourceInfo.put("work_cond_txt",work_cond_txt);
        resourceInfo.put("work_cond_icon","");

        Map<String,Object> avatar = new HashMap<>();//头像信息
        params.put("userId",userid);
        Map<String,Object> userIconInfo = ((HrmUserIconService) ServiceUtil.getService(HrmUserIconServiceImpl.class, user)).getUserIcon(params,user);
        String media_id = userIconInfo.get("messagerurl").toString();
        //这种格式得手机端显示不出来,取默认设置图片
        if(media_id.startsWith("/weaver/weaver.file.FileDownload")){
          media_id = userIconInfo.get("defaultmessagerurl").toString();
        }else if(messagerurl.length()>0){
          String filePath = GCONST.getRootPath() + media_id;
          File file = new File(filePath);
          if(!file.exists()){
            media_id = userIconInfo.get("defaultmessagerurl").toString();
          }
        }
        resourceInfo.put("avatar",avatar);
        avatar.put("default","");
        avatar.put("show_name", userIconInfo.get("shortname").toString());
        avatar.put("show_color",userIconInfo.get("background").toString());
        avatar.put("media_id",media_id);

        //部门信息
        List<Map<String,Object>> deptlist = new ArrayList<Map<String,Object>>();
        Map<String,Object> deptInfo = new HashMap<String,Object>();

        String deptId = rs.getString("departmentid");
        String subId = departmentComInfo.getSubcompanyid1(deptId);
        List <Integer> orgids = new ArrayList<Integer>();
        List <String> orgnames = new ArrayList<String>();
        List<String> allparentsubcompanyid1 = new ArrayList<String>();
        List<String> allparentdeptid  = new ArrayList<String>();
        String allSupdept = departmentComInfo.getAllSupDepartment(deptId);
        String allSupSub = sci.getAllSupCompany(subId);
        if(allSupdept.length()>0){
          allSupdept = deptId+","+allSupdept;
        }else{
          allSupdept = deptId;
        }

        if(allSupSub.length()>0){
          allSupSub = subId+","+allSupSub;
        }else{
          allSupSub = subId;
        }
        allparentsubcompanyid1 = Util.TokenizerString(allSupSub,",");
        allparentdeptid = Util.TokenizerString(allSupdept,",");
        for(int i = allparentsubcompanyid1.size()-1;i>=0;i--){
          String orgid = allparentsubcompanyid1.get(i);
          orgids.add(Util.getIntValue(orgid));
          orgnames.add(sci.getSubCompanyname(orgid));
        }
        for(int i = allparentdeptid.size()-1;i>=0;i--){
          String orgid = allparentdeptid.get(i);
          orgids.add(Util.getIntValue(orgid));
          orgnames.add(departmentComInfo.getDepartmentname(orgid));
        }
        Integer [] pathid = new Integer[orgids.size()];
        orgids.toArray(pathid);
        //部门全路径id集合
        deptInfo.put("pathid",pathid);
        //部门全路径名称集合
        deptInfo.put("path",orgnames);
        //要查看的人的部门名称
        deptInfo.put("name",departmentComInfo.getDepartmentName(deptId));
        //要查看的人的部门id
        deptInfo.put("id",Util.getIntValue(deptId));
        //要查看的人的上级部门id
        deptInfo.put("parentid",Util.getIntValue(departmentComInfo.getDepartmentsupdepid()));
        deptlist.add(deptInfo);
        resourceInfo.put("deptlist",deptlist);

        //自定义字段信息
        List<Map<String,Object>> attrs = new ArrayList<Map<String,Object>>();
        Map<String,Object> extattr = null;
        Map<String,Object> cusData = null;
        String basicinfo = rs.getPropValue("hrmFieldSync","basicinfo");
        String workinfo = rs.getPropValue("hrmFieldSync","workinfo");
        String personalinfo = rs.getPropValue("hrmFieldSync","personalinfo");
        String isOpen = rs.getPropValue("hrmFieldSync","isOpen");
        String standardFields = rs.getPropValue("hrmFieldSync","standardFields");
        String cusFields = rs.getPropValue("hrmFieldSync","cusFields");
        Map<String,Integer> scopeIds = new HashMap<>();
        if(basicinfo.equals("1")||isOpen.equals("1")){
          //基本信息中的自定义字段
          scopeIds.put("0",-1);
        }
        if(personalinfo.equals("1")||isOpen.equals("1")){
          //个人信息中的自定义字段
          scopeIds.put("1",1);
        }
        if(workinfo.equals("1")||isOpen.equals("1")){
          //工作信息中的自定义字段
          scopeIds.put("2",3);
        }

        HrmFieldManager hfm = null;
        int scopeId = 0;
        for(Map.Entry<String,Integer>entry: scopeIds.entrySet()){
          scopeId = entry.getValue();
          int i = Util.getIntValue(entry.getKey());
          String prefix = "";
          if(i==0){
            prefix = "basic_";
          }else if(i==1){
            prefix = "personal_";
          }else if(i==2){
            prefix = "work_";
          }
          hfm = new HrmFieldManager("HrmCustomFieldByInfoType", scopeId);
          CustomFieldManager cfm = new CustomFieldManager("HrmCustomFieldByInfoType", scopeId);
          hfm.getHrmData(Util.getIntValue(id));
          cfm.getCustomData(Util.getIntValue(id));
          hfm.getCustomFields();
          while (hfm.next()) {
            if (!hfm.isUse()) continue;
            if (hfm.getHtmlType().equals("6")) continue;//屏蔽附件上传
            if (hfm.getHtmlType().equals("3")&&(hfm.getType()==161||hfm.getType()==162)) continue;//因获取自定义浏览框的值有性能问题，查询特别慢故暂屏蔽自定义浏览框
            String fieldName = hfm.getFieldname();
            String fieldValue = "";
            //EM标准字段与自定义字段需要自定义是否显示
            if(isOpen.equals("1")){
              if (hfm.isBaseField(fieldName)){
                if(!(","+standardFields+",").contains(","+fieldName+","))continue;
                if(fieldName.equalsIgnoreCase("jobactivity")){
                  fieldValue = jobActivitiesComInfo.getJobActivitiesmarks(jobTitlesComInfo.getJobactivityid(jobtitle));
                }else if(fieldName.equalsIgnoreCase("jobgroupid")){
                  fieldValue = jobGroupsComInfo.getJobGroupsremarks(jobActivitiesComInfo.getJobgroupid(jobTitlesComInfo.getJobactivityid(jobtitle)));
                }else{
                  fieldValue = hfm.getHrmData(fieldName);
                }
              }else{
                if(!(","+cusFields+",").contains(","+prefix+fieldName+","))continue;
                fieldValue = cfm.getData("field" + hfm.getFieldid());
              }
              if(fieldValue.length()>0&&(!fieldName.equalsIgnoreCase("jobactivity")&&!fieldName.equalsIgnoreCase("jobgroupid"))){
                fieldValue=hfm.getFieldvalue(user, hfm.getDmrUrl(), hfm.getFieldid(), Util.getIntValue(hfm.getHtmlType()), hfm.getType(), fieldValue , 0);
              }
              if(fieldName.equalsIgnoreCase("mobile")){
                fieldValue = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "mobile", fieldValue, mapShowSets, mapShowTypeDefaults, pc);
              }
              if(fieldName.equalsIgnoreCase("telephone")){
                fieldValue = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "telephone", fieldValue, mapShowSets, mapShowTypeDefaults, pc);
              }
              if(fieldName.equalsIgnoreCase("email")){
                fieldValue = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "email", fieldValue, mapShowSets, mapShowTypeDefaults, pc);
              }
              if(fieldName.equalsIgnoreCase("mobilecall")){
                fieldValue = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "mobilecall", fieldValue, mapShowSets, mapShowTypeDefaults, pc);
              }
              if(fieldName.equalsIgnoreCase("fax")){
                fieldValue = PrivacyUtil.checkByPrivacy(Util.null2String(userid), user, "fax", fieldValue, mapShowSets, mapShowTypeDefaults, pc);
              }
            }else{
              if(hfm.isBaseField(fieldName))continue;
              fieldValue = cfm.getData("field" + hfm.getFieldid());
              if(fieldValue.length()>0){
                fieldValue=hfm.getFieldvalue(user, hfm.getDmrUrl(), hfm.getFieldid(), Util.getIntValue(hfm.getHtmlType()), hfm.getType(), fieldValue , 0);
              }
            }
            fieldValue = EncryptConfigBiz.getDecryptData(fieldValue);
            cusData = new HashMap<String,Object>();
            cusData.put("name", SystemEnv.getHtmlLabelName(Util.getIntValue(hfm.getLable()), user.getLanguage()));
            cusData.put("value", fieldValue);
            attrs.add(cusData);
          }
        }
        extattr = new HashMap<>();
        extattr.put("attrs",attrs);
        resourceInfo.put("extattr",extattr);
        resourceInfo.put("call",true);
        resourceInfo.put("send",true);
        resourceInfo.put("isattend",true);
        resourceList.add(resourceInfo);
      }else if(resourceComInfo.isAdmin(id)){
        new BaseBean().writeLog("==zj==(如果是管理员)");
        resourceInfo = new HashMap<>();
        resourceInfo.put("workcode",resourceComInfo.getSex(id));
        resourceInfo.put("gender",resourceComInfo.getSex(id));
        resourceInfo.put("position","");
        resourceInfo.put("user_type",1);
        resourceInfo.put("status",resourceComInfo.getStatus(id));
        resourceInfo.put("id",id);
        resourceInfo.put("name",Util.formatMultiLang(resourceComInfo.getLastnames(id),""+user.getLanguage()));
        resourceInfo.put("manager_id","");
        resourceInfo.put("manager_name","");
        resourceInfo.put("mobile","");
        resourceInfo.put("telephone","");
        resourceInfo.put("email","");
        resourceInfo.put("otherphone","");
        if(enable_main_account){
          resourceInfo.put("main_base_user_id","");
          resourceInfo.put("main_base_user_name ","");
        }

        resourceInfo.put("nick_name","");
        resourceInfo.put("mobile_prefix","+86");

        resourceInfo.put("name_simple_pingyin","");
        resourceInfo.put("name_full_pingyin","");
        resourceInfo.put("work_cond_txt","我太难了~");
        resourceInfo.put("work_cond_icon","\uD83D\uDC36");

        Map<String,Object> avatar = new HashMap<>();//头像信息
        params.put("userId",id);
        Map<String,Object> userIconInfo = ((HrmUserIconService) ServiceUtil.getService(HrmUserIconServiceImpl.class, user)).getUserIcon(params,user);
        String media_id = userIconInfo.get("messagerurl").toString();
        //这种格式得手机端显示不出来,取默认设置图片
        if(media_id.startsWith("/weaver/weaver.file.FileDownload")){
          media_id = userIconInfo.get("defaultmessagerurl").toString();
        }
        resourceInfo.put("avatar",avatar);
        avatar.put("default","");
        avatar.put("show_name", userIconInfo.get("shortname").toString());
        avatar.put("show_color",userIconInfo.get("background").toString());
        avatar.put("media_id",media_id);
        Map<String,Object> extattr = new HashMap<>();
        List<Map<String,Object>> attrs = new ArrayList<Map<String,Object>>();
        extattr.put("attrs",attrs);
        resourceInfo.put("extattr",extattr);
        resourceInfo.put("call",true);
        resourceInfo.put("send",true);
        resourceInfo.put("isattend",true);
        resourceList.add(resourceInfo);
      }
      retmap.put("userlist",resourceList);
      retmap.put("errcode",0);
      retmap.put("errmsg","");
      retmap.put("operationLogId","");
      retmap.put("totalcount",resourceList.size());
    }catch (Exception e){
      retmap.put("errcode",-1);
      retmap.put("errmsg","catch exception : " + e.getMessage());
      writeLog(e);
    }

    /*hou  2196658 */
    try {
      ViewSharingUtil viewSharingUtil = new ViewSharingUtil();
      String id = Util.null2String(params.get("id")).trim();
      HashMap<String, String> fields = viewSharingUtil.getFields(user, id, "-1");//拿到隐私集合
      if(fields.size()!=0){
        //基础信息主表
        ArrayList<HashMap<String,Object>> userlist = (ArrayList<HashMap<String, Object>>) retmap.get("userlist");
        for (int i = 0; i <userlist.size();i++) {
          HashMap<String, Object> map = userlist.get(i);
          Set<Map.Entry<String, String>> entries = fields.entrySet();
          for (Map.Entry<String, String> entry:entries) {
            String value = entry.getValue();
            if(value.equals("mobilecall")){
              value = "otherphone";
            }
            if(value.equals("departmentid")){
              value = "deptlist";
            }
            if(value.equals("managerid")){
              value = "manager_name";
            }
            if(value.equals("jobtitle")){
              value = "position";
            }
            Object o = map.get(value);
            if(o!=null){
              if(!"status".equals(value)){
                map.remove(value);
              }
            }
          }
          HashMap<String,Object> extattr = (HashMap<String, Object>) map.get("extattr");
          ArrayList<HashMap<String,Object>> attrs = (ArrayList<HashMap<String,Object>>) extattr.get("attrs");
          for (int j = 0; j < attrs.size(); ) {
            HashMap<String, Object> map1 = attrs.get(j);
            for (Map.Entry<String, String> entry:entries) {
              String key = Util.null2String(entry.getKey());
              String s = Util.null2String((String) map1.get("name"));
              //这个地方传入key
              if(s.equals(key)){
                attrs.remove(j);
                j--;
                break;
              }
            }
            j++;
          }
        }
      }
    }catch (Exception e){
      writeLog("==hou 2196658 =============前台人事卡片 基础信息 报错==");
      writeLog(e);
    }
    return retmap;
  }

  /***
   * 获取组织结构
   * @return
   */
  private Map<String, Object> getOrganizationInfo(){
    Map<String, Object> retmap = new HashMap<String, Object>();
    try{
      String id = Util.null2String(params.get("id"));//节点id
      String type = "";//节点类型 1 com，2 subcom,  3 dept
      String tmpStr = id;
      if(id.length()>0) {
        if (id.startsWith("-")) {
          tmpStr = id.replace("-", "");
          type = tmpStr.substring(0, 1);
          id = "-" + tmpStr.substring(1, tmpStr.length());
        } else {
          type = tmpStr.substring(0, 1);
          id = tmpStr.substring(1, tmpStr.length());
        }

        if (type.equals("1")) {
          type = "com";
          id="0";
        }else{
          type = type.equals("2") ? "com" : "dept";
        }
      }

      String lang_tag = Util.null2String(params.get("lang_tag"));//多语言

      if(id.length()==0){//获取主节点
        List<Object> companylist = new ArrayList<>();
        Map<String, Object> companyInfo = null;
        if(user.isAdmin()||user.getUserDepartment()>0) {
          CompanyComInfo CompanyComInfo = new CompanyComInfo();
          if (CompanyComInfo.getCompanyNum() > 0) {
            CompanyComInfo.setTofirstRow();
            while (CompanyComInfo.next()) {
              companyInfo = new HashMap<>();
              companyInfo.put("id", Util.getIntValue("1" + CompanyComInfo.getCompanyid()));
              companyInfo.put("name", Util.formatMultiLang(CompanyComInfo.getCompanyname(), "" + user.getLanguage()));
              companyInfo.put("parentid", 0);
              companyInfo.put("total_count", this.getResourceNum("com",CompanyComInfo.getCompanyid()));
              companyInfo.put("hasnext", "true");
              companyInfo.put("showorder", "1");
              companylist.add(companyInfo);
            }
          }
        }

        CompanyVirtualComInfo CompanyVirtualComInfo = new CompanyVirtualComInfo();
        if(CompanyVirtualComInfo.getCompanyNum()>0){
          CompanyVirtualComInfo.setTofirstRow();
          while(CompanyVirtualComInfo.next()){
            if(HrmUtil.isHideDefaultDimension(Util.null2String(CompanyVirtualComInfo.getCompanyid()))){
              continue;
            }

            if(!user.isAdmin()&&user.getUserDepartment()==0&&!CompanyVirtualComInfo.getCompanyid().equals("-10000"))continue;
            companyInfo = new HashMap<>();
            companyInfo.put("id",Util.getIntValue("-1"+CompanyVirtualComInfo.getCompanyid().replace("-","")));
            companyInfo.put("name",Util.formatMultiLang(CompanyVirtualComInfo.getVirtualType(),""+user.getLanguage()));
            companyInfo.put("parentid",0);
            companyInfo.put("total_count", this.getResourceNum("com",CompanyVirtualComInfo.getCompanyid()));
            companyInfo.put("hasnext","true");
            companyInfo.put("showorder",CompanyVirtualComInfo.getShowOrder());
            companylist.add(companyInfo);
          }
        }
        retmap.put("department",companylist);
      }else{
        if(type.equals("com")){
          if(Util.null2String(params.get("id")).startsWith("-")){
            retmap.putAll(getSubCompanyVirtualList(id));
          }else{
            retmap.putAll(getSubCompanyList(id));
          }
        }else if(type.equals("dept")){
          if(Util.null2String(params.get("id")).startsWith("-")) {
            retmap.putAll(getDepartmentVirtualList("", id));
          }else{
            retmap.putAll(getDepartmentList("", id));
          }
        }
      }
    }catch (Exception e){
      writeLog(e);
    }
    return retmap;
  }

  private Map<String,Object> getSubCompanyList(String subId){
    Map<String,Object> resultMap = new HashMap<>();
    List<Object> subcompanyList = new ArrayList<>();
    Map<String,Object> subcompanyInfo = null;
    try{

      String fetch_mine = Util.null2String(params.get("fetch_mine"));// 0 全部 1-我的部门 2-我的分部
      String parentid = Util.null2String(params.get("id"));
      if(adci==null) adci = new AppDetachComInfo(user);
      adci.setVirtualType("1");
      List<String> allowsubcompany=adci.getAlllowsubcompany();//能查看的分部
      List<String> allowsubcompanyview=adci.getAlllowsubcompanyview();//能查看的分部含必要的上级
      SubCompanyComInfo rs = new SubCompanyComInfo();
      rs.setTofirstRow();
      String allparentsubcompanyid = "";
      String allchildsubcompanyid = "";
      if(fetch_mine.equals("1") || fetch_mine.equals("2")){
        allparentsubcompanyid = rs.getAllSupCompany(""+user.getUserSubCompany1());
        allchildsubcompanyid = rs.getAllChildSubcompanyId(""+user.getUserSubCompany1(),allchildsubcompanyid);
        if(!allchildsubcompanyid.endsWith(",")){
          allchildsubcompanyid+=","+user.getUserSubCompany1();
        }else{
          allchildsubcompanyid+=user.getUserSubCompany1();
        }
        allchildsubcompanyid = ","+allchildsubcompanyid+",";
        allparentsubcompanyid = ","+allparentsubcompanyid+",";
      }


      if(fetch_mine.equals("1") || fetch_mine.equals("2")){
        if(allchildsubcompanyid.indexOf(","+subId+",")>=0){
          resultMap.putAll(this.getDepartmentList(subId, "0"));
        }
      }else {
        resultMap.putAll(this.getDepartmentList(subId, "0"));
      }

      while (rs.next()) {
        String id = rs.getSubCompanyid();
        String supsubcomid = rs.getSupsubcomid();
        if (supsubcomid.equals(""))supsubcomid = "0";
        if (!supsubcomid.equals(subId))continue;
        if("1".equals(rs.getCompanyiscanceled()))continue;
        //我的部门，不管需不需要显示所有下级，分部都只展示本分部
        if(fetch_mine.equals("1")){
          if(allparentsubcompanyid.indexOf(","+id+",")<0 && !id.equals(user.getUserSubCompany1()+"")) continue;
        }else if(fetch_mine.equals("2")){
          //我的分部
          if(this.showChild){
            if(allparentsubcompanyid.indexOf(","+id+",")<0 && allchildsubcompanyid.indexOf(","+id+",")<0) continue;
          }else{
            if(allparentsubcompanyid.indexOf(","+id+",")<0 && !id.equals(user.getUserSubCompany1()+"")) continue;
          }
        }

        boolean flag = true;
        if(allowsubcompany.size()>0||allowsubcompanyview.size()>0){
          flag = false;
          if(allowsubcompany.contains(id)){
            flag = true;
          }

          if(!flag && allowsubcompanyview.contains(id)){
            flag = true;
          }
        }
        if(!flag)continue;

        String name = rs.getSubCompanyname();
        String showorder = rs.getShoworder();
        if(!flag)continue;
        subcompanyInfo = new HashMap<>();
        subcompanyInfo.put("id",Util.getIntValue("2"+id));
        subcompanyInfo.put("name",Util.formatMultiLang(name,""+user.getLanguage()));
        subcompanyInfo.put("hasnext",hasChild("subcompany", id));
        subcompanyInfo.put("parentid",Util.getIntValue(parentid));
        subcompanyInfo.put("total_count",this.getResourceNum("subcom",id));
        subcompanyInfo.put("showorder",showorder);
        subcompanyList.add(subcompanyInfo);
      }
      if(resultMap.get("department")!=null){
        List<Object> tmpList = (List<Object>)resultMap.get("department");
        tmpList.addAll(subcompanyList);
        resultMap.put("department",tmpList);
      }else{
        resultMap.put("department",subcompanyList);
      }
    }catch (Exception e){
      writeLog(e);
    }finally {

    }
    return resultMap;
  }

  private Map<String,Object> getDepartmentList(String subId, String departmentId){
    Map<String,Object> returnMap = new HashMap<>();
    List<Object> departmentList = new ArrayList<>();
    Map<String,Object> departmentInfo = null;
    try{
      DepartmentComInfo rsDepartment = new DepartmentComInfo();
      rsDepartment.setTofirstRow();

      String fetch_mine = Util.null2String(params.get("fetch_mine"));// 0 全部 1-我的部门 2-我的分部
      String parentid = Util.null2String(params.get("id"));
      String allparentdepartmentid = "";
      String allchilddepartmentid = "";
      if(fetch_mine.equals("1")){
        allparentdepartmentid = rsDepartment.getAllSupDepartment(""+user.getUserDepartment());
        allchilddepartmentid = rsDepartment.getAllChildDepartId(""+user.getUserDepartment(),allchilddepartmentid);
        if(!allchilddepartmentid.endsWith(",")){
          allchilddepartmentid+=","+user.getUserDepartment();
        }else{
          allchilddepartmentid+=user.getUserDepartment();
        }
        allchilddepartmentid = ","+allchilddepartmentid+",";
        allparentdepartmentid = ","+allparentdepartmentid+",";
      }
      if(Util.getIntValue(departmentId)>0){
        subId = rsDepartment.getSubcompanyid1(departmentId);
        if(fetch_mine.equals("1")){
          if(allchilddepartmentid.indexOf(","+departmentId+",")>=0){
            returnMap.put("userlist",this.getResourceList(departmentId));
          }
        }else {
          returnMap.put("userlist", this.getResourceList(departmentId));
        }
      }

      if(adci==null) adci = new AppDetachComInfo(user);
      adci.setVirtualType("1");
      List<String> allowsubcompany=adci.getAlllowsubcompany();//能查看的分部
      List<String> allowdepartment=adci.getAlllowdepartment();//能查看的部门
      List<String> allowdepartmentview=adci.getAlllowdepartmentview();//能查看的部门含必要的上级
      while (rsDepartment.next()) {
        if(departmentId.equals(rsDepartment.getDepartmentid()))continue;
        if("1".equals(rsDepartment.getDeparmentcanceled()))continue;
        String supdepid = rsDepartment.getDepartmentsupdepid();
        if (departmentId.equals("0") && supdepid.equals(""))supdepid = "0";
        if(this.showChild){
          if(fetch_mine.equals("1") && allparentdepartmentid.indexOf(","+rsDepartment.getDepartmentid()+",")<0 && allchilddepartmentid.indexOf(","+rsDepartment.getDepartmentid()+",")<0){
            continue;
          }
        }else{
          if(fetch_mine.equals("1") && allparentdepartmentid.indexOf(","+rsDepartment.getDepartmentid()+",")<0 && !rsDepartment.getDepartmentid().equals(user.getUserDepartment()+"")){
            continue;
          }else if(fetch_mine.equals("2") && !rsDepartment.getSubcompanyid1().equals(subId)){
            continue;
          }
        }

        if (!(rsDepartment.getSubcompanyid1().equals(subId) && (supdepid.equals(departmentId)||(!rsDepartment.getSubcompanyid1(supdepid).equals(subId)&&departmentId.equals("0"))))) continue;

        String id = rsDepartment.getDepartmentid();
        String subcompanyid1 = rsDepartment.getSubcompanyid1();
        String name = rsDepartment.getDepartmentname();
        String showorder = rsDepartment.getShoworder();

        boolean flag = true;
        if(allowsubcompany.size()>0||allowdepartment.size()>0||allowdepartmentview.size()>0){
          flag = false;
          if(allowdepartment.contains(id)){
            flag = true;
          }

          if(!flag && allowdepartmentview.contains(id)){
            flag = true;
          }

          if(!flag && allowsubcompany.contains(subcompanyid1)){
            flag = true;
          }
        }
        if(!flag)continue;
        departmentInfo = new HashMap<>();
        departmentInfo.put("id",Util.getIntValue("3"+id));
        departmentInfo.put("name",Util.formatMultiLang(name,""+user.getLanguage()));
        departmentInfo.put("hasnext",hasChild("dept", id));
        departmentInfo.put("parentid",Util.getIntValue(parentid));
        departmentInfo.put("showorder",showorder);
        departmentInfo.put("total_count",this.getResourceNum("dept",id));//部门下的人数
        departmentList.add(departmentInfo);
      }
      returnMap.put("department",departmentList);
    }catch (Exception e){
      writeLog(e);
    }
    return returnMap;
  }

  /***
   * 根据部门获取人员
   * @param departmentId
   * @return
   */
  private List<Object> getResourceList(String departmentId){
    List<Object> resourceList = new ArrayList<>();
    Map<String,Object> resourceInfo = null;
    RecordSet rs = new RecordSet();
    String sql = "";
    try{
      DepartmentComInfo departmentComInfo = new DepartmentComInfo();
      ResourceComInfo resourceComInfo = new ResourceComInfo();
      JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
      boolean enable_main_account = "1".equals(Util.null2String(params.get("enable_main_account")).trim());
      departmentComInfo.setTofirstRow();
      if(adci==null) adci = new AppDetachComInfo(user);
      sql = " select hr.id, hr.lastname, hr.sex, hr.managerid, hr.jobtitle, hr.status, hr.dsporder, hr.messagerurl, hr.pinyinlastname, hr.accounttype, hr.belongto  "
              + " from hrmresource hr, hrmdepartment t2 "
              + " where hr.departmentid=t2.id and t2.id=?" ;
      String sqlwhere =" and hr.status in (0,1,2,3)";
      if(adci.isUseAppDetach()){
        String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
        String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
        sqlwhere+=tempstr;
      }
      if(sqlwhere.length()>0)sql+=sqlwhere;
      sql += " order by hr.dsporder ";
      rs.executeQuery(sql,departmentId);
      while (rs.next()) {
        String userid = rs.getString("id");
        String lastname = Util.formatMultiLang(rs.getString("lastname"),""+user.getLanguage());
        String accounttype = rs.getString("accounttype");
        String belongto = "1".equals(accounttype)?Util.null2String(rs.getString("belongto")):"";
        String belongto_name = Util.formatMultiLang(resourceComInfo.getLastname(belongto),""+user.getLanguage());
        String pinyinlastname = Util.null2String(rs.getString("pinyinlastname"));
        String sex = Util.null2String(rs.getString("sex"),"0");
        String managerid = rs.getString("managerid");
        String managername = Util.formatMultiLang(managerid.length() > 0 ? resourceComInfo.getResourcename(managerid) : "",""+user.getLanguage());
        String jobtitle = rs.getString("jobtitle");
        String jobtitlename = Util.formatMultiLang(jobTitlesComInfo.getJobTitlesname(jobtitle),""+user.getLanguage());
        String status = rs.getString("status");
        String showorder = Util.toDecimalDigits(rs.getString("dsporder"),2);
        String messagerurl = rs.getString("messagerurl");
        resourceInfo = new HashMap<>();

        resourceInfo.put("userid",userid);
        resourceInfo.put("name",lastname);
        if(enable_main_account){
          resourceInfo.put("main_base_user_id",belongto );
          resourceInfo.put("main_base_user_name",belongto_name);
        }
        resourceInfo.put("manager_id",managerid);
        resourceInfo.put("manager_name",managername);
        resourceInfo.put("position",jobtitlename);
        resourceInfo.put("status",status);
        resourceInfo.put("showorder",showorder);
        resourceInfo.put("gender",Util.getIntValue(sex));
        resourceInfo.put("base_user_id",userid);
        resourceInfo.put("base_user_name",lastname);
        resourceInfo.put("name_simple_pingyin",pinyinlastname);
        resourceInfo.put("name_full_pingyin",pinyinlastname);
        resourceInfo.put("guangliyuanxiashu",pinyinlastname);
        resourceInfo.put("base_name_simple_pingyin",pinyinlastname);
        resourceInfo.put("base_name_full_pingyin",pinyinlastname);
        Map<String,Object> avatar = new HashMap<>();//头像信息
        params.put("userId",userid);
        Map<String,Object> userIconInfo = ((HrmUserIconService) ServiceUtil.getService(HrmUserIconServiceImpl.class, user)).getUserIcon(params,user);
        String media_id = userIconInfo.get("messagerurl").toString();
        //这种格式得手机端显示不出来,取默认设置图片
        if(media_id.startsWith("/weaver/weaver.file.FileDownload")){
          media_id = userIconInfo.get("defaultmessagerurl").toString();
        }else if(messagerurl.length()>0){
          String filePath = GCONST.getRootPath() + media_id;
          File file = new File(filePath);
          if(!file.exists()){
            media_id = userIconInfo.get("defaultmessagerurl").toString();
          }
        }
        resourceInfo.put("avatar",avatar);
        avatar.put("default","");
        avatar.put("show_name", userIconInfo.get("shortname").toString());
        avatar.put("show_color",userIconInfo.get("background").toString());
        avatar.put("media_id",media_id);
        resourceList.add(resourceInfo);
      }
    }catch (Exception e){
      writeLog(e);
    }
    return resourceList;
  }

  /**
   * 指定节点下是否有子节点
   * @param type  com:分部;dept:部门
   * @param id   节点id
   * @return  boolean
   * @throws Exception
   */
  private boolean hasChild(String type, String id) throws Exception {
    boolean hasChild = false;
    if (type.equals("subcompany")) {
      SuperSubCompanyComInfo superSubCompanyComInfo = LoadComInfo.getInstance(SuperSubCompanyComInfo.class) ;
      SubCompanyComInfo subCompanyComInfo = new SubCompanyComInfo() ;

      String downSubIds = superSubCompanyComInfo.getDownIdsBySuperId(id) ;
      if(!StringUtils.isBlank(downSubIds)){
        return true ;
      }

      SuperDepartmentComInfo superDepartmentComInfo = LoadComInfo.getInstance(SuperDepartmentComInfo.class) ;
      String downDeptIds = superDepartmentComInfo.getDepartIdsBySubcompanyId(id) ;
      if(StringUtils.isNotBlank(downDeptIds)){
        return true ;
      }

    } else if (type.equals("dept")) {
      SuperDepartmentComInfo superDepartmentComInfo = LoadComInfo.getInstance(SuperDepartmentComInfo.class) ;
      String downDeptIds = superDepartmentComInfo.getDepartIdsBySuperId(id) ;
      if(StringUtils.isNotBlank(downDeptIds)){
        return true ;
      }
      KVResourceComInfo kvResourceComInfo = LoadComInfo.getInstance(KVResourceComInfo.class) ;
      String resourceids = kvResourceComInfo.getResourceIdsByDeptId(id) ;
      if(StringUtils.isNotBlank(resourceids)){
        return true ;
      }


    }
    return hasChild;
  }


  private Map<String,Object> getSubCompanyVirtualList(String subId){
    Map<String,Object> resultMap = new HashMap<>();
    List<Object> subcompanyList = new ArrayList<>();
    Map<String,Object> subcompanyInfo = null;
    try{
      resultMap.putAll(this.getDepartmentVirtualList(subId, "0"));
      String fetch_mine = Util.null2String(params.get("fetch_mine"));//是否本部门
      String parentid = Util.null2String(params.get("id"));
      SubCompanyVirtualComInfo rs = new SubCompanyVirtualComInfo();
      rs.setTofirstRow();
      String virtualtype = "";
      if(subId.equals("0")){
        String tmpStr = Util.null2String(params.get("id")).replace("-", "");
        virtualtype = "-" + tmpStr.substring(1, tmpStr.length());
      }else{
        virtualtype = rs.getVirtualtypeid(subId);
      }

      while (rs.next()) {
        String id = rs.getSubCompanyid();
        String supsubcomid = rs.getSupsubcomid();
        if (supsubcomid.equals(""))supsubcomid = "0";
        if (!supsubcomid.equals(subId))continue;
        if("1".equals(rs.getCompanyiscanceled()))continue;
        if(!virtualtype.equals(rs.getCompanyid()))continue;
//        if (!this.checkDetach("com", id)) {//检查机构分权和应用分权
//          continue;
//        }
        String name = rs.getSubCompanyname();
        String showorder = rs.getShowOrder();

        subcompanyInfo = new HashMap<>();
        subcompanyInfo.put("id",+Util.getIntValue("-2"+id.replace("-","")));
        subcompanyInfo.put("name",Util.formatMultiLang(name,""+user.getLanguage()));
        subcompanyInfo.put("hasnext",hasChildVirtual("subcompany", id));
        subcompanyInfo.put("parentid",Util.getIntValue(parentid));
        subcompanyInfo.put("showorder",showorder);
        subcompanyInfo.put("total_count",this.getResourceNum("subcom",id));
        subcompanyList.add(subcompanyInfo);
      }
      if(resultMap.get("department")!=null){
        List<Object> tmpList = (List<Object>)resultMap.get("department");
        tmpList.addAll(subcompanyList);
        resultMap.put("department",tmpList);
      }else{
        resultMap.put("department",subcompanyList);
      }
    }catch (Exception e){
      writeLog(e);
    }finally {

    }
    return resultMap;
  }

  private Map<String,Object> getDepartmentVirtualList(String subId, String departmentId){
    Map<String,Object> returnMap = new HashMap<>();
    List<Object> departmentList = new ArrayList<>();
    Map<String,Object> departmentInfo = null;
    try{
      DepartmentVirtualComInfo rsDepartment = new DepartmentVirtualComInfo();
      rsDepartment.setTofirstRow();
      String parentid = Util.null2String(params.get("id"));
      if(Util.null2String(departmentId).startsWith("-")){
        subId = rsDepartment.getSubcompanyid1(departmentId);
        returnMap.put("userlist",this.getResourceVirtualList(departmentId));
      }
      while (rsDepartment.next()) {
        if(departmentId.equals(rsDepartment.getDepartmentid()))continue;
        if("1".equals(rsDepartment.getDeparmentcanceled()))continue;
        String supdepid = rsDepartment.getDepartmentsupdepid();
        if (departmentId.equals("0") && supdepid.equals(""))supdepid = "0";
        if (!(rsDepartment.getSubcompanyid1().equals(subId) && (supdepid.equals(departmentId)||(!rsDepartment.getSubcompanyid1(supdepid).equals(subId)&&departmentId.equals("0"))))) continue;

        String id = rsDepartment.getDepartmentid();
        String subcompanyid1 = rsDepartment.getSubcompanyid1();
        String name = rsDepartment.getDepartmentname();
        String showorder = rsDepartment.getShowOrder();
//        if (!this.checkDetach("dept", id)) {//检查机构分权和应用分权
//          continue;
//        }
        departmentInfo = new HashMap<>();
        departmentInfo.put("id",+Util.getIntValue("-3"+id.replace("-","")));
        departmentInfo.put("name",Util.formatMultiLang(name,""+user.getLanguage()));
        departmentInfo.put("hasnext",hasChildVirtual("dept", id));
        departmentInfo.put("parentid",Util.getIntValue(parentid));
        departmentInfo.put("showorder",showorder);
        departmentInfo.put("total_count",this.getResourceNum("dept",id));//部门下的人数
        departmentList.add(departmentInfo);
      }
      returnMap.put("department",departmentList);
    }catch (Exception e){
      writeLog(e);
    }
    return returnMap;
  }

  /***
   * 根据部门获取人员
   * @param departmentId
   * @return
   */
  private List<Object> getResourceVirtualList(String departmentId){
    List<Object> resourceList = new ArrayList<>();
    Map<String,Object> resourceInfo = null;
    RecordSet rs = new RecordSet();
    String sql = "";
    try{
      ResourceComInfo resourceComInfo = new ResourceComInfo();
      JobTitlesComInfo jobTitlesComInfo = new JobTitlesComInfo();
      if(adci==null) adci = new AppDetachComInfo(user);
      sql = " select hr.id, hr.lastname, hr.sex, hr.managerid, hr.jobtitle, hr.status, hr.dsporder, hr.messagerurl, hr.pinyinlastname,hr.dsporder "
              + " from hrmresource hr "
              + " where 1=1 ";
      String sqlwhere =" and hr.status in (0,1,2,3)";
      if(adci.isUseAppDetach()){
        String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
        String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
        sqlwhere+=tempstr;
      }
      if(sqlwhere.length()>0)sql+=sqlwhere;
      sql += " and exists ( select * from hrmresourcevirtual where hr.id = resourceid and departmentid= ? )";
      sql += " order by hr.dsporder ";
      rs.executeQuery(sql,departmentId);
      while (rs.next()) {
        String userid = rs.getString("id");
        String lastname = Util.formatMultiLang(rs.getString("lastname"),""+user.getLanguage());
        String pinyinlastname = Util.null2String(rs.getString("pinyinlastname"));
        String sex = Util.null2String(rs.getString("sex"),"0");
        String managerid = rs.getString("managerid");
        String managername = Util.formatMultiLang(managerid.length() > 0 ? resourceComInfo.getResourcename(managerid) : "",""+user.getLanguage());
        String jobtitle = rs.getString("jobtitle");
        String jobtitlename = Util.formatMultiLang(jobTitlesComInfo.getJobTitlesname(jobtitle),""+user.getLanguage());
        String status = rs.getString("status");
        String showorder = Util.toDecimalDigits(rs.getString("dsporder"),2);
        String messagerurl = rs.getString("messagerurl");
        resourceInfo = new HashMap<>();

        resourceInfo.put("userid",userid);
        resourceInfo.put("name",lastname);
        resourceInfo.put("manager_id",managerid);
        resourceInfo.put("manager_name",managername);
        resourceInfo.put("position",jobtitlename);
        resourceInfo.put("status",status);
        resourceInfo.put("showorder",showorder);
        resourceInfo.put("gender",Util.getIntValue(sex));
        resourceInfo.put("base_user_id",userid);
        resourceInfo.put("base_user_name",lastname);
        resourceInfo.put("name_simple_pingyin",pinyinlastname);
        resourceInfo.put("name_full_pingyin",pinyinlastname);
        resourceInfo.put("guangliyuanxiashu",pinyinlastname);
        resourceInfo.put("base_name_simple_pingyin",pinyinlastname);
        resourceInfo.put("base_name_full_pingyin",pinyinlastname);
        Map<String,Object> avatar = new HashMap<>();//头像信息
        resourceInfo.put("avatar",avatar);
        avatar.put("default","");
        avatar.put("show_name", User.getLastname(lastname));
        avatar.put("show_color",sex.equals("0")?"#008cee":"#FF0000");
        avatar.put("media_id",messagerurl);
        resourceList.add(resourceInfo);
      }
    }catch (Exception e){
      writeLog(e);
    }
    return resourceList;
  }

  /**
   * 指定节点下是否有子节点
   * @param type  com:分部;dept:部门
   * @param id   节点id
   * @return  boolean
   * @throws Exception
   */
  private boolean hasChildVirtual(String type, String id) throws Exception {
    boolean hasChild = false;
    if (type.equals("subcompany")) {
      SubCompanyVirtualComInfo rs = new SubCompanyVirtualComInfo();
      rs.setTofirstRow();
      while (rs.next()) {
        if (rs.getSupsubcomid().equals(id) && !"1".equals(rs.getCompanyiscanceled()))
          hasChild = true;
      }
      DepartmentVirtualComInfo rsDepartment = new DepartmentVirtualComInfo();
      rsDepartment.setTofirstRow();
      while (rsDepartment.next()) {
        if (rsDepartment.getSubcompanyid1().equals(id) && !"1".equals(rsDepartment.getDeparmentcanceled())) {
          hasChild = true;
        }
      }
    } else if (type.equals("dept")) {
      DepartmentVirtualComInfo rsDepartment = new DepartmentVirtualComInfo();
      rsDepartment.setTofirstRow();
      while (rsDepartment.next()) {
        String str = rsDepartment.getSubcompanyid1(id);
        if (rsDepartment.getSubcompanyid1().equals(str) && rsDepartment.getDepartmentsupdepid().equals(id) && !"1".equals(rsDepartment.getDeparmentcanceled()))
          hasChild = true;
      }
      if(!hasChild){
        RecordSet rs = new RecordSet();
        rs.executeQuery("select count(*) from HrmResourceVirtualView t1 where t1.status in (0,1,2,3) and t1.departmentid=" + id);
        if(rs.next()){
          if(rs.getInt(1)>0){
            hasChild=true;
          }
        }
      }
    }
    return hasChild;
  }

  /**
   * 检查机构分权和应用分权
   *
   * @param type
   * @param id
   * @return
   */
  private boolean checkDetach(String type, String id) {
    boolean hasRight = true;
    if (type.equals("com")) {
      if (adci == null) {
        adci = new AppDetachComInfo(user);
      }
      if (adci.isUseAppDetach()) {
        if (adci.checkUserAppDetach(id, "2") == 0) {
          hasRight = false;
        } else {
          hasRight = true;
        }
      }
    } else if (type.equals("dept")) {
      if (adci == null) {
        adci = new AppDetachComInfo(user);
      }
      if (adci.isUseAppDetach()) {
        if (adci.checkUserAppDetach(id, "3") == 0) {
          hasRight = false;
        } else {
          hasRight = true;
        }
      }
    }
    return hasRight;
  }

  private int getResourceDepts(String selfDepartContainChilds){
    KVResourceComInfo kvResourceComInfo = LoadComInfo.getInstance(KVResourceComInfo.class) ;
    if(StringUtils.isBlank(selfDepartContainChilds)) return 0 ;
    StringTokenizer tokenizer = new StringTokenizer(selfDepartContainChilds,",") ;
    int num = 0 ;
    while(tokenizer.hasMoreTokens()){
      String singleDeptId = tokenizer.nextToken() ;
      String resourceids = kvResourceComInfo.getResourceIdsByDeptId(singleDeptId) ;
      num += getResourceNumWithDetach(resourceids);
    }

    return num ;
  }

  public int getResourceNumWithDetach(String resourceids){
    int num = 0 ;
    if(StringUtils.isBlank(resourceids)) return num ;
    StringTokenizer resourceToken = new StringTokenizer(resourceids,",") ;
    while(resourceToken.hasMoreTokens()){
      String resourceId = resourceToken.nextToken() ;
      if(adci.isUseAppDetach() && adci.isDetachUser(user.getUID()+"")){
        if (adci.checkUserResourceInfo(resourceId)) {
          num += 1;
        }
      }else{
        num+= 1 ;
      }
    }

    return num ;
  }

  public int getResourceSubcompanyIds(String subids){
    KVResourceComInfo kvResourceComInfo = LoadComInfo.getInstance(KVResourceComInfo.class) ;
    if(StringUtils.isBlank(subids)) return 0 ;
    StringTokenizer tokenizer = new StringTokenizer(subids,",") ;
    int num = 0 ;
    while(tokenizer.hasMoreTokens()){
      String subId = tokenizer.nextToken() ;
      String resourceids = kvResourceComInfo.getResourceBySubcompanyId(subId) ;
      num += getResourceNumWithDetach(resourceids);
    }

    return num ;
  }


  /**
   * 统计部门人数
   * @param type
   * @param id
   * @return
   * @throws Exception
   */
  private int getResourceNum(String type,String id)throws Exception{
    int resourceNum = 0;
    if(Util.null2String(this.params.get("needcount")).equals("0")){//如果禁用统计人数，则返回0
      return resourceNum;
    }

    String fetch_mine = Util.null2String(params.get("fetch_mine"));//是否本部门
    if (id2count.containsKey(type+"_"+id) && !fetch_mine.equals("1")) {
      resourceNum = Util.getIntValue(Util.null2String(id2count.get(type+"_"+id)));
      return resourceNum<0?0:resourceNum;
    }

    RecordSet rs = new RecordSet();
    String sql = " ";
    String sqlwhere = "";

    String selfDepartContainChilds = "" ;

    if(fetch_mine.equals("1")){
      String mychildids = "";
      if(id.startsWith("-")) {
        mychildids = DepartmentVirtualComInfo.getAllChildDepartId("" + user.getUserDepartment(), mychildids);
      }else if(this.showChild){
        mychildids = DepartmentComInfo.getAllChildDepartId("" + user.getUserDepartment(), mychildids);
      }
      if(mychildids.length()>0){
        if(mychildids.startsWith(",")){
          mychildids=user.getUserDepartment()+mychildids;
        }else{
          mychildids=user.getUserDepartment()+","+mychildids;
        }
      }else{
        mychildids=""+user.getUserDepartment();
      }
      if (type.equals("dept")&&("," + mychildids + ",").indexOf("," + id + ",") > 0) {
      } else {
        if (id.startsWith("-")) {
          sqlwhere = " and hrv.departmentid in (" + mychildids + ") ";
        } else {
          //sqlwhere = " and hr.departmentid in (" + mychildids + ") ";

          SuperDepartmentComInfo superDepartmentComInfo = LoadComInfo.getInstance(SuperDepartmentComInfo.class) ;
          String downDeptIds = superDepartmentComInfo.getAllDownIdsBySuperId(user.getUserDepartment()+"") ;
          if(StringUtils.isNotBlank(downDeptIds) && this.showChild ){
            if(selfDepartContainChilds.length() > 0) selfDepartContainChilds+="," ;
            selfDepartContainChilds += downDeptIds ;
          }
          if(selfDepartContainChilds.length() > 0) selfDepartContainChilds += "," ;
          selfDepartContainChilds += user.getUserDepartment() ;

          return getResourceDepts(selfDepartContainChilds) ;

        }
      }
    }
    if(!id.startsWith("-")) {
      if(type.equals("dept")){
        String deptids = DepartmentComInfo.getAllChildDepartId(id, id) ;
        //我的部门，展示所有下级，那么人数就应该包含下级部门
        //我的部门，不展示所有下级，人数就显示本部门人数即可
        //我的分部，不管展不展示下级，都是显示所有部门，所以需要包含下级部门人数
        if(fetch_mine.equals("1") && !this.showChild){
          deptids = id;
        }
        return getResourceDepts(deptids) ;
      }else if(type.equals("subcom")){
        String subIds = id;
        String currentUserSubId = user.getUserSubCompany1()+"";
        if(fetch_mine.equals("2")){
          String subchilds = SubCompanyComInfo.getAllChildSubcompanyId(currentUserSubId,currentUserSubId) ;
          if(!(","+subchilds+",").contains(","+id+",")){
            subIds = currentUserSubId;
          }
          //我的分部需要根据是否显示下级开关控制
          if(this.showChild ){
            subIds = SubCompanyComInfo.getAllChildSubcompanyId(subIds, subIds) ;
          }
        }else{
          //不是我的分部时不走这个开关
          subIds = SubCompanyComInfo.getAllChildSubcompanyId(subIds, subIds) ;
        }
        return getResourceSubcompanyIds(subIds) ;
      }else{
        sql = " select count(1) from hrmresource hr where status in (0,1,2,3) ";
        if(fetch_mine.equals("2")){
          String allSubIds = user.getUserSubCompany1()+"";
          if(this.showChild ){
            allSubIds = SubCompanyComInfo.getAllChildSubcompanyId(allSubIds, allSubIds) ;
          }
          sql = " select count(1) from hrmresource hr where status in (0,1,2,3) and subcompanyid1 in ( "+allSubIds+" )";
        }
      }
    }else{
      sql = " select count(1) from hrmresource hr, hrmresourcevirtual hrv where hr.id=hrv.resourceid and hr.status in (0,1,2,3) ";
      if(type.equals("dept")){
        sql += " and hrv.departmentid in ( "+ DepartmentVirtualComInfo.getAllChildDepartId(id, id) +" )";
      }else if(type.equals("subcom")){
        sql += " and hrv.subcompanyid in ( "+ SubCompanyVirtualComInfo.getAllChildSubcompanyId(id, id) +" )";
      }else if(type.equals("com")){
        sql += " and hrv.virtualtype ="+id;
      }
    }

    if(adci.isUseAppDetach()){
      String appdetawhere = adci.getScopeSqlByHrmResourceSearch(user.getUID()+"",true,"resource_hr");
      String tempstr= (appdetawhere!=null&&!"".equals(appdetawhere)?(" and " + appdetawhere):"");
      sqlwhere+=tempstr;
    }
    if(sqlwhere.length()>0)sql+=sqlwhere;
    rs.executeQuery(sql);
    if(rs.next()){
      resourceNum = rs.getInt(1);
    }
//    if(!fetch_mine.equals("1")) {
//      id2count.put(type + "_" + id, resourceNum);
//    }
    return resourceNum;
  }

  public void needInit(){
    RecordSet rs = new RecordSet();
    String sql = " ";
    sql = " select max(modified) as modified from (" +
            " SELECT max(modified) as modified FROM HrmResource " +
            " union all " +
            " SELECT max(modified) as modified FROM hrmdepartment "+
            " union all " +
            " SELECT max(modified) as modified FROM HrmSubCompany) t ";
    rs.executeQuery(sql);
    if(rs.next()){
      String modified = Util.null2String(rs.getString("modified"));
      if(modified.length()>0){
        if(id2count.get("modified")==null){
          id2count.clear();
          id2count.put("modified",rs.getString("modified"));
        }else{
          if(Util.null2String(id2count.get("modified")).compareTo(modified)<0){
            id2count.clear();
          }
        }
      }
    }
  }

  @Override
  public BizLogContext getLogContext() {
    // TODO Auto-generated method stub
    return null;
  }
}
