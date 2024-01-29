package com.engine.encrypt.biz;

import weaver.cache.*;
import weaver.conn.RecordSet;
import weaver.general.Util;

/**
 * 明文人员缓存类 用于加密查找
 */
public class DecryptResourceComInfo extends CacheBase {
  protected static String TABLE_NAME = "hrmresource";
  /**
   * sql中的where信息，不要以where开始
   */
  protected static String TABLE_WHERE = null;
  /**
   * sql中的order by信息，不要以order by开始
   */
  protected static String TABLE_ORDER = "id";

  @PKColumn(type = CacheColumnType.NUMBER)
  protected static String PK_NAME = "id";

  @CacheColumn(name = "mobile")
  protected static int mobile;

  @CacheColumn(name = "telephone")
  protected static int telephone;

  @CacheColumn(name = "email")
  protected static int email;

  @CacheColumn(name = "certificatenum")
  protected static int certificatenum;

  @CacheColumn(name = "status")
  protected static int status;

  @CacheColumn(name = "loginid")
  protected static int loginid;

  @CacheColumn(name = "workcode")
  protected static int workcode;

  @Override
  protected boolean autoInitIfNotFound() {
    return false;
  }

  public CacheItem initCache(String key) {
    if (Util.getIntValue(key) <= 0) {
      return null;
    }

    boolean isHrmResource = false;
    CacheItem cacheItem = null;
    RecordSet rs = new RecordSet();
    rs.isAutoDecrypt(true);
    rs.isReturnDecryptData(true);
    String sql = " select id,mobile,telephone,email,certificatenum,status,loginid,workcode from hrmresource  where id=" + key;
    rs.executeQuery(sql);
    if (rs.next()) {
      cacheItem = createCacheItem();
      parseResultSetToCacheItem(rs, cacheItem);
      modifyCacheItem(key, cacheItem);
      isHrmResource = true;
    }

    if (!isHrmResource) {
      sql = "SELECT id,mobile,'' as telephone,'' as email,'' as certificatenum,status,'' as loginid,'' as workcode FROM HrmResourceManager where id=" + key;
      rs.executeQuery(sql);
      if (rs.next()) {
        cacheItem = createCacheItem();
        parseResultSetToCacheItem(rs, cacheItem);
        modifyCacheItem(key, cacheItem);
      }
    }
    return cacheItem;
  }

  @Override
  public CacheMap initCache() {
    CacheMap localData = createCacheMap();
    RecordSet rs = new RecordSet();
    rs.isAutoDecrypt(true);
    rs.isReturnDecryptData(true);
    String sql = "";
    try {
      sql = " select id,mobile,telephone,email,certificatenum,status,loginid,workcode from hrmresource order by dsporder ";
      rs.executeQuery(sql);
      while (rs.next()) {
        CacheItem cacheItem = createCacheItem();
        parseResultSetToCacheItem(rs, cacheItem);
        String id = rs.getString("id");
        modifyCacheItem(id, cacheItem);
        localData.put(id, cacheItem);
      }

      sql = " select id,mobile,'' as telephone,'' as email,'' as certificatenum,status,'' as loginid,'' as workcode from hrmresourcemanager order by id ";
      rs.executeQuery(sql);
      while (rs.next()) {
        CacheItem cacheItem = createCacheItem();
        parseResultSetToCacheItem(rs, cacheItem);
        String id = rs.getString("id");
        modifyCacheItem(id, cacheItem);
        localData.put(id, cacheItem);
      }
    } catch (Exception e) {
      writeLog(e);
    }
    return localData;
  }

  public String getId() {
    return (String) getRowValue(PK_INDEX);
  }

  public String getMobile() {
    return (String) getRowValue(mobile);
  }

  public String getMobile(String key) {
    return (String) getValue(mobile, key);
  }

  public String getTelephone() {
    return (String) getRowValue(telephone);
  }

  public String getTelephone(String key) {
    return (String) getValue(telephone, key);
  }

  public String getEmail() {
    return (String) getRowValue(email);
  }

  public String getEmail(String key) {
    return (String) getValue(email, key);
  }

  public String getCertificatenum() {
    return (String) getRowValue(certificatenum);
  }

  public String getCertificatenum(String key) {
    return (String) getValue(certificatenum, key);
  }

  public String getStatus() {
    return (String) getRowValue(status);
  }

  public String getStatus(String key) {
    return (String) getValue(status, key);
  }

  public String getLoginid() {
    return (String) getRowValue(loginid);
  }

  public String getLoginid(String key) {
    return (String) getValue(loginid, key);
  }

  public String getWorkcode() {
    return (String) getRowValue(workcode);
  }

  public String getWorkcode(String key) {
    return (String) getValue(workcode, key);
  }


}