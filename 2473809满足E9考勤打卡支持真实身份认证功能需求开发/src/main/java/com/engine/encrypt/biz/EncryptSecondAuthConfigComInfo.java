package com.engine.encrypt.biz;

import com.engine.common.entity.EncryptSecondAuthEntity;
import weaver.cache.CacheBase;
import weaver.cache.CacheColumn;
import weaver.cache.CacheColumnType;
import weaver.cache.PKColumn;

/**
 * 数据二次认证配置缓存
 */
public class EncryptSecondAuthConfigComInfo extends CacheBase {
  protected static String TABLE_NAME = "enc_secondauth_config_info";
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

  @CacheColumn(name = "mouldcode")
  protected static int mouldcode;

  @CacheColumn(name = "itemcode")
  protected static int itemcode;

  @CacheColumn(name = "isenable")
  protected static int isenable;

  @CacheColumn(name = "doubleauth")
  protected static int doubleauth;

  @CacheColumn(name = "verifier")
  protected static int verifier;

  @CacheColumn(name = "authtype")
  protected static int authtype;

  @CacheColumn(name = "showorder")
  protected static int showorder;

  @CacheColumn(name = "filepath")
  protected static int filepath;

  @CacheColumn(name = "authscope")
  protected static int authscope;

  public String getId() {
    return (String) getRowValue(PK_INDEX);
  }

  public String getMouldCode() {
    return (String) getRowValue(mouldcode);
  }

  public String getMouldCode(String key) {
    return (String) getValue(mouldcode, key);
  }

  public String getItemcode() {
    return (String) getRowValue(itemcode);
  }

  public String getItemcode(String key) {
    return (String) getValue(itemcode, key);
  }

  public String getIsEnable() {
    return (String) getRowValue(isenable);
  }

  public String getIsEnable(String key) {
    return (String) getValue(isenable, key);
  }

  public String getDoubleauth() {
    return (String) getRowValue(doubleauth);
  }

  public String getDoubleauth(String key) {
    return (String) getValue(doubleauth, key);
  }

  public String getVerifier() {
    return (String) getRowValue(verifier);
  }

  public String getVerifier(String key) {
    return (String) getValue(verifier, key);
  }

  public String getAuthType() {
    return (String) getRowValue(authtype);
  }

  public String getAuthType(String key) {
    return (String) getValue(authtype, key);
  }

  public String getShowOrder() {
    return (String) getRowValue(showorder);
  }

  public String getShowOrder(String key) {
    return (String) getValue(showorder, key);
  }

  public String getFilePath() {
    return (String) getRowValue(filepath);
  }

  public String getFilePath(String key) {
    return (String) getValue(filepath, key);
  }

  public String getAuthscope() {
    return (String) getRowValue(authscope);
  }

  public String getAuthscope(String key) {
    return (String) getValue(authscope, key);
  }

  public EncryptSecondAuthEntity getSecondAuthEncryptConfig(String mouldCode, String itemCode){
    EncryptSecondAuthEntity encryptSecondAuthEntity = null;
    this.setTofirstRow();
    while(this.next()){
      if(this.getMouldCode().equalsIgnoreCase(mouldCode) && this.getItemcode().equalsIgnoreCase(itemCode)){
        encryptSecondAuthEntity = new EncryptSecondAuthEntity();
        encryptSecondAuthEntity.setId(this.getId());
        encryptSecondAuthEntity.setMouldCode(this.getMouldCode());
        encryptSecondAuthEntity.setItemCode(this.getItemcode());
        encryptSecondAuthEntity.setIsEnable(this.getIsEnable());
        encryptSecondAuthEntity.setDoubleAuth(this.getDoubleauth());
        encryptSecondAuthEntity.setVerifier(this.getVerifier());
        encryptSecondAuthEntity.setAuthType(this.getAuthType());
        encryptSecondAuthEntity.setShowOrder(this.getShowOrder());
        encryptSecondAuthEntity.setFilePath(this.getFilePath());
        encryptSecondAuthEntity.setAuthScope(this.getAuthscope());
        break;
      }
    }
    return encryptSecondAuthEntity;
  }
}