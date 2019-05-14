package io.mycat.proxy;

import io.mycat.beans.mycat.MySQLDataNode;
import io.mycat.beans.mycat.MycatDataNode;
import io.mycat.beans.mycat.MycatSchema;
import io.mycat.beans.mysql.packet.MySQLPacketSplitter;
import io.mycat.plug.loadBalance.LoadBalanceStrategy;
import io.mycat.proxy.buffer.ProxyBuffer;
import io.mycat.proxy.handler.LocalInFileRequestHandler.LocalInFileSession;
import io.mycat.proxy.handler.PrepareStatementHandler.PrepareStatementSession;
import io.mycat.proxy.packet.MySQLPacketUtil;
import io.mycat.proxy.session.MySQLProxySession;
import io.mycat.proxy.session.MySQLServerSession;
import io.mycat.proxy.session.MycatSession;
import java.io.IOException;

/**
 * @author jamie12221
 * @date 2019-05-12 22:41
 * mycat session用户视图,屏蔽proxy复杂性
 **/
public interface MycatSessionView extends LocalInFileSession, PrepareStatementSession,
                                              MySQLServerSession<MycatSession> {

  default boolean proxyBackend(byte[] payload, String dataNodeName, boolean runOnSlave,
      LoadBalanceStrategy strategy, boolean noResponse,
      AsyncTaskCallBack<MycatSessionView> finallyCallBack) {
    MycatSession mycat = (MycatSession) this;
    mycat.resetProxyBuffer(MySQLPacketUtil.generateMySQLPacket(0, payload));
    return proxyBackend(dataNodeName, runOnSlave, strategy, noResponse, finallyCallBack);
  }

//  default boolean proxyBackend(byte[][] payloadList,String[] dataNodeName, boolean runOnSlaveList,
//      LoadBalanceStrategy strategy,
//      boolean noResponse, AsyncTaskCallBack<MycatSessionView> finallyCallBack) {
//    assert dataNodeName != null && !"".equals(dataNodeName);
//    MycatSession mycat = (MycatSession) this;
//    MycatDataNode mycatDataNode = ProxyRuntime.INSTANCE
//                                      .getDataNodeByName(dataNodeName);
//    mycat.setCallBack(finallyCallBack);
//    boolean isMySQLDataNode = mycatDataNode instanceof MySQLDataNode;
//    mycat.setCallBack(finallyCallBack);
//    if (!isMySQLDataNode) {
//      mycat.setLastMessage("can not get mysql dataNode");
//      writeErrorEndPacket();
//      return false;
//    }
//    ProxyBuffer proxyBuffer = mycat.currentProxyBuffer();
//    if (proxyBuffer.channelReadEndIndex() > MySQLPacketSplitter.MAX_PACKET_SIZE) {
//      String message = "More than "
//                           + MySQLPacketSplitter.MAX_PACKET_SIZE
//                           + " so it can't be transmitted through";
//      mycat.setLastMessage(message);
//      writeErrorEndPacket();
//      return false;
//    }
//    proxyBuffer.channelWriteStartIndex(0);
//    proxyBuffer.channelWriteEndIndex(proxyBuffer.channelReadEndIndex());
//    mycat.getBackend(runOnSlave, (MySQLDataNode) mycatDataNode, strategy,
//        (mysql, sender, success, result, throwable) -> {
//          if (success) {
//            mycat.clearReadWriteOpts();
//            mycat.switchWriteHandler(MySQLProxySession.WriteHandler.INSTANCE);
//            mysql.setNoResponse(noResponse);
//            mysql.switchProxyNioHandler();
//            try {
//              mysql.writeProxyBufferToChannel(mycat.currentProxyBuffer());
//            } catch (IOException e) {
//              String message = setLastMessage(e);
//              writeErrorEndPacket();
//            }
//            return;
//          } else {
//            writeErrorEndPacket();
//          }
//        });
//    return true;
//  }

  default boolean proxyBackend(String dataNodeName, boolean runOnSlave,
      LoadBalanceStrategy strategy,
      boolean noResponse, AsyncTaskCallBack<MycatSessionView> finallyCallBack) {
    assert dataNodeName != null && !"".equals(dataNodeName);
    MycatSession mycat = (MycatSession) this;
    MycatDataNode mycatDataNode = ProxyRuntime.INSTANCE
                                      .getDataNodeByName(dataNodeName);
    mycat.setCallBack(finallyCallBack);
    boolean isMySQLDataNode = mycatDataNode instanceof MySQLDataNode;
    mycat.setCallBack(finallyCallBack);
    if (!isMySQLDataNode) {
      mycat.setLastMessage("can not get mysql dataNode");
      writeErrorEndPacket();
      return false;
    }
    ProxyBuffer proxyBuffer = mycat.currentProxyBuffer();
    if (proxyBuffer.channelReadEndIndex() > MySQLPacketSplitter.MAX_PACKET_SIZE) {
      String message = "More than "
                           + MySQLPacketSplitter.MAX_PACKET_SIZE
                           + " so it can't be transmitted through";
      mycat.setLastMessage(message);
      writeErrorEndPacket();
      return false;
    }
    proxyBuffer.channelWriteStartIndex(0);
    proxyBuffer.channelWriteEndIndex(proxyBuffer.channelReadEndIndex());
    mycat.getBackend(runOnSlave, (MySQLDataNode) mycatDataNode, strategy,
        (mysql, sender, success, result, attr) -> {
          if (success) {
            mycat.clearReadWriteOpts();
            mycat.switchWriteHandler(MySQLProxySession.WriteHandler.INSTANCE);
            mysql.setNoResponse(noResponse);
            mysql.switchProxyNioHandler();
            try {
              mysql.writeProxyBufferToChannel(mycat.currentProxyBuffer());
            } catch (IOException e) {
              String message = setLastMessage(e);
              writeErrorEndPacket();
            }
            return;
          } else {
            mycat.setLastMessage((String) result);
            writeErrorEndPacket();
          }
        });
    return true;
  }

  MycatSchema getSchema();

  void useSchema(MycatSchema schemaName);

  boolean hasResultset();

  boolean hasCursor();

  void countDownResultSet();

  void setResultSetCount(int count);
}
