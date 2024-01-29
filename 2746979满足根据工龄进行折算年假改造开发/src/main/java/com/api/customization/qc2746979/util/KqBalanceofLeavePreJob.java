package com.api.customization.qc2746979.util;

import com.engine.kq.biz.KQBalanceOfLeaveBiz;
import com.engine.kq.log.KQLog;
import weaver.common.DateUtil;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.schedule.BaseCronJob;

public class KqBalanceofLeavePreJob extends BaseCronJob {
    private KQLog kqLog = new KQLog();

    public void execute() {
        try {
            new BaseBean().writeLog("->>>>>>>>>>>>>>>>>>>>>>KqBalanceofLeavePreJob 批处理下一年年假 start<<<<<<<<<<<<<<<<<<<<<<");
            kqLog.info("begin do KqBalanceofLeavePreJob invoke ...");
            //今天的日期
            String currentDate = DateUtil.getCurrentDate();
            String belongYear =  Util.null2String(Util.getIntValue(currentDate.substring(0,4)) + 1);
            //获取年假的假期id
            String annualLeaveId = new BaseBean().getPropValue("qc2746979","annualLeaveId");
            KQBalanceOfLeaveBiz.createData(annualLeaveId,belongYear,0,"","1");
            kqLog.info("end do KqBalanceofLeavePreJob invoke  ...");
            new BaseBean().writeLog("->>>>>>>>>>>>>>>>>>>>>>KqBalanceofLeavePreJob 批处理"+belongYear+"年假 end<<<<<<<<<<<<<<<<<<<<<<");
        } catch (Exception e) {
            kqLog.info(e);
            new BaseBean().writeLog(">>>>>>>>>>>KqBalanceofLeavePreJob err:"+e);
        }
    }
}
