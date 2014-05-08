package com.v6ak.ia158.legway.client

import android.content.Intent
import android.bluetooth.{BluetoothSocket, BluetoothDevice}
import org.scaloid.common._
import java.util.UUID
import java.util.concurrent.Executors
import java.io.IOException
import scala.concurrent.{ExecutionContext, Future, Promise}

object ConnectionService{
  /*trait Handler{
    def connected(session: Session): Unit

  }*/
  trait Session{
    def device: BluetoothDevice

    def setSpeed(speed: Int)
  }

  import language.implicitConversions
  private implicit def funcToRunnable(f: () => Unit) = new Runnable {
    override def run() = f()
  }

  trait Command{
    def toBytes: Array[Byte]
  }

  case class WriteMessage(handle: Byte, data: Array[Byte]) extends Command{
    def toBytes: Array[Byte] = Array(0x00.toByte, 0x09.toByte, handle) ++ data
  }

  object WriteMessage{

    def withLength(handle: Byte, data: Array[Byte]) = apply(handle, Array(data.size.toByte) ++ data)

    def string(handle: Byte, message: String) = {
      withLength(handle, message.getBytes ++ Seq(0.toByte))
    }


  }
}



class ConnectionService extends LocalService {
  import ConnectionService._
  //def send(i: Int) = toast(s"service: $i")

  @volatile private var socketOption: Option[BluetoothSocket] = None

  @volatile private var started: Option[Future[Session]] = None

  private def reportIOException(e: IOException) = {
    e.printStackTrace()
    toast("BT I/O problem: "+e)
    stopSelf()
  }

  onDestroy{
    toast("destroying...")
    for(socket <- socketOption){
      socket.close()
    }
    toast("destroyed")
  }

  override def onUnbind(intent: Intent) = {
    toast("unbind")
    for(socket <- socketOption){
      socket.close()
    }
    super.onUnbind(intent)
  }

  private def startThread(f: => Unit) = {
    val thread = new Thread(f)
    thread.start()
    thread
  }

  private def deviceThread(bluetoothDevice: BluetoothDevice, sessionPromise: Promise[Session])(handler: (Session, Boolean) => Unit){
    try {
      val socket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID fromString "00001101-0000-1000-8000-00805F9B34FB")
      socketOption = Some(socket)
      socket.connect()

      startThread{
        val in = socket.getInputStream
        var c: Int = 0
        toast("Listening started")
        try {
          while ( {
            c = in.read()
            c != -1
          }) {
            toast(s"received from Legway: $c")
          }
        } catch {
          case e: IOException =>
            reportIOException(e)
        }
        toast("Service done")
        stopSelf()
      }

      val out = socket.getOutputStream
      def sendRawCommand(bytes: Array[Byte]){
        val messageLength = bytes.size
        out.write(messageLength)
        out.write(messageLength >> 8)
        out.write(bytes, 0, messageLength)
        out.flush()
      }
      def sendCommand(command: Command){
        sendRawCommand(command.toBytes)
      }
      val outputThread = Thread.currentThread
      out.flush()
      toast("bytes written")
      @volatile var currentSpeed = 0
      val session = new Session {
        override def setSpeed(speed: Int) = {
          currentSpeed = speed
          outputThread.interrupt()
          toast("setSpeed")
        }
        override def device = bluetoothDevice
      }
      sessionPromise.success(session)
      handler(session, true)
      try{
        sendCommand(WriteMessage.string(1, s"hello"))
        val startTime = System.currentTimeMillis() + 150 // TODO: wait for reply
        while(true){
          val validUntil = startTime - System.currentTimeMillis() + 2000
          sendCommand(WriteMessage.string(1, s"$currentSpeed $validUntil"))
          try{
            Thread.sleep(1000)
          }catch{
            case e: InterruptedException => // Just start working!
          }
        }
      }catch{
        case e: IOException => reportIOException(e)
      }
      toast("startWithService task")
    }catch {
      case e: IOException =>
        toast("startWithService task tailed")
        reportIOException(e)
    }
  }

  def startWithDevice(device: BluetoothDevice)(handler: (Session, Boolean) => Unit){
    synchronized {
      started match {
        case Some(sessionFuture) =>
          implicit val ec = ExecutionContext.global
          for (session <- sessionFuture) {
            handler(session, false)
          }
        // If it fails, the service will be disconnected, so errors are also handled
        case None =>
          val sessionPromise = Promise[Session]()
          started = Some(sessionPromise.future)
          startThread{
            deviceThread(device, sessionPromise)(handler)
          }
      }
    }
  }

  /*private def start(intent: Intent){
    intent.toUri(0) match {
      case RemoteControlActivity(addr) =>
        val device = BluetoothAdapter.getDefaultAdapter.getRemoteDevice(addr)
        deviceOption = Some(device)
      case uri =>
        toast("Bad uri: "+uri)
        stopSelf()
    }
  }*/

  /*override def onStartCommand(intent: Intent, flags: Int, startId: Int) = {
    val osc = super.onStartCommand(intent, flags, startId)
    start(intent)
    Service.START_STICKY
  }*/

}