package com.engine.common.biz;

import com.alibaba.fastjson.JSON;
import com.api.browser.bean.SearchConditionOption;
import com.engine.common.entity.EncryptFieldEntity;
import com.engine.common.entity.EncryptSecondAuthEntity;
import com.engine.common.entity.EncryptShareSettingEntity;
import com.engine.common.entity.EncryptSpecialAuthEntity;
import com.engine.common.enums.EncryptMould;
import com.engine.common.enums.SecondAuthType;
import com.engine.encrypt.biz.*;
import com.engine.encrypt.util.EncryptUtil;
import com.engine.hrm.biz.HrmSanyuanAdminBiz;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import weaver.common.DateUtil;
import weaver.conn.RecordSet;
import weaver.filter.XssUtil;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;
import weaver.hrm.settings.HrmSettingsComInfo;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 数据安全公共类
 */
public class EncryptConfigBiz extends BaseBean {

  /**
   * 是否有数据安全维护权限
   * 1、如果开启了【三员分立管理】，则数据安全总入口的设置只能由一级保密员操作，具体业务模块的加密设置还是由相应的管理员操作，一级保密员可在总入口看到所有设置情况。
   * 2、如果没有开启【三员分立管理】，则数据安全总入口的设置由系统管理员（sysadmin)操作，具体业务模块加密设置可由相应的管理员操作，sysadmin可在总入口看到所有设置情况
   *
   * @param user
   * @return
   */
  public static boolean hasRight(User user) {
    boolean hasRight = false;
    boolean sanyuanAble = HrmSanyuanAdminBiz.getSanyuanAble();
    if(HrmUserVarify.checkUserRight("DataSecurity:Manage", user)){
      hasRight = true;
    }else{
      hasRight = user.getUID() == 1;
    }
    return hasRight;
  }
  /**
   * 根据表名和字段名获取字段配置
   *
   * @param tablename
   * @param fieldname
   * @param fromdb 是否来自数据库
   * @return
   */
  public static EncryptFieldEntity getFieldEncryptConfig(String tablename, String fieldname, boolean fromdb) {
    EncryptFieldEntity encryptFieldEntity = null;
    if(fromdb){
      String sql = " select * from enc_field_config_info where tablename=? and fieldname = ?";
      RecordSet rs = new RecordSet();
      rs.executeQuery(sql,tablename,fieldname);
      if(rs.next()){
        encryptFieldEntity = new EncryptFieldEntity();
        encryptFieldEntity.setId(rs.getString("id"));
        encryptFieldEntity.setMouldCode(rs.getString("mouldcode"));
        encryptFieldEntity.setTablename(rs.getString("tablename"));
        encryptFieldEntity.setFieldname(rs.getString("fieldname"));
        encryptFieldEntity.setScope(rs.getString("scope"));
        encryptFieldEntity.setScopeid(rs.getString("scopeid"));
        encryptFieldEntity.setDatatablename(rs.getString("datatablename"));
        encryptFieldEntity.setIsEncrypt(rs.getString("isencrypt"));
        encryptFieldEntity.setDesensitization(rs.getString("desensitization"));
        encryptFieldEntity.setSecondauth(rs.getString("secondauth"));
        encryptFieldEntity.setViewScope(rs.getString("viewscope"));
        encryptFieldEntity.setTransMethod(rs.getString("transMethod"));
        encryptFieldEntity.setPrimarykey(rs.getString("primarykey"));
      }
    }
    return encryptFieldEntity;
  }

  /**
   * 根据表名和字段名获取字段配置
   *
   * @param tablename
   * @param fieldname
   * @return
   */
  public static EncryptFieldEntity getFieldEncryptConfig(String tablename, String fieldname) {
    return getFieldEncryptConfig(tablename, fieldname, "", "");
  }

  public static EncryptFieldEntity getFieldEncryptConfig(String tablename, String fieldname, String scope, String scopeid) {
    return new EncryptFieldConfigComInfo().getFieldEncryptConfig(tablename, fieldname, scope, scopeid);
  }

  /**
   * 根据模块和功能获取二次验证标识
   *
   * @param mouldCode
   * @param itemCode
   * @return
   */
  public static EncryptSecondAuthEntity getSecondAuthEncryptConfig(String mouldCode, String itemCode) {
    return new EncryptSecondAuthConfigComInfo().getSecondAuthEncryptConfig(mouldCode, itemCode);
  }

  /**
   * 是否启用二次身份校验的功能
   * @param mouldCode
   * @param itemCode
   * @return
   */
  public static boolean getSecondAuthEnable(String mouldCode, String itemCode, User user) {
    EncryptSecondAuthEntity encryptSecondAuthEntity = EncryptConfigBiz.getSecondAuthEncryptConfig(mouldCode, itemCode);
    return encryptSecondAuthEntity!=null && encryptSecondAuthEntity.getIsEnable().equals("1");
  }

  /**
   * 是否启用二次身份校验的功能
   * @param mouldCode
   * @param itemCode
   * @return
   */
  public static String getSecondAuthFilePath(String mouldCode, String itemCode) {
    String filePath = "";
    EncryptSecondAuthEntity encryptSecondAuthEntity = EncryptConfigBiz.getSecondAuthEncryptConfig(mouldCode, itemCode);
    if(encryptSecondAuthEntity!=null){
      filePath = encryptSecondAuthEntity.getFilePath();
    }
    return filePath;
  }

  /**
   * 根据模块和功能获取特殊身份验证标识
   *
   * @param userId
   * @param itemCode
   * @return
   */
  public static EncryptSpecialAuthEntity getSpecialAuthEncryptConfig(String userId, String itemCode) {
    return new EncryptSpecialAuthConfigComInfo().getSpecialAuthEncryptConfig(userId, itemCode);
  }

  /**
   * 根据模块和功能获取分享加密相关属性
   *
   * @param mouldCode
   * @param itemCode
   * @return
   */
  public static EncryptShareSettingEntity getEncryptShareSetting(String mouldCode, String itemCode) {
    return new EncryptShareConfigComInfo().getEncryptShareSettingConfig(mouldCode, itemCode);
  }

  /**
   * 保存数据库字段加密相关属性
   *
   * @param encryptFieldEntity
   * @param user
   */
  public void saveEncryptFieldConfig(EncryptFieldEntity encryptFieldEntity, User user) {
    List<EncryptFieldEntity> lsEncryptFieldEntity = new ArrayList<>();
    lsEncryptFieldEntity.add(encryptFieldEntity);
    this.saveEncryptFieldConfig(lsEncryptFieldEntity, user);
  }

  /**
   * 保存数据库字段加密相关属性
   *
   * @param lsEncryptFieldEntity
   * @param user
   */
  public void saveEncryptFieldConfig(List<EncryptFieldEntity> lsEncryptFieldEntity, User user) {
    EncryptFieldEntity encryptFieldEntity = null;
    List paramObj = null;
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      List<EncryptFieldEntity> lsEncryptFieldEntityInit = new ArrayList<>();
      EncryptFieldConfigComInfo encryptFieldConfigComInfo = new EncryptFieldConfigComInfo();

      for (int i = 0; i < lsEncryptFieldEntity.size(); i++) {
        encryptFieldEntity = lsEncryptFieldEntity.get(i);
        if (Util.null2String(encryptFieldEntity.getId()).length() == 0) {
          //兼容不传id的case
          sql = " select id from enc_field_config_info where tablename=? and fieldname=?";
          if (Util.null2String(encryptFieldEntity.getScopeid()).length() > 0) {
            sql += " and scopeid = '" + encryptFieldEntity.getScopeid() + "'";
          }
          rs.executeQuery(sql, encryptFieldEntity.getTablename(), encryptFieldEntity.getFieldname());
          if (rs.next()) {
            encryptFieldEntity.setId(rs.getString("id"));
          }
        }

        if (Util.null2String(encryptFieldEntity.getDatatablename()).length() == 0) {
          String tmpTableName = encryptFieldEntity.getTablename() + "_encdata";
          tmpTableName = tmpTableName.replace("formtable_main", "fm");//流程主表超长
          if (tmpTableName.length() > 30) {
            return;
          } else {
            encryptFieldEntity.setDatatablename(tmpTableName);
          }

        }

        if (Util.null2String(encryptFieldEntity.getId()).length() > 0) {
          sql = " select isencrypt from enc_field_config_info where id=? ";
          rs.executeQuery(sql, encryptFieldEntity.getId());
          if (rs.next()) {
            if (rs.getInt("isencrypt") == 1) {
              //加密开启后不能关闭
              encryptFieldEntity.setIsEncrypt("1");
            } else {
              if (Util.null2String(encryptFieldEntity.getNeedInitData()).equals("1") && Util.null2String(encryptFieldEntity.getIsEncrypt()).equals("1")) {
                //首次开启需要初始化
                lsEncryptFieldEntityInit.add(encryptFieldEntity);
              }
            }
          }
          paramObj = new ArrayList();
          paramObj.add(encryptFieldEntity.getIsEncrypt());
          paramObj.add(encryptFieldEntity.getDesensitization());
          paramObj.add(encryptFieldEntity.getSecondauth());
          paramObj.add(encryptFieldEntity.getViewScope());
          paramObj.add(encryptFieldEntity.getTransMethod());
          paramObj.add(encryptFieldEntity.getPrimarykey());
          paramObj.add(user.getUID());
          paramObj.add(DateUtil.getFullDate());
          paramObj.add(encryptFieldEntity.getId());
          sql = " update enc_field_config_info set isencrypt=?, desensitization=?, secondauth=?, viewscope=?,transMethod=?,primarykey=?,modifier=?,modified=? where id = ? ";
          rs.executeUpdate(sql, paramObj);
          encryptFieldConfigComInfo.updateCache(encryptFieldEntity.getId());
        } else {
          paramObj = new ArrayList();
          paramObj.add(encryptFieldEntity.getMouldCode());
          paramObj.add(encryptFieldEntity.getTablename());
          paramObj.add(encryptFieldEntity.getFieldname());
          paramObj.add(encryptFieldEntity.getScope());
          paramObj.add(encryptFieldEntity.getScopeid());
          paramObj.add(encryptFieldEntity.getDatatablename());
          paramObj.add(encryptFieldEntity.getIsEncrypt().length() == 0 ? "0" : encryptFieldEntity.getIsEncrypt());
          paramObj.add(encryptFieldEntity.getDesensitization().length() == 0 ? "0" : encryptFieldEntity.getDesensitization());
          paramObj.add(encryptFieldEntity.getSecondauth().length() == 0 ? "0" : encryptFieldEntity.getSecondauth());
          paramObj.add(encryptFieldEntity.getViewScope());
          paramObj.add(encryptFieldEntity.getTransMethod());
          paramObj.add(encryptFieldEntity.getPrimarykey());
          paramObj.add(user.getUID());
          paramObj.add(DateUtil.getFullDate());
          paramObj.add(user.getUID());
          paramObj.add(DateUtil.getFullDate());
          this.createEncryptDataTable(encryptFieldEntity.getDatatablename());
          sql = " insert into enc_field_config_info(mouldcode,tablename,fieldname,scope,scopeid,datatablename,isencrypt,desensitization,secondauth,viewscope,transMethod,primarykey,creater,created,modifier,modified) " +
            " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
          rs.executeUpdate(sql, paramObj);
          //用于解决缓存异步加载问题
          sql = " select max(id) as id from enc_field_config_info ";
          rs.executeQuery(sql);
          if(rs.next()){
            encryptFieldConfigComInfo.addCache(rs.getString("id"));
          }
          if (Util.null2String(encryptFieldEntity.getNeedInitData()).equals("1") && Util.null2String(encryptFieldEntity.getIsEncrypt()).equals("1")) {
            //首次开启需要初始化
            lsEncryptFieldEntityInit.add(encryptFieldEntity);
          }
        }
      }
      new EncryptFieldViewScopeConfigComInfo().removeCache();
      this.initFieldData(lsEncryptFieldEntityInit);
    } catch (Exception e) {
      writeLog(e);
    }
  }

  public void initFieldData(List<EncryptFieldEntity> lsEncryptFieldEntity) {
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      List<List> sqlParams = new ArrayList<List>();
      List<Object> sqlParam = null;

      if (lsEncryptFieldEntity.size() == 0) return;

      weaver.encrypt.EncryptUtil encryptUtil = new weaver.encrypt.EncryptUtil();
      Map<String, List<String>> encryptConfing = new HashMap<>();
      EncryptFieldEntity encryptFieldEntity = null;
      for (int i = 0; i < lsEncryptFieldEntity.size(); i++) {
        encryptFieldEntity = lsEncryptFieldEntity.get(i);
        String tablename = encryptFieldEntity.getTablename() + "#" + encryptFieldEntity.getPrimarykey();
        if (encryptFieldEntity.getTablename().equals("cus_fielddata")) {
          tablename += "#" + encryptFieldEntity.getScope() + "#" + encryptFieldEntity.getScopeid();
        }
        String fieldname = encryptFieldEntity.getFieldname().toLowerCase();
        List<String> fieldnames = encryptConfing.get(tablename);
        if (fieldnames == null) {
          fieldnames = new ArrayList<>();
        }
        fieldnames.add(fieldname);
        encryptConfing.put(tablename, fieldnames);
      }

      for (Map.Entry<String, List<String>> entry : encryptConfing.entrySet()) {
        String[] tableInfo = entry.getKey().split("#");
        String tablename = tableInfo[0];
        String parimarykey = tableInfo[1];
        String[] arrParimarykey = parimarykey.split(",");
        List<String> fieldnames = entry.getValue();
        if (arrParimarykey.length == 0) {
          writeLog("tablename parimarykey is null");
          continue;
        }
        String fields = StringUtils.join(fieldnames, ",");
        fields = fields + "," + parimarykey;

        sql = "select " + fields + " from " + tablename;
        if(tablename.equals("cus_fielddata")){
          sql += " where scope='"+tableInfo[2]+"' and scopeId="+tableInfo[3];
        }
        rs.executeQuery(sql);
        rs.isReturnDecryptData(true);
        String[] columnName = rs.getColumnName();
        String[] columnTypeName = rs.getColumnTypeName();
        Map<String, String> columnInfo = new HashMap();
        for (int i = 0; i < columnName.length; i++) {
          columnInfo.put(columnName[i].toLowerCase(), columnTypeName[i]);
        }
        while (rs.next()) {
          sqlParam = new ArrayList<Object>();
          for (int i = 0; i < fieldnames.size(); i++) {
            String fieldname = fieldnames.get(i);
            String value = "";
            if (tablename.equals("cus_fielddata")) {
              String scope = tableInfo[2];
              String scopeId = tableInfo[3];
              value = rs.getString(fieldname);
              value = Util.null2String(encryptUtil.encryt("cus_fielddata", fieldnames.get(i), scope, scopeId, value, value));
            } else {
              value = rs.getString(fieldname);
              value = Util.null2String(encryptUtil.encryt(tablename, fieldname, value, value));
            }
            if (columnInfo.get(fieldname).equalsIgnoreCase("decimal")
              || columnInfo.get(fieldname).equalsIgnoreCase("numeric")) {
              sqlParam.add(Util.null2String(value).length()==0 ? null : Util.getDoubleValue(value));
            } else if (columnInfo.get(fieldname).equalsIgnoreCase("float")) {
              sqlParam.add(Util.null2String(value).length()==0 ? null : Util.getFloatValue(value));
            } else if (columnInfo.get(fieldname).equalsIgnoreCase("int")) {
              sqlParam.add(Util.null2String(value).length()==0 ? null : Util.getIntValue(value));
            } else {
              sqlParam.add(value);
            }
          }
          for (int i = 0; i < arrParimarykey.length; i++) {
            sqlParam.add(rs.getString(arrParimarykey[i]));
          }
          sqlParams.add(sqlParam);
        }
        String updateSql = "";
        for (int i = 0; i < fieldnames.size(); i++) {
          if (updateSql.length() > 0) updateSql += ",";
          updateSql += fieldnames.get(i) + "=?";
        }
        String whereSql = "";
        for (int i = 0; i < arrParimarykey.length; i++) {
          if (whereSql.length() > 0) whereSql += " and ";
          whereSql += arrParimarykey[i] + "=?";
        }
        sql = "update " + tablename + " set " + updateSql + " where " + whereSql;
        rs.setNoAutoEncrypt(true);
        rs.executeBatchSql(sql, sqlParams);
      }
    } catch (Exception e) {
      writeLog(e);
    }
  }

  /**
   * 创建加密存储表
   *
   * @param tablename
   */
  private void createEncryptDataTable(String tablename) {
    RecordSet rs = new RecordSet();
    String sql = "";

    try {
      boolean isExits = false;
      try {
        sql = " select * from " + tablename;
        isExits = rs.executeQuery(sql);
      } catch (Exception e) {
      }

      if (!isExits) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(" CREATE TABLE ").append(tablename);
        sqlBuffer.append(" (id varchar(32) not null primary key, ");
        sqlBuffer.append(" tablename varchar(50) NOT NULL, ");
        sqlBuffer.append(" fieldname varchar(50) NOT NULL, ");
        sqlBuffer.append(" enc_value varchar(4000) NOT NULL, ");
        sqlBuffer.append(" skey varchar(4000) NOT NULL, ");
        sqlBuffer.append(" crc varchar(4000) NOT NULL, ");
        sqlBuffer.append(" creater INTEGER NOT NULL, ");
        sqlBuffer.append(" created varchar(50) NULL, ");
        sqlBuffer.append(" modifier INTEGER NULL, ");
        sqlBuffer.append(" modified varchar(50) NULL) ");
        rs.executeUpdate(sqlBuffer.toString());
      }
    } catch (Exception e) {
      writeLog(e);
    }
  }

  /**
   * 保存二次身份校验相关属性
   *
   * @param encryptSecondAuthEntity
   * @param user
   */
  public void saveEncryptSecondAuthConfig(EncryptSecondAuthEntity encryptSecondAuthEntity, User user) {
    List paramObj = null;
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      paramObj = new ArrayList();
      paramObj.add(Util.null2String(encryptSecondAuthEntity.getIsEnable()).length() == 0 ? "null" : encryptSecondAuthEntity.getIsEnable());
      paramObj.add(Util.null2String(encryptSecondAuthEntity.getDoubleAuth()).length() == 0 ? "null" : encryptSecondAuthEntity.getDoubleAuth());
      paramObj.add(Util.null2String(encryptSecondAuthEntity.getVerifier()).length() == 0 ? "null" : encryptSecondAuthEntity.getVerifier());
      paramObj.add(Util.null2String(encryptSecondAuthEntity.getAuthType()).length() == 0 ? "null" : encryptSecondAuthEntity.getAuthType());
      paramObj.add(Util.null2String(encryptSecondAuthEntity.getAuthScope()));
      paramObj.add(Util.null2String(encryptSecondAuthEntity.getFilePath()));
      paramObj.add(user.getUID());
      paramObj.add(DateUtil.getFullDate());
      paramObj.add(encryptSecondAuthEntity.getMouldCode());
      paramObj.add(encryptSecondAuthEntity.getItemCode());
      sql = " update enc_secondauth_config_info set isenable = ?,doubleauth=?,verifier=?,authtype=?,authscope=?,filepath=?,modifier=?,modified=? where mouldCode=? and itemcode=? ";
      rs.executeUpdate(sql, paramObj);
      new EncryptSecondAuthConfigComInfo().removeCache();
    } catch (Exception e) {
      writeLog(e);
    }
  }

  /**
   * 保存加密分享相关属性
   *
   * @param encryptShareSettingEntity
   * @param user
   */
  public void saveEncryptShareSetting(EncryptShareSettingEntity encryptShareSettingEntity, User user) {
    List paramObj = null;
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      paramObj = new ArrayList();
      paramObj.add(Util.null2String(encryptShareSettingEntity.getIsEnable()).length() == 0 ? "null" : encryptShareSettingEntity.getIsEnable());
      paramObj.add(Util.null2String(encryptShareSettingEntity.getShareType()).length() == 0 ? "null" : encryptShareSettingEntity.getShareType());
      paramObj.add(user.getUID());
      paramObj.add(DateUtil.getFullDate());
      paramObj.add(encryptShareSettingEntity.getMouldCode());
      paramObj.add(encryptShareSettingEntity.getItemCode());
      sql = " update enc_share_config_info set isenable = ?,sharetype = ?,modifier=?,modified=? where mouldCode=? and itemcode=? ";
      rs.executeUpdate(sql, paramObj);
      new EncryptShareConfigComInfo().removeCache();
    } catch (Exception e) {
      writeLog(e);
    }
  }

  /**
   * 字段级别二次校验
   *
   * @param configId
   * @param request
   * @return
   */
  public static boolean checkSecondAuthToken4Field(String configId, HttpServletRequest request) {
    User user = (User) request.getSession(true).getAttribute("weaver_user@bean");
    String token = Util.null2String(request.getParameter("token"));
    int authType = EncryptUtil.getDefaultSecondAuthType().getId();
    return isSecondAuthFreeseCret(authType,null,null,configId,token,user);
  }

  /**
   * 字段级别二次校验
   *
   * @param tablename
   * @param fieldname
   * @param request
   * @return
   */
  public static boolean checkSecondAuthToken4Field(String tablename, String fieldname, String scope, String scopeid, HttpServletRequest request) {
    boolean checkSecondAuthToken = false;
    User user = (User) request.getSession(true).getAttribute("weaver_user@bean");
    EncryptFieldEntity encryptFieldEntity = getFieldEncryptConfig(tablename, fieldname, scope, scopeid);
    if (encryptFieldEntity.getSecondauth().equals("1")) {
      int authType = EncryptUtil.getDefaultSecondAuthType().getId();
      String token = Util.null2String(request.getParameter("token"));
      checkSecondAuthToken = isSecondAuthFreeseCret(authType, null,null, encryptFieldEntity.getId(),token, user);
    } else {
      checkSecondAuthToken = true;
    }
    return checkSecondAuthToken;
  }

  public boolean checkSecondAuthToken(String mouldCode, String itemCode, HttpServletRequest request) {
    return checkSecondAuthToken(mouldCode, itemCode, null, request);
  }

  /**
   * 身份二次验证后端接口认证
   *
   * @param mouldCode
   * @param itemCode
   * @param request
   * @return
   */
  public boolean checkSecondAuthToken(String mouldCode, String itemCode, Map<String, Object> params, @NotNull HttpServletRequest request) {
    boolean checkAuthToken = false;
    User user = (User) request.getSession(true).getAttribute("weaver_user@bean");
    if(!EncryptUtil.userSecondAuthStatus(user)){
      checkAuthToken = true;
      return checkAuthToken;
    }

    String strToken = Util.null2String(request.getParameter("token"));
    if(strToken.length()==0){
      strToken = Util.null2String(request.getParameter("secondAuthToken"));
    }
    String[] token = Util.splitString(strToken,"_");
    if (params == null) {
      params = new HashMap<>();
    }
    params.put("mouldCode", mouldCode);
    params.put("itemCode", itemCode);
    Map<String, Object> secondAuthConfig = new EncryptUtil().getSecondAuthConfig(params, request, user);
    if(secondAuthConfig==null){
      checkAuthToken = true;
      return checkAuthToken;
    }
    //是否需要二次认证（自己）
    boolean isNeedSecondAuth = (Boolean) secondAuthConfig.get("isNeedSecondAuth");
    //是否需要双重认证（他人）
    boolean isNeedDoubleAuth = (Boolean) secondAuthConfig.get("isNeedDoubleAuth");
    //二次认证token
    String secondAuthToken = token.length>0?token[0]:"";
    //双重认证方式token
    String doubleAuthToken  = token.length>1?token[1]:"";
    int secondAuthType = Util.getIntValue(Util.null2String(secondAuthConfig.get("secondAuthType")));
    int doubleAuthType = Util.getIntValue(Util.null2String(secondAuthConfig.get("doubleAuthType")));
    boolean checkSecondAuthToken = true;
    boolean checkDoubleAuthToken = true;
    if (isNeedSecondAuth) {
      checkSecondAuthToken = isSecondAuthFreeseCret(secondAuthType,mouldCode,itemCode,null,secondAuthToken,user);
    }
    if (isNeedDoubleAuth) {
      //认证人
      String verifier = Util.null2String(secondAuthConfig.get("verifier"));
      if (verifier.length() > 0) {
        //设置验证人员id
        User tmpUser = new User(Util.getIntValue(verifier));
        checkDoubleAuthToken = isSecondAuthFreeseCret(doubleAuthType,mouldCode,itemCode,null,doubleAuthToken,tmpUser);
      }
    }

    checkAuthToken = checkSecondAuthToken&&checkDoubleAuthToken;


    new BaseBean().writeLog("==zj==(checkSecondAuthToken和checkDoubleAuthToken最新2023-09-22)" + checkSecondAuthToken + "  |  " + checkDoubleAuthToken);
    return checkAuthToken;
  }

  /**
   * 获取是否在免密时间内
   *
   * @param mouldCode
   * @param itemCode
   * @param user
   * @return
   */
  public static boolean isSecondAuthFreeseCret(String mouldCode, String itemCode, String token, User user) {
    boolean isSecondAuthFreeseCret = false;
    EncryptSecondAuthEntity encryptSecondAuthEntity = getSecondAuthEncryptConfig(mouldCode, itemCode);
    if (encryptSecondAuthEntity != null && encryptSecondAuthEntity.getIsEnable().equals("1")) {
      isSecondAuthFreeseCret = isSecondAuthFreeseCret(Util.getIntValue(encryptSecondAuthEntity.getAuthType()),mouldCode,itemCode,null,token,user);
    }
    return isSecondAuthFreeseCret;
  }

  /**
   * 获取是否在面密时间内
   *
   * @param authType
   * @param user
   * @return
   */
  private static boolean isSecondAuthFreeseCret(int authType,String mouldCode,String itemCode,String configId,String checktoken,User user) {
    boolean isSecondAuthFreeseCret = false;
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      String currentTime = TimeUtil.getCurrentTimeString();
      String freeSecretTime = "";
      String token = "";
      if (user != null) {
        int userId = user.getUID();
        sql = "select token, freeSecretTime from hrm_secondauth_freesecret where userId = ? and userType = ? and authType = ?";
        List<Object> sqlParam = new ArrayList<>();
        sqlParam.add(userId);
        sqlParam.add(0);
        sqlParam.add(authType);
        if(Util.null2String(mouldCode).length()>0){
          sql += " and mouldCode= ? ";
          sqlParam.add(mouldCode);
        }
        if(Util.null2String(itemCode).length()>0){
          sql += " and itemCode= ? ";
          sqlParam.add(itemCode);
        }
        if(Util.null2String(configId).length()>0){
          sql += " and configId= ? ";
          sqlParam.add(configId);
        }
        new BaseBean().writeLog("==zj==(免密验证sql)" + sql + " | " + JSON.toJSONString(sqlParam));
        rs.executeQuery(sql, sqlParam);
        if (rs.next()) {
          freeSecretTime = Util.null2String(rs.getString("freeSecretTime"));
          token = Util.null2String(rs.getString("token"));
        }
      }

      new BaseBean().writeLog("==zj==(验证)" + isSecondAuthFreeseCret + " | "+currentTime+" | " + checktoken + " | "+token.equals(checktoken));
      if (freeSecretTime.compareTo(currentTime) > 0 && checktoken.length()>0 && token.equals(checktoken)) {//说明仍然在免密时间内,免密时间内，不需要二次验证
        isSecondAuthFreeseCret = true;
      }
    } catch (Exception e) {
      new BaseBean().writeLog("isSecondAuthFreeseCret error>>>>>>>>>>>>>>>>>>>>>");
      new BaseBean().writeLog(e);
    }
    return isSecondAuthFreeseCret;
  }


  public static String getSecondAuthToken(String mouldCode,String itemCode,String configId, int authType, User user) {
    String secondAuthToken = "";
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      String currentTime = TimeUtil.getCurrentTimeString();
      String freeSecretTime = "";
      if (user != null) {
        int userId = user.getUID();
        sql = "select token,freeSecretTime from hrm_secondauth_freesecret where userId = ? and userType = ? and authType = ?";
        List<Object> sqlParam = new ArrayList<>();
        sqlParam.add(userId);
        sqlParam.add(0);
        sqlParam.add(authType);
        if(Util.null2String(mouldCode).length()>0){
          sql += " and mouldCode= ? ";
          sqlParam.add(mouldCode);
        }
        if(Util.null2String(itemCode).length()>0){
          sql += " and itemCode= ? ";
          sqlParam.add(itemCode);
        }
        if(Util.null2String(configId).length()>0){
          sql += " and configId= ? ";
          sqlParam.add(configId);
        }
        rs.executeQuery(sql, sqlParam);
        if (rs.next()) {
          freeSecretTime = Util.null2String(rs.getString("freeSecretTime"));
        }
      }
      if (freeSecretTime.compareTo(currentTime) > 0) {//说明仍然在免密时间内,免密时间内，不需要二次验证
        secondAuthToken = Util.null2String(rs.getString("token"));
      }
    } catch (Exception e) {
      new BaseBean().writeLog("getSecondAuthToken error>>>>>>>>>>>>>>>>>>>>>");
      new BaseBean().writeLog(e);
    }
    return secondAuthToken;
  }

  /**
   * 获取二次身份校验类型
   *
   * @param defalutValue
   * @param user
   * @return
   */
  public static List<SearchConditionOption> getSecondAuthOptions(String defalutValue, User user) {
    return getSecondAuthOptions(Util.getIntValue(defalutValue), user);
  }

  /**
   * 获取二次身份校验类型
   *
   * @param defalutValue
   * @param user
   * @return
   */
  public static List<SearchConditionOption> getSecondAuthOptions(int defalutValue, User user) {
    List<SearchConditionOption> options = new ArrayList();
    SearchConditionOption option = null;

    HrmSettingsComInfo hrmSettingsComInfo = new HrmSettingsComInfo();

    if ("1".equals(hrmSettingsComInfo.getSecondNeedDynapass())) {
      option = new SearchConditionOption();
      option.setKey("" + SecondAuthType.DynamicPassword.getId());
      option.setShowname(SystemEnv.getHtmlLabelName(SecondAuthType.DynamicPassword.getLabel(), user.getLanguage()));
      option.setSelected(defalutValue == SecondAuthType.DynamicPassword.getId());
      options.add(option);
    }

    if ("1".equals(hrmSettingsComInfo.getSecondNeedusbDt())) {
      option = new SearchConditionOption();
      option.setKey("" + SecondAuthType.DynamicToken.getId());
      option.setShowname(SystemEnv.getHtmlLabelName(SecondAuthType.DynamicToken.getLabel(), user.getLanguage()));
      option.setSelected(defalutValue == SecondAuthType.DynamicToken.getId());
      options.add(option);
    }

    if ("1".equals(hrmSettingsComInfo.getSecondCA())) {
      option = new SearchConditionOption();
      option.setKey("" + SecondAuthType.CAAuth.getId());
      option.setShowname(SystemEnv.getHtmlLabelName(SecondAuthType.CAAuth.getLabel(), user.getLanguage()));
      option.setSelected(defalutValue == SecondAuthType.CAAuth.getId());
      options.add(option);
    }

    if ("1".equals(hrmSettingsComInfo.getSecondCL())) {
      option = new SearchConditionOption();
      option.setKey("" + SecondAuthType.QYS.getId());
      option.setShowname(SystemEnv.getHtmlLabelName(SecondAuthType.QYS.getLabel(), user.getLanguage()));
      option.setSelected(defalutValue == SecondAuthType.QYS.getId());
      options.add(option);
    }

    if ("1".equals(hrmSettingsComInfo.getSecondPassword())) {
      option = new SearchConditionOption();
      option.setKey("" + SecondAuthType.SecondAuthPassword.getId());
      option.setShowname(SystemEnv.getHtmlLabelName(SecondAuthType.SecondAuthPassword.getLabel(), user.getLanguage()));
      option.setSelected(defalutValue == SecondAuthType.SecondAuthPassword.getId());
      options.add(option);
    }
    return options;
  }

  /**
   * 判断是否加密/脱敏格式数据
   */
  public static boolean judgeDecryptData(String value) {
    return value != null &&
      ((value.startsWith("desensitization__") && value.length() == 59)
        || (value.startsWith("encryption__") && value.length() == 54));
  }

  public static String forceFormatDecryptData(String value) {
    return judgeDecryptData(value) ? "****" : value;
  }

  public static String getDecryptData(String value) {
    return getDecryptData(value, null);
  }

  /***
   * 脱敏数据、加密数据解密
   * @param value
   * @return
   */
  public static String getDecryptData(String value, Map<String, Object> params) {
    String returnVal = value;
    try {
      if (value.startsWith("desensitization__") || value.startsWith("encryption__")) {
        if (value.startsWith("desensitization__")) {
          value = new XssUtil().get(value.replace("desensitization__", ""));
        } else {
          value = new XssUtil().get(value.replace("encryption__", ""));
        }

        boolean isDefined = false;
        if (value.indexOf(EncryptMould.INTEGRATION.getCode() + "_") > -1) {
          isDefined = true;
          value = value.replace((EncryptMould.INTEGRATION.getCode() + "_"), "");
        }
        String[] values = Util.splitString(value, "__");
        String configId = values[0];
        String id = values[1];
        if (isDefined) {
          returnVal = id;
        } else {
          weaver.encrypt.EncryptUtil eu = new weaver.encrypt.EncryptUtil();
          EncryptFieldConfigComInfo encryptConfigComInfo = new EncryptFieldConfigComInfo();
          String tablename = Util.null2String(encryptConfigComInfo.getTablename(configId));
          String fieldname = Util.null2String(encryptConfigComInfo.getFieldname(configId));
          String isEncrypt = Util.null2String(encryptConfigComInfo.getIsEncrypt(configId));
          String transMethod = Util.null2String(encryptConfigComInfo.getTransMethod(configId));
          if (isEncrypt.equals("1")) {
            returnVal = Util.null2String(eu.decrypt(tablename, fieldname, id, true));
          } else {
            returnVal = id;
          }
          if (transMethod.length() > 0) {
            //有自定义处理方法的需要自己处理
            String classname = transMethod.substring(0, transMethod.lastIndexOf("."));
            String func = transMethod.substring(transMethod.lastIndexOf(".") + 1);
            Class clazz = Class.forName(classname);
            Method method = clazz.getMethod(func, new Class[]{String.class, Map.class});
            returnVal = Util.null2String(method.invoke(clazz.newInstance(), new Object[]{value, params}));
          }
        }
      }
    } catch (Exception e) {
      new BaseBean().writeLog(e);
    }
    return returnVal;
  }

  /**
   * 保存分享密码
   *
   * @param mouldCode
   * @param itemCode
   * @return
   */
  public String saveSharePwd(String mouldCode, String itemCode, String serialid, String pwd, User user) {
    String sql = "";
    RecordSet rs = new RecordSet();
    try {
      sql = "insert into enc_share_pwd(mouldcode,itemcode,serialid,pwd,creater,created) values(?,?,?,?,?,?)";
      pwd = weaver.sm.SM3Utils.getEncrypt(pwd);
      rs.executeUpdate(sql, mouldCode, itemCode, serialid, pwd, user.getUID(), DateUtil.getFullDate());
    } catch (Exception e) {
      writeLog(e);
    }
    return serialid;
  }

  /**
   * 校验分享密码
   *
   * @param mouldCode
   * @param itemCode
   * @param serialid
   * @return
   */
  public boolean checkSharePwd(String mouldCode, String itemCode, String serialid, String pwd) {
    boolean checkSharePwd = false;
    String sql = "";
    RecordSet rs = new RecordSet();
    try {
      sql = " select pwd from enc_share_pwd where mouldCode=? and itemCode=? and serialid=? ";
      rs.executeQuery(sql, mouldCode, itemCode, serialid);
      if (rs.next()) {
        checkSharePwd = weaver.sm.SM3Utils.getEncrypt(pwd).equals(rs.getString("pwd"));
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return checkSharePwd;
  }

  /**
   * 加密分享后端接口认证
   *
   * @param mouldCode
   * @param itemCode
   * @param request
   * @return
   */
  public boolean checkShareToken(String mouldCode, String itemCode, Map<String, Object> params, HttpServletRequest request, User user) {
    boolean checkAuthToken = false;
    RecordSet rs = new RecordSet();
    String sql = "";
    try {
      EncryptShareSettingEntity encryptShareSettingEntity = EncryptConfigBiz.getEncryptShareSetting(mouldCode, itemCode);
      if (encryptShareSettingEntity != null && encryptShareSettingEntity.getIsEnable().equals("1")) {
        String serialid = Util.null2String(params.get("serialid"));
        String resourcetype = Util.null2String(params.get("resourcetype"));
        String targetid = Util.null2String(params.get("targetid"));
        String sharer = Util.null2String(params.get("sharer"));

        if (serialid.length() == 0) {
          //通过resourceid等信息判断是否为普通分享
          String basesql = "select b.serialid from mobile_ChatResourceShareScope a inner join mobile_chatresourceshare b on a.shareid=b.id where b.resourcetype=? and b.resourceid=? and b.sharer=? ";
          rs.executeQuery(basesql + " and a.resoueceid=? and a.resouecetype=0 order by b.created desc ", resourcetype, targetid, sharer, user.getUID());
          if (rs.next()) {
            serialid = Util.null2String(rs.getString("serialid"));
          }
        }

        if (serialid.length() == 0) {
          checkAuthToken = true;
        } else {
          if (Util.null2String(serialid).endsWith("_random_")) {
            String tmpSerialid = serialid.substring(0, serialid.lastIndexOf("_random_"));
            String[] values = Util.splitString(tmpSerialid, "__");
            if (!targetid.equals(values[0])) {
              //错误的targetid
            } else {
              String key = serialid + "_" + user.getUID();
              if ("1".equals(Util.null2String(request.getSession(true).getAttribute(key)))) {
                Long ltime = (Long) request.getSession(true).getAttribute(key + "_time");
                //根据免密时间比较
                if (ltime != null && new Date().getTime() - ltime > EncryptUtil.getFreeseCretTime(-1)) {
                  request.getSession(true).removeAttribute(key);
                  request.getSession(true).removeAttribute(key + "_time");
                } else {
                  checkAuthToken = true;
                }
              }
            }
          }
        }
      } else {
        checkAuthToken = true;
      }
      String token = Util.null2String(request.getParameter("token"));
      boolean isSecondAuthFreeseCret = isSecondAuthFreeseCret(mouldCode, itemCode, token,user);
      if (isSecondAuthFreeseCret) {
        checkAuthToken = true;
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return checkAuthToken;
  }

  /**
   * 根据手机号查找加密字段对应人员id
   *
   * @param fieldvalue 需要查找的值
   * @param fieldname  字段名
   * @return
   */
  public static String getResourceIdByFieldValue(String fieldvalue, String fieldname) {
    String resourceid = "";
    RecordSet rs = new RecordSet();
    String sql = "";

    boolean isOpenEncrypt = false;
    EncryptFieldConfigComInfo encryptFieldConfigComInfo = new EncryptFieldConfigComInfo();
    EncryptFieldEntity encryptFieldEntity = encryptFieldConfigComInfo.getFieldEncryptConfig("hrmresource", fieldname);
    if(encryptFieldEntity!=null){
      isOpenEncrypt = Util.null2String(encryptFieldEntity.getIsEncrypt()).equals("1");
    }
    if(isOpenEncrypt){
      DecryptResourceComInfo decryptResourceComInfo = new DecryptResourceComInfo();
      while (decryptResourceComInfo.next()) {
        String status = Util.null2String(decryptResourceComInfo.getStatus());
        if(status.equals("0")||status.equals("1")||status.equals("2")||status.equals("3")){

        }else{
          //排查非在职人员
          continue;
        }
        if (fieldname.equals("mobile")) {
          if (decryptResourceComInfo.getMobile().equals(fieldvalue)) {
            resourceid = decryptResourceComInfo.getId();
            break;
          }
        } else if (fieldname.equals("telephone")) {
          if (decryptResourceComInfo.getTelephone().equals(fieldvalue)) {
            resourceid = decryptResourceComInfo.getId();
            break;
          }
        } else if (fieldname.equals("email")) {
          if (decryptResourceComInfo.getEmail().equals(fieldvalue)) {
            resourceid = decryptResourceComInfo.getId();
            break;
          }
        } else if (fieldname.equals("certificatenum")) {
          if (decryptResourceComInfo.getCertificatenum().equals(fieldvalue)) {
            resourceid = decryptResourceComInfo.getId();
            break;
          }
        }else if (fieldname.equals("loginid")) {
          if (decryptResourceComInfo.getLoginid().equals(fieldvalue)) {
            resourceid = decryptResourceComInfo.getId();
            break;
          }
        }else if (fieldname.equals("workcode")) {
          if (decryptResourceComInfo.getWorkcode().equals(fieldvalue)) {
            resourceid = decryptResourceComInfo.getId();
            break;
          }
        }else if (fieldname.equals("id")) {
          if (decryptResourceComInfo.getId().equals(fieldvalue)) {
            resourceid = decryptResourceComInfo.getId();
            break;
          }
        }
      }
    }else{
      sql = " select id from hrmresource where "+fieldname+"='"+fieldvalue+"'";
      rs.executeQuery(sql);
      if(rs.next()){
        resourceid = rs.getString("id");
      }
    }
    return resourceid;
  }

  /**
   * 数据加密是否开启
   *
   * @return
   */
  public static boolean getEncryptEnable(String mouldCode) {
    if (mouldCode.equals(EncryptMould.INTEGRATION.getCode())) {

    }else {
      EncryptBasicConfigComInfo encryptBasicConfigComInfo = new EncryptBasicConfigComInfo();
      encryptBasicConfigComInfo.next();
      if (!Util.null2String(encryptBasicConfigComInfo.getEnableStatus()).equals("1")) return false;
    }

    if (mouldCode.equals(EncryptMould.WORKFLOW.getCode())) return true;
//    EncryptBasicConfigComInfo encryptBasicConfigComInfo = new EncryptBasicConfigComInfo();
//    encryptBasicConfigComInfo.next();
//    boolean encryptEnable = encryptBasicConfigComInfo.getEnableStatus().equals("1");
//    if(encryptEnable){}
    boolean encryptEnable = false;
    EncryptMouldConfigComInfo encryptMouldConfigComInfo = new EncryptMouldConfigComInfo();
    while (encryptMouldConfigComInfo.next()) {
      if (encryptMouldConfigComInfo.getMouldcode().equalsIgnoreCase(mouldCode)) {
        encryptEnable = encryptMouldConfigComInfo.getIsencrypt().equals("1");
        break;
      }
    }
    return encryptEnable;
  }

  /**
   * 二次认证是否开启
   *
   * @return
   */
  public static boolean getSecondAuthEnable(String mouldCode) {
    boolean secondAuthEnable = false;
    EncryptMouldConfigComInfo encryptMouldConfigComInfo = new EncryptMouldConfigComInfo();
    while (encryptMouldConfigComInfo.next()) {
      if (encryptMouldConfigComInfo.getMouldcode().equalsIgnoreCase(mouldCode)) {
        secondAuthEnable = encryptMouldConfigComInfo.getSecondauth().equals("1");
        break;
      }
    }
    return secondAuthEnable;
  }

  /**
   * 加密分享是否开启
   *
   * @return
   */
  public static boolean getEncryptShareEnable(String mouldCode) {
    boolean encryptShareEnable = false;
    EncryptMouldConfigComInfo encryptMouldConfigComInfo = new EncryptMouldConfigComInfo();
    while (encryptMouldConfigComInfo.next()) {
      if (encryptMouldConfigComInfo.getMouldcode().equalsIgnoreCase(mouldCode)) {
        encryptShareEnable = encryptMouldConfigComInfo.getEncryptshare().equals("1");
        break;
      }
    }
    return encryptShareEnable;
  }

  /***
   * 自定义脱敏数据
   * @param mouldCode
   * @param configId
   * @param value
   * @return
   */
  public String getDesensitization(String mouldCode, String configId, String value) {
    XssUtil xssUtil = new XssUtil();
    return "desensitization__" + xssUtil.put(mouldCode + "+" + configId + "__" + value);
  }

  /***
   * 自定义脱敏数据(与标准格式一致)
   * @param configId
   * @param value
   * @return
   */
  public String getDesensitization(String configId, String value) {
    XssUtil xssUtil = new XssUtil();
    return "desensitization__" + xssUtil.put(configId + "__" + value);
  }

  /***
   * 是否开启数据防篡改
   * @return
   */
  public static boolean getEncryptBasicIsCrc(){
    EncryptBasicConfigComInfo encryptBasicConfigComInfo = new EncryptBasicConfigComInfo();
    encryptBasicConfigComInfo.next();
    return encryptBasicConfigComInfo.getIsCrc().equals("1");
  }


}
