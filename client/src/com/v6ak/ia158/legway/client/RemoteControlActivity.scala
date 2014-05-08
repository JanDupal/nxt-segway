package com.v6ak.ia158.legway.client

import org.scaloid.common._
import android.bluetooth.{BluetoothDevice, BluetoothAdapter}
import scala.Some
import android.content._
import android.os.IBinder
import android.widget.SeekBar
import android.app.SearchManager.OnCancelListener
import scala.Some

object RemoteControlActivity{
  private val RegEx = """^x-vnd-legway://([0-9a-zA-Z:]+)(?:#.*)?$""".r
  def unapply(uri: String): Option[String] = uri match {
    case RegEx(addr) => Some(addr)
    case _ => None
  }
  def apply(addr: String): String = s"x-vnd-legway://$addr"
  def apply(device: BluetoothDevice): String = apply(device.getAddress)

}

class RemoteControlActivity extends SActivity{

  def build(device: BluetoothDevice){
    val dialog = spinnerDialog("Connecting...", s"Connecting to ${device.getName}\n(${device.getAddress})")
    val connection = new ServiceConnection{
      override def onServiceConnected(p1: ComponentName, binder: IBinder){
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener {
          override def onCancel(p1: DialogInterface){
            finish()
          }
        })
        dialog.setCancelable(true)
        val service = binder.asInstanceOf[LocalService#ScaloidServiceBinder].service.asInstanceOf[ConnectionService]
        service.startWithDevice(device){(session, isNew) =>
          runOnUiThread {
            if((!isNew) && (session.device != device) ){
              alert("Old session", s"You was connected to another session, the session was restored. As a result, you are connected to ${session.device.getName}.")
            }
            dialog.hide()
            contentView = new SVerticalLayout {
              val sb = SSeekBar()
              sb.minimumHeight = 50.dip
              val min = -100
              val max = 100
              sb.max = max-min
              sb.onProgressChanged { (b: SeekBar, num: Int, fromUser: Boolean) =>
                session.setSpeed(min+num)
              }
              def stop(){sb.progress = 0-min}
              stop()
              SButton("Stop", {stop()})
              SButton("Disconnect", {finish()})
            }
          }
        }
      }
      override def onServiceDisconnected(p1: ComponentName){
        dialog.hide()
        alert("Odpojeno", "Nějak nám tu uchcíplo spojení.", {finish()})
      }
    }
    val intent = new Intent(ctx, classOf[ConnectionService])
    intent.setData(RemoteControlActivity(device.getAddress))
    val res = bindService(intent, connection, Context.BIND_AUTO_CREATE)
    if(!res){
      alert("Something is fucked up!", "Whoa, bindService returned false. This is a serious problem.", () => finish())
    }
  }

  onCreate{
    theme = android.R.style.Theme_Holo
    Option(BluetoothAdapter.getDefaultAdapter) match {
      case Some(btAdapter) =>
        getIntent.toUri(0) match {
          case RemoteControlActivity(addr) =>
            val device = btAdapter.getRemoteDevice(addr)
            build(device)
          case uri =>
            alert("Error", s"Bad URI: $uri", {
              finish()
            })
        }
      case None =>
        alert("Error", "Yay, you do not seem to have Bluetooth-enabled device", {
          finish()
        })
    }


  }

}