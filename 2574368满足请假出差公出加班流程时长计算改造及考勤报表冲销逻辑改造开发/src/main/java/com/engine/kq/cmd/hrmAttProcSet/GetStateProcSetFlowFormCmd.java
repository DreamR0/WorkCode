package com.engine.kq.cmd.hrmAttProcSet;

import com.api.browser.bean.SearchConditionItem;
import com.api.browser.bean.SearchConditionOption;
import com.api.hrm.bean.HrmFieldBean;
import com.api.hrm.util.HrmFieldSearchConditionComInfo;
import com.engine.common.biz.AbstractCommonCommand;
import com.engine.common.entity.BizLogContext;
import com.engine.core.interceptor.CommandContext;
import com.engine.kq.wfset.attendance.domain.HrmAttProcSet;
import com.engine.kq.wfset.attendance.manager.HrmAttProcSetManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.hrm.company.SubCompanyComInfo;
import weaver.hrm.moduledetach.ManageDetachComInfo;
import weaver.systeminfo.SystemEnv;
import weaver.systeminfo.systemright.CheckSubCompanyRight;
import weaver.workflow.workflow.WorkflowBillComInfo;
import weaver.workflow.workflow.WorkflowComInfo;

/**
 * 获取考勤流程设置 流程概览信息
 * @author pzy
 *
 */
public class GetStateProcSetFlowFormCmd extends AbstractCommonCommand<Map<String, Object>>{

	public GetStateProcSetFlowFormCmd(Map<String, Object> params, User user) {
		this.user = user;
		this.params = params;
	}

	@Override
	public BizLogContext getLogContext() {
		return null;
	}


	@Override
	public Map<String, Object> execute(CommandContext commandContext) {
		Map<String,Object> retmap = new HashMap<String,Object>();
		 
		List<Object> lsGroup = new ArrayList<Object>();
		Map<String,Object> groupitem = null;
		List<Object> itemlist = null;
		try {

			ManageDetachComInfo ManageDetachComInfo = new ManageDetachComInfo();
      boolean hrmdetachable = ManageDetachComInfo.isUseHrmManageDetach();//是否开启了人力资源模块的管理分权
      String hrmdftsubcomid = ManageDetachComInfo.getHrmdftsubcomid();//分权默认分部

      CheckSubCompanyRight newCheck=new CheckSubCompanyRight();
			RecordSet rs = new RecordSet();
			HrmAttProcSetManager attProcSetManager = new HrmAttProcSetManager();
			WorkflowComInfo wfci=new WorkflowComInfo();
			WorkflowBillComInfo wbc = new WorkflowBillComInfo();
			SubCompanyComInfo sc = new SubCompanyComInfo();
			
			String id = StringUtil.vString(params.get("id"));
			int subcompanyid = StringUtil.parseToInt(StringUtil.vString(params.get("subcompanyid")), 0);
			subcompanyid = subcompanyid < 0 ? 0 : subcompanyid;

			boolean isEdit = id.length() > 0;
			HrmAttProcSet bean = isEdit ? attProcSetManager.get(id) : null;
			bean = bean == null ? new HrmAttProcSet() : bean;
			boolean isForm = bean.isSysForm();
			if(bean.getField004() <= 0) bean.setField004(subcompanyid);
//			String field002Name = "";
//			if(bean.getField002() != 0){
//				rs.executeSql("select b.labelname from WorkFlow_Bill a left join HtmlLabelInfo b on a.nameLabel = b.indexID and b.languageid = "+user.getLanguage()+" where a.id = "+bean.getField002());
//				field002Name = rs.next() ? rs.getString(1) : "";
//			}
			//流程路径 对应表单 所属分部 是否启用 变更流程类型 是否启用明细 模板文件 模板文件(手机版) 模板地址 流程可抵扣打卡記錄
			String[] fields = new String[]{"field001,34067,3,-99991","field002,15600,3,wfFormBrowser","field004,19799,3,169",
                                "field005,18095,4,2","field006,84791,5,1","usedetail,18411,4,2","flow_deduct_card,512290,4,2",
                                "templetfile,19971,1,1","templetmobilefile,19971,1,1","templetroute,82482,1,1"};
			String[] values = new String[fields.length];
			if(isEdit){
				values[0] = ""+bean.getField001();
				values[1] = ""+bean.getField002();
				values[2] = ""+bean.getField004();
        subcompanyid = bean.getField004();
				values[3] = Util.null2o(""+bean.getField005());
				values[4] = ""+bean.getField006();
        values[5] = ""+bean.getUsedetail();
        values[6] = ""+bean.getFlow_deduct_card();
        String custompage = "";
        String custompage4Emoble = "";
        String templetroute = "";
        switch (bean.getField006()){
          case 0 :
            custompage = KqTempletEnum.LEAVE.getTempletfile();
            custompage4Emoble = KqTempletEnum.LEAVE.getTempletmobilefile();
            templetroute = KqTempletEnum.LEAVE.getTempletroute();
            break;
          case 1 :
            custompage = KqTempletEnum.EVECTION.getTempletfile();
            custompage4Emoble = KqTempletEnum.EVECTION.getTempletmobilefile();
            templetroute = KqTempletEnum.EVECTION.getTempletroute();
            break;
          case 2 :
            custompage = KqTempletEnum.OUT.getTempletfile();
            custompage4Emoble = KqTempletEnum.OUT.getTempletmobilefile();
            templetroute = KqTempletEnum.OUT.getTempletroute();
            break;
          case 3 :
            custompage = KqTempletEnum.OVERTIME.getTempletfile();
            custompage4Emoble = KqTempletEnum.OVERTIME.getTempletmobilefile();
            templetroute = KqTempletEnum.OVERTIME.getTempletroute();
            break;
          case 4 :
            custompage = KqTempletEnum.OTHER.getTempletfile();
            custompage4Emoble = KqTempletEnum.OTHER.getTempletmobilefile();
            templetroute = KqTempletEnum.OTHER.getTempletroute();
            break;
          case 5 :
            custompage = KqTempletEnum.SHIFT.getTempletfile();
            custompage4Emoble = KqTempletEnum.SHIFT.getTempletmobilefile();
            templetroute = KqTempletEnum.SHIFT.getTempletroute();
            break;
          case 6 :
            custompage = KqTempletEnum.LEAVEBACK.getTempletfile();
            custompage4Emoble = KqTempletEnum.LEAVEBACK.getTempletmobilefile();
            templetroute = KqTempletEnum.LEAVEBACK.getTempletroute();
            break;
          case 7 :
            custompage = KqTempletEnum.Card.getTempletfile();
            custompage4Emoble = KqTempletEnum.Card.getTempletmobilefile();
            templetroute = KqTempletEnum.Card.getTempletroute();
            break;
          case 8 :
            custompage = KqTempletEnum.PROCESSCHANGE.getTempletfile();
            custompage4Emoble = KqTempletEnum.PROCESSCHANGE.getTempletmobilefile();
            templetroute = KqTempletEnum.PROCESSCHANGE.getTempletroute();
            break;
          default:
            break;
        }
        values[7] = ""+Util.null2s(bean.getTempletfile(), custompage);
        values[8] = ""+Util.null2s(bean.getTempletmobilefile(), custompage4Emoble);
        values[9] = ""+Util.null2s(bean.getTempletroute(), templetroute);
			}

      Map<String,Object> descMaps = new HashMap<>();
      Map<String,Object> tipMaps = new HashMap<>();

			String[] optionIndex = new String[]{"-1","0","1","2","3","5","6","7","8"};
			String[] optionLabel = new String[]{"","670,18015","20084,18015","24058,18015","6151,18015","390737","24473,18015","390274","513400"};

			//增加对于字段的说明
      handleDescMap(descMaps,optionIndex,tipMaps);

			HrmFieldSearchConditionComInfo hrmFieldSearchConditionComInfo = new HrmFieldSearchConditionComInfo();
			SearchConditionItem searchConditionItem = null;
			HrmFieldBean hrmFieldBean = null;
			itemlist = new ArrayList<Object>();
			groupitem = new HashMap<String,Object>();
			List<Map<String, Object>> replaceDatas = new ArrayList<Map<String,Object>>();
			Map<String, Object>  datas = new HashMap<String, Object>();
			groupitem.put("title", SystemEnv.getHtmlLabelName(1361, Util.getIntValue(user.getLanguage())));
			groupitem.put("defaultshow", true);
			for(int i=0;i<fields.length;i++){
				String[] fieldinfo = fields[i].split(",");
				hrmFieldBean = new HrmFieldBean();
				hrmFieldBean.setFieldname(fieldinfo[0]);
				hrmFieldBean.setFieldlabel(fieldinfo[1]);
				hrmFieldBean.setFieldhtmltype(fieldinfo[2]);
				hrmFieldBean.setType(fieldinfo[3]);
				hrmFieldBean.setIsFormField(true);
        hrmFieldBean.setViewAttr(3);
        if("usedetail".equals(hrmFieldBean.getFieldname())){
          hrmFieldBean.setFieldlabel("18095,17463");
        }
        if("templetmobilefile".equals(hrmFieldBean.getFieldname())){
          hrmFieldBean.setFieldlabel("19971,81913,31506,81914");
        }
				if("field005".equals(hrmFieldBean.getFieldname())){
					hrmFieldBean.setFieldvalue(isEdit?values[i]:"1");
					if(isForm){
						hrmFieldBean.setViewAttr(1);
					}
				}else if("field006".equals(hrmFieldBean.getFieldname())){
					hrmFieldBean.setFieldlabel("15880,18015,63");
					hrmFieldBean.setFieldvalue(isEdit?values[i]:"0");
				}else if("usedetail".equals(hrmFieldBean.getFieldname())){
          hrmFieldBean.setFieldvalue(isEdit?values[i]:"0");
          hrmFieldBean.setViewAttr(2);
        }else if("flow_deduct_card".equals(hrmFieldBean.getFieldname())){
          hrmFieldBean.setFieldvalue(isEdit?values[i]:"0");
          hrmFieldBean.setViewAttr(2);
        }else if("templetfile".equals(hrmFieldBean.getFieldname())){
          hrmFieldBean.setFieldvalue(isEdit?values[i]: KqTempletEnum.LEAVE.getTempletfile());
          hrmFieldBean.setViewAttr(2);
          hrmFieldBean.setMultilang(false);
        }else if("templetmobilefile".equals(hrmFieldBean.getFieldname())){
          hrmFieldBean.setFieldvalue(isEdit?values[i]: KqTempletEnum.LEAVE.getTempletmobilefile());
          hrmFieldBean.setViewAttr(2);
          hrmFieldBean.setMultilang(false);
        }else if("templetroute".equals(hrmFieldBean.getFieldname())){
          hrmFieldBean.setFieldvalue(isEdit?values[i]: KqTempletEnum.LEAVE.getTempletroute());
          hrmFieldBean.setViewAttr(1);
          hrmFieldBean.setMultilang(false);
        }else{
					hrmFieldBean.setFieldvalue(isEdit?values[i]:"");
				}
        if("field004".equals(hrmFieldBean.getFieldname())){
          if(hrmdetachable){
            hrmFieldBean.setViewAttr(3);
            hrmFieldBean.setRules("required|string");
            String defaultSubcompanyid = "";
            int[] subcomids = newCheck.getSubComByUserRightId(user.getUID(),"HrmAttendanceProcess:setting",0);
            ManageDetachComInfo detachComInfo = new ManageDetachComInfo();
            if(detachComInfo.isUseHrmManageDetach()){
              defaultSubcompanyid = detachComInfo.getHrmdftsubcomid();
            }else{
              rs.executeProc("SystemSet_Select","");
              if(rs.next()){
                if(subcompanyid == 0){
                  defaultSubcompanyid = Util.null2String(rs.getString("dftsubcomid"));
                }
              }
            }

            boolean hasRight = false;
            for (int j = 0; subcomids!=null&& j < subcomids.length; j++) {
              if((""+subcomids[j]).equals(defaultSubcompanyid)){
                hasRight = true;
                break;
              }
            }

            if(!hasRight){
              defaultSubcompanyid = "";
            }
            //表示左侧分部树选择了
            if(Util.getIntValue(Util.null2String(values[i])) > 0){
              hrmFieldBean.setFieldvalue(subcompanyid);
            }else{
              hrmFieldBean.setFieldvalue(defaultSubcompanyid);
            }

          }else{
            //不开启分权的话，不显示分部
            continue;
          }
        }

	  			replaceDatas = new ArrayList<Map<String,Object>>();
	  			datas = new HashMap<String, Object>();
				
				if("field006".equals(hrmFieldBean.getFieldname())){
					List<SearchConditionOption> options = new ArrayList<SearchConditionOption>();
					if(isEdit){
						for(int k = 0; k < optionIndex.length ;  k++){
							if(k == Util.getIntValue(values[i])){
								options.add(new SearchConditionOption(optionIndex[Util.getIntValue(values[i])],SystemEnv.getHtmlLabelNames(optionLabel[Util.getIntValue(values[i])], user.getLanguage()),true));
							}else{
								options.add(new SearchConditionOption(optionIndex[k],SystemEnv.getHtmlLabelNames(optionLabel[k],user.getLanguage())));
							}
						}
					}else{
						for(int k = 0; k < optionIndex.length ;  k++){
							SearchConditionOption  SearchConditionOption1 = new SearchConditionOption(optionIndex[k],SystemEnv.getHtmlLabelNames(optionLabel[k],user.getLanguage()));
							if(k == 0){
								SearchConditionOption1.setSelected(true);
							}
							options.add(SearchConditionOption1);
						}
					}
					hrmFieldBean.setSelectOption(options);
				}
				searchConditionItem = hrmFieldSearchConditionComInfo.getSearchConditionItem(hrmFieldBean, user);
			  	if("field001".equals(hrmFieldBean.getFieldname())){
			  	  //流程树是不支持多版本的，只有列表的是可以显示多版本的
			  		searchConditionItem.getBrowserConditionParam().getDataParams().put("isWfTree", "1");
			  		searchConditionItem.getBrowserConditionParam().getDataParams().put("isvalid", "1,3");
            searchConditionItem.getBrowserConditionParam().getCompleteParams().put("isvalid", "1,3");
			  		searchConditionItem.getBrowserConditionParam().setHasAddBtn(true);
			  		searchConditionItem.getBrowserConditionParam().setViewAttr(3);
			  		if(isEdit){
			  			String tmpWfId = values[i];
			  			String wfname = wfci.getWorkflowname(tmpWfId);
			  			datas.put("id",tmpWfId);
			  			datas.put("name",wfname);
			  			replaceDatas.add(datas);
			  			searchConditionItem.getBrowserConditionParam().setReplaceDatas(replaceDatas);
			  		}
			  	}
			  	if("field002".equals(hrmFieldBean.getFieldname())){
			  		searchConditionItem.getBrowserConditionParam().setHasAddBtn(true);
			  		searchConditionItem.getBrowserConditionParam().setViewAttr(3);
			  		searchConditionItem.setHelpfulTip(SystemEnv.getHtmlLabelName(82815,user.getLanguage()));
			  		if(isEdit){
			  			String tmpFormId = values[i];
			  			String wfname = SystemEnv.getHtmlLabelNames(wbc.getNamelabel(tmpFormId),user.getLanguage());
			  			datas.put("id",tmpFormId);
			  			datas.put("name",wfname);
			  			replaceDatas.add(datas);
			  			searchConditionItem.getBrowserConditionParam().setReplaceDatas(replaceDatas);
			  		}
			  	}
        if("flow_deduct_card".equals(hrmFieldBean.getFieldname())){
          searchConditionItem.setHelpfulTip(SystemEnv.getHtmlLabelName(512291,user.getLanguage()));
        }
			  	if("field004".equals(hrmFieldBean.getFieldname())){
          searchConditionItem.getBrowserConditionParam().getDataParams().put("rightStr", "HrmAttendanceProcess:setting");
          searchConditionItem.getBrowserConditionParam().getCompleteParams().put("rightStr", "HrmAttendanceProcess:setting");
        }
				if("field005".equals(hrmFieldBean.getFieldname())){
					if(isForm){
						searchConditionItem.setViewAttr(1);
					}
				}
				searchConditionItem.setRules("required|string");
			  	searchConditionItem.setLabelcol(6);
			  	searchConditionItem.setFieldcol(18);

				itemlist.add(searchConditionItem);
			}
			groupitem.put("items", itemlist);
			lsGroup.add(groupitem);
			retmap.put("condition", lsGroup);
			retmap.put("desc", descMaps);
      retmap.put("tips", tipMaps);
			retmap.put("status", "1");

      int operatelevel = -1;
      if(hrmdetachable){
        if(subcompanyid > 0){
          CheckSubCompanyRight checkSubCompanyRight = new CheckSubCompanyRight();
          operatelevel=checkSubCompanyRight.ChkComRightByUserRightCompanyId(user.getUID(),"HrmAttendanceProcess:setting",subcompanyid);
        }
      }else{
        operatelevel = 2;
      }
      if(!isEdit){
        operatelevel = 2;
      }

      if(user.getUID() == 1){
        operatelevel = 2;
      }
      if(operatelevel > 0){
        retmap.put("canAdd", true);
      }else{
        retmap.put("canAdd", false);
      }

		} catch (Exception e) {
			retmap.put("status", "-1");
			retmap.put("message",  SystemEnv.getHtmlLabelName(382661,user.getLanguage()));
			writeLog(e);
		}
			
		return retmap;
	}

  /**
   * 增加对于字段的说明
   * @param descMaps
   * @param tipMaps
   */
  private void handleDescMap(Map<String, Object> descMaps, String[] optionIndex,
      Map<String, Object> tipMaps) {
    Map<String,Object> descMap = new HashMap<>();
    Map<String,Object> tipMap = new HashMap<>();
    for(int i = 0 ; i < optionIndex.length ; i++){
      if("5".equalsIgnoreCase(optionIndex[i])){
        //排班流程 仅支持明细表单
        descMap.put(""+i, SystemEnv.getHtmlLabelName(391247, user.getLanguage()));
      }else if("6".equalsIgnoreCase(optionIndex[i]) || "7".equalsIgnoreCase(optionIndex[i])){
        //补卡或者销假 仅支持明细表单
        descMap.put(""+i, SystemEnv.getHtmlLabelName(391247, user.getLanguage()));
      }else if("8".equalsIgnoreCase(optionIndex[i])){
        //考勤变更流程 仅支持明细表单
        descMap.put(""+i, SystemEnv.getHtmlLabelName(391247, user.getLanguage()));
        tipMap.put(""+i, SystemEnv.getHtmlLabelName(513401, user.getLanguage()));
      }else{
        //其他流程 支持主表或者明细表单
        if(i < 0){
          descMap.put(""+i, "");
        }else{
          descMap.put(""+i, SystemEnv.getHtmlLabelName(391249, user.getLanguage()));
        }
      }
    }
    descMaps.put("field006", descMap);
    tipMaps.put("field006", tipMap);
  }
}
