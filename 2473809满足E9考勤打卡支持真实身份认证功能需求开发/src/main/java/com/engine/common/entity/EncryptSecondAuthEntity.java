package com.engine.common.entity;

public class EncryptSecondAuthEntity {
  private String id;
  //所属模块
  private String mouldcode;
  //模块功能
  private String itemcode;
  //是否启用
  private String isenable;
  //双重身份校验
  private String doubleAuth;
  //双重身份校验人
  private String verifier;
  //校验方式
  private String authtype;
  //显示顺序
  private String showorder;
  //文档目录
  private String filePath;
  //认证人员范围
  private String authScope;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMouldCode() {
    return mouldcode;
  }

  public void setMouldCode(String mouldcode) {
    this.mouldcode = mouldcode;
  }

  public String getItemCode() {
    return itemcode;
  }

  public void setItemCode(String itemcode) {
    this.itemcode = itemcode;
  }

  public String getIsEnable() {
    return isenable;
  }

  public void setIsEnable(String isenable) {
    this.isenable = isenable;
  }

  public String getDoubleAuth() {
    return doubleAuth;
  }

  public void setDoubleAuth(String doubleAuth) {
    this.doubleAuth = doubleAuth;
  }

  public String getVerifier() {
    return verifier;
  }

  public void setVerifier(String verifier) {
    this.verifier = verifier;
  }

  public String getAuthType() {
    return authtype;
  }

  public void setAuthType(String authtype) {
    this.authtype = authtype;
  }

  public String getShowOrder() {
    return showorder;
  }

  public void setShowOrder(String showorder) {
    this.showorder = showorder;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public String getAuthScope() {
    return authScope;
  }

  public void setAuthScope(String authScope) {
    this.authScope = authScope;
  }
}
