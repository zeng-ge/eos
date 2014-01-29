package com.sunsharing.eos.server.zookeeper;

import com.alibaba.fastjson.JSONObject;
import com.sunsharing.eos.common.zookeeper.PathConstant;
import com.sunsharing.eos.common.zookeeper.ZookeeperUtils;
import com.sunsharing.eos.server.sys.SysProp;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;

/**
 * Created by criss on 14-1-28.
 */
public class ServiceRegister {

    Logger logger = Logger.getLogger(ServiceRegister.class);

    static ServiceRegister register = new ServiceRegister();

    private ServiceRegister()
    {

    }

    public static ServiceRegister getInstance()
    {
        return register;
    }

    /**
     * 用线程初始化
     */
    public void init()
    {
        ZookeeperUtils utils = ZookeeperUtils.getInstance();
        utils.setZooKeeperIP(SysProp.zookeeperIp);
        utils.setZooKeeperPort(SysProp.zookeeperPort);
        utils.setCallBack(new ServerConnectCallBack());
        //如果zookeeper没有启动，会同步重试，请用线程初始化
        utils.connect();
    }

    /**
     * 先注册EOS
     * @param eosIds
     */
    public synchronized void registerEos(String eosIds) throws Exception
    {
        ZookeeperUtils utils = ZookeeperUtils.getInstance();
        String []eosArr = eosIds.split(",");
        for(int i=0;i<eosArr.length;i++)
        {
            utils.watchNode(PathConstant.EOS_STATE+"/"+eosArr[i]);
        }
    }

    /**
     *
     * @param eosIds 用逗号分隔的EOSID
     * @param json 必须要有的字段包括如下：
     *  JSONObject obj = new JSONObject();
     *  obj.put("appId",appId);
     *  obj.put("serviceId",serviceId);
     *  obj.put("version", version);
     */
    public synchronized boolean registerService(String eosIds,String json)
    {
        ZookeeperUtils utils = ZookeeperUtils.getInstance();
        String[] ids = eosIds.split(",");
        JSONObject obj = JSONObject.parseObject(json);
        String servicePath = obj.getString(PathConstant.APPID_KEY)+
                obj.getString(PathConstant.SERVICE_ID_KEY)+
                obj.getString(PathConstant.VERSION_KEY);
        boolean result = false;
        for(int i=0;i<ids.length;i++)
        {
            String eosId = ids[i];
            try
            {
                boolean r = utils.isExists(PathConstant.EOS_STATE+"/"+eosId);
                if(!r)
                {
                    logger.warn("EOS:"+eosId+",不在线无法注册");
                    continue;
                }
                utils.createNode(PathConstant.SERVICE_STATE+"/"+eosId+"/"+servicePath,json, CreateMode.EPHEMERAL);
                result = true;
            }catch (Exception e)
            {
                logger.error("注册失败",e);
            }
        }
        return result;
    }


}
