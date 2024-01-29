package com.engine.portal.biz.nonstandardfunction;

import com.weaver.general.BaseBean;
import com.weaver.general.Util;
import com.weaver.upgrade.FunctionUpgrade;
import com.weaver.upgrade.FunctionUpgradeUtil;
import weaver.conn.RecordSet;
import weaver.file.Prop;
import weaver.general.GCONST;
import weaver.general.OrderProperties;
import weaver.hrm.online.IPUtil;
import weaver.system.License;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 系统模块开启状态查询接口
 * sunzy
 */
public class SysModuleInfoBiz {
    /**
     *
     * @param moduleid  模块标识id，例如微博：ModuleConstants.Blog、流程：ModuleConstants.Workflow等
     * @return
     */
    public static boolean checkModuleStatus(String moduleid){
        BaseBean bb=new BaseBean();
        String filePath = GCONST.getPropertyPath() + "module.properties";
        OrderProperties prop = new OrderProperties();
        prop.load(filePath,"GBK");
        String cid = new License().getCId();
        RecordSet rs=new RecordSet();
        String propValue="";
        String openValue="";
        String checkOpenValue="";
        boolean isOpen=false;
        switch (moduleid){
            //门户
            case "home":
                propValue=prop.get("portal.status");
                if("1".equals(propValue)) isOpen=true;
                break;
            //协作
            case "cowork" :
                propValue=prop.get("cwork.status");
                if("1".equals(propValue)) isOpen=true;
                break;
            //流程
            case "workflow" :
                propValue=prop.get("workflow.status");
                if("1".equals(propValue)) isOpen=true;
                break;
            //会议
            case "meeting" :
                propValue=prop.get("meeting.status");
                if("1".equals(propValue)) isOpen=true;
                break;
            //微博
            case "blog" :
                    propValue=prop.get("weibo.status");
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(392, Integer.parseInt(cid))+"' and type='left'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                 checkOpenValue=FunctionUpgradeUtil.getMenuStatus(392, 1, Integer.parseInt(cid))+"";
                if("1".equals(propValue)&&openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //客户
            case "crm" :
                propValue=prop.get("crm.status");
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(3, Integer.parseInt(cid))+"' and type='left'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                 checkOpenValue=FunctionUpgradeUtil.getMenuStatus(3, 1, Integer.parseInt(cid))+"";
                if("1".equals(propValue)&&openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //相册
            case "photo" :
                propValue=prop.get("photo.status");
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(199, Integer.parseInt(cid))+"' and type='left'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(199, 1, Integer.parseInt(cid))+"";
                if("1".equals(propValue)&&openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //人事
            case "hrm" :
                propValue=prop.get("hrm.status");
                if("1".equals(propValue)) isOpen=true;
                break;
            //资产
            case "fa" :
                propValue=prop.get("cpt.status");
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(7, Integer.parseInt(cid))+"' and type='left'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(7, 1, Integer.parseInt(cid))+"";
                if("1".equals(propValue)&&openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //知识
            case "doc" :
                propValue=prop.get("doc.status");
                if("1".equals(propValue)) isOpen=true;
                break;
            //项目
            case "project" :
                propValue=prop.get("proj.status");
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(4, Integer.parseInt(cid))+"' and type='left'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(4, 1, Integer.parseInt(cid))+"";
                if("1".equals(propValue)&&openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //执行力平台
            case "implement" :
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(643, Integer.parseInt(cid))+"' and type='left'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(643, 1, Integer.parseInt(cid))+"";
                if(openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //集成中心
            case "integration" :
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(10007, Integer.parseInt(cid))+"' and type='top'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(10007, 1, Integer.parseInt(cid))+"";
                if(openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //公文
            case "official" :
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(616, Integer.parseInt(cid))+"' and type='left'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(616, 1, Integer.parseInt(cid))+"";
                if(openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //日程
            case "schedule" :
                propValue=prop.get("scheme.status");
                if("1".equals(propValue)) isOpen=true;
                break;
            //通信
            case "message" :
                propValue=prop.get("message.status");
                if("1".equals(propValue)) isOpen=true;
                break;
            //车辆
            case "car" :
                propValue=prop.get("car.status");
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(144, Integer.parseInt(cid))+"' and type='left'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(144, 1, Integer.parseInt(cid))+"";
                if("1".equals(propValue)&&openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //微搜
            case "ws" :
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(559, Integer.parseInt(cid))+"' and type='left'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(559, 1, Integer.parseInt(cid))+"";
                if(openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //表单建模
            case "modelingengine" :
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(10005, Integer.parseInt(cid))+"' and type='top'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(10005, 1, Integer.parseInt(cid))+"";
                if(openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //移动引擎
            case "mobileengine" :
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(10006, Integer.parseInt(cid))+"' and type='top'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(10006, 1, Integer.parseInt(cid))+"";
                if("1".equals(propValue)&&openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //财务
            case "finance" :
                propValue=prop.get("finance.status");
                if("1".equals(propValue)) isOpen=true;
                break;
            //督查督办
            case "govern":
            	 propValue=prop.get("govern.status");
                 if("1".equals(propValue)) isOpen=true;
                 break;
            //移动门户
            case "mobileportal":
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(10324, Integer.parseInt(cid))+"' and type='top'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(10324, 1, Integer.parseInt(cid))+"";
                if(openValue.equals(checkOpenValue)) isOpen=true;
                break;
            //邮件(标准功能)
            case "email":
                isOpen = true;
                break;
            case "clockingin":
                rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(100003, Integer.parseInt(cid))+"' and type='top'");
                if(rs.next()){
                    openValue=rs.getString("isopen");
                }
                checkOpenValue=FunctionUpgradeUtil.getMenuStatus(100003, 1, Integer.parseInt(cid))+"";
                if(openValue.equals(checkOpenValue)) isOpen=true;
                break;
        }
        return isOpen;
    }

    /**
     *  校验菜单隐藏状态接口
     * @param menuId  菜单id
     * @param menuType 菜单类型left:前端 ，top：后端
     * @return
     */
    public static boolean  checkMenuStatus(String menuId,String menuType){
        String cid = new License().getCId();
        boolean isOpen=false;
        String openValue="";
        String checkOpenValue="";
        RecordSet rs=new RecordSet();
        rs.executeQuery("select isopen from menucontrollist where menuid = '"+ FunctionUpgradeUtil.getMenuId(Util.getIntValue(menuId), Integer.parseInt(cid))+"' and type='"+menuType+"'");
        if(rs.next()){
            openValue=rs.getString("isopen");
        }
        checkOpenValue=FunctionUpgradeUtil.getMenuStatus(Util.getIntValue(menuId), 1, Integer.parseInt(cid))+"";
        if(openValue.equals(checkOpenValue)) isOpen=true;
        return isOpen;
    }


    /**
     * 单个非标状态判断接口
     * @param num 非标编号
     * @return
     */
    public static boolean  checkNonstandardStatus(String num){
        RecordSet rs=new RecordSet();
        String hostaddr = "";
        boolean isOpen=false;
        String status="0";
        String sql="";
        //判断是否开启了IP集群化控制，1启用，0未启用
        if ("1".equals(Prop.getPropValue("ClusterIpController", "flag"))) {
//            hostaddr =getLocalIp();
            hostaddr = IPUtil.getLocalIp();
//            hostaddr ="192.168.1.3";
            sql = "select t1.id as id,t2.status from  hp_nonstandard_function_info t1 LEFT JOIN hp_nonstandard_func_server t2 on t2.funcid=t1.num LEFT JOIN hp_server_info t3 on t3.id=t2.serverid where t3.serverIP='" + hostaddr + "' and t1.num=? ";
        }else{
            sql = "select status from  hp_nonstandard_function_info  where num=? ";
        }
        rs.executeQuery(sql,num);
        if(rs.next()){
            status=rs.getString("status");
        }
        if("1".equals(status)){
            isOpen=true;
        }
        return isOpen;
    }

    /**
     * 获取当前服务器的ip地址，兼容集群环境
     * @return
     */
    public static String getLocalIp() {
        String hostaddr = "";
        //获取本机IP并验证其是否表hp_server_info中有备案，如果没有，则录入
        try {
            InetAddress ia = InetAddress.getLocalHost();
            //获取本机的ip地址
            hostaddr = ia.getHostAddress();

        } catch (UnknownHostException e) {
            //如果获取不到本机IP(服务器没有配置hostname IP映射)则默认ip地址为127.0.0.1
            hostaddr="127.0.0.1";
            e.printStackTrace();
        }
        return hostaddr;
    }

}
