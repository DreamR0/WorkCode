package com.api.hrm.cmd.search;

import com.api.hrm.bean.RightMenu;
import com.api.hrm.bean.RightMenuType;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import weaver.hrm.HrmUserVarify;
import weaver.hrm.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取右键菜单
 */
public class GetRightMenuCmd extends AbstractCommonCommand<Map<String, Object>> {

    public GetRightMenuCmd(Map<String, Object> params, User user) {
        this.user = user;
        this.params = params;
    }

    @Override
    public BizLogContext getLogContext() {
        return null;
    }

    @Override
    public Map<String, Object> execute(CommandContext commandContext) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        try {
            List<RightMenu> rightMenus = new ArrayList<RightMenu>();
            int language = user.getLanguage();

            rightMenus.add(new RightMenu(language, RightMenuType.BTN_SENDEMESSAGE, "sendEmessage", true));
            rightMenus.add(new RightMenu(language, RightMenuType.BTN_SEARCH, "doSearch"));
            rightMenus.add(new RightMenu(language, RightMenuType.BTN_RESEARCH, "reSearch"));
            //是否具有【人力资源邮件群发】权限
            if (HrmUserVarify.checkUserRight("HrmMailMerge:Merge", user) && false) {
                rightMenus.add(new RightMenu(language, RightMenuType.BTN_SENDMAIL, "sendMail"));
            }
            //是否具有【人员信息导出】权限
            if (HrmUserVarify.checkUserRight("HrmResourceInfo:Import", user)) {
                //==zj 这里多个判断如果非管理员就把导出设置隐藏
                rightMenus.add(new RightMenu(language, RightMenuType.BTN_EXPORTEXCEL, "exportExcel", true));

                if (user.getUID() == 1){
                    rightMenus.add(new RightMenu(language, RightMenuType.BTN_EXPORT_SETTING, "doExportSetting", false));
                }
                rightMenus.add(new RightMenu(language, RightMenuType.BTN_EXPORT_LOG, "doLog", false));
            }
            //默认排序设置
            rightMenus.add(new RightMenu(language, RightMenuType.BTN_SORTBYCOL, "doSortSetting", false));
            rightMenus.add(new RightMenu(language, RightMenuType.BTN_COLUMN, "definedColumn"));
            resultMap.put("defaultShowLeft", true);
            resultMap.put("status", "1");
            resultMap.put("rightMenus", rightMenus);
        } catch (Exception e) {
            writeLog(e);
            resultMap.put("status", "-1");
            resultMap.put("message", ""+weaver.systeminfo.SystemEnv.getHtmlLabelName(10004501,weaver.general.ThreadVarLanguage.getLang())+"");
        }
        return resultMap;
    }
}
