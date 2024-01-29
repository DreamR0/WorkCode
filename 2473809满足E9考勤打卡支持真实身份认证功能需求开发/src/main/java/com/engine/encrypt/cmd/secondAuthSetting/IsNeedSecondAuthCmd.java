package com.engine.encrypt.cmd.secondAuthSetting;

import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.encrypt.biz.EncryptFieldConfigComInfo;
import com.engine.encrypt.util.EncryptUtil;
import weaver.filter.XssUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.systeminfo.SystemEnv;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class IsNeedSecondAuthCmd extends AbstractCommonCommand<Map<String, Object>> {
  private HttpServletRequest request;

  public IsNeedSecondAuthCmd(Map<String, Object> params, HttpServletRequest request, User user) {
    this.user = user;
    this.params = params;
    this.request = request;
  }

  @Override
  public Map<String, Object> execute(CommandContext commandContext) {
    Map<String, Object> retmap = new HashMap<String, Object>();
    try {
      retmap = new EncryptUtil().getSecondAuthConfig(params, request, user);
      retmap.remove("mouldCode");
      retmap.remove("itemCode");
      retmap.remove("configId");
      retmap.remove("secondAuthType");
      retmap.remove("doubleAuthType");
      retmap.remove("secondAuthToken");
      retmap.remove("doubleAuthToken");
      retmap.put("status", "1");
    } catch (Exception e) {
      retmap.put("status", "-1");
      retmap.put("message", SystemEnv.getHtmlLabelName(382661, user.getLanguage()));
      writeLog(e);
    }
    return retmap;
  }

  @Override
  public BizLogContext getLogContext() {
    return null;
  }

}
