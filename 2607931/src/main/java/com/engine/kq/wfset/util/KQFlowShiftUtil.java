package com.engine.kq.wfset.util;

import com.alibaba.fastjson.JSON;
import com.api.customization.qc2607931.util.CheckUtil;
import com.engine.kq.biz.KQAttFlowFieldsSetBiz;
import com.engine.kq.biz.KQFormatBiz;
import com.engine.kq.biz.KQFormatData;
import com.engine.kq.biz.KQGroupMemberComInfo;
import com.engine.kq.biz.KQShiftscheduleBiz;
import com.engine.kq.entity.KQGroupEntity;
import com.engine.kq.log.KQLog;
import com.engine.kq.wfset.bean.SplitBean;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.resource.ResourceComInfo;

public class KQFlowShiftUtil {
  private KQLog kqLog = new KQLog();

  /**
   * 拆分排班数据
   * @param sqlMap
   * @param splitBeans
   * @param datetimeFormatter
   * @param workflowId
   * @param requestId
   * @param rci
   * @return
   * @throws Exception
   */
  public void handleKQShiftAction(Map<String, String> sqlMap,
      List<SplitBean> splitBeans, DateTimeFormatter datetimeFormatter, int workflowId,
      int requestId, ResourceComInfo rci) throws Exception{

    KQGroupMemberComInfo kqGroupMemberComInfo = new KQGroupMemberComInfo();
    KQShiftscheduleBiz kqShiftscheduleBiz = new KQShiftscheduleBiz();

    RecordSet rs1 = new RecordSet();
    String detail_resourceId = "";
    String detail_fromDate = "";
    String detail_toDate = "";
    String detail_shift = "";
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    String[] shiftFields = KQAttFlowFieldsSetBiz.shiftDetailFields;
    kqLog.info("handleKQShiftAction: : sqlMap:"+sqlMap);

    if(!sqlMap.isEmpty()){
      KQFormatData kqFormatData = new KQFormatData();
      for(Map.Entry<String,String> me : sqlMap.entrySet()){
        String concort = "###" ;
        String key = me.getKey();
        String value = me.getValue();

        String wfId =  key.split(concort)[3] ;
        int usedetail =  Util.getIntValue(key.split(concort)[2], 0);
        String tableDetailName=  key.split(concort)[1] ;
        String tableName=  key.split(concort)[0] ;
        String idVal = "";
        String id = "dataId";
        if(usedetail == 1){
          id = "detailId";
        }
        rs1.execute(value);
        CheckUtil checkUtil = new CheckUtil();
        while (rs1.next()) {
          idVal = Util.null2s(rs1.getString(id), "");

          String f_detail_resourceId = shiftFields[0];
          String f_detail_fromDate = shiftFields[1];
          String f_detail_toDate = shiftFields[2];
          String f_detail_shift = shiftFields[3];

          detail_resourceId = Util.null2s(rs1.getString(f_detail_resourceId), "");
          detail_fromDate = Util.null2s(rs1.getString(f_detail_fromDate), "");
          detail_toDate = Util.null2s(rs1.getString(f_detail_toDate), "");
          detail_shift = Util.null2s(rs1.getString(f_detail_shift), "");

          KQGroupEntity kQGroupEntity = kqGroupMemberComInfo.getUserKQGroupInfo(detail_resourceId,detail_fromDate,true);
          kqLog.info("handleKQShiftAction: : detail_resourceId:"+detail_resourceId+":detail_fromDate:"+detail_fromDate+":detail_toDate:"+detail_toDate+":detail_shift:"+detail_shift);

          if(kQGroupEntity == null){
            kqLog.info("handleKQShiftAction: kQGroupEntity is null : detail_resourceId:"+detail_resourceId+":detail_fromDate:"+detail_fromDate);
            continue;
          }
          kqLog.info("handleKQShiftAction: kQGroupEntity : kQGroupEntity:"+ JSON.toJSON(kQGroupEntity)+":detail_resourceId:"+detail_resourceId
              +":detail_fromDate:"+detail_fromDate+":detail_toDate:"+detail_toDate+":detail_shift:"+detail_shift);

          String kqtype = kQGroupEntity.getKqtype();
          if ("2".equalsIgnoreCase(kqtype)) {
            kqShiftscheduleBiz.saveShiftschedule(detail_resourceId, detail_fromDate, detail_toDate, detail_shift,kQGroupEntity.getId());
            //排班流程 生成之后还需要格式化下对应人员下的考勤数据
            LocalDate fromLocal = LocalDate.parse(detail_fromDate);
            LocalDate toLocal = LocalDate.parse(detail_toDate);
            if(!toLocal.isBefore(fromLocal)){
              long betweenDays = toLocal.toEpochDay() - fromLocal.toEpochDay();
              for (int i = 0; i <= betweenDays; i++) {
                LocalDate curLocalDate = fromLocal.plusDays(i);
                String shiftDate = curLocalDate.format(dateFormatter);
                kqLog.info("handleKQShiftAction: formatDate : detail_resourceId:"+detail_resourceId+":shiftDate:"+shiftDate);
                new KQFormatData().formatKqDate(detail_resourceId, shiftDate);
              }
            }
          }else{
            kqLog.info("handleKQShiftAction: kqtype is not 2 : detail_resourceId:"+detail_resourceId+":detail_fromDate:"+detail_fromDate+":detail_toDate:"+detail_toDate+":kqtype:"+kqtype);
          }
        }
      }
    }
  }
}
