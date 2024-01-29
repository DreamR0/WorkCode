package weaver.hrm.loginstrategy;

import org.apache.commons.lang3.StringUtils;
import weaver.conn.RecordSet;
import weaver.general.IpUtils;
import weaver.hrm.loginstrategy.exception.LoginStrategyException;
import weaver.hrm.loginstrategy.style.LoginStrategy;
import weaver.hrm.resource.ResourceComInfo;

public class LoginStrategyManager {

    private static final String NO_FOCUS_IP = "0:0:0:0:0:0:0:1" ;

    public static void checkLoginStrategy(String loginid,String clientIp)throws LoginStrategyException {
        // 不关注 超级管理员
       // if("sysadmin".equals(loginid)) return ;



        // 网段策略
        LoginStrategy segmentStrategy = LoginStrategy.SEGMENT_STRATEGY ;
        boolean isSegmentOn = new LoginStrategyComInfo().isFlagOn(segmentStrategy.toString()) ;

        if(isSegmentOn){ // key open
            RecordSet rs = new RecordSet() ;
            rs.executeQuery("select userusbtype,usbstate from hrmresourcemanager where loginid=?",loginid) ;
            if(!rs.next()){ // 非管理员
                rs.executeQuery("select userusbtype,usbstate from hrmresource where loginid=?",loginid) ;
                if(!rs.next()){ // 非普通人员
                    return ;
                }
            }

            String userusbtype = rs.getString(1) ;
            String usbstate = rs.getString(2) ;

            if((segmentStrategy.getUserType()+"").equals(userusbtype)){ // 使用 网段策略
                if(!"0".equals(usbstate)){
                    return ;
                }
                // 启用
                checkIpSegmentStrategy(clientIp) ;
            }
        }
    }

    public static boolean isInIp(String ip){
        if(StringUtils.isBlank(ip)) return true ;
        if(NO_FOCUS_IP.equals(ip)) return true;

        RecordSet rs = new RecordSet() ;
        rs.executeQuery("select * from HrmnetworkSegStr") ;
        boolean isIpPass = true ;
        while(rs.next()){
            isIpPass = false ;
            String inceptipaddress =rs.getString("inceptipaddress");
            String endipaddress = rs.getString("endipaddress");
            String ipAddressType = rs.getString("ipAddressType").equals("IPv6") ? "IPv6" : "IPv4";

            if(ipAddressType.equals("IPv4") && ip.indexOf(".") > -1){
                long ip1 = IpUtils.ip2number(inceptipaddress);
                long ip2 = IpUtils.ip2number(endipaddress);
                long ip3 = IpUtils.ip2number(ip);
                if(ip3>=ip1 && ip3<=ip2){
                    isIpPass = true ;
                    break ;
                }
            }else if(ipAddressType.equals("IPv6") && ip.indexOf(":") > -1){
                String ip1 = IpUtils.parseAbbreviationToFullIPv6(inceptipaddress);
                String ip2 = IpUtils.parseAbbreviationToFullIPv6(endipaddress);
                String ip3 = IpUtils.parseAbbreviationToFullIPv6(ip);
                if (ip3.compareTo(ip1) >= 0 && ip3.compareTo(ip2) <= 0) {
                    isIpPass = true;
                    break;
                }
            }
        }

        return isIpPass ;
    }

    private static void checkIpSegmentStrategy(String ip) throws LoginStrategyException{
        if(!isInIp(ip)){
            throw new LoginStrategyException("88","网段策略检查失败！") ;
        }
    }
}
